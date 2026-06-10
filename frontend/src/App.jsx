import React, { useState } from 'react';
import BiometricAuth from './components/BiometricAuth';
import EmailDashboard from './components/EmailDashboard';

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(true);

  const toggleTheme = () => {
    setIsDarkMode(!isDarkMode);
  };

  return (
    <div style={{
      ...styles.appContainer,
      backgroundColor: isDarkMode ? '#0F172A' : '#F8FAFC',
      color: isDarkMode ? '#E2E8F0' : '#1E293B'
    }}>
      {isAuthenticated ? (
        <EmailDashboard 
          onLogout={() => setIsAuthenticated(false)} 
          isDarkMode={isDarkMode}
          onToggleTheme={toggleTheme}
        />
      ) : (
        <BiometricAuth 
          onAuthenticated={() => setIsAuthenticated(true)} 
          isDarkMode={isDarkMode}
          onToggleTheme={toggleTheme}
        />
      )}
    </div>
  );
}

const styles = {
  appContainer: {
    margin: 0,
    padding: 0,
    width: '100vw',
    height: '100vh',
    overflow: 'hidden'
  }
};
