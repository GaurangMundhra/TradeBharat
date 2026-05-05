import React from 'react';

export default function WalletCard({ wallet }) {
  // Backend WalletResponse shape: { balance, totalDeposits, totalWithdrawals, transactionCount, ... }
  const balance = wallet?.balance ?? 0;
  const totalDeposits = wallet?.totalDeposits ?? 0;
  const totalWithdrawals = wallet?.totalWithdrawals ?? 0;

  const fmt = (n) => Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  return (
    <div className="card wallet-card">
      <h3 className="card-title">Wallet</h3>
      <div className="wallet-balance">₹{fmt(balance)}</div>
      <div className="wallet-info">
        <div className="wallet-stat">
          <span className="wallet-stat-label">Total Deposits</span>
          <span className="wallet-stat-value">₹{fmt(totalDeposits)}</span>
        </div>
        <div className="wallet-stat">
          <span className="wallet-stat-label">Total Withdrawals</span>
          <span className="wallet-stat-value">₹{fmt(totalWithdrawals)}</span>
        </div>
      </div>
    </div>
  );
}
