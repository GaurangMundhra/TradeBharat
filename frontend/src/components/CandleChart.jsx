import React, { useEffect, useRef, useState } from 'react';
import { createChart } from 'lightweight-charts';
import './CandleChart.css';

/**
 * Real-time Trading Chart Component
 * 
 * Features:
 * - Historical candle fetching via REST API
 * - Real-time candle updates via WebSocket
 * - Asset selector
 * - Current price display
 * - Error handling & automatic reconnection
 * - Smooth real-time updates (no full re-renders)
 */

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/api/ws/candles';

const CandleChart = ({
  defaultAsset = 'AAPL',
  interval = '1m',
  limit = 100
}) => {
  // State Management
  const [asset, setAsset] = useState(defaultAsset);
  const [currentPrice, setCurrentPrice] = useState(null);
  const [priceChange, setPriceChange] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [connected, setConnected] = useState(false);

  // Refs
  const containerRef = useRef(null);
  const chartRef = useRef(null);
  const candleSeriesRef = useRef(null);
  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const lastCandleTimeRef = useRef(null);
  const candlesMapRef = useRef(new Map()); // For deduplication

  // Asset options
  const ASSETS = ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NVDA', 'AMD'];

  /**
   * Initialize TradingView Lightweight Chart
   */
  const initChart = () => {
    if (!containerRef.current) return;

    // Create chart
    const chart = createChart(containerRef.current, {
      layout: {
        textColor: '#d1d5db',
        background: { color: '#1f2937' }
      },
      timeScale: {
        timeVisible: true,
        secondsVisible: true,
      },
      width: containerRef.current.clientWidth,
      height: 400,
    });

    // Create candlestick series
    const candleSeries = chart.addCandlestickSeries({
      upColor: '#00ff00',
      downColor: '#ff0000',
      borderUpColor: '#00ff00',
      borderDownColor: '#ff0000',
      wickUpColor: '#00ff00',
      wickDownColor: '#ff0000',
    });

    // Configure time scale
    chart.timeScale().fitContent();

    chartRef.current = chart;
    candleSeriesRef.current = candleSeries;

    // Handle window resize
    const handleResize = () => {
      if (containerRef.current && chartRef.current) {
        chartRef.current.applyOptions({
          width: containerRef.current.clientWidth
        });
      }
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  };

  /**
   * Fetch historical candles from REST API
   */
  const fetchHistoricalCandles = async (assetSymbol) => {
    try {
      setLoading(true);
      setError(null);

      const token = localStorage.getItem("token"); // or wherever you store JWT

const response = await fetch(
  `${API_BASE_URL}/candles?asset=${assetSymbol}&interval=${interval}&limit=${limit}`,
  {
    headers: {
      "Authorization": `Bearer ${token}`,
      "Content-Type": "application/json"
    }
  }
);

      if (!response.ok) {
        throw new Error(`Failed to fetch candles: ${response.status}`);
      }

      const responseData = await response.json();

      if (!responseData.success || !responseData.data) {
        throw new Error(responseData.message || 'Invalid response format');
      }

      const candles = responseData.data;

      if (candles.length === 0) {
        console.warn(`No candles found for ${assetSymbol}`);
        setLoading(false);
        return;
      }

      // Convert API format to chart format (reverse order: oldest first)
      const chartCandles = candles
        .reverse()
        .map(candle => ({
          time: Math.floor(candle.startTime / 1000),
          open: candle.open,
          high: candle.high,
          low: candle.low,
          close: candle.close,
          volume: candle.volume,
          fullData: candle // Store full data for reference
        }));

      // Set candles on chart
      if (candleSeriesRef.current) {
        candleSeriesRef.current.setData(chartCandles);

        // Store last candle timestamp
        if (chartCandles.length > 0) {
          lastCandleTimeRef.current = chartCandles[chartCandles.length - 1].time;

          // Update current price from last candle
          const lastCandle = chartCandles[chartCandles.length - 1];
          setCurrentPrice(lastCandle.close.toFixed(2));
        }

        // Store candles in map for update logic
        candlesMapRef.current.clear();
        chartCandles.forEach(c => {
          candlesMapRef.current.set(c.time, { ...c });
        });
      }

      setLoading(false);
    } catch (err) {
      console.error('Error fetching candles:', err);
      setError(err.message);
      setLoading(false);
    }
  };

  /**
   * Update chart with new candle from WebSocket
   */
  const updateChartWithCandle = (candle) => {
    if (!candleSeriesRef.current) return;

    const candleTime = Math.floor(candle.startTime / 1000);

    // Check if candle already exists (update) or is new
    const existingCandle = candlesMapRef.current.get(candleTime);

    if (existingCandle) {
      // Update existing candle (same interval window)
      const updatedCandle = {
        time: candleTime,
        open: candle.open,
        high: candle.high,
        low: candle.low,
        close: candle.close,
        volume: candle.volume,
        fullData: candle
      };

      candleSeriesRef.current.update(updatedCandle);
      candlesMapRef.current.set(candleTime, updatedCandle);
    } else {
      // Add new candle (new interval window)
      const newCandle = {
        time: candleTime,
        open: candle.open,
        high: candle.high,
        low: candle.low,
        close: candle.close,
        volume: candle.volume,
        fullData: candle
      };

      candleSeriesRef.current.update(newCandle);
      candlesMapRef.current.set(candleTime, newCandle);
      lastCandleTimeRef.current = candleTime;
    }

    // Update current price and change
    const prevPrice = currentPrice ? parseFloat(currentPrice) : null;
    const newPrice = candle.close;
    setCurrentPrice(newPrice.toFixed(2));

    if (prevPrice) {
      const change = newPrice - prevPrice;
      setPriceChange(change.toFixed(2));
    }
  };

  /**
   * Connect to WebSocket for real-time candle updates
   */
  const connectWebSocket = (assetSymbol) => {
    try {
      wsRef.current = new WebSocket(WS_URL);

      wsRef.current.onopen = () => {
        console.log('WebSocket connected');
        setConnected(true);

        // Subscribe to candles
        const subscribeMessage = {
          type: 'SUBSCRIBE_CANDLES',
          payload: {
            asset: assetSymbol,
            interval
          }
        };

        wsRef.current.send(JSON.stringify(subscribeMessage));
      };

      wsRef.current.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);

          if (message.type === 'CANDLE_UPDATE') {
            const candle = message.payload;

            // Only update if it's for the current asset
            if (candle.asset === assetSymbol) {
              updateChartWithCandle(candle);
            }
          }
        } catch (err) {
          console.error('Error processing WebSocket message:', err);
        }
      };

      wsRef.current.onerror = (error) => {
        console.error('WebSocket error:', error);
        setConnected(false);
        setError('WebSocket connection error');
      };

      wsRef.current.onclose = () => {
        console.log('WebSocket disconnected');
        setConnected(false);

        // Attempt reconnection after 3 seconds
        reconnectTimeoutRef.current = setTimeout(() => {
          console.log('Attempting to reconnect...');
          connectWebSocket(assetSymbol);
        }, 3000);
      };
    } catch (err) {
      console.error('Failed to connect WebSocket:', err);
      setError('Failed to connect to real-time updates');
      setConnected(false);

      // Retry after 3 seconds
      reconnectTimeoutRef.current = setTimeout(() => {
        connectWebSocket(assetSymbol);
      }, 3000);
    }
  };

  /**
   * Handle asset selector change
   */
  const handleAssetChange = async (e) => {
    const newAsset = e.target.value;
    setAsset(newAsset);
    setPriceChange(null);

    // Unsubscribe from current asset
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      const unsubscribeMessage = {
        type: 'UNSUBSCRIBE_CANDLES',
        payload: {
          asset,
          interval
        }
      };
      wsRef.current.send(JSON.stringify(unsubscribeMessage));
    }

    // Fetch new asset's data
    candlesMapRef.current.clear();
    await fetchHistoricalCandles(newAsset);

    // Subscribe to new asset
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      const subscribeMessage = {
        type: 'SUBSCRIBE_CANDLES',
        payload: {
          asset: newAsset,
          interval
        }
      };
      wsRef.current.send(JSON.stringify(subscribeMessage));
    }
  };

  /**
   * Lifecycle: Initialize chart and fetch data on mount
   */
  useEffect(() => {
    initChart();
    fetchHistoricalCandles(asset);
    connectWebSocket(asset);

    return () => {
      // Cleanup
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }

      if (wsRef.current) {
        wsRef.current.close();
      }

      if (chartRef.current) {
        chartRef.current.remove();
      }
    };
  }, []);

  /**
   * Lifecycle: Handle asset changes
   */
  useEffect(() => {
    // This is handled in handleAssetChange to control the flow
  }, [asset]);

  return (
    <div className="candle-chart-container">
      {/* Header */}
      <div className="chart-header">
        <div className="header-left">
          <h2>Trading Chart</h2>
          
          {/* Asset Selector */}
          <select 
            value={asset} 
            onChange={handleAssetChange}
            className="asset-selector"
            disabled={loading}
          >
            {ASSETS.map(a => (
              <option key={a} value={a}>{a}</option>
            ))}
          </select>
        </div>

        {/* Connection Status */}
        <div className="connection-status">
          <span className={`status-dot ${connected ? 'connected' : 'disconnected'}`}></span>
          <span className="status-text">
            {connected ? 'Live' : 'Offline'}
          </span>
        </div>
      </div>

      {/* Current Price Display */}
      <div className="price-display">
        <div className="current-price">
          <span className="label">Current Price</span>
          <span className="price">
            {currentPrice ? `$${currentPrice}` : '-'}
          </span>
        </div>

        {priceChange !== null && (
          <div className={`price-change ${priceChange >= 0 ? 'up' : 'down'}`}>
            <span className="label">Change</span>
            <span className="change">
              {priceChange >= 0 ? '+' : ''}{priceChange}
            </span>
          </div>
        )}
      </div>

      {/* Chart Container */}
      <div 
        ref={containerRef}
        className="chart-container"
        style={{ width: '100%', height: '400px' }}
      />

      {/* Loading State */}
      {loading && (
        <div className="loading-overlay">
          <div className="spinner"></div>
          <p>Loading candles...</p>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="error-message">
          <p>⚠️ {error}</p>
          <button onClick={() => setError(null)}>Dismiss</button>
        </div>
      )}

      {/* Interval Information */}
      <div className="chart-info">
        <p>Interval: <strong>{interval}</strong> | Candles: <strong>{candlesMapRef.current.size}</strong></p>
      </div>
    </div>
  );
};

export default CandleChart;
