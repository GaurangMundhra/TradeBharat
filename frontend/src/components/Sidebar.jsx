import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import '../styles/sidebar.css';

export default function Sidebar({ isOpen, onToggle }) {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
    { label: 'Dashboard', icon: '📊', href: '/dashboard', enabled: true },
    { label: 'Orders',    icon: '📋', href: '/orders',    enabled: true },
    { label: 'Trades',    icon: '💹', href: '/trades',    enabled: true },
    { label: 'Wallet',    icon: '💰', href: '/wallet',    enabled: true },
    { label: 'Settings',  icon: '⚙️', href: '/settings',  enabled: true },
  ];

  const handleLogout = () => {
    if (window.confirm('Are you sure you want to logout?')) {
      logout();
      navigate('/login');
    }
  };

  return (
    <>
      <button className="sidebar-toggle" onClick={onToggle}>
        {isOpen ? '◄' : '►'}
      </button>

      <aside className={`sidebar ${isOpen ? 'open' : 'closed'}`}>
        <div className="sidebar-header">
          <h1 className="sidebar-logo">TradeBharat</h1>
        </div>

        <nav className="sidebar-menu">
          {menuItems.map((item) => (
            <button
              key={item.label}
              className={`menu-item ${location.pathname === item.href ? 'active' : ''}`}
              title={item.label}
              onClick={() => navigate(item.href)}
            >
              <span className="menu-icon">{item.icon}</span>
              <span className="menu-label">{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <button className="logout-button" onClick={handleLogout} title="Logout">
            <span className="menu-icon">🚪</span>
            <span className="menu-label">Logout</span>
          </button>
        </div>
      </aside>
    </>
  );
}
