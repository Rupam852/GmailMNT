import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion, useScroll, useTransform } from 'framer-motion';
import {
  Mail, Shield, Smartphone, Zap, Lock, RefreshCw, Fingerprint,
  Moon, Paperclip, Undo2, Bell, Search, Archive, Trash2,
  Download, ChevronRight, Star, Clock, Wifi, WifiOff, Github,
  Heart, ArrowRight, Check, Sparkles, Layers, Users, MessageSquare,
  AlertTriangle
} from 'lucide-react';

const APP_DOWNLOAD_URL = "https://drive.google.com/file/d/1PYGq9WjXb66TDl3bOt_ugxccDa2g5dRE/view?usp=sharing";
const SUPPORT_EMAIL = "rupambairagya08@gmail.com";

// Animation variants
const fadeInUp = {
  hidden: { opacity: 0, y: 40 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.6, ease: "easeOut" } }
};

const fadeIn = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { duration: 0.5 } }
};

const staggerContainer = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.12 } }
};

const scaleIn = {
  hidden: { opacity: 0, scale: 0.8 },
  visible: { opacity: 1, scale: 1, transition: { duration: 0.5, ease: "easeOut" } }
};

const slideInLeft = {
  hidden: { opacity: 0, x: -60 },
  visible: { opacity: 1, x: 0, transition: { duration: 0.6, ease: "easeOut" } }
};

const slideInRight = {
  hidden: { opacity: 0, x: 60 },
  visible: { opacity: 1, x: 0, transition: { duration: 0.6, ease: "easeOut" } }
};

// Features data
const features = [
  { icon: Shield, title: "Biometric Security", desc: "Unlock your emails with fingerprint or face ID. Your data stays protected with military-grade encryption.", color: "#6366F1" },
  { icon: Undo2, title: "Undo Send", desc: "Sent something by mistake? You get 5 seconds to cancel and edit before it's delivered.", color: "#EC4899" },
  { icon: Paperclip, title: "Smart Attachments", desc: "Send and receive file attachments seamlessly. Preview, download, and manage attachments with ease.", color: "#F59E0B" },
  { icon: Moon, title: "Dark & Light Mode", desc: "Beautiful themes that adapt to your preference. Easy on the eyes, day or night.", color: "#8B5CF6" },
  { icon: RefreshCw, title: "Real-time Sync", desc: "Differential sync technology fetches only new emails, saving data and battery life.", color: "#10B981" },
  { icon: Users, title: "Multi-Account", desc: "Manage multiple Gmail accounts from a single app. Switch between accounts instantly.", color: "#3B82F6" },
  { icon: Layers, title: "Smart Categories", desc: "Emails auto-sorted into Primary, Social, Promotions, Updates, and Forums for quick access.", color: "#EF4444" },
  { icon: WifiOff, title: "Offline Support", desc: "Read cached emails and compose new ones even without internet. Auto-sends when you're back online.", color: "#14B8A6" },
  { icon: Search, title: "Powerful Search", desc: "Find any email instantly with fast full-text search across all your accounts.", color: "#F97316" },
  { icon: Bell, title: "Smart Notifications", desc: "Get notified about important emails with intelligent notification management.", color: "#A855F7" },
  { icon: Archive, title: "Archive & Trash", desc: "Quick actions to archive, trash, or mark as read with a single tap.", color: "#64748B" },
  { icon: Sparkles, title: "AI-Powered", desc: "Built with modern technology stack for a smooth and intelligent email experience.", color: "#06B6D4" },
];

const stats = [
  { value: "14 MB", label: "Lightweight App", icon: Smartphone },
  { value: "150+", label: "Emails Cached", icon: Mail },
  { value: "5 sec", label: "Undo Window", icon: Clock },
  { value: "< 20 MB", label: "Total Storage", icon: Layers },
];

function useInView(threshold = 0.1) {
  const [ref, setRef] = useState(null);
  const [inView, setInView] = useState(false);

  useEffect(() => {
    if (!ref) return;
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) setInView(true); },
      { threshold }
    );
    observer.observe(ref);
    return () => observer.disconnect();
  }, [ref, threshold]);

  return [setRef, inView];
}

// Floating particles background
function FloatingParticles() {
  return (
    <div className="particles-container">
      {Array.from({ length: 20 }).map((_, i) => (
        <motion.div
          key={i}
          className="particle"
          initial={{
            x: Math.random() * (typeof window !== 'undefined' ? window.innerWidth : 1200),
            y: Math.random() * (typeof window !== 'undefined' ? window.innerHeight : 800),
            scale: Math.random() * 0.5 + 0.3,
          }}
          animate={{
            y: [null, Math.random() * -200 - 100],
            opacity: [0.2, 0.6, 0.2],
          }}
          transition={{
            duration: Math.random() * 8 + 6,
            repeat: Infinity,
            repeatType: "reverse",
            ease: "easeInOut",
          }}
        />
      ))}
    </div>
  );
}

// Navbar
function Navbar() {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 50);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <motion.nav
      className={`navbar ${scrolled ? 'navbar-scrolled' : ''}`}
      initial={{ y: -80 }}
      animate={{ y: 0 }}
      transition={{ duration: 0.5, ease: "easeOut" }}
    >
      <div className="nav-content">
        <div className="nav-brand">
          <div className="nav-logo">
            <Mail size={24} />
          </div>
          <span className="nav-title">GmailMNT</span>
        </div>
        <div className="nav-links">
          <a href="#features" className="nav-link">Features</a>
          <a href="#how-it-works" className="nav-link">How it Works</a>
          <a href="#download" className="nav-link">Download</a>
          <Link to="/privacy" className="nav-link">Privacy</Link>
        </div>
        <a href={APP_DOWNLOAD_URL} target="_blank" rel="noopener noreferrer" className="nav-download-btn">
          <Download size={16} /> Download
        </a>
      </div>
    </motion.nav>
  );
}

// Hero Section
function HeroSection() {
  const { scrollY } = useScroll();
  const y = useTransform(scrollY, [0, 500], [0, 150]);
  const opacity = useTransform(scrollY, [0, 400], [1, 0]);
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(SUPPORT_EMAIL);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <section className="hero-section">
      <FloatingParticles />
      <motion.div className="hero-bg-gradient" style={{ y, opacity }} />
      <div className="hero-content">
        <motion.div
          className="hero-badge"
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.2, duration: 0.4 }}
        >
          <Sparkles size={14} /> Now with Undo Send & Attachments
        </motion.div>

        <motion.h1
          className="hero-title"
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.7, ease: "easeOut" }}
        >
          Your Email,{' '}
          <span className="gradient-text">Reimagined</span>
        </motion.h1>

        <motion.p
          className="hero-subtitle"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.6 }}
        >
          GmailMNT is a modern, secure, and lightning-fast email client for Android.
          Built with biometric auth, smart categories, and offline support — 
          everything you need in just 14 MB.
        </motion.p>

        <motion.div
          className="testing-warning-card"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6, duration: 0.6 }}
        >
          <div className="warning-header">
            <AlertTriangle size={20} />
            <span>Google OAuth Testing Mode Notice</span>
          </div>
          <p className="warning-desc">
            GmailMNT is currently in <strong>Google OAuth Testing Mode</strong>. To sign in and test the app, Google requires your Gmail address to be manually registered in our test user whitelist. Without this, Google will block your access during sign-in.
          </p>
          <div className="warning-action">
            <a href={`mailto:${SUPPORT_EMAIL}?subject=GmailMNT%20Whitelist%20Request&body=Hi%2C%20please%20add%20my%20email%20address%20to%20the%20GmailMNT%20test%20users%20whitelist.%0A%0AMy%20Google%20Email%3A%20`} className="btn-warning-email">
              <Mail size={16} /> Request Whitelist Access
            </a>
            <span className="warning-separator">or manually email:</span>
            <div className="email-copy-box">
              <code>{SUPPORT_EMAIL}</code>
              <button 
                onClick={handleCopy}
                className={`copy-btn ${copied ? 'copied' : ''}`}
              >
                {copied ? 'Copied!' : 'Copy'}
              </button>
            </div>
          </div>
        </motion.div>

        <motion.div
          className="hero-cta"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7, duration: 0.5 }}
        >
          <a href={APP_DOWNLOAD_URL} target="_blank" rel="noopener noreferrer" className="btn-primary">
            <Download size={20} /> Download Free
          </a>
          <a href="#features" className="btn-secondary">
            Explore Features <ChevronRight size={18} />
          </a>
        </motion.div>

        <motion.div
          className="hero-stats"
          variants={staggerContainer}
          initial="hidden"
          animate="visible"
        >
          {stats.map((stat, i) => (
            <motion.div key={i} className="stat-card" variants={scaleIn}>
              <stat.icon size={20} className="stat-icon" />
              <div className="stat-value">{stat.value}</div>
              <div className="stat-label">{stat.label}</div>
            </motion.div>
          ))}
        </motion.div>
      </div>

    </section>
  );
}

// Features Section
function FeaturesSection() {
  const [ref, inView] = useInView(0.05);

  return (
    <section id="features" className="features-section" ref={ref}>
      <motion.div
        className="section-header"
        variants={fadeInUp}
        initial="hidden"
        animate={inView ? "visible" : "hidden"}
      >
        <span className="section-tag">Features</span>
        <h2 className="section-title">Everything You Need</h2>
        <p className="section-subtitle">
          A full-featured email experience, crafted for simplicity and speed
        </p>
      </motion.div>

      <motion.div
        className="features-grid"
        variants={staggerContainer}
        initial="hidden"
        animate={inView ? "visible" : "hidden"}
      >
        {features.map((feature, i) => (
          <motion.div
            key={i}
            className="feature-card"
            variants={fadeInUp}
            whileHover={{ y: -8, transition: { duration: 0.2 } }}
          >
            <div className="feature-icon-wrapper" style={{ background: `${feature.color}18` }}>
              <feature.icon size={24} style={{ color: feature.color }} />
            </div>
            <h3 className="feature-title">{feature.title}</h3>
            <p className="feature-desc">{feature.desc}</p>
          </motion.div>
        ))}
      </motion.div>
    </section>
  );
}

// How it Works Section
function HowItWorksSection() {
  const [ref, inView] = useInView(0.05);

  const steps = [
    { num: "01", title: "Download & Install", desc: "Get GmailMNT from our secure download link. The APK is just 14 MB — installs in seconds.", icon: Download },
    { num: "02", title: "Sign in with Google", desc: "Connect your Gmail account securely with OAuth 2.0. (Note: Testing mode requires email whitelisting via support first).", icon: Shield },
    { num: "03", title: "Enable Biometric Lock", desc: "Add an extra layer of security with fingerprint or face unlock for quick and safe access.", icon: Fingerprint },
    { num: "04", title: "Start Managing Emails", desc: "Enjoy smart categories, undo send, attachments, offline mode, and more — all in one beautiful app.", icon: Mail },
  ];

  return (
    <section id="how-it-works" className="how-it-works-section" ref={ref}>
      <motion.div
        className="section-header"
        variants={fadeInUp}
        initial="hidden"
        animate={inView ? "visible" : "hidden"}
      >
        <span className="section-tag">How it Works</span>
        <h2 className="section-title">Get Started in Minutes</h2>
        <p className="section-subtitle">Four simple steps to a better email experience</p>
      </motion.div>

      <div className="steps-container">
        {steps.map((step, i) => (
          <motion.div
            key={i}
            className={`step-card ${i % 2 === 0 ? 'step-left' : 'step-right'}`}
            variants={i % 2 === 0 ? slideInLeft : slideInRight}
            initial="hidden"
            animate={inView ? "visible" : "hidden"}
            transition={{ delay: i * 0.15 }}
          >
            <div className="step-number">{step.num}</div>
            <div className="step-content">
              <div className="step-icon-wrapper">
                <step.icon size={22} />
              </div>
              <h3 className="step-title">{step.title}</h3>
              <p className="step-desc">{step.desc}</p>
            </div>
            {i < steps.length - 1 && <div className="step-connector" />}
          </motion.div>
        ))}
      </div>
    </section>
  );
}

// Security Section
function SecuritySection() {
  const [ref, inView] = useInView(0.1);

  const securityFeatures = [
    { icon: Lock, text: "OAuth 2.0 Authentication — No password stored" },
    { icon: Fingerprint, text: "Biometric lock with fingerprint & face ID" },
    { icon: Shield, text: "All data stays on your device" },
    { icon: Zap, text: "Secure token-based API communication" },
  ];

  return (
    <section className="security-section" ref={ref}>
      <div className="security-content">
        <motion.div
          className="security-left"
          variants={slideInLeft}
          initial="hidden"
          animate={inView ? "visible" : "hidden"}
        >
          <div className="security-visual">
            <motion.div
              className="shield-icon-large"
              animate={{ rotate: [0, 5, -5, 0] }}
              transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
            >
              <Shield size={80} />
            </motion.div>
            <div className="security-rings">
              <motion.div
                className="ring ring-1"
                animate={{ scale: [1, 1.2, 1], opacity: [0.3, 0.6, 0.3] }}
                transition={{ duration: 3, repeat: Infinity }}
              />
              <motion.div
                className="ring ring-2"
                animate={{ scale: [1, 1.4, 1], opacity: [0.2, 0.4, 0.2] }}
                transition={{ duration: 3, repeat: Infinity, delay: 0.5 }}
              />
            </div>
          </div>
        </motion.div>

        <motion.div
          className="security-right"
          variants={slideInRight}
          initial="hidden"
          animate={inView ? "visible" : "hidden"}
        >
          <span className="section-tag">Security First</span>
          <h2 className="section-title" style={{ textAlign: 'left' }}>Your Privacy, Our Priority</h2>
          <p className="security-desc">
            GmailMNT is built from the ground up with security as the foundation.
            Your emails and credentials never touch our servers — everything stays encrypted on your device.
          </p>
          <div className="security-features-list">
            {securityFeatures.map((feat, i) => (
              <motion.div
                key={i}
                className="security-feature-item"
                variants={fadeInUp}
                initial="hidden"
                animate={inView ? "visible" : "hidden"}
                transition={{ delay: 0.3 + i * 0.1 }}
              >
                <div className="check-circle">
                  <Check size={14} />
                </div>
                <span>{feat.text}</span>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </div>
    </section>
  );
}

// Download CTA Section
function DownloadSection() {
  const [ref, inView] = useInView(0.1);

  return (
    <section id="download" className="download-section" ref={ref}>
      <motion.div
        className="download-content"
        variants={scaleIn}
        initial="hidden"
        animate={inView ? "visible" : "hidden"}
      >
        <motion.div
          className="download-glow"
          animate={{ opacity: [0.5, 1, 0.5] }}
          transition={{ duration: 3, repeat: Infinity }}
        />
        <div className="download-icon-wrapper">
          <Download size={48} />
        </div>
        <h2 className="download-title">Ready to Transform Your Email?</h2>
        <p className="download-subtitle">
          Join users who trust GmailMNT for their daily email needs.
          Free, lightweight, and packed with features.
        </p>
        <a href={APP_DOWNLOAD_URL} target="_blank" rel="noopener noreferrer" className="btn-primary btn-large">
          <Download size={22} /> Download APK — 14 MB
        </a>
        <p style={{ marginTop: '16px', fontSize: '0.85rem', color: '#F59E0B', fontWeight: 500 }}>
          * Notice: App is in testing mode. Please email <a href={`mailto:${SUPPORT_EMAIL}`} style={{ textDecoration: 'underline', color: '#F59E0B' }}>{SUPPORT_EMAIL}</a> to whitelist your Google account before signing in.
        </p>
        <div className="download-trust">
          <div className="trust-item">
            <Shield size={14} /> Secure Download
          </div>
          <div className="trust-item">
            <Check size={14} /> No Ads
          </div>
          <div className="trust-item">
            <Star size={14} /> Free Forever
          </div>
        </div>
      </motion.div>
    </section>
  );
}

// Footer
function Footer() {
  return (
    <footer className="footer">
      <div className="footer-content">
        <div className="footer-brand">
          <div className="footer-logo">
            <Mail size={20} />
            <span>GmailMNT</span>
          </div>
          <p className="footer-tagline">Your email, reimagined.</p>
        </div>
        <div className="footer-links">
          <div className="footer-col">
            <h4>Product</h4>
            <a href="#features">Features</a>
            <a href="#how-it-works">How it Works</a>
            <a href={APP_DOWNLOAD_URL} target="_blank" rel="noopener noreferrer">Download</a>
          </div>
          <div className="footer-col">
            <h4>Legal</h4>
            <Link to="/privacy">Privacy Policy</Link>
          </div>
          <div className="footer-col">
            <h4>Support</h4>
            <a href={`mailto:${SUPPORT_EMAIL}`}>{SUPPORT_EMAIL}</a>
          </div>
        </div>
        <div className="footer-bottom">
          <p>Made with <Heart size={14} className="heart-icon" /> by GmailMNT Team</p>
          <p className="footer-copyright">&copy; {new Date().getFullYear()} GmailMNT. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
}

// Main LandingPage
export default function LandingPage() {
  return (
    <div className="landing-page">
      <Navbar />
      <HeroSection />
      <FeaturesSection />
      <HowItWorksSection />
      <SecuritySection />
      <DownloadSection />
      <Footer />
    </div>
  );
}
