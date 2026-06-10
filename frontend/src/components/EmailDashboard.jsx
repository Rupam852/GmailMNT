import React, { useState, useEffect } from 'react';
import EmailComposition from './EmailComposition';

const mockMails = [
  { id: '1', from: 'finances@groww-clone.com', subject: 'Your June Portfolio Summary', body: 'Hi User, your investments are growing! Here is your latest monthly breakdown. Keep expanding.', category: 'Primary', date: 'June 10' },
  { id: '2', from: 'alerts@security.google.com', subject: 'New Sign-in Detected', body: 'Google blocked a suspicious authentication on your device from an unrecognized server.', category: 'Updates', date: 'June 9' },
  { id: '3', from: 'newsletter@newsletter.com', subject: 'The Weekly Tech Circular', body: 'Discover the latest releases in generative models and dynamic Material 3 components inside Kotlin.', category: 'Promotions', date: 'June 8' },
];

export default function EmailDashboard({ onLogout, isDarkMode = true, onToggleTheme }) {
  const [mails, setMails] = useState(mockMails);
  const [selectedFolder, setSelectedFolder] = useState('All');
  const [activeMail, setActiveMail] = useState(mockMails[0]);
  const [composing, setComposing] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const themeColors = {
    bg: isDarkMode ? '#0F172A' : '#F8FAFC',
    surface: isDarkMode ? '#1E293B' : '#FFFFFF',
    border: isDarkMode ? '#334155' : '#E2E8F0',
    text: isDarkMode ? '#E2E8F0' : '#1E293B',
    textMuted: isDarkMode ? '#94A3B8' : '#64748B',
    activeBg: isDarkMode ? '#334155' : '#F1F5F9',
    titleColor: isDarkMode ? '#F8FAFC' : '#0F172A'
  };

  // Monitor screen sizing to guarantee responsiveness
  useEffect(() => {
    const handleResize = () => {
      const mobileMode = window.innerWidth < 768;
      setIsMobile(mobileMode);
      if (mobileMode) {
        setSidebarOpen(false);
      } else {
        setSidebarOpen(true);
      }
    };
    window.addEventListener('resize', handleResize);
    handleResize(); // Initial trigger
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const handleSendCompose = (newMail) => {
    const freshMail = {
      id: Date.now().toString(),
      from: 'me@client.com',
      subject: newMail.subject,
      body: newMail.body,
      category: newMail.category,
      date: 'Just Now'
    };
    setMails([freshMail, ...mails]);
    setActiveMail(freshMail);
    setComposing(false);
  };

  const filteredMails = selectedFolder === 'All' 
    ? mails 
    : mails.filter(m => m.category === selectedFolder);

  return (
    <div style={{ ...styles.dashboardContainer, backgroundColor: themeColors.bg, color: themeColors.text }}>
      {/* 1. TOP HEADER */}
      <header style={{ ...styles.header, backgroundColor: themeColors.surface, borderBottom: `1px solid ${themeColors.border}` }}>
        <div style={styles.logoRow}>
          {isMobile && (
            <button 
              onClick={() => setSidebarOpen(!sidebarOpen)} 
              style={styles.menuToggle}
            >
              ☰
            </button>
          )}
          <span style={styles.logoText}>GmailMNT Co-Pilot</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button 
            onClick={onToggleTheme} 
            style={{
              backgroundColor: isDarkMode ? 'rgba(56, 189, 248, 0.1)' : 'rgba(15, 23, 42, 0.05)',
              border: `1px solid ${themeColors.border}`,
              color: themeColors.text,
              borderRadius: '6px',
              padding: '6px 14px',
              cursor: 'pointer',
              fontSize: '13px',
              fontWeight: '600'
            }}
          >
            {isDarkMode ? '☀️ Light' : '🌙 Dark'}
          </button>
          <button onClick={onLogout} style={{ ...styles.logoutBtn, borderColor: themeColors.border, color: themeColors.textMuted }}>Logout</button>
        </div>
      </header>

      {/* 2. BODY LAYOUT */}
      <div style={styles.bodyLayout}>
        {/* SIDEBAR FOR FOLDER NAVIGATION */}
        {sidebarOpen && (
          <aside style={{ ...styles.sidebar, backgroundColor: themeColors.surface, borderRight: `1px solid ${themeColors.border}` }}>
            <div style={{ ...styles.sidebarHeader, color: themeColors.textMuted }}>Folders & Folders</div>
            <nav style={styles.navStack}>
              {['All', 'Primary', 'Updates', 'Social', 'Promotions'].map(folder => (
                <button
                  key={folder}
                  onClick={() => {
                    setSelectedFolder(folder);
                    setComposing(false);
                    if (isMobile) setSidebarOpen(false);
                  }}
                  style={{
                    ...styles.navButton,
                    backgroundColor: selectedFolder === folder && !composing ? 'rgba(56, 189, 248, 0.15)' : 'transparent',
                    color: selectedFolder === folder && !composing ? '#38BDF8' : themeColors.textMuted
                  }}
                >
                  ✉  {folder} Feed
                </button>
              ))}
            </nav>

            <button 
              onClick={() => {
                setComposing(true);
                if (isMobile) setSidebarOpen(false);
              }}
              style={styles.composeBtn}
            >
              📝 Compose Message
            </button>
          </aside>
        )}

        {/* MAIN MESSAGE VIEWING / STREAM AREA */}
        <main style={styles.mainContent}>
          {composing ? (
            <EmailComposition 
              onSendMail={handleSendCompose} 
              onCancel={() => setComposing(false)} 
              isDarkMode={isDarkMode}
            />
          ) : (
            <div style={styles.splitPane}>
              {/* Message List Pane */}
              <div style={{
                ...styles.listPane,
                width: isMobile ? '100%' : '380px',
                display: isMobile && activeMail ? 'none' : 'block'
              }}>
                <h4 style={{ ...styles.paneTitle, color: themeColors.textMuted }}>{selectedFolder} Messages</h4>
                {filteredMails.length === 0 ? (
                  <div style={{ ...styles.emptyText, color: themeColors.textMuted }}>No messages here.</div>
                ) : (
                  filteredMails.map(mail => (
                    <div 
                      key={mail.id} 
                      onClick={() => setActiveMail(mail)}
                      style={{
                        ...styles.mailCard,
                        backgroundColor: activeMail?.id === mail.id ? themeColors.activeBg : themeColors.surface,
                        borderColor: activeMail?.id === mail.id ? '#38BDF8' : 'transparent'
                      }}
                    >
                      <div style={styles.cardHeader}>
                        <span style={styles.cardFrom}>{mail.from}</span>
                        <span style={{ ...styles.cardDate, color: themeColors.textMuted }}>{mail.date}</span>
                      </div>
                      <div style={{ ...styles.cardSub, color: themeColors.titleColor }}>{mail.subject}</div>
                      <p style={{ ...styles.cardBody, color: themeColors.textMuted }}>{mail.body.substring(0, 60)}...</p>
                    </div>
                  ))
                )}
              </div>

              {/* Message Detail Pane */}
              {activeMail && (!isMobile || (isMobile && activeMail)) && (
                <div style={{ ...styles.detailPane, backgroundColor: themeColors.surface }}>
                  {isMobile && (
                    <button 
                      onClick={() => setActiveMail(null)} 
                      style={styles.backBtn}
                    >
                      ← Back to inbox list
                    </button>
                  )}
                  <div style={styles.detailCard}>
                    <h2 style={{ ...styles.detailSubject, color: themeColors.titleColor }}>{activeMail.subject}</h2>
                    <div style={{ ...styles.detailMeta, borderColor: themeColors.border, color: themeColors.textMuted }}>
                      <span>From: <strong style={{ color: themeColors.titleColor }}>{activeMail.from}</strong></span>
                      <span>{activeMail.date}</span>
                    </div>
                    <div style={styles.categoryBadge}>{activeMail.category}</div>
                    <p style={{ ...styles.detailBody, color: themeColors.text }}>{activeMail.body}</p>
                  </div>
                </div>
              )}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

const styles = {
  dashboardContainer: {
    display: 'flex',
    flexDirection: 'column',
    height: '100vh',
    backgroundColor: '#0F172A',
    color: '#E2E8F0',
    fontFamily: 'Inter, sans-serif'
  },
  header: {
    height: '64px',
    backgroundColor: '#1E293B',
    borderBottom: '1px solid #334155',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '0 24px',
    boxSizing: 'border-box'
  },
  logoRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px'
  },
  menuToggle: {
    background: 'none',
    border: 'none',
    color: '#38BDF8',
    fontSize: '20px',
    cursor: 'pointer'
  },
  logoText: {
    fontSize: '18px',
    fontWeight: '800',
    color: '#38BDF8'
  },
  logoutBtn: {
    backgroundColor: 'transparent',
    border: '1px solid #475569',
    color: '#94A3B8',
    borderRadius: '6px',
    padding: '6px 14px',
    fontSize: '13px',
    cursor: 'pointer'
  },
  bodyLayout: {
    display: 'flex',
    flex: 1,
    overflow: 'hidden'
  },
  sidebar: {
    width: '240px',
    backgroundColor: '#1E293B',
    borderRight: '1px solid #334155',
    padding: '20px',
    boxSizing: 'border-box',
    display: 'flex',
    flexDirection: 'column',
    gap: '20px'
  },
  sidebarHeader: {
    fontSize: '12px',
    fontWeight: '700',
    letterSpacing: '1px',
    color: '#64748B',
    textTransform: 'uppercase'
  },
  navStack: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px'
  },
  navButton: {
    border: 'none',
    borderRadius: '8px',
    padding: '10px 14px',
    fontSize: '14px',
    textAlign: 'left',
    cursor: 'pointer',
    transition: 'all 0.2s',
    fontWeight: '600'
  },
  composeBtn: {
    marginTop: 'auto',
    backgroundColor: '#7C3AED',
    color: '#FFFFFF',
    border: 'none',
    borderRadius: '8px',
    padding: '12px',
    fontSize: '14px',
    fontWeight: 'bold',
    cursor: 'pointer',
    transition: 'background-color 0.2s'
  },
  mainContent: {
    flex: 1,
    padding: '24px',
    overflowY: 'auto',
    boxSizing: 'border-box'
  },
  splitPane: {
    display: 'flex',
    height: '100%',
    gap: '24px'
  },
  listPane: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    overflowY: 'auto'
  },
  paneTitle: {
    margin: '0 0 12px 0',
    fontSize: '15px',
    fontWeight: '700',
    color: '#94A3B8'
  },
  emptyText: {
    color: '#64748B',
    fontSize: '14px',
    textAlign: 'center',
    padding: '40px 0'
  },
  mailCard: {
    padding: '16px',
    borderRadius: '10px',
    cursor: 'pointer',
    border: '1.5px solid transparent',
    transition: 'all 0.15s'
  },
  cardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '6px'
  },
  cardFrom: {
    fontSize: '13px',
    fontWeight: '700',
    color: '#38BDF8'
  },
  cardDate: {
    fontSize: '11px',
    color: '#64748B'
  },
  cardSub: {
    fontSize: '14px',
    fontWeight: '600',
    marginBottom: '6px',
    color: '#F1F5F9'
  },
  cardBody: {
    margin: 0,
    fontSize: '13px',
    color: '#94A3B8',
    lineHeight: '1.4'
  },
  detailPane: {
    flex: 1,
    backgroundColor: '#1E293B',
    borderRadius: '12px',
    padding: '24px',
    overflowY: 'auto'
  },
  backBtn: {
    backgroundColor: 'transparent',
    border: 'none',
    color: '#38BDF8',
    fontSize: '14px',
    cursor: 'pointer',
    marginBottom: '20px',
    padding: 0
  },
  detailCard: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  detailSubject: {
    fontSize: '22px',
    fontWeight: '800',
    color: '#F8FAFC',
    margin: 0
  },
  detailMeta: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: '13px',
    color: '#94A3B8',
    borderBottom: '1px solid #334155',
    paddingBottom: '12px'
  },
  categoryBadge: {
    alignSelf: 'flex-start',
    backgroundColor: 'rgba(56, 189, 248, 0.15)',
    color: '#38BDF8',
    padding: '4px 8px',
    borderRadius: '6px',
    fontSize: '11px',
    fontWeight: '700'
  },
  detailBody: {
    fontSize: '15px',
    lineHeight: '1.7',
    color: '#E2E8F0',
    whiteSpace: 'pre-wrap',
    margin: 0
  }
};
