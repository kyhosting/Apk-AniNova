# AniNova Android App — Cara Build APK

## Prasyarat
- **Android Studio** (versi terbaru, Koala atau lebih baru)
- **JDK 17**
- **Android SDK** (API 26+)

---

## Langkah Build

### 1. Atur Base URL API
Buka file `app/build.gradle.kts` dan ganti URL dengan URL Replit kamu:

```kotlin
buildConfigField("String", "BASE_URL", "\"https://namaproject.namauser.replit.app/\"")
```

> Cara dapat URL: Deploy project Flask kamu di Replit terlebih dahulu.

### 2. Buka Project di Android Studio
1. Buka **Android Studio**
2. Pilih **Open** → pilih folder `android/` dari project ini
3. Tunggu Gradle sync selesai (bisa 5-10 menit pertama kali)

### 3. Build APK Debug (untuk testing)
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```
APK tersedia di: `app/build/outputs/apk/debug/app-debug.apk`

### 4. Build APK Release (untuk distribusi)
1. **Build → Generate Signed Bundle / APK**
2. Pilih **APK**
3. Buat atau pilih **keystore** (untuk signing)
4. Pilih build variant **release**
5. Klik **Finish**

APK tersedia di: `app/build/outputs/apk/release/app-release.apk`

---

## Struktur Project

```
android/
├── app/src/main/java/com/aninova/app/
│   ├── AniNovaApp.kt          ← Application class (Hilt)
│   ├── MainActivity.kt         ← Entry point
│   ├── data/
│   │   ├── api/               ← Retrofit API service
│   │   ├── model/             ← Data models (Anime, Episode, User, dll)
│   │   ├── local/             ← DataStore (simpan token JWT)
│   │   └── repository/        ← Repository layer
│   ├── di/                    ← Hilt dependency injection
│   └── ui/
│       ├── theme/             ← Dark theme (merah-hitam)
│       ├── navigation/        ← NavGraph & routes
│       ├── components/        ← AnimeCard, BottomNavBar, dll
│       ├── screens/           ← Semua halaman UI
│       └── viewmodel/         ← ViewModel per screen
```

## Fitur Android App
- Beranda dengan section anime (Latest, Popular, dll)
- Search anime real-time
- Detail anime (synopsis, episodes, genres, likes, komentar)
- Video player built-in (ExoPlayer) + multi-server
- Navigasi prev/next episode
- Daftar (Register) dengan OTP email
- Login dengan JWT
- Watchlist & History
- Profil user

## Tech Stack
| Library | Fungsi |
|---|---|
| Jetpack Compose | UI modern |
| Hilt | Dependency injection |
| Retrofit | HTTP client ke Flask API |
| OkHttp | Networking layer |
| Coil | Load gambar/thumbnail |
| ExoPlayer (Media3) | Video player |
| DataStore | Simpan token JWT lokal |
| Navigation Compose | Navigasi antar screen |
