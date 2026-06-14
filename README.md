# CineTeca 🎬

> Tu lista personal de películas y series, guardada directamente desde otras apps.

CineTeca es una app Android minimalista para guardar lo que querés ver después. Compartís un enlace desde Instagram, YouTube, Netflix u cualquier otra app — y CineTeca lo guarda automáticamente con miniatura, descripción y badge de plataforma.

---

## Capturas

| Lista principal | Card con miniatura |
|---|---|
| Badges de plataforma, chips de filtro y swipe gestures | Miniatura automática, descripción y chip de estado |

---

## Funcionalidades

- **Guardar desde cualquier app** — usá el botón "Compartir" de Instagram, YouTube, Netflix, Letterboxd, etc.
- **Miniaturas automáticas** — sin API keys, sin registro:
  - YouTube → thumbnail directa vía `img.youtube.com`
  - Cualquier web → Open Graph scraping (`og:image`)
  - Fallback → Wikipedia REST API
- **Detección de plataforma** — badge con color identificador para YouTube, Netflix, Instagram, Disney+, Max, Prime Video, MUBI, Letterboxd, IMDb, Filmaffinity y más
- **Descripción automática** — extraída del Open Graph o Wikipedia
- **Filtros** — Todas / Por ver / Vistas
- **Swipe gestures** — deslizá a la derecha para marcar como vista, a la izquierda para eliminar
- **Edición de título** — tocá el ícono de lápiz en cualquier card
- **Base de datos local** — Room/SQLite, todo queda en el dispositivo sin depender de ningún servidor
- **Material Design 3** — tema oscuro con color dinámico en Android 12+

### Próximamente
- [ ] Sincronización con Google Drive (código preparado en `auth/GoogleDriveManager.kt`)
- [ ] Login con Google

---

## Stack técnico

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Base de datos | Room 2.6.1 (SQLite) |
| Imágenes | Coil 2.5.0 |
| Scraping | Jsoup 1.17.2 |
| Async | Kotlin Coroutines |
| Mínimo Android | API 34 (Android 14) |
| Kotlin | 1.9.20 |

---

## Cómo funciona el fetch de metadata

Cuando compartís una URL, la app ejecuta esta cadena hasta encontrar información:

```
URL compartida
    │
    ├─ ¿Es YouTube? ──► thumbnail directa (img.youtube.com/vi/{id}/hqdefault.jpg)
    │                    + título via oEmbed (youtube.com/oembed) ── sin API key
    │
    ├─ Cualquier URL ──► Open Graph scraping con Jsoup
    │                    og:image · og:title · og:description
    │
    └─ Sin imagen aún ──► Wikipedia REST API (en.wikipedia.org/api/rest_v1)
                          Busca el título y trae el thumbnail del artículo
```

Instagram: intenta Open Graph con user-agent de Safari móvil. Funciona para posts públicos; los privados o bloqueados se guardan sin imagen pero con badge "Instagram".

---

## Compilar el proyecto

### Requisitos
- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- Android SDK API 34+

### Pasos
```bash
git clone https://github.com/davidzeus/cineTeca.git
cd cineTeca
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

### Descargar APK directamente
Los APKs de cada build están disponibles como artifacts en [GitHub Actions](../../actions).

---

## Configurar sincronización con Google Drive (opcional)

> Esta feature está implementada pero desactivada. Para activarla necesitás configurar OAuth.

1. Creá un proyecto en [Google Cloud Console](https://console.cloud.google.com)
2. Habilitá **Google Drive API**
3. Configurá la pantalla de consentimiento OAuth (tipo Externo, scope `drive.appdata`)
4. Creá credenciales → **Android** → package `com.example.cineteca` + SHA-1 de tu keystore
5. En `MainActivity.kt`, descomentá el flujo de auth (ver `auth/GoogleDriveManager.kt` y `ui/LoginScreen.kt`)

SHA-1 del keystore de debug:
```bash
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android
```

---

## Estructura del proyecto

```
app/src/main/java/com/example/cineteca/
├── MainActivity.kt              # UI principal, lista de películas
├── ShareReceiverActivity.kt     # Recibe intents de compartir
├── auth/
│   └── GoogleDriveManager.kt   # Google Sign-In + backup a Drive (preparado)
├── data/
│   ├── Movie.kt                 # Entidad Room
│   ├── MovieDao.kt              # Queries
│   └── AppDatabase.kt           # Configuración DB + migraciones
├── ui/
│   ├── LoginScreen.kt           # Pantalla de login con Google (preparada)
│   └── theme/
│       ├── Theme.kt             # Material You + tema oscuro
│       └── Color.kt             # Paleta de colores
└── utils/
    ├── MetadataFetcher.kt       # Cadena de fetch: YouTube → OG → Wikipedia
    └── PlatformDetector.kt      # URL → plataforma + color
```

---

## Plataformas soportadas para detección

| Plataforma | Color |
|---|---|
| YouTube | Rojo |
| Netflix | Rojo Netflix |
| Max (HBO) | Azul |
| Disney+ | Azul oscuro |
| Prime Video | Celeste |
| MUBI | Gris oscuro |
| Letterboxd | Verde |
| IMDb | Amarillo |
| Filmaffinity | Rojo oscuro |
| Instagram | Rosa/fucsia |
| TikTok | Negro |
| Vimeo | Celeste |
| Apple TV+ | Negro |
| Paramount+ | Azul |

---

## Licencia

MIT — hacé lo que quieras con el código.
