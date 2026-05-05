import React, { useEffect, useRef, useState, useCallback } from 'react';
import { createChart, CrosshairMode, LineStyle } from 'lightweight-charts';
import axiosInstance from '../services/api';
import './CandleChart.css';

const WS_BASE_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/api/ws';

const INTERVALS = [
  { label: '1m', value: '1m' },
  { label: '5m', value: '5m' },
  { label: '15m', value: '15m' },
  { label: '1h', value: '1h' },
  { label: '1D', value: '1d' },
];

const CHART_THEMES = {
  dark: {
    background: '#0d1117',
    surface: '#161b22',
    surfaceElevated: '#1c2230',
    border: '#30363d',
    text: '#e6edf3',
    textMuted: '#7d8590',
    accent: '#58a6ff',
    green: '#3fb950',
    red: '#f85149',
    gridLine: '#21262d',
  },
};

const theme = CHART_THEMES.dark;

/**
 * CandleChart — Controlled component.
 *
 * Props:
 *   asset    – trading symbol (e.g. "BTC"), controlled by parent
 *   interval – candle interval (default "1m"), controlled by parent (optional)
 *   limit    – max historical candles to fetch (default 200)
 */
const CandleChart = ({ asset = 'BTC', interval: intervalProp = '1m', limit = 200 }) => {
  // Local interval state so the user can switch intervals within the chart
  // while the parent only controls the asset.
  const [chartInterval, setChartInterval] = useState(intervalProp);
  const [currentPrice, setCurrentPrice] = useState(null);
  const [openPrice, setOpenPrice] = useState(null);
  const [high, setHigh] = useState(null);
  const [low, setLow] = useState(null);
  const [priceChange, setPriceChange] = useState(null);
  const [pctChange, setPctChange] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [connected, setConnected] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [candleCount, setCandleCount] = useState(0);

  const containerRef = useRef(null);
  const chartRef = useRef(null);
  const candleSeriesRef = useRef(null);
  const volumeSeriesRef = useRef(null);
  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const reconnectAttemptsRef = useRef(0);
  const currentPriceRef = useRef(null);
  const sessionOpenRef = useRef(null);
  // Track whether the component is still mounted to prevent reconnect after unmount
  const mountedRef = useRef(true);

  /* ── Chart Init ── */
  const destroyChart = useCallback(() => {
    if (chartRef.current) {
      try { chartRef.current.remove(); } catch (_) {}
      chartRef.current = null;
      candleSeriesRef.current = null;
      volumeSeriesRef.current = null;
    }
  }, []);

  const initChart = useCallback(() => {
    if (!containerRef.current) return;
    destroyChart();

    const chart = createChart(containerRef.current, {
      layout: {
        textColor: theme.textMuted,
        background: { color: theme.background },
        fontFamily: "'IBM Plex Mono', monospace",
        fontSize: 11,
      },
      width: containerRef.current.clientWidth,
      height: 420,
      timeScale: {
        timeVisible: true,
        secondsVisible: chartInterval === '1m' || chartInterval === '5m',
        barSpacing: 10,
        rightBarStaysOnScroll: true,
        shiftVisibleRangeOnNewBar: true,
        borderColor: theme.border,
        fixLeftEdge: false,
        lockVisibleTimeRangeOnResize: true,
      },
      crosshair: {
        mode: CrosshairMode.Normal,
        vertLine: {
          color: `${theme.textMuted}80`,
          width: 1,
          style: LineStyle.Dashed,
          labelBackgroundColor: theme.surfaceElevated,
        },
        horzLine: {
          color: `${theme.textMuted}80`,
          width: 1,
          style: LineStyle.Dashed,
          labelBackgroundColor: theme.surfaceElevated,
        },
      },
      grid: {
        vertLines: { color: theme.gridLine, style: LineStyle.Dotted },
        horzLines: { color: theme.gridLine, style: LineStyle.Dotted },
      },
      rightPriceScale: {
        borderColor: theme.border,
        scaleMargins: { top: 0.08, bottom: 0.28 },
      },
      leftPriceScale: { visible: false },
      handleScroll: { mouseWheel: true, pressedMouseMove: true, horzTouchDrag: true },
      handleScale: { mouseWheel: true, pinch: true },
    });

    const candleSeries = chart.addCandlestickSeries({
      upColor: theme.green,
      downColor: theme.red,
      borderUpColor: theme.green,
      borderDownColor: theme.red,
      wickUpColor: `${theme.green}99`,
      wickDownColor: `${theme.red}99`,
    });

    const volumeSeries = chart.addHistogramSeries({
      priceFormat: { type: 'volume' },
      priceScaleId: 'volume',
    });

    chart.priceScale('volume').applyOptions({
      scaleMargins: { top: 0.78, bottom: 0 },
      borderVisible: false,
    });

    chart.timeScale().fitContent();

    chartRef.current = chart;
    candleSeriesRef.current = candleSeries;
    volumeSeriesRef.current = volumeSeries;

    const handleResize = () => {
      if (containerRef.current && chartRef.current) {
        chartRef.current.applyOptions({ width: containerRef.current.clientWidth });
      }
    };
    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
      destroyChart();
    };
  }, [chartInterval, destroyChart]);

  /* ── Fetch Historical via shared axiosInstance (auto-attaches Bearer token) ── */
  const fetchHistoricalCandles = useCallback(async (sym, ivl) => {
    if (!candleSeriesRef.current) return;
    try {
      setLoading(true);
      setError(null);

      const response = await axiosInstance.get('/candles', {
        params: { asset: sym, interval: ivl, limit },
      });

      // Backend returns: { success: true, data: [...candles] }
      const candles = response.data?.data || [];
      const sorted = [...candles].reverse();

      const chartCandles = sorted.map(c => ({
        time: Math.floor(c.startTime / 1000),
        open: c.open,
        high: c.high,
        low: c.low,
        close: c.close,
      }));

      const volumeData = sorted.map(c => ({
        time: Math.floor(c.startTime / 1000),
        value: c.volume ?? 0,
        color: c.close >= c.open ? `${theme.green}55` : `${theme.red}55`,
      }));

      candleSeriesRef.current.setData(chartCandles);
      volumeSeriesRef.current?.setData(volumeData);
      chartRef.current?.timeScale().fitContent();

      if (chartCandles.length > 0) {
        const last = chartCandles[chartCandles.length - 1];
        const first = chartCandles[0];

        currentPriceRef.current = last.close;
        sessionOpenRef.current = first.open;

        setCurrentPrice(last.close);
        setOpenPrice(first.open);
        setHigh(Math.max(...chartCandles.map(c => c.high)));
        setLow(Math.min(...chartCandles.map(c => c.low)));
        setCandleCount(chartCandles.length);

        const delta = last.close - first.open;
        const pct = (delta / first.open) * 100;
        setPriceChange(delta);
        setPctChange(pct);
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load candles');
    } finally {
      setLoading(false);
    }
  }, [limit]);

  /* ── Live Update ── */
  const updateChartWithCandle = useCallback((candle) => {
    if (!candleSeriesRef.current) return;

    const bar = {
      time: Math.floor(candle.startTime / 1000),
      open: candle.open,
      high: candle.high,
      low: candle.low,
      close: candle.close,
    };

    candleSeriesRef.current.update(bar);

    if (volumeSeriesRef.current) {
      volumeSeriesRef.current.update({
        time: bar.time,
        value: candle.volume ?? 0,
        color: candle.close >= candle.open ? `${theme.green}55` : `${theme.red}55`,
      });
    }

    currentPriceRef.current = candle.close;
    setCurrentPrice(candle.close);
    setLastUpdated(new Date());

    if (sessionOpenRef.current !== null) {
      const delta = candle.close - sessionOpenRef.current;
      const pct = (delta / sessionOpenRef.current) * 100;
      setPriceChange(delta);
      setPctChange(pct);
    }
  }, []);

  /* ── WebSocket ── */
  const connectWebSocket = useCallback((sym, ivl) => {
    if (wsRef.current) {
      wsRef.current.onclose = null; // prevent reconnect on intentional close
      wsRef.current.close();
    }

    const url = `${WS_BASE_URL}/candles`;
    const ws = new WebSocket(url);
    wsRef.current = ws;

    ws.onopen = () => {
      reconnectAttemptsRef.current = 0;
      setConnected(true);
      ws.send(JSON.stringify({
        type: 'SUBSCRIBE_CANDLES',
        payload: { asset: sym, interval: ivl },
      }));
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        if (msg.type === 'CANDLE_UPDATE') updateChartWithCandle(msg.payload);
      } catch (_) {}
    };

    ws.onerror = () => setError('WebSocket error — retrying…');

    ws.onclose = () => {
      setConnected(false);
      // Only reconnect if the component is still mounted
      if (!mountedRef.current) return;
      reconnectAttemptsRef.current++;
      const delay = Math.min(1000 * 2 ** reconnectAttemptsRef.current, 30000);
      reconnectTimeoutRef.current = window.setTimeout(() => connectWebSocket(sym, ivl), delay);
    };
  }, [updateChartWithCandle]);

  /* ── React to asset/interval changes ── */
  useEffect(() => {
    const resizeCleanup = initChart();

    fetchHistoricalCandles(asset, chartInterval);
    connectWebSocket(asset, chartInterval);

    // Reset stats when asset/interval changes
    setPriceChange(null);
    setPctChange(null);

    return () => {
      clearTimeout(reconnectTimeoutRef.current);
      if (wsRef.current) {
        wsRef.current.onclose = null; // prevent reconnect loop
        wsRef.current.close();
        wsRef.current = null;
      }
      resizeCleanup?.();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [asset, chartInterval]);

  /* ── Unmount guard ── */
  useEffect(() => {
    mountedRef.current = true;
    return () => { mountedRef.current = false; };
  }, []);

  /* ── Derived display values ── */
  const fmt = (n) => (n != null ? n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '—');
  const isPositive = (priceChange ?? 0) >= 0;

  return (
    <div className="candle-chart-wrapper">
      {/* Keyframes */}
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;600;700&display=swap');
        @keyframes spin { to { transform: rotate(360deg); } }
        @keyframes pulse-dot {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.4; }
        }
      `}</style>

      {/* ── Toolbar ── */}
      <div className="candle-chart-toolbar">
        <div className="candle-chart-toolbar-left">
          {/* Interval selector */}
          {INTERVALS.map(i => (
            <button
              key={i.value}
              className={`interval-pill ${i.value === chartInterval ? 'active' : ''}`}
              onClick={() => setChartInterval(i.value)}
            >
              {i.label}
            </button>
          ))}

          <span className="toolbar-divider" />

          {/* Connection status */}
          <span className={`status-indicator ${connected ? 'connected' : ''}`} />
          <span className={`status-text ${connected ? 'connected' : ''}`}>
            {connected ? 'LIVE' : 'OFFLINE'}
          </span>
        </div>

        {/* Price info */}
        <div className="candle-chart-toolbar-right">
          <span className="toolbar-price">${fmt(currentPrice)}</span>
          {priceChange != null && (
            <span className={`toolbar-delta ${isPositive ? 'positive' : 'negative'}`}>
              {isPositive ? '▲' : '▼'} {isPositive ? '+' : ''}{fmt(priceChange)} ({isPositive ? '+' : ''}{pctChange?.toFixed(2)}%)
            </span>
          )}
        </div>
      </div>

      {/* ── Stats Row ── */}
      <div className="candle-chart-stats">
        {[
          { label: 'Open', value: fmt(openPrice) },
          { label: 'High', value: fmt(high), className: 'stat-green' },
          { label: 'Low', value: fmt(low), className: 'stat-red' },
          { label: 'Candles', value: candleCount || '—' },
          { label: 'Interval', value: chartInterval.toUpperCase() },
        ].map(stat => (
          <div key={stat.label} className="chart-stat-item">
            <div className="chart-stat-label">{stat.label}</div>
            <div className={`chart-stat-value ${stat.className || ''}`}>{stat.value}</div>
          </div>
        ))}
      </div>

      {/* ── Chart Area ── */}
      <div className="candle-chart-area">
        <div ref={containerRef} style={{ height: 420 }} />
        {loading && (
          <div className="candle-chart-overlay">
            <div className="candle-chart-spinner" />
          </div>
        )}
      </div>

      {/* ── Error Banner ── */}
      {error && (
        <div className="candle-chart-error">
          <span>⚠</span>
          <span>{error}</span>
          <button
            onClick={() => { setError(null); fetchHistoricalCandles(asset, chartInterval); }}
            className="candle-chart-retry-btn"
          >
            Retry
          </button>
        </div>
      )}

      {/* ── Footer ── */}
      <div className="candle-chart-footer">
        <span>TRADEBHARAT · MARKET DATA</span>
        <span>
          {lastUpdated
            ? `UPDATED ${lastUpdated.toLocaleTimeString('en-IN', { hour12: false })}`
            : `${limit} CANDLES LOADED`}
        </span>
      </div>
    </div>
  );
};

export default CandleChart;