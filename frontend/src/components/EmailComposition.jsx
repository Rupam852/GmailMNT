import React, { useState, useEffect } from 'react';

// React component representing the email composition view with 'Gemini Draft' integration
export default function EmailComposition({ onSendMail, onCancel, isDarkMode = true }) {
  // Load saved drafts from localStorage / Local Storage if they exist
  const [to, setTo] = useState(() => localStorage.getItem('react_draft_to') || '');
  const [subject, setSubject] = useState(() => localStorage.getItem('react_draft_subject') || '');
  const [body, setBody] = useState(() => localStorage.getItem('react_draft_body') || '');
  const [category, setCategory] = useState(() => localStorage.getItem('react_draft_category') || 'Primary');
  
  const [drafting, setDrafting] = useState(false);
  const [prompt, setPrompt] = useState('');
  const [showGeminiModal, setShowGeminiModal] = useState(false);

  const compTheme = {
    bg: isDarkMode ? '#1E293B' : '#FFFFFF',
    text: isDarkMode ? '#E2E8F0' : '#1E293B',
    border: isDarkMode ? '#334155' : '#E2E8F0',
    inputText: isDarkMode ? '#F8FAFC' : '#0F172A',
    placeholderColor: isDarkMode ? '#64748B' : '#94A3B8',
    selectBg: isDarkMode ? '#334155' : '#F1F5F9',
    selectText: isDarkMode ? '#F8FAFC' : '#1E293B'
  };

  // Periodically/Immediately auto-save draft to local storage while typing
  useEffect(() => {
    localStorage.setItem('react_draft_to', to);
    localStorage.setItem('react_draft_subject', subject);
    localStorage.setItem('react_draft_body', body);
    localStorage.setItem('react_draft_category', category);
  }, [to, subject, body, category]);

  const clearLocalDraft = () => {
    localStorage.removeItem('react_draft_to');
    localStorage.removeItem('react_draft_subject');
    localStorage.removeItem('react_draft_body');
    localStorage.removeItem('react_draft_category');
  };

  const triggerGeminiDraft = async () => {
    if (!prompt.trim()) {
      alert("Please provide a prompt describing what you want to write!");
      return;
    }
    try {
      setDrafting(true);
      // Simulating Gemini API Call or calling a local mock
      // In production, this retrofits to a client/server integration
      setTimeout(() => {
        const mockResponses = [
          `Hi,\n\nI hope this email finds you well. Regarding "${subject || "our previous discussion"}", I wanted to follow up and see if you had any updates. Let's touch base soon.\n\nBest regards,\n[My Name]`,
          `Dear Team,\n\nThis is a quick summary about our next steps on ${subject || "the email structure initiative"}. Please check the shared documents and let me know if you have questions.\n\nWarmly,\n[My Name]`,
          `Hello,\n\nThanks for reaching out! I appreciate your message regarding "${subject || "your query"}". I am currently looking into this and will get back to you as soon as possible.\n\nSincerely,\n[My Name]`
        ];
        const randomResponse = mockResponses[Math.floor(Math.random() * mockResponses.length)];
        setBody(randomResponse);
        setShowGeminiModal(false);
        setPrompt('');
        setDrafting(false);
      }, 1500);
    } catch (error) {
      console.error("Gemini draft generation failure:", error);
      alert("Failed to draft content using Gemini.");
      setDrafting(false);
    }
  };

  const handleSend = () => {
    if (!to || !subject || !body) {
      alert("All fields (To, Subject, Body) are required!");
      return;
    }
    onSendMail({ to, subject, body, category });
    clearLocalDraft();
    alert("Email successfully sent.");
    // Clear form
    setTo('');
    setSubject('');
    setBody('');
  };

  return (
    <div style={{ ...styles.composeContainer, backgroundColor: compTheme.bg, color: compTheme.text }}>
      <div style={{ ...styles.formHeader, borderBottom: `1px solid ${compTheme.border}` }}>
        <h3 style={styles.headerTitle}>New Message</h3>
        <button onClick={onCancel} style={styles.closeBtn}>✕</button>
      </div>

      <div style={{ ...styles.formRow, borderBottom: `1px solid ${compTheme.border}` }}>
        <label style={styles.label}>To</label>
        <input 
          type="email" 
          value={to} 
          onChange={(e) => setTo(e.target.value)} 
          placeholder="recipient@example.com"
          style={{ ...styles.input, color: compTheme.inputText }}
        />
      </div>

      <div style={{ ...styles.formRow, borderBottom: `1px solid ${compTheme.border}` }}>
        <label style={styles.label}>Subject</label>
        <input 
          type="text" 
          value={subject} 
          onChange={(e) => setSubject(e.target.value)} 
          placeholder="Enter subject title"
          style={{ ...styles.input, color: compTheme.inputText }}
        />
      </div>

      <div style={{ ...styles.formRow, borderBottom: `1px solid ${compTheme.border}` }}>
        <label style={styles.label}>Category</label>
        <select 
          value={category} 
          onChange={(e) => setCategory(e.target.value)} 
          style={{ ...styles.select, backgroundColor: compTheme.selectBg, color: compTheme.selectText }}
        >
          <option value="Primary">Primary</option>
          <option value="Updates">Updates</option>
          <option value="Social">Social</option>
          <option value="Promotions">Promotions</option>
        </select>
      </div>

      <div style={styles.bodyRow}>
        <textarea 
          value={body} 
          onChange={(e) => setBody(e.target.value)} 
          placeholder="Type your message here..."
          style={{ ...styles.textarea, color: compTheme.inputText }}
        ></textarea>
      </div>

      <div style={{ ...styles.actionRow, borderTop: `1px solid ${compTheme.border}` }}>
        {/* 'Gemini Draft' button explicitly requested by the user */}
        <button 
          onClick={() => setShowGeminiModal(true)} 
          style={styles.geminiBtn}
          id="gemini-draft-button"
        >
          ✨ Gemini Draft
        </button>

        <button onClick={handleSend} style={styles.sendBtn}>
          Send Message
        </button>
      </div>

      {showGeminiModal && (
        <div style={styles.modalOverlay}>
          <div style={{ ...styles.modalContent, backgroundColor: compTheme.bg, color: compTheme.text }}>
            <h4 style={styles.modalHeader}>✨ Gemini Draft Assistant</h4>
            <p style={styles.modalSub}>Describe what you want to write or reply, and Gemini will draft it for you:</p>
            <input 
              type="text" 
              value={prompt} 
              onChange={(e) => setPrompt(e.target.value)} 
              placeholder="e.g., A professional follow up email..."
              style={{ ...styles.modalInput, backgroundColor: compTheme.selectBg, color: compTheme.inputText, borderColor: compTheme.border }}
              disabled={drafting}
            />
            <div style={styles.modalButtons}>
              <button 
                onClick={() => setShowGeminiModal(false)} 
                style={styles.modalCancel}
                disabled={drafting}
              >
                Cancel
              </button>
              <button 
                onClick={triggerGeminiDraft} 
                style={styles.modalConfirm}
                disabled={drafting}
              >
                {drafting ? "Thinking..." : "Generate Draft"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const styles = {
  composeContainer: {
    backgroundColor: '#1E293B',
    borderRadius: '12px',
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
    color: '#E2E8F0',
    fontFamily: 'Inter, sans-serif'
  },
  formHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderBottom: '1px solid #334155',
    paddingBottom: '12px',
    marginBottom: '16px'
  },
  headerTitle: {
    fontSize: '18px',
    fontWeight: '700',
    color: '#38BDF8',
    margin: 0
  },
  closeBtn: {
    background: 'none',
    border: 'none',
    color: '#94A3B8',
    fontSize: '16px',
    cursor: 'pointer'
  },
  formRow: {
    display: 'flex',
    alignItems: 'center',
    marginBottom: '12px',
    borderBottom: '1px solid #334155',
    paddingBottom: '8px'
  },
  label: {
    width: '80px',
    fontSize: '14px',
    color: '#94A3B8',
    fontWeight: '600'
  },
  input: {
    flex: 1,
    background: 'transparent',
    border: 'none',
    color: '#F8FAFC',
    outline: 'none',
    fontSize: '14px'
  },
  select: {
    flex: 1,
    background: '#334155',
    border: 'none',
    borderRadius: '4px',
    color: '#F8FAFC',
    outline: 'none',
    fontSize: '14px',
    padding: '4px 8px'
  },
  bodyRow: {
    flex: 1,
    display: 'flex',
    marginTop: '8px',
    minHeight: '200px'
  },
  textarea: {
    flex: 1,
    backgroundColor: 'transparent',
    border: 'none',
    resize: 'none',
    color: '#F8FAFC',
    outline: 'none',
    fontSize: '14px',
    lineHeight: '1.6',
    whiteSpace: 'pre-wrap'
  },
  actionRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: '16px',
    paddingTop: '16px',
    borderTop: '1px solid #334155'
  },
  geminiBtn: {
    backgroundColor: 'rgba(124, 58, 237, 0.2)',
    color: '#C084FC',
    border: '1px solid #C084FC',
    borderRadius: '8px',
    padding: '10px 16px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    transition: 'background-color 0.2s'
  },
  sendBtn: {
    backgroundColor: '#0EA5E9',
    color: '#FFFFFF',
    border: 'none',
    borderRadius: '8px',
    padding: '10px 20px',
    fontSize: '14px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'background-color 0.2s'
  },
  modalOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(15, 23, 42, 0.75)',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 1000
  },
  modalContent: {
    backgroundColor: '#1E293B',
    borderRadius: '12px',
    boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
    padding: '24px',
    maxWidth: '450px',
    width: '100%',
    color: '#F8FAFC'
  },
  modalHeader: {
    margin: '0 0 8px 0',
    fontSize: '18px',
    fontWeight: '700',
    color: '#A78BFA'
  },
  modalSub: {
    fontSize: '14px',
    color: '#94A3B8',
    marginBottom: '16px',
    lineHeight: '1.4'
  },
  modalInput: {
    width: '100%',
    backgroundColor: '#334155',
    border: '1px solid #475569',
    borderRadius: '8px',
    padding: '10px',
    color: '#F8FAFC',
    outline: 'none',
    fontSize: '14px',
    boxSizing: 'border-box'
  },
  modalButtons: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '12px',
    marginTop: '20px'
  },
  modalCancel: {
    backgroundColor: 'transparent',
    color: '#94A3B8',
    border: 'none',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: '600'
  },
  modalConfirm: {
    backgroundColor: '#7C3AED',
    color: '#FFFFFF',
    border: 'none',
    borderRadius: '8px',
    padding: '8px 16px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: '600'
  }
};
