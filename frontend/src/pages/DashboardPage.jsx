import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../hooks/useAuth';
import { orderAPI, walletAPI, tradeAPI, portfolioAPI } from '../services/api';
import Sidebar from '../components/Sidebar';
import Header from '../components/Header';
import WalletCard from '../components/WalletCard';
import OrdersCard from '../components/OrdersCard';
import TradesCard from '../components/TradesCard';
import OrderBook from '../components/OrderBook';
import OrderPanel from '../components/OrderPanel';
import CandleChart from '../components/CandleChart';
import '../styles/dashboard.css';

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const [wallet, setWallet] = useState(null);
  const [portfolio, setPortfolio] = useState(null);
  const [orders, setOrders] = useState([]);
  const [trades, setTrades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedAsset, setSelectedAsset] = useState('BTC');
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const fetchDashboardData = useCallback(async () => {
    try {
      setLoading(true);

      const results = await Promise.allSettled([
        walletAPI.getWallet(),      // Full WalletResponse (not /balance which returns raw number)
        portfolioAPI.getSummary(),   // PortfolioResponse with positions
        orderAPI.getOrders(),        // Page<OrderResponse>
        tradeAPI.getUserTrades(),    // List<TradeResponse>
      ]);

      // Wallet — backend returns { success, data: { balance, totalDeposits, totalWithdrawals, ... } }
      if (results[0].status === 'fulfilled') {
        setWallet(results[0].value.data?.data);
      }

      // Portfolio — backend returns { success, data: { cashBalance, portfolioValue, positions, positionCount, ... } }
      if (results[1].status === 'fulfilled') {
        setPortfolio(results[1].value.data?.data);
      }

      // Orders — backend returns Page<OrderResponse> with .content array
      if (results[2].status === 'fulfilled') {
        const ordersData = results[2].value.data?.data;
        const ordersList = Array.isArray(ordersData)
          ? ordersData
          : Array.isArray(ordersData?.content)
            ? ordersData.content
            : [];
        setOrders(ordersList.slice(0, 5));
      }

      // Trades
      if (results[3].status === 'fulfilled') {
        const tradesData = results[3].value.data?.data;
        setTrades(Array.isArray(tradesData) ? tradesData.slice(0, 10) : []);
      }

      setError('');
    } catch (err) {
      setError('Failed to load dashboard data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 30000);
    return () => clearInterval(interval);
  }, [fetchDashboardData]);

  const handleOrderPlaced = async () => {
    try {
      const [updatedOrders, updatedWallet, updatedPortfolio] = await Promise.all([
        orderAPI.getOrders(),
        walletAPI.getWallet(),
        portfolioAPI.getSummary(),
      ]);

      const ordersData = updatedOrders.data?.data;
      const ordersList = Array.isArray(ordersData)
        ? ordersData
        : Array.isArray(ordersData?.content)
          ? ordersData.content
          : [];
      setOrders(ordersList.slice(0, 5));
      setWallet(updatedWallet.data?.data);
      setPortfolio(updatedPortfolio.data?.data);
    } catch (err) {
      console.error('Failed to refresh data after order:', err);
    }
  };

  // Helper for formatting currency
  const fmt = (n) => n != null ? Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '—';

  if (loading && !wallet) {
    return <div className="loading-page">Loading dashboard...</div>;
  }

  return (
    <div className="dashboard">
      <Sidebar isOpen={sidebarOpen} onToggle={() => setSidebarOpen(!sidebarOpen)} />

      <main className="dashboard-main">
        <Header 
          user={user} 
          onLogout={logout}
          selectedAsset={selectedAsset}
          onAssetChange={setSelectedAsset}
        />

        {error && <div className="error-banner">{error}</div>}

        <div className="dashboard-grid">
          {/* Left Column - Charts and Order Book */}
          <div className="dashboard-left">
            {/* Candlestick Chart */}
            <div className="chart-container">
              <h2 className="section-title">{selectedAsset} Chart</h2>
              <CandleChart asset={selectedAsset} interval="1m" />
            </div>

            {/* Order Book */}
            <div className="orderbook-container">
              <h2 className="section-title">Order Book - {selectedAsset}</h2>
              <OrderBook asset={selectedAsset} />
            </div>
          </div>

          {/* Right Column - Panels and Cards */}
          <div className="dashboard-right">
            {/* Order Placement Panel */}
            <OrderPanel 
              selectedAsset={selectedAsset}
              onOrderSuccess={handleOrderPlaced}
            />

            {/* Wallet Card */}
            {wallet && <WalletCard wallet={wallet} />}

            {/* Portfolio Summary */}
            {portfolio && (
              <div className="card portfolio-card">
                <h3 className="card-title">Portfolio</h3>
                <div className="portfolio-stats">
                  <div className="stat">
                    <span className="stat-label">Cash Balance</span>
                    <span className="stat-value">₹{fmt(portfolio.cashBalance)}</span>
                  </div>
                  <div className="stat">
                    <span className="stat-label">Portfolio Value</span>
                    <span className="stat-value">₹{fmt(portfolio.portfolioValue)}</span>
                  </div>
                  <div className="stat">
                    <span className="stat-label">Unrealized P&L</span>
                    <span className={`stat-value ${(portfolio.unrealizedPnL ?? 0) >= 0 ? 'gain' : 'loss'}`}>
                      ₹{fmt(portfolio.unrealizedPnL)}
                    </span>
                  </div>
                </div>

                {/* Holdings / Positions */}
                {portfolio.positions && portfolio.positions.length > 0 && (
                  <div className="holdings-section">
                    <h4 className="holdings-title">Holdings ({portfolio.positionCount || portfolio.positions.length})</h4>
                    <div className="holdings-list">
                      {portfolio.positions.map((pos, idx) => (
                        <div key={pos.id || idx} className="holding-item">
                          <div className="holding-asset">
                            <span className="holding-symbol">{pos.asset}</span>
                            <span className="holding-qty">{Number(pos.quantity).toFixed(2)} units</span>
                          </div>
                          <div className="holding-values">
                            <span className="holding-avg">Avg: ₹{fmt(pos.averageCost)}</span>
                            {pos.currentValue != null && (
                              <span className={`holding-pnl ${(pos.unrealizedPnL ?? 0) >= 0 ? 'gain' : 'loss'}`}>
                                P&L: ₹{fmt(pos.unrealizedPnL)}
                              </span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {(!portfolio.positions || portfolio.positions.length === 0) && (
                  <div className="holdings-empty">
                    <p>No holdings yet. Place your first trade!</p>
                  </div>
                )}
              </div>
            )}

            {/* Orders Summary */}
            {orders.length > 0 && <OrdersCard orders={orders} />}

            {/* Recent Trades */}
            {trades.length > 0 && <TradesCard trades={trades} />}
          </div>
        </div>

        {/* Full Width - Recent Trades Table */}
        {trades.length > 0 && (
          <div className="trades-table-container">
            <h2 className="section-title">Recent Market Trades</h2>
            <table className="trades-table">
              <thead>
                <tr>
                  <th>Asset</th>
                  <th>Price</th>
                  <th>Quantity</th>
                  <th>Type</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>
                {trades.map((trade, idx) => (
                  <tr key={trade.id || idx}>
                    <td className="asset">{trade.asset}</td>
                    <td>₹{Number(trade.price).toFixed(2)}</td>
                    <td>{trade.quantity}</td>
                    <td>
                      <span className={`type ${trade.type?.toLowerCase()}`}>
                        {trade.type}
                      </span>
                    </td>
                    <td className="time">{new Date(trade.timestamp).toLocaleTimeString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
