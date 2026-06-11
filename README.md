# GmailMNT — Secure AI-Powered Gmail Client

GmailMNT is a secure, modern Android email client featuring a premium, dark-mode design system. It is integrated with offline queueing support, incremental synchronization, strictly enforced local biometric security, and Gemini AI smart email composition.

---

## 🚀 Key Features

### 1. Incremental Sync (Gmail History API)
* **High Efficiency:** Utilizes `users.history.list` to retrieve only incremental updates (new messages, deleted messages, starred/read status modifications) since the last sync.
* **Quota Preservation:** Greatly reduces API usage by avoiding full inbox refreshes.
* **Graceful Fallbacks:** Automatically triggers a full mailbox scan if the stored history ID has expired on Google's servers.

### 2. Offline Outbox Queue (WorkManager)
* **Zero Trust Offline Sending:** Allows users to compose and "send" emails even without an active internet connection.
* **Room DB Storage:** Offline emails are saved locally in a secure Room database outbox queue.
* **Auto-Resend:** A background `SendEmailWorker` enqueued via Android `WorkManager` with a `CONNECTED` network constraint automatically pushes queued emails once connection is restored.

### 3. Strictly Enforced Biometric Security
* **Default Settings:** Biometric lock is **OFF by default** on fresh installations, allowing immediate entry for new users.
* **Manual Setup:** Users can toggle biometric security ON or OFF in the application settings page.
* **Hard-Lock Launch Flow:** When enabled, the app enforces a biometric prompt on startup. Cancelling or failing the prompt will not allow access to the dashboard. The app displays an elegant **"Unlock App"** retry button to re-trigger authentication.

### 4. Gemini AI Email Composer
* **Smart Generation:** Enter custom prompts to generate formal or informal email replies and drafts.
* **Cascading Model Fallbacks:** Automatically falls back through multiple Gemini models (e.g., trying Gemini 2.5 Flash first, then subsequent versions) to ensure robust API availability and prevent error screens on service throttling (HTTP 503).

---

## ⚠️ Known Limitations & Design Rules

### 1. Google Privacy Policy (Sender Avatars)
* **Avatar Restrictions:** Due to Google’s privacy regulations, profile pictures of arbitrary Google accounts cannot be fetched publicly.
* **Mitigations:** The app uses a secure hierarchy:
  1. Corporate domains show official logos fetched via Clearbit API (e.g., `@github.com`, `@groww.in`).
  2. Registered Gravatar pictures are retrieved if available.
  3. Displays customized initial letters styled dynamically with a colored circle.

### 2. Live Synchronization Latency
* **Background Sync limits:** Out-of-the-box sync utilizes `WorkManager` periodic requests running every 15 minutes (as per Android OS guidelines).
* **Instant Sync Requirements:** Real-time push notifications require configuring Google Cloud Pub/Sub `watch()` endpoints alongside Firebase Cloud Messaging (FCM) to deliver silent wake-up events.

### 3. Backend Proxy Hosting (Free Tier)
* **Spin-up Latency:** The optional helper server used for OAuth and backend proxy runs on Render's free tier. 
* **Behavior:** If inactive, the backend container spins down and may take up to 30–50 seconds to boot on the first API request.

---

## 🛠️ Build & Local Setup

### Prerequisites
* **Android Studio** (Koala or later recommended)
* Android SDK Platform 34+
* JDK 17+

### Environment Configuration
1. Open the project in Android Studio.
2. Create a `.env` file in the project root directory.
3. Add your developer credentials securely:
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   RENDER_BACKEND_URL=https://your-custom-backend.onrender.com
   ```
4. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🔒 Security Policy
GmailMNT stores access tokens and cached email content locally using SQLite/Room. No sensitive user credentials, access tokens, or draft emails are uploaded to any external server other than direct Google APIs and the user's selected Gemini AI endpoint.
