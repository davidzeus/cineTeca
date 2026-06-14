package com.example.cineteca.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.example.cineteca.data.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume

class GoogleDriveManager(private val context: Context) {

    companion object {
        const val RC_SIGN_IN = 9001
        private const val BACKUP_FILENAME = "cineteca_backup.json"
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }

    private val signInClient: GoogleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
            .build()
    )

    fun getSignInIntent(): Intent = signInClient.signInIntent

    fun getCurrentAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    fun isSignedIn(): Boolean {
        val account = getCurrentAccount() ?: return false
        return GoogleSignIn.hasPermissions(account, Scope(DRIVE_APPDATA_SCOPE))
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            signInClient.signOut().addOnCompleteListener { cont.resume(Unit) }
        }
    }

    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val account = getCurrentAccount() ?: return@withContext null
            GoogleAuthUtil.getToken(context, account.account!!, "oauth2:$DRIVE_APPDATA_SCOPE")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun backupToDrive(movies: List<Movie>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
                ?: return@withContext Result.failure(Exception("Sesión expirada, vuelve a iniciar sesión"))
            val json = buildBackupJson(movies)
            val existingId = findBackupFile(token)
            if (existingId != null) updateFile(token, existingId, json)
            else createFile(token, json)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromDrive(): Result<List<Movie>> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
                ?: return@withContext Result.failure(Exception("Sesión expirada"))
            val fileId = findBackupFile(token)
                ?: return@withContext Result.failure(Exception("No se encontró ningún backup en Drive"))
            val content = downloadFile(token, fileId)
            Result.success(parseMoviesFromJson(content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildBackupJson(movies: List<Movie>): String {
        val arr = JSONArray()
        movies.forEach { m ->
            arr.put(JSONObject().apply {
                put("title", m.title)
                put("url", m.url ?: "")
                put("addedAt", m.addedAt)
                put("isWatched", m.isWatched)
            })
        }
        return JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("movies", arr)
        }.toString()
    }

    private fun parseMoviesFromJson(content: String): List<Movie> {
        val arr = JSONObject(content).getJSONArray("movies")
        return (0 until arr.length()).map { i ->
            arr.getJSONObject(i).let { o ->
                Movie(
                    title = o.getString("title"),
                    url = o.getString("url").takeIf { it.isNotEmpty() },
                    addedAt = o.getLong("addedAt"),
                    isWatched = o.getBoolean("isWatched")
                )
            }
        }
    }

    private fun findBackupFile(token: String): String? {
        val query = URLEncoder.encode("name='$BACKUP_FILENAME'", "UTF-8")
        val url = URL("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$query&fields=files(id)")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
        }
        if (conn.responseCode != 200) return null
        val files = JSONObject(conn.inputStream.bufferedReader().readText()).getJSONArray("files")
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    private fun createFile(token: String, content: String) {
        val boundary = "cineteca_mp_boundary"
        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            doOutput = true
        }
        val metadata = """{"name":"$BACKUP_FILENAME","parents":["appDataFolder"]}"""
        val body = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n" +
                "--$boundary\r\nContent-Type: application/json\r\n\r\n$content\r\n--$boundary--"
        conn.outputStream.write(body.toByteArray())
        conn.responseCode
    }

    private fun updateFile(token: String, fileId: String, content: String) {
        val url = URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }
        conn.outputStream.write(content.toByteArray())
        conn.responseCode
    }

    private fun downloadFile(token: String, fileId: String): String {
        val url = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
        }
        return conn.inputStream.bufferedReader().readText()
    }
}
