# Orbit Browser — Testing Checklist & Known Limitations

## Feature Testing Checklist

### 1. Browser Engine
- [ ] **Start Page**: Shows hero banner, smart instant-filter search box, 13 categories, and 114 international sites.
- [ ] **OmniBar & Search Engines**: Enter a search term and switch default engines (Google, Bing, Yahoo, DuckDuckGo, Yandex, Baidu, Brave, Wikipedia, YouTube).
- [ ] **In-App Navigation**: Tap links, `target="_blank"` popups, and redirects; confirm links open inside Orbit Browser tabs and never launch external browsers.
- [ ] **Special Schemes**: Test `tel:`, `mailto:`, and `intent:` links to confirm safe intent handoff.
- [ ] **Tabs & Incognito**: Create normal tabs and Incognito tabs, switch between them, close tabs, and verify Incognito visits do not appear in History.
- [ ] **Page Tools**: Pull-to-refresh, Back/Forward, Find in Page (next/prev), Desktop Site toggle, Web Dark Mode toggle, Print Page, Clear Browsing Data.
- [ ] **Downloads**: Trigger a file/image download via WebView and view progress/status in the Downloads sheet.
- [ ] **Media, Uploads & Dialogs**: Test fullscreen video playback, JS `alert`/`confirm`/`prompt` dialogs, file chooser upload, and Camera/Mic/Location runtime permission prompts.
- [ ] **Long-Press Menu**: Long-press a link or image -> Open in New Tab, Open in Incognito, Copy URL, Download, or Share to Orbit Chat.
- [ ] **Error & SSL Handling**: Disconnect internet -> verify friendly error page with Retry & Start Page buttons.

### 2. Orbit AI Assistant (`gemini-3.8-flash`)
- [ ] **Multilingual Q&A**: Ask factual questions in Bengali and English; verify streaming responses appear quickly and accurately.
- [ ] **Page Context**: Open a webpage, switch to Orbit AI, toggle "Page context", and ask a question about that webpage.
- [ ] **Share AI Answer**: Tap Share on an AI answer and send it to an Orbit Chat.

### 3. Accounts & Security
- [ ] **Google Sign-In**: Launch cold -> sign in via Jetpack Credential Manager (`GetSignInWithGoogleOption`).
- [ ] **Profile Sync & Edit**: Edit display name, username, bio, status, phone, and avatar URL; verify persistence across restarts.
- [ ] **Sign Out & Delete**: Test Sign Out (returns cleanly to AuthScreen without permission errors) and Delete Account confirmation.

### 4. Communication (Chat & Calls)
- [ ] **Contacts Search**: Search public users by username or display name.
- [ ] **1-to-1 & Group Chat**: Start a direct chat and a multi-user group chat; send text, emojis, photos (Photo Picker), voice notes (Mic), files, and In-App SMS tags.
- [ ] **Web Share**: Share a webpage or image from Browser directly into a chat and tap the card in chat to open it back inside Orbit Browser.
- [ ] **Read Receipts**: Open chat on recipient account and verify delivered (`✓`) updates to seen (`✓✓`).
- [ ] **Voice, Video & Group Calls**: Start a 1-to-1 or group call (up to 8 participants); test incoming call banner Accept/Decline, CameraX preview, Mute, Camera Flip, Speakerphone, Screen Share toggle, and Call History.

## Known Limitations
1. **Auth Provider Scope**: In AI Studio's provisioned Firebase environment, Google Sign-In (`CredentialManager`) is configured as the exclusive sign-in provider. Email/Password and Phone OTP auth require enabling those providers in your own Firebase Console.
2. **Real-Time Audio/Video Media Transport**: Call rooms, participant tracking (up to 8 people), CameraX local video preview, `AudioManager` speaker/mute controls, and Firestore SDP signaling are live; full peer-to-peer WebRTC media relay across NATs requires adding a WebRTC native library + TURN/STUN server in production.
3. **Media/Voice Cloud Blobs & Background Push**: Firebase Storage and FCM push notifications are not provisioned automatically in this environment; photos/voice notes currently share local/web URIs and incoming calls/messages notify in real time while the app is active.
4. **Carrier SMS**: To stay compliant with Google Play permissions policy, sending carrier SMS delegates cleanly to the device's default SMS composer (`smsto:` intent) instead of requesting restricted `SEND_SMS` permissions.
