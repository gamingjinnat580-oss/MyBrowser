# Orbit Browser — Beginner Setup Guide

## 1. What Is Already Provisioned in AI Studio
- **Firebase Project**: `gen-lang-client-0624648874`
- **Firestore Enterprise Database**: `ai-studio-android-omnibrow-325a36b6-1b5b-4b4e-8aa2-abfece42b8a2`
- **Google Sign-In + Firebase AI Logic (`gemini-3.8-flash`) + App Check**: Pre-wired via `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml`.
- **Hardened Firestore Security Rules**: Already tested and deployed from `firestore.rules`.

## 2. Setting Up Your Own Independent Firebase Project (Optional / Export)
If you export this project to GitHub and want to connect it to your own Firebase Console project:

1. Go to [Firebase Console](https://console.firebase.google.com/) and click **Create a project**.
2. Click **Add app -> Android**:
   - Package name: `com.aistudio.orbitbrowser.kxmpzq`
   - Generate SHA-1 / SHA-256 from your keystore (`keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android`) and paste both into Firebase.
3. Download `google-services.json` and replace `app/google-services.json`.
4. In Firebase Console:
   - **Authentication -> Sign-in method**: Enable **Google** (and optionally **Email/Password** or **Phone** if you want to extend auth beyond AI Studio's Google Sign-In).
   - **Firestore Database**: Create database and deploy `firestore.rules`.
   - **Firebase AI Logic**: Enable Gemini Developer API & register App Check (Play Integrity / reCAPTCHA Enterprise for release).
   - **Optional Extensions**: Enable **Firebase Storage** (for multi-device cloud media blobs) and **Cloud Messaging (FCM)** (for background push notifications).

## 3. Building the APK from Your Phone Using GitHub Actions
1. Push this repository to GitHub (via AI Studio's **Push to GitHub** menu).
2. On your phone, open your GitHub repo -> **Actions** tab -> **Build Orbit Browser APK**.
3. Tap **Run workflow** (or push a commit to `main`).
4. Once finished (~3 minutes), scroll down to **Artifacts** and tap **orbit-browser-debug-apk** to download and install the APK on your Android phone.
5. *(Tip)*: If you don't commit `app/google-services.json` to a public repo, base64-encode it and save it under **GitHub Repo -> Settings -> Secrets and variables -> Actions -> `GOOGLE_SERVICES_JSON_BASE64`**.
