import React, { useState, useEffect, useCallback } from 'react';
import { tradeAPI } from '../services/api';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import { useAuth } from '../hooks/useAuth';
import '../styles/pages.css';

export default function TradesPage() {
  const { user, logout } = useAuth();
  const [trades, setTrades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const fetchTrades = useCallback(async () => {
    try {
      setLoading(true);
      const res = await tradeAPI.getUserTrades();
      const data = res.data?.data;
      setTrades(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to fetch trades:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchTrades(); }, [fetchTrades]);

  const fmt = (n) => n != null ? Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '—';

  const totalVolume = trades.reduce((sum, t) => sum + (Number(t.price) * Number(t.quantity)), 0);

  return (
    <div className="dashboard">
      <Sidebar isOpen={sidebarOpen} onToggle={() => setSidebarOpen(!sidebarOpen)} />
      <main className="dashboard-main">
        <Header user={user} onLogout={logout} selectedAsset="BTC" onAssetChange={() => {}} />

        <div className="page-container">
          <div className="page-header">
            <h1 className="page-title">💹 Trade History</h1>
            <button className="btn-refresh" onClick={fetchTrades}>↻ Refresh</button>
          </div>

          <div className="page-stats">
            <div className="stat-chip">Total Trades: {trades.length}</div>
            <div className="stat-chip">Volume: ₹{fmt(totalVolume)}</div>
          </div>

          {loading ? (
            <div className="page-loading">Loading trades...</div>
          ) : trades.length === 0 ? (
            <div className="page-empty">No trades executed yet. Place matching BUY and SELL orders to generate trades!</div>
          ) : (
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Asset</th>
                    <th>Price</th>
                    <th>Quantity</th>
                    <th>Total Value</th>
                    <th>Buyer</th>
                    <th>Seller</th>
                    <th>Time</th>
                  </tr>
                </thead>
                <tbody>
                  {trades.map(trade => (
                    <tr key={trade.id}>
                      <td className="td-id">#{trade.id}</td>
                      <td className="td-asset">{trade.asset}</td>
                      <td>₹{fmt(trade.price)}</td>
                      <td>{fmt(trade.quantity)}</td>
                      <td className="td-asset">₹{fmt(Number(trade.price) * Number(trade.quantity))}</td>
                      <td>{trade.buyerUsername || trade.buyerId}</td>
                      <td>{trade.sellerUsername || trade.sellerId}</td>
                      <td className="td-time">{trade.createdAt ? new Date(trade.createdAt).toLocaleString('en-IN') : '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
