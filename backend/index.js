const express = require('express');
const cors = require('cors');
const { google } = require('googleapis');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Load credentials from environment
const CLIENT_ID = process.env.GOOGLE_CLIENT_ID;
const CLIENT_SECRET = process.env.GOOGLE_CLIENT_SECRET;
const REDIRECT_URI = process.env.GOOGLE_REDIRECT_URI || `http://localhost:${PORT}/callback`;

const oauth2Client = new google.auth.OAuth2(
  CLIENT_ID,
  CLIENT_SECRET,
  REDIRECT_URI
);

// Scopes required for the application
const SCOPES = [
  'https://www.googleapis.com/auth/userinfo.profile',
  'https://www.googleapis.com/auth/userinfo.email',
  'https://www.googleapis.com/auth/gmail.readonly',
  'https://www.googleapis.com/auth/gmail.send',
  'https://www.googleapis.com/auth/gmail.modify'
];

/**
 * 1. GET /privacy-policy
 * Public page containing the privacy policy. Submitted during Google OAuth Verification.
 */
app.get('/privacy-policy', (req, res) => {
  res.setHeader('Content-Type', 'text/html');
  res.send(`
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Gemini Mail - Privacy Policy</title>
        <style>
            body {
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                line-height: 1.6;
                color: #1e293b;
                max-width: 800px;
                margin: 40px auto;
                padding: 0 20px;
                background-color: #f8fafc;
            }
            .container {
                background: white;
                padding: 40px;
                border-radius: 12px;
                box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
            }
            h1 { color: #0f172a; border-bottom: 2px solid #e2e8f0; padding-bottom: 15px; }
            h2 { color: #1e293b; margin-top: 30px; }
            p { margin-bottom: 15px; }
            ul { margin-bottom: 15px; padding-left: 20px; }
            .footer { margin-top: 50px; text-align: center; font-size: 0.875rem; color: #64748b; }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>Privacy Policy for Gemini Mail</h1>
            <p><strong>Effective Date: June 9, 2026</strong></p>
            <p>Gemini Mail ("we," "our," or "us") is dedicated to protecting your privacy. This Privacy Policy describes how we handle information in connection with the Gemini Mail Android application and its associated backend service.</p>
            
            <h2>1. Information We Collect and Access</h2>
            <p>Gemini Mail accesses your Google Mail (Gmail) accounts strictly to enable the core functionalities of fetching, composing, reading, and deleting emails on your behalf as the user. The specific scopes accessed include:</p>
            <ul>
                <li><strong>gmail.readonly:</strong> To query and sync your inbox locally on your device for offline reading.</li>
                <li><strong>gmail.send:</strong> To compose and send emails directly from your verified email address.</li>
                <li><strong>gmail.modify:</strong> To flag, archive, or delete emails upon your command.</li>
                <li><strong>userinfo.profile and userinfo.email:</strong> To identify your active account and retrieve your display name.</li>
            </ul>

            <h2>2. No Permanent Session Storage on Our Backend</h2>
            <p>Your privacy and data security are paramount to us. Therefore, we do not store your emails, personal identity files, access tokens, or refresh tokens on our servers or backends indefinately.</p>
            <ul>
                <li><strong>Local Device Storage:</strong> All access tokens, refresh tokens, and synchronized email caches are stored locally on your Android device in a secure sandbox using Android ROOM database and secure device keychains.</li>
                <li><strong>Stateless Backend Proxy:</strong> Our backend on Render serves solely as an authorization proxy to initiate OAuth handshake with Google and process token refreshes. Once your tokens are returned to your device, no copies are preserved on our servers.</li>
            </ul>

            <h2>3. Local Processing and Gemini AI</h2>
            <p>Gemini Mail allows you to optionally use the Google Gemini API to assist with email drafting, replies, and summaries. This processing happens by making secure API requests using the Gemini API key you provide within the application settings. No data from these sessions is stored by us or utilized for model training.</p>

            <h2>4. Security</h2>
            <p>We implement a range of security measures to protect your local data, including support for biometric authentication (such as fingerprint and face unlock) on your Android device to prevent unauthorized access.</p>

            <h2>5. Changes to This Policy</h2>
            <p>We may update this Privacy Policy from time to time. We will notify you of any material changes by posting the new policy on this page with an updated effective date.</p>

            <h2>6. Contact Us</h2>
            <p>If you have any questions or feedback regarding this Privacy Policy, please contact us at: <a href="mailto:rupambairagya08@gmail.com">rupambairagya08@gmail.com</a>.</p>

            <div class="footer">
                <p>&copy; 2026 Gemini Mail. All rights reserved.</p>
            </div>
        </div>
    </body>
    </html>
  `);
});

/**
 * 2. GET /auth
 * Redirects user to Google OAuth Concent Screen
 */
app.get('/auth', (req, res) => {
  const accountId = req.query.accountId || 'default'; // handle multiple accounts linkage
  const authUrl = oauth2Client.generateAuthUrl({
    access_type: 'offline',
    scope: SCOPES,
    prompt: 'consent', // ensures refresh token is returned
    state: accountId
  });
  res.redirect(authUrl);
});

/**
 * 3. GET /callback
 * Google OAuth Callback URL. Exchanges auth code for tokens and delivers to user.
 */
app.get('/callback', async (req, res) => {
  const { code, state } = req.query;
  
  if (!code) {
    return res.status(400).send('Authorization code is missing.');
  }

  try {
    const { tokens } = await oauth2Client.getToken(code);
    
    // Express returns the authentication completion page. We deep link back to Android
    // using a custom scheme (geminimail://)
    const email = await getUserEmail(tokens.access_token);
    
    // Construct Android redirections deep link
    // geminimail://oauth-callback?email=...&access_token=...&refresh_token=...&expires_at=...
    const redirectData = {
      email: email,
      access_token: tokens.access_token,
      refresh_token: tokens.refresh_token,
      expires_at: Date.now() + (tokens.expiry_date || 3600 * 1000)
    };

    const schemeUrl = `geminimail://oauth-callback?email=${encodeURIComponent(redirectData.email)}` + 
                      `&access_token=${encodeURIComponent(redirectData.access_token)}` + 
                      `&refresh_token=${encodeURIComponent(redirectData.refresh_token || '')}` + 
                      `&expires_at=${redirectData.expires_at}`;

    res.setHeader('Content-Type', 'text/html');
    res.send(`
      <!DOCTYPE html>
      <html>
      <head>
        <title>Authentication Successful</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          body { font-family: system-ui; text-align: center; padding: 40px; background: #eef2f6; }
          .card { background: white; padding: 30px; border-radius: 12px; display: inline-block; box-shadow: 0 4px 6px -1px rgb(0 0 0/0.1); max-width: 400px; }
          .btn { background: #10b981; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: bold; display: inline-block; margin-top: 20px; }
        </style>
      </head>
      <body>
        <div class="card">
          <h2>Authentication Successful!</h2>
          <p>Logged in successfully as <strong>${email}</strong>.</p>
          <p>Please click the button below to return to the Gemini Mail app and complete registration.</p>
          <a class="btn" href="${schemeUrl}">Return to Gemini Mail</a>
        </div>
        <script>
          // Automatic redirection to deep link
          setTimeout(function() {
            window.location.href = "${schemeUrl}";
          }, 1500);
        </script>
      </body>
      </html>
    `);
  } catch (err) {
    console.error('Error during authorization:', err);
    res.status(500).send(`Authentication failed: ${err.message}`);
  }
});

/**
 * 4. POST /refresh
 * Token refreshing endpoint. Receives user's refreshToken, asks Google, returns new tokens.
 * Secure because tokens are NOT saved on Render database, ensuring zero-trust backend.
 */
app.post('/refresh', async (req, res) => {
  const { refresh_token } = req.body;
  if (!refresh_token) {
    return res.status(400).json({ error: 'Refresh token is required.' });
  }

  try {
    const client = new google.auth.OAuth2(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);
    client.setCredentials({ refresh_token });
    
    const tokenResponse = await client.getAccessToken();
    const credentials = tokenResponse.res.data;

    res.json({
      access_token: credentials.access_token,
      expires_at: Date.now() + (credentials.expires_in * 1000)
    });
  } catch (err) {
    console.error('Error refreshing token:', err);
    res.status(500).json({ error: `Refresh failed: ${err.message}` });
  }
});

/**
 * Helper: Query user profile info to get email address
 */
async function getUserEmail(accessToken) {
  const tempClient = new google.auth.OAuth2();
  tempClient.setCredentials({ access_token: accessToken });
  const oauth2 = google.oauth2({ version: 'v2', auth: tempClient });
  const userInfo = await oauth2.userinfo.get();
  return userInfo.data.email;
}

// Default layout greeting
app.get('/', (req, res) => {
  res.send('Gemini Mail Backend Service is running securely on Render. Use /privacy-policy for verification.');
});

app.listen(PORT, () => {
  console.log(`Backend secure server listening on port ${PORT}`);
});
