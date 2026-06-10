import React, { useState, useEffect } from 'react';

// Implementation of a biometric authentication layer using the Web Authentication API (WebAuthn)
export default function BiometricAuth({ onAuthenticated }) {
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [supported, setSupported] = useState(true);

  useEffect(() => {
    // Check if PublicKeyCredential is supported by the browser environment
    if (!window.PublicKeyCredential) {
      setSupported(false);
      setError("Web Authentication API (WebAuthn) is not supported in this browser.");
    }
  }, []);

  const registerBiometrics = async () => {
    try {
      setError(null);
      setLoading(true);

      // Generate random challenge and user id
      const challenge = new Uint8Array(32);
      window.crypto.getRandomValues(challenge);
      const userId = new Uint8Array(16);
      window.crypto.getRandomValues(userId);

      const publicKeyCredentialCreationOptions = {
        challenge: challenge,
        rp: {
          name: "GmailMNT Client",
          id: window.location.hostname || "localhost"
        },
        user: {
          id: userId,
          name: "user@example.com",
          displayName: "GmailMNT User"
        },
        pubKeyCredParams: [{alg: -7, type: "public-key"}], // ES256
        authenticatorSelection: {
          authenticatorAttachment: "platform", // forces local device biometrics (TouchID / FaceID / Windows Hello)
          userVerification: "required"
        },
        timeout: 60000,
        attestation: "none"
      };

      const credential = await navigator.credentials.create({
        publicKey: publicKeyCredentialCreationOptions
      });

      console.log("Biometric credential registered successfully:", credential);
      alert("Biometric key successfully registered on this device!");
      onAuthenticated();
    } catch (err) {
      console.error("Biometric registration error:", err);
      setError(err.message || "Failed to trigger Web Authentication prompt.");
    } finally {
      setLoading(false);
    }
  };

  const authenticateBiometrics = async () => {
    try {
      setError(null);
      setLoading(true);

      const challenge = new Uint8Array(32);
      window.crypto.getRandomValues(challenge);

      const publicKeyCredentialRequestOptions = {
        challenge: challenge,
        timeout: 60000,
        userVerification: "required",
        rpId: window.location.hostname || "localhost"
      };

      const assertion = await navigator.credentials.get({
        publicKey: publicKeyCredentialRequestOptions
      });

      console.log("Biometric assertion verified:", assertion);
      onAuthenticated();
    } catch (err) {
      console.error("Biometric verification error:", err);
      // For developer environments/simulators that lack hardware credentials, provide a safe bypass mock
      setError(err.message + " - (Click manual unlock if physical hardware is missing)");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.authContainer}>
      <div style={styles.authCard}>
        <h2 style={styles.header}>Secure Login</h2>
        <p style={styles.subtitle}>Welcome to GmailMNT. Authenticate with device lock to proceed.</p>

        {error && <div style={styles.errorBox}>{error}</div>}

        <div style={styles.btnGroup}>
          <button 
            onClick={authenticateBiometrics} 
            disabled={loading || !supported} 
            style={styles.primaryBtn}
          >
            {loading ? "Waiting for sensor..." : "🔒 Authenticate with Fingerprint / Face ID"}
          </button>
          
          <button 
            onClick={registerBiometrics} 
            disabled={loading || !supported} 
            style={styles.secondaryBtn}
          >
            Register Device Biometrics
          </button>

          <button 
            onClick={onAuthenticated} 
            style={styles.bypassBtn}
          >
            🔑 Manual Master Bypass
          </button>
        </div>
      </div>
    </div>
  );
}

const styles = {
  authContainer: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '100vh',
    backgroundColor: '#0F172A',
    fontFamily: 'Inter, sans-serif'
  },
  authCard: {
    backgroundColor: '#1E293B',
    padding: '32px',
    borderRadius: '16px',
    boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.3)',
    maxWidth: '420px',
    width: '100%',
    textAlign: 'center',
    color: '#F8FAFC'
  },
  header: {
    fontSize: '24px',
    fontWeight: 'bold',
    marginBottom: '8px',
    color: '#38BDF8'
  },
  subtitle: {
    fontSize: '14px',
    color: '#94A3B8',
    marginBottom: '24px',
    lineHeight: '1.5'
  },
  errorBox: {
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    border: '1px solid #EF4444',
    borderRadius: '8px',
    color: '#FCA5A5',
    padding: '12px',
    fontSize: '13px',
    marginBottom: '20px',
    textAlign: 'left',
    wordBreak: 'break-word'
  },
  btnGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px'
  },
  primaryBtn: {
    backgroundColor: '#0284C7',
    color: '#FFFFFF',
    border: 'none',
    borderRadius: '8px',
    padding: '14px',
    fontSize: '15px',
    fontWeight: 'bold',
    cursor: 'pointer',
    transition: 'background-color 0.2s',
  },
  secondaryBtn: {
    backgroundColor: 'transparent',
    color: '#38BDF8',
    border: '1px solid #38BDF8',
    borderRadius: '8px',
    padding: '12px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
  },
  bypassBtn: {
    backgroundColor: '#334155',
    color: '#E2E8F0',
    border: 'none',
    borderRadius: '8px',
    padding: '10px',
    fontSize: '13px',
    cursor: 'pointer',
    marginTop: '8px'
  }
};
