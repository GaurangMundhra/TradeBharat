import React, { useState, useEffect, useCallback } from 'react';
import { orderAPI } from '../services/api';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import { useAuth } from '../hooks/useAuth';
import '../styles/pages.css';

export default function OrdersPage() {
  const { user, logout } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');
  const [assetFilter, setAssetFilter] = useState('ALL');
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [toast, setToast] = useState(null);

  const fetchOrders = useCallback(async () => {
    try {
      setLoading(true);
      const res = await orderAPI.getAllOrders();
      const data = res.data?.data;
      setOrders(Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : []);
    } catch (err) {
      console.error('Failed to fetch orders:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  const handleCancel = async (orderId) => {
    if (!window.confirm('Cancel this order?')) return;
    try {
      await orderAPI.cancelOrder(orderId);
      setToast({ type: 'success', message: 'Order cancelled successfully' });
      fetchOrders();
    } catch (err) {
      setToast({ type: 'error', message: err.response?.data?.message || 'Failed to cancel order' });
    }
    setTimeout(() => setToast(null), 3000);
  };

  const filtered = orders.filter(o => {
    if (filter !== 'ALL' && o.status !== filter) return false;
    if (assetFilter !== 'ALL' && o.asset !== assetFilter) return false;
    return true;
  });

  const assets = [...new Set(orders.map(o => o.asset))];
  const fmt = (n) => n != null ? Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '—';

  return (
    <div className="dashboard">
      <Sidebar isOpen={sidebarOpen} onToggle={() => setSidebarOpen(!sidebarOpen)} />
      <main className="dashboard-main">
        <Header user={user} onLogout={logout} selectedAsset="BTC" onAssetChange={() => {}} />
        
        {toast && <div className={`toast toast-${toast.type}`}>{toast.message}</div>}

        <div className="page-container">
          <div className="page-header">
            <h1 className="page-title">📋 Order History</h1>
            <div className="page-filters">
              <select value={filter} onChange={e => setFilter(e.target.value)} className="filter-select">
                <option value="ALL">All Status</option>
                <option value="OPEN">Open</option>
                <option value="PARTIAL">Partial</option>
                <option value="FILLED">Filled</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
              <select value={assetFilter} onChange={e => setAssetFilter(e.target.value)} className="filter-select">
                <option value="ALL">All Assets</option>
                {assets.map(a => <option key={a} value={a}>{a}</option>)}
              </select>
              <button className="btn-refresh" onClick={fetchOrders}>↻ Refresh</button>
            </div>
          </div>

          {loading ? (
            <div className="page-loading">Loading orders...</div>
          ) : filtered.length === 0 ? (
            <div className="page-empty">No orders found. Place your first trade on the Dashboard!</div>
          ) : (
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Asset</th>
                    <th>Type</th>
                    <th>Price</th>
                    <th>Quantity</th>
                    <th>Filled</th>
                    <th>Status</th>
                    <th>Time</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(order => (
                    <tr key={order.id}>
                      <td className="td-id">#{order.id}</td>
                      <td className="td-asset">{order.asset}</td>
                      <td><span className={`badge-type ${order.type}`}>{order.type}</span></td>
                      <td>₹{fmt(order.price)}</td>
                      <td>{fmt(order.quantity)}</td>
                      <td>{fmt(order.filledQuantity)}</td>
                      <td><span className={`badge-status ${order.status}`}>{order.status}</span></td>
                      <td className="td-time">{order.createdAt ? new Date(order.createdAt).toLocaleString('en-IN') : '—'}</td>
                      <td>
                        {(order.status === 'OPEN' || order.status === 'PARTIAL') && (
                          <button className="btn-cancel" onClick={() => handleCancel(order.id)}>Cancel</button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="page-stats">
            <div className="stat-chip">Total: {orders.length}</div>
            <div className="stat-chip open">Open: {orders.filter(o => o.status === 'OPEN').length}</div>
            <div className="stat-chip filled">Filled: {orders.filter(o => o.status === 'FILLED').length}</div>
            <div className="stat-chip cancelled">Cancelled: {orders.filter(o => o.status === 'CANCELLED').length}</div>
          </div>
        </div>
      </main>
    </div>
  );
}
