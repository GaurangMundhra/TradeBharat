import React, { useState, useEffect, useCallback } from 'react';
import { walletAPI } from '../services/api';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import { useAuth } from '../hooks/useAuth';
import '../styles/pages.css';

export default function WalletPage() {
  const { user, logout } = useAuth();
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [toast, setToast] = useState(null);

  // Deposit/Withdraw form
  const [action, setAction] = useState('DEPOSIT');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [walletRes, txnRes] = await Promise.allSettled([
        walletAPI.getWallet(),
        walletAPI.getTransactions(),
      ]);
      if (walletRes.status === 'fulfilled') setWallet(walletRes.value.data?.data);
      if (txnRes.status === 'fulfilled') {
        const txns = txnRes.value.data?.data;
        setTransactions(Array.isArray(txns) ? txns : Array.isArray(txns?.content) ? txns.content : []);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const amt = parseFloat(amount);
    if (!amt || amt <= 0) {
      setToast({ type: 'error', message: 'Enter a valid amount' });
      setTimeout(() => setToast(null), 3000);
      return;
    }
    setSubmitting(true);
    try {
      if (action === 'DEPOSIT') {
        await walletAPI.deposit({ amount: amt, description: description || 'Manual deposit' });
        setToast({ type: 'success', message: `₹${amt.toFixed(2)} deposited successfully` });
      } else {
        await walletAPI.withdraw({ amount: amt, description: description || 'Manual withdrawal' });
        setToast({ type: 'success', message: `₹${amt.toFixed(2)} withdrawn successfully` });
      }
      setAmount('');
      setDescription('');
      fetchData();
    } catch (err) {
      setToast({ type: 'error', message: err.response?.data?.message || 'Transaction failed' });
    } finally {
      setSubmitting(false);
      setTimeout(() => setToast(null), 3000);
    }
  };

  const fmt = (n) => n != null ? Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00';

  return (
    <div className="dashboard">
      <Sidebar isOpen={sidebarOpen} onToggle={() => setSidebarOpen(!sidebarOpen)} />
      <main className="dashboard-main">
        <Header user={user} onLogout={logout} selectedAsset="BTC" onAssetChange={() => {}} />

        {toast && <div className={`toast toast-${toast.type}`}>{toast.message}</div>}

        <div className="page-container">
          <h1 className="page-title">💰 Wallet</h1>

          <div className="wallet-page-grid">
            {/* Balance Card */}
            <div className="card wallet-hero-card">
              <div className="wallet-hero-balance">₹{fmt(wallet?.balance)}</div>
              <div className="wallet-hero-label">Available Balance</div>
              <div className="wallet-hero-stats">
                <div className="hero-stat">
                  <span className="hero-stat-label">Total Deposits</span>
                  <span className="hero-stat-value gain">₹{fmt(wallet?.totalDeposits)}</span>
                </div>
                <div className="hero-stat">
                  <span className="hero-stat-label">Total Withdrawals</span>
                  <span className="hero-stat-value loss">₹{fmt(wallet?.totalWithdrawals)}</span>
                </div>
                <div className="hero-stat">
                  <span className="hero-stat-label">Transactions</span>
                  <span className="hero-stat-value">{wallet?.transactionCount || 0}</span>
                </div>
              </div>
            </div>

            {/* Deposit/Withdraw Form */}
            <div className="card wallet-action-card">
              <h3 className="card-title">Quick Action</h3>
              <div className="action-toggle">
                <button className={`toggle-btn ${action === 'DEPOSIT' ? 'active-deposit' : ''}`} onClick={() => setAction('DEPOSIT')}>Deposit</button>
                <button className={`toggle-btn ${action === 'WITHDRAW' ? 'active-withdraw' : ''}`} onClick={() => setAction('WITHDRAW')}>Withdraw</button>
              </div>
              <form onSubmit={handleSubmit} className="action-form">
                <div className="form-group">
                  <label>Amount (₹)</label>
                  <input type="number" step="0.01" min="0.01" value={amount} onChange={e => setAmount(e.target.value)} placeholder="Enter amount" className="form-input" />
                </div>
                <div className="form-group">
                  <label>Description (optional)</label>
                  <input type="text" value={description} onChange={e => setDescription(e.target.value)} placeholder="e.g. Salary credit" className="form-input" />
                </div>
                <button type="submit" disabled={submitting} className={`btn-submit ${action === 'DEPOSIT' ? 'btn-deposit' : 'btn-withdraw'}`}>
                  {submitting ? 'Processing...' : action === 'DEPOSIT' ? 'Deposit Funds' : 'Withdraw Funds'}
                </button>
              </form>
            </div>
          </div>

          {/* Transaction History */}
          <h2 className="section-subtitle">Transaction History</h2>
          {loading ? (
            <div className="page-loading">Loading transactions...</div>
          ) : transactions.length === 0 ? (
            <div className="page-empty">No transactions yet.</div>
          ) : (
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Type</th>
                    <th>Amount</th>
                    <th>Before</th>
                    <th>After</th>
                    <th>Description</th>
                    <th>Time</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map(txn => (
                    <tr key={txn.id}>
                      <td className="td-id">#{txn.id}</td>
                      <td><span className={`badge-txn ${txn.transactionType}`}>{txn.transactionType?.replace('_', ' ')}</span></td>
                      <td className={txn.transactionType === 'DEPOSIT' || txn.transactionType === 'TRADE_EXECUTION' ? 'gain' : 'loss'}>
                        {txn.transactionType === 'DEPOSIT' || txn.transactionType === 'TRADE_EXECUTION' ? '+' : '-'}₹{fmt(txn.amount)}
                      </td>
                      <td>₹{fmt(txn.balanceBefore)}</td>
                      <td>₹{fmt(txn.balanceAfter)}</td>
                      <td className="td-desc">{txn.description || '—'}</td>
                      <td className="td-time">{txn.createdAt ? new Date(txn.createdAt).toLocaleString('en-IN') : '—'}</td>
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
