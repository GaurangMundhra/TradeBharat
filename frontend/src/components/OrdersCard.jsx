import React from 'react';

export default function OrdersCard({ orders }) {
  return (
    <div className="card orders-card">
      <h3 className="card-title">Recent Orders</h3>
      <div className="orders-list">
        {orders.map((order) => (
          <div key={order.id} className="order-item">
            <div className="order-info">
              <span className={`order-type ${order.type}`}>{order.type}</span>
              <span className="order-price">₹{order.price?.toFixed(2)}</span>
            </div>
            <div className="order-status">
              <span className={`status-badge ${order.status}`}>{order.status}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
