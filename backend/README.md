# Gemini Mail Backend Deployment Guide

This backend service facilitates Google OAuth with the Gmail API, provides a secure stateless OAuth refresh proxy, and hosts the public Privacy Policy.

## Prerequisites

1. **Google Cloud Console Project**: Set up a project to access Google APIs.
2. **Render Account**: A free account on [Render](https://render.com) to host this Express server.

---

## Part 1: Google Cloud Console Configuration

To link your server and Android application to the Gmail API, configure the Google Cloud OAuth Consent Screen:

1. **Create/Open Project**: Go to the [Google Cloud Console](https://console.cloud.google.com/) and create/select your project.
2. **Enable APIs**:
   - Search for **Gmail API** and click **Enable**.
   - Search for **Google People API** or **Google OAuth2 API** and make sure they are enabled.
3. **Configure OAuth Consent Screen**:
   - Go to **APIs & Services > OAuth Consent screen**.
   - Choose **External** (unless you are a Google Workspace Enterprise user).
   - **App name**: `Gemini Mail`
   - **User support email**: `your-email@gmail.com`
   - **Developer contact information**: `your-email@gmail.com`
   - Under **Scopes**: Add the following scopes:
     - `.../auth/userinfo.profile` (Profile info)
     - `.../auth/userinfo.email` (Email address)
     - `.../auth/gmail.readonly` (Read access to emails)
     - `.../auth/gmail.send` (Send emails on user's behalf)
     - `.../auth/gmail.modify` (Archive/delete/modify emails)
   - **Privacy Policy URL**: Enter your deployed Render endpoint, e.g., `https://your-backend.onrender.com/privacy-policy`.
4. **Create Credentials**:
   - Go to **APIs & Services > Credentials**.
   - Click **Create Credentials > OAuth client ID**.
   - **Application Type**: select **Web Application** (since this server handles the OAuth redirect).
   - Under **Authorized redirect URIs**, add:
     - `http://localhost:3000/callback` (for local development testing)
     - `https://your-backend.onrender.com/callback` (replace with your Render URL after deployment).
   - Click **Create**. Copy your **Client ID** and **Client Secret**.

---

## Part 2: Deploying to Render

1. **Push to GitHub**: Push this `/backend` subdirectory to a GitHub repository.
2. **Create Render Web Service**:
   - Log in to [Render Console](https://dashboard.render.com).
   - Click **New > Web Service**.
   - Connect your GitHub repository containing the backend code. (Specify Root Directory as `backend` if inside a subdirectory).
   - Select **Node** runtime.
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
3. **Configure Environment Variables**: In the **Environment** tab, add:
   - `GOOGLE_CLIENT_ID` = `[Your Google Client ID]`
   - `GOOGLE_CLIENT_SECRET` = `[Your Google Client Secret]`
   - `GOOGLE_REDIRECT_URI` = `https://[your-app-subdomain].onrender.com/callback`
   - `PORT` = `3000` (Optional, Render maps ports automatically)
4. **Deploy**: Click **Deploy Web Service**. Once running, copy your deployment URL (e.g. `https://your-app.onrender.com`).
5. **Update Google Developers Console**: Go back to your Client ID credentials, and replace `https://your-backend.onrender.com/callback` with your actual Render URL callback!

---

## Part 3: Testing Endpoints

Once deployed, verify:
- **Index**: `https://your-backend.onrender.com/` (Should print Gemini Mail Backend Service is running...)
- **Privacy Policy**: `https://your-backend.onrender.com/privacy-policy` (Hosts the official policy document for app review verification).
- **Authenticate**: `https://your-backend.onrender.com/auth` will kick off Gmail authentication.
