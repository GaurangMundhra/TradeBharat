import React from 'react';
import '../styles/header.css';

export default function Header({ user, onLogout, selectedAsset, onAssetChange }) {
  const assets = ['BTC', 'ETH', 'AAPL', 'GOOGL', 'MSFT', 'TSLA'];

  return (
    <header className="header">
      <div className="header-left">
        <h2 className="header-title">Trading Dashboard</h2>
      </div>

      <div className="header-center">
        <div className="asset-selector">
          <label htmlFor="asset-select" className="selector-label">Asset:</label>
          <select
            id="asset-select"
            value={selectedAsset}
            onChange={(e) => onAssetChange(e.target.value)}
            className="asset-select"
          >
            {assets.map((asset) => (
              <option key={asset} value={asset}>
                {asset}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="header-right">
        <div className="user-info">
          <div className="user-avatar">{user?.username?.charAt(0)?.toUpperCase() || 'U'}</div>
          <div className="user-details">
            <p className="user-name">{user?.username || 'User'}</p>
          </div>
        </div>

        <button className="logout-btn" onClick={onLogout} title="Logout">
          🚪
        </button>
      </div>
    </header>
  );
}
