import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft, Mail, Shield } from 'lucide-react';

const SUPPORT_EMAIL = "rupambairagya08@gmail.com";

const fadeInUp = {
  hidden: { opacity: 0, y: 30 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: "easeOut" } }
};

export default function PrivacyPolicy() {
  return (
    <div className="privacy-page">
      <motion.header className="privacy-header" variants={fadeInUp} initial="hidden" animate="visible">
        <Link to="/" className="back-link">
          <ArrowLeft size={20} /> Back to Home
        </Link>
        <div className="privacy-header-content">
          <div className="privacy-icon">
            <Shield size={40} />
          </div>
          <h1>Privacy Policy</h1>
          <p className="privacy-effective">Effective Date: June 12, 2026</p>
        </div>
      </motion.header>

      <motion.main
        className="privacy-content"
        variants={fadeInUp}
        initial="hidden"
        animate="visible"
        transition={{ delay: 0.2 }}
      >
        <div className="privacy-intro">
          <p>
            GmailMNT ("we", "our", or "us") is committed to protecting your privacy.
            This Privacy Policy explains how we collect, use, and safeguard your information
            when you use our Android application ("App").
          </p>
        </div>

        <section className="policy-section">
          <h2>1. Information We Collect</h2>
          <h3>1.1 Information You Provide</h3>
          <ul>
            <li><strong>Gmail Account Access:</strong> When you sign in with your Google account, we access your Gmail data (emails, labels, contacts) solely to display and manage your emails within the App.</li>
            <li><strong>OAuth 2.0 Tokens:</strong> We use Google's OAuth 2.0 authentication. Your Google password is never collected or stored by us.</li>
          </ul>

          <h3>1.2 Automatically Collected Information</h3>
          <ul>
            <li><strong>Device Information:</strong> Device model, OS version, and unique device identifier for compatibility and troubleshooting.</li>
            <li><strong>Usage Data:</strong> App usage patterns such as feature usage frequency (not personal email content).</li>
            <li><strong>Local Storage Data:</strong> Email metadata and cached content stored locally on your device for offline access.</li>
          </ul>
        </section>

        <section className="policy-section">
          <h2>2. How We Use Your Information</h2>
          <ul>
            <li>To display, organize, and manage your Gmail emails within the App</li>
            <li>To enable sending emails from the App through Gmail's API</li>
            <li>To provide features like smart categories, search, and offline access</li>
            <li>To sync new emails efficiently using differential sync</li>
            <li>To improve app performance and fix bugs</li>
          </ul>
        </section>

        <section className="policy-section">
          <h2>3. Data Storage & Security</h2>
          <ul>
            <li><strong>Local Storage:</strong> Email data is stored locally on your device using Room Database. This data is not transmitted to any third-party servers.</li>
            <li><strong>No Server Storage:</strong> We do not operate any servers that store your email content. All email data flows directly between your device and Google's Gmail API.</li>
            <li><strong>Biometric Security:</strong> The App supports biometric authentication (fingerprint/face) to protect access to your emails on the device.</li>
            <li><strong>Encryption:</strong> All communication with Google APIs uses HTTPS/TLS encryption.</li>
            <li><strong>Attachments:</strong> Downloaded attachments are stored in the app's cache directory, which is automatically managed by the Android OS.</li>
          </ul>
        </section>

        <section className="policy-section">
          <h2>4. Google API Services</h2>
          <p>
            The App uses Google's Gmail API to read, send, and manage your emails.
            Your use of Google services through the App is also governed by Google's Privacy Policy.
          </p>
          <ul>
            <li>We request only the minimum required Gmail API scopes</li>
            <li>We do not sell or share your Google data with third parties</li>
            <li>We comply with the Google API Services User Data Policy</li>
            <li>You can revoke access at any time from your Google Account settings</li>
          </ul>
        </section>

        <section className="policy-section">
          <h2>5. Third-Party Services</h2>
          <p>
            The App may use the following third-party services:
          </p>
          <ul>
            <li><strong>Google Gmail API:</strong> For email access and management</li>
            <li><strong>Google OAuth 2.0:</strong> For secure authentication</li>
            <li><strong>Gemini AI (optional):</strong> For AI-powered email features, if enabled</li>
          </ul>
          <p>
            We do not integrate any advertising SDKs or analytics trackers that collect personal data.
          </p>
        </section>

        <section className="policy-section">
          <h2>6. Data Sharing & Disclosure</h2>
          <p>We do not sell, trade, or otherwise transfer your personal information to third parties. We may disclose information only in the following circumstances:</p>
          <ul>
            <li>When required by law or legal process</li>
            <li>To protect our rights or prevent fraud</li>
            <li>With your explicit consent</li>
          </ul>
        </section>

        <section className="policy-section">
          <h2>7. Your Rights & Choices</h2>
          <ul>
            <li><strong>Access & Control:</strong> You can view, modify, or delete your email data within the App at any time.</li>
            <li><strong>Revoke Access:</strong> You can disconnect your Gmail account from the App at any time through the App settings or Google Account settings.</li>
            <li><strong>Data Deletion:</strong> Uninstalling the App will remove all locally stored data from your device.</li>
            <li><strong>Biometric Lock:</strong> You can enable or disable biometric authentication from the App settings.</li>
          </ul>
        </section>

        <section className="policy-section">
          <h2>8. Children's Privacy</h2>
          <p>
            The App is not intended for use by children under the age of 13.
            We do not knowingly collect personal information from children under 13.
            If we become aware that a child under 13 has provided us with personal information,
            we will take steps to delete such information.
          </p>
        </section>

        <section className="policy-section">
          <h2>9. Changes to This Policy</h2>
          <p>
            We may update this Privacy Policy from time to time. We will notify you of any changes
            by posting the new Privacy Policy on this page and updating the "Effective Date" at the top.
            You are advised to review this Privacy Policy periodically for any changes.
          </p>
        </section>

        <section className="policy-section">
          <h2>10. Contact Us</h2>
          <p>
            If you have any questions, concerns, or requests regarding this Privacy Policy
            or our data practices, please contact us at:
          </p>
          <div className="contact-card">
            <Mail size={20} />
            <div>
              <strong>Email</strong>
              <a href={`mailto:${SUPPORT_EMAIL}`}>{SUPPORT_EMAIL}</a>
            </div>
          </div>
        </section>

        <div className="policy-footer-note">
          <p>
            By using GmailMNT, you agree to the collection and use of information
            in accordance with this Privacy Policy.
          </p>
        </div>
      </motion.main>
    </div>
  );
}
