import React, { createContext, useState, useCallback, useEffect } from "react";
import { authAPI } from "../services/api";

export const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Initialize from localStorage
  useEffect(() => {
    const storedToken = localStorage.getItem("authToken");
    const storedUser = localStorage.getItem("user");

    if (storedToken && storedUser && storedUser !== "undefined") {
      try {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
      } catch (e) {
        console.warn("Invalid user in localStorage, clearing...");
        localStorage.removeItem("user");
        localStorage.removeItem("authToken");
      }
    }

    setLoading(false);
  }, []);

  // Register user
  const register = useCallback(async (data) => {
    try {
      setError(null);
      setLoading(true);

      const response = await authAPI.register(data);

      // Backend returns: { success: true, data: { token } }
      const authToken = response.data?.data?.token;

      if (!authToken) {
        throw new Error("No token in response");
      }

      localStorage.setItem("authToken", authToken);
      localStorage.setItem("user", JSON.stringify({ username: data.username }));

      setToken(authToken);
      setUser({ username: data.username });

      return { success: true };
    } catch (err) {
      const message =
        err.response?.data?.message ||
        JSON.stringify(err.response?.data) ||
        "Registration failed";

      setError(message);
      return { success: false, error: message };
    } finally {
      setLoading(false);
    }
  }, []);

  // Login user
  const login = useCallback(async (username, password) => {
    try {
      setError(null);
      setLoading(true);

      const response = await authAPI.login(username, password);

      // Backend returns: { success: true, data: { token } }
      const authToken = response.data?.data?.token;

      if (!authToken) {
        throw new Error("No token in response");
      }

      localStorage.setItem("authToken", authToken);
      localStorage.setItem("user", JSON.stringify({ username }));

      setToken(authToken);
      setUser({ username });

      return { success: true };
    } catch (err) {
      const message =
        err.response?.data?.message ||
        JSON.stringify(err.response?.data) ||
        "Login failed";

      setError(message);
      return { success: false, error: message };
    } finally {
      setLoading(false);
    }
  }, []);

  // Logout user
  const logout = useCallback(() => {
    localStorage.removeItem("authToken");
    localStorage.removeItem("user");
    setToken(null);
    setUser(null);
    setError(null);
  }, []);


  const value = {
    user,
    token,
    loading,
    error,
    register,
    login,
    logout,
    isAuthenticated: !!token,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
