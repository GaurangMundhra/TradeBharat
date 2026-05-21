import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import OrdersPage from './pages/OrdersPage';
import TradesPage from './pages/TradesPage';
import WalletPage from './pages/WalletPage';
import SettingsPage from './pages/SettingsPage';
import './styles/index.css';

/**
 * TradeBharat Trading Platform
 */

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>

          {/* PUBLIC ROUTE */}
          <Route path="/login" element={<LoginPage />} />

          {/* PROTECTED ROUTES */}
          <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute><OrdersPage /></ProtectedRoute>} />
          <Route path="/trades" element={<ProtectedRoute><TradesPage /></ProtectedRoute>} />
          <Route path="/wallet" element={<ProtectedRoute><WalletPage /></ProtectedRoute>} />
          <Route path="/settings" element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />

          {/* DEFAULT REDIRECT */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />

          {/* 404 */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />

        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;