import React from 'react';

export default function TradesCard({ trades }) {
  return (
    <div className="card trades-card">
      <h3 className="card-title">Recent Trades</h3>
      <div className="trades-list">
        {trades.map((trade, idx) => (
          <div key={idx} className="trade-item">
            <div className="trade-info">
              <span className="trade-asset">{trade.asset}</span>
              <span className="trade-price">₹{trade.price?.toFixed(2)}</span>
            </div>
            <div className="trade-amount">
              <span>×{trade.quantity}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
