import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import '../styles/auth.css';
import { useNavigate } from 'react-router-dom';

export default function LoginPage() {
  const { login, register, loading, error } = useAuth();

  const [username, setUsername] = useState('');   // ✅ FIX
  const [email, setEmail] = useState('');         // used only for register
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLogin, setIsLogin] = useState(true);
  const [localError, setLocalError] = useState('');
const navigate = useNavigate();
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLocalError('');

    if (!username || !password) {
      setLocalError('Username and password are required');
      return;
    }

    if (!isLogin) {
      if (!email) {
        setLocalError('Email is required');
        return;
      }

      if (!confirmPassword) {
        setLocalError('Please confirm your password');
        return;
      }

      if (password !== confirmPassword) {
        setLocalError('Passwords do not match');
        return;
      }
    }

    const result = isLogin
      ? await login(username, password)   // ✅ FIX
      : await register({
          username: username,             // ✅ FIX
          email: email.toLowerCase(),
          password,
          confirmPassword
        });

    if (result.success) {
  // small delay ensures state update
  setTimeout(() => {
    navigate('/dashboard');
  }, 100);
} else {
  setLocalError(result.error || 'An error occurred');
}
  };

  return (
    <div className="auth-container">
      <div className="auth-box">
        <h1 className="auth-title">TradeBharat</h1>
        <p className="auth-subtitle">{isLogin ? 'Login' : 'Create Account'}</p>

        {(error || localError) && (
          <div className="auth-error">{error || localError}</div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">

          {/* Username (ALWAYS REQUIRED) */}
          <div className="form-group">
            <label>Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter username"
              disabled={loading}
            />
          </div>

          {/* Registration Only Fields */}
          {!isLogin && (
            <>
              <div className="form-group">
                <label>Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="your@email.com"
                  disabled={loading}
                />
              </div>
            </>
          )}

          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              disabled={loading}
            />
          </div>

          {!isLogin && (
            <div className="form-group">
              <label>Confirm Password</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="••••••••"
                disabled={loading}
              />
            </div>
          )}

          <button type="submit" className="auth-button" disabled={loading}>
            {loading ? 'Loading...' : (isLogin ? 'Login' : 'Register')}
          </button>
        </form>

        <div className="auth-toggle">
          <p>
            {isLogin ? "Don't have an account? " : 'Already have an account? '}
            <button
              type="button"
              onClick={() => {
                setIsLogin(!isLogin);
                setLocalError('');
              }}
              disabled={loading}
              className="toggle-button"
            >
              {isLogin ? 'Register' : 'Login'}
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}