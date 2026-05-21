import React, { useState } from 'react';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import { useAuth } from '../hooks/useAuth';
import '../styles/pages.css';

export default function SettingsPage() {
  const { user, logout } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  return (
    <div className="dashboard">
      <Sidebar isOpen={sidebarOpen} onToggle={() => setSidebarOpen(!sidebarOpen)} />
      <main className="dashboard-main">
        <Header user={user} onLogout={logout} selectedAsset="BTC" onAssetChange={() => {}} />

        <div className="page-container">
          <h1 className="page-title">⚙️ Settings</h1>

          <div className="settings-grid">
            {/* Profile Card */}
            <div className="card settings-card">
              <h3 className="card-title">Profile</h3>
              <div className="settings-row">
                <span className="settings-label">Username</span>
                <span className="settings-value">{user?.username || '—'}</span>
              </div>
              <div className="settings-row">
                <span className="settings-label">Email</span>
                <span className="settings-value">{user?.email || '—'}</span>
              </div>
              <div className="settings-row">
                <span className="settings-label">User ID</span>
                <span className="settings-value">#{user?.userId || '—'}</span>
              </div>
              <div className="settings-row">
                <span className="settings-label">Role</span>
                <span className="settings-value">{user?.role || 'USER'}</span>
              </div>
            </div>

            {/* Platform Info */}
            <div className="card settings-card">
              <h3 className="card-title">Platform</h3>
              <div className="settings-row">
                <span className="settings-label">Version</span>
                <span className="settings-value">1.0.0</span>
              </div>
              <div className="settings-row">
                <span className="settings-label">Backend</span>
                <span className="settings-value">Spring Boot 3.2.1</span>
              </div>
              <div className="settings-row">
                <span className="settings-label">Frontend</span>
                <span className="settings-value">React 18</span>
              </div>
              <div className="settings-row">
                <span className="settings-label">Initial Paper Money</span>
                <span className="settings-value">₹1,00,000</span>
              </div>
              <div className="settings-row">
                <span className="settings-label">Self-Trade</span>
                <span className="settings-value" style={{color: '#4adc81'}}>Enabled</span>
              </div>
            </div>

            {/* Supported Assets */}
            <div className="card settings-card">
              <h3 className="card-title">Supported Assets</h3>
              <div className="asset-grid">
                {['BTC', 'ETH', 'AAPL', 'GOOGL', 'MSFT', 'TSLA'].map(asset => (
                  <div key={asset} className="asset-chip">{asset}</div>
                ))}
              </div>
            </div>

            {/* Danger Zone */}
            <div className="card settings-card danger-card">
              <h3 className="card-title">Session</h3>
              <p className="settings-desc">Logging out will clear your JWT token. You'll need to login again.</p>
              <button className="btn-danger" onClick={() => { if (window.confirm('Logout?')) { logout(); } }}>
                Logout
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
