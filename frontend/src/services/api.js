import axios from "axios";

// Base URL from environment or default
const API_BASE_URL =
  process.env.REACT_APP_API_URL || "http://localhost:8080/api";

// Create axios instance
const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor - Add JWT token
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("authToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// Response interceptor - Handle auth errors
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid - logout
      localStorage.removeItem("authToken");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);

// ==================== AUTHENTICATION ENDPOINTS ====================

export const authAPI = {
  register: (data) => axiosInstance.post("/auth/register", data),

  login: (username, password) =>
    axiosInstance.post("/auth/login", { username, password }),
};

// ==================== WALLET ENDPOINTS ====================

export const walletAPI = {
  // GET /api/wallet — returns full WalletResponse
  getWallet: () => axiosInstance.get("/wallet"),

  // GET /api/wallet/balance — returns raw BigDecimal
  getBalance: () => axiosInstance.get("/wallet/balance"),

  // POST /api/wallet/deposit — { amount, description }
  deposit: (data) => axiosInstance.post("/wallet/deposit", data),

  // POST /api/wallet/withdraw — { amount, description }
  withdraw: (data) => axiosInstance.post("/wallet/withdraw", data),

  // GET /api/wallet/transactions/all — all transactions
  getTransactions: () => axiosInstance.get("/wallet/transactions/all"),
};

// ==================== ORDER ENDPOINTS ====================

export const orderAPI = {
  // POST /api/orders — { asset, type, price, quantity }
  placeOrder: (asset, type, quantity, price) =>
    axiosInstance.post("/orders", { asset, type, quantity, price }),

  // GET /api/orders — returns Page<OrderResponse>
  getOrders: () => axiosInstance.get("/orders"),

  // GET /api/orders/{orderId}
  getOrderById: (orderId) => axiosInstance.get(`/orders/${orderId}`),

  // DELETE /api/orders/{orderId}
  cancelOrder: (orderId) => axiosInstance.delete(`/orders/${orderId}`),

  // GET /api/orders/active
  getActiveOrders: () => axiosInstance.get("/orders/active"),

  // GET /api/orders/list/all
  getAllOrders: () => axiosInstance.get("/orders/list/all"),
};

// ==================== TRADE ENDPOINTS ====================

export const tradeAPI = {
  // GET /api/trades/me
  getUserTrades: () => axiosInstance.get("/trades/me"),

  // GET /api/trades/asset/{asset}
  getTrades: (asset, limit = 100) =>
    axiosInstance.get(`/trades/asset/${asset}`, { params: { limit } }),
};

// ==================== PORTFOLIO ENDPOINTS ====================

export const portfolioAPI = {
  // GET /api/settlements/portfolio/me
  getSummary: () => axiosInstance.get("/settlements/portfolio/me"),
};

export default axiosInstance;
