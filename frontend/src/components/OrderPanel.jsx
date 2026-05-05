import React, { useState } from 'react';
import { orderAPI } from '../services/api';
import '../styles/orderpanel.css';

export default function OrderPanel({ selectedAsset, onOrderSuccess }) {
  const [orderType, setOrderType] = useState('BUY');
  const [price, setPrice] = useState('');
  const [quantity, setQuantity] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!price || !quantity) {
      setError('Price and quantity are required');
      return;
    }

    if (isNaN(price) || isNaN(quantity) || price <= 0 || quantity <= 0) {
      setError('Price and quantity must be positive numbers');
      return;
    }

    try {
      setLoading(true);
      const response = await orderAPI.placeOrder(
        selectedAsset,
        orderType,
        parseFloat(quantity),
        parseFloat(price)
      );

      setSuccess(`Order placed successfully! Order ID: ${response.data?.data?.id}`);
      setPrice('');
      setQuantity('');
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(''), 3000);
      
      // Notify parent to refresh data
      if (onOrderSuccess) {
        onOrderSuccess();
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to place order');
    } finally {
      setLoading(false);
    }
  };

  const totalAmount = (parseFloat(price) || 0) * (parseFloat(quantity) || 0);

  return (
    <div className="card order-panel">
      <h3 className="card-title">Place Order</h3>

      {error && <div className="order-error">{error}</div>}
      {success && <div className="order-success">{success}</div>}

      <form onSubmit={handleSubmit} className="order-form">
        {/* Asset Display */}
        <div className="form-row">
          <div className="form-group">
            <label>Asset</label>
            <input
              type="text"
              value={selectedAsset}
              disabled
              className="asset-display"
            />
          </div>
        </div>

        {/* Order Type Toggle */}
        <div className="form-row">
          <div className="order-type-toggle">
            <button
              type="button"
              className={`toggle-btn buy ${orderType === 'BUY' ? 'active' : ''}`}
              onClick={() => setOrderType('BUY')}
              disabled={loading}
            >
              BUY
            </button>
            <button
              type="button"
              className={`toggle-btn sell ${orderType === 'SELL' ? 'active' : ''}`}
              onClick={() => setOrderType('SELL')}
              disabled={loading}
            >
              SELL
            </button>
          </div>
        </div>

        {/* Price and Quantity */}
        <div className="form-row">
          <div className="form-group">
            <label>Price (₹)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              placeholder="0.00"
              disabled={loading}
              required
            />
          </div>
          <div className="form-group">
            <label>Quantity</label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              placeholder="0.00"
              disabled={loading}
              required
            />
          </div>
        </div>

        {/* Total Amount */}
        <div className="total-amount">
          <span>Total Amount:</span>
          <span className="amount-value">₹{totalAmount.toFixed(2)}</span>
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          className={`submit-btn ${orderType.toLowerCase()}`}
          disabled={loading}
        >
          {loading ? 'Placing Order...' : `${orderType} ${selectedAsset}`}
        </button>
      </form>
    </div>
  );
}
