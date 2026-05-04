import React, { useState } from 'react';
import CandleChart from './components/CandleChart';
import './App.css';

/**
 * Trading Dashboard Main App
 * 
 * Example implementation of CandleChart component
 * Shows historical + real-time candlestick data
 */

function App() {
  const [interval, setInterval] = useState('1m');
  const [limit, setLimit] = useState(100);

  return (
    <div className="app-container">
      {/* Navigation */}
      <header className="app-header">
        <div className="header-content">
          <h1>📈 TradeBharat Trading Platform</h1>
          <p className="subtitle">Real-time Market Data & Charts</p>
        </div>
      </header>

      {/* Main Content */}
      <main className="app-main">
        <div className="chart-section">
          {/* Chart */}
          <CandleChart 
            defaultAsset="AAPL"
            interval={interval}
            limit={limit}
          />
        </div>

        {/* Controls Sidebar */}
        <aside className="controls-sidebar">
          <h3>Chart Settings</h3>

          {/* Interval Selector */}
          <div className="control-group">
            <label htmlFor="interval-select">Candle Interval</label>
            <select 
              id="interval-select"
              value={interval} 
              onChange={(e) => setInterval(e.target.value)}
              className="control-select"
            >
              <option value="1m">1 Minute</option>
              <option value="5m">5 Minutes</option>
              <option value="15m">15 Minutes</option>
              <option value="1h">1 Hour</option>
              <option value="4h">4 Hours</option>
              <option value="1d">1 Day</option>
            </select>
          </div>

          {/* Limit Selector */}
          <div className="control-group">
            <label htmlFor="limit-select">Candles to Display</label>
            <select 
              id="limit-select"
              value={limit} 
              onChange={(e) => setLimit(parseInt(e.target.value))}
              className="control-select"
            >
              <option value={50}>Last 50</option>
              <option value={100}>Last 100</option>
              <option value={200}>Last 200</option>
              <option value={500}>Last 500</option>
            </select>
          </div>

          {/* Info Section */}
          <div className="info-section">
            <h4>Features</h4>
            <ul className="feature-list">
              <li>✅ Real-time candle updates</li>
              <li>✅ Historical data fetching</li>
              <li>✅ Live price display</li>
              <li>✅ Auto-reconnection</li>
              <li>✅ Asset switching</li>
              <li>✅ Error handling</li>
            </ul>
          </div>

          {/* API Info */}
          <div className="api-info">
            <h4>API Endpoints</h4>
            <code className="code-block">GET /api/candles</code>
            <p className="api-desc">Fetches historical candles</p>
            
            <h4>WebSocket</h4>
            <code className="code-block">ws://localhost:8080/api/ws/candles</code>
            <p className="api-desc">Live candle updates</p>
          </div>
        </aside>
      </main>

      {/* Footer */}
      <footer className="app-footer">
        <p>TradeBharat © 2026 | Real-time Trading Platform</p>
      </footer>
    </div>
  );
}

export default App;
