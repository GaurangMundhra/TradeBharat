import React, { useEffect, useState, useRef } from "react";
import '../styles/orderbook.css';

const WS_BASE_URL = process.env.REACT_APP_WS_URL || "ws://localhost:8080/api/ws";
const MAX_RECONNECT_ATTEMPTS = 5;

export default function OrderBook({ asset = "BTC" }) {
  const [bids, setBids] = useState([]);
  const [asks, setAsks] = useState([]);
  const [connected, setConnected] = useState(false);
  const wsRef = useRef(null);
  const reconnectAttemptsRef = useRef(0);
  const reconnectTimeoutRef = useRef(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;

    function connect() {
      const ws = new WebSocket(`${WS_BASE_URL}/order-book`);
      wsRef.current = ws;

      ws.onopen = () => {
        reconnectAttemptsRef.current = 0;
        setConnected(true);
        ws.send(JSON.stringify({
          type: "SUBSCRIBE_ORDER_BOOK",
          payload: {
            asset,
            depth: 10,
          },
        }));
      };

      ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data);
          if (msg.type === "ORDER_BOOK_UPDATE") {
            setBids(msg.payload.bids || []);
            setAsks(msg.payload.asks || []);
          }
        } catch (err) {
          console.error("Failed to parse order book data:", err);
        }
      };

      ws.onerror = () => {
        setConnected(false);
      };

      ws.onclose = () => {
        setConnected(false);
        // Only reconnect if still mounted
        if (!mountedRef.current) return;
        if (reconnectAttemptsRef.current < MAX_RECONNECT_ATTEMPTS) {
          reconnectAttemptsRef.current++;
          const delay = 1000 * Math.pow(2, reconnectAttemptsRef.current - 1);
          reconnectTimeoutRef.current = window.setTimeout(connect, delay);
        }
      };
    }

    connect();

    return () => {
      mountedRef.current = false;
      clearTimeout(reconnectTimeoutRef.current);
      if (wsRef.current) {
        wsRef.current.onclose = null; // prevent reconnect on intentional close
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [asset]);

  // Sorting
  const sortedBids = [...bids].sort((a, b) => b.price - a.price);
  const sortedAsks = [...asks].sort((a, b) => a.price - b.price);

  // Max quantity for depth bars
  const maxQty = Math.max(
    ...sortedBids.map(b => b.quantity || 0),
    ...sortedAsks.map(a => a.quantity || 0),
    1
  );

  return (
    <div className="orderbook">
      <div className="orderbook-header-row">
        <h3>Order Book</h3>
        <span className={`ob-status ${connected ? 'connected' : ''}`}>
          {connected ? '● LIVE' : '○ OFFLINE'}
        </span>
      </div>

      {/* ASKS — displayed in reverse so lowest ask is nearest to spread */}
      <div className="asks">
        {sortedAsks.slice().reverse().map((a, i) => (
          <Row key={i} type="ask" price={a.price} quantity={a.quantity} maxQty={maxQty} />
        ))}
      </div>

      {/* SPREAD */}
      <div className="spread">
        Spread: ₹{" "}
        {sortedAsks[0] && sortedBids[0]
          ? (sortedAsks[0].price - sortedBids[0].price).toFixed(2)
          : "--"}
      </div>

      {/* BIDS */}
      <div className="bids">
        {sortedBids.map((b, i) => (
          <Row key={i} type="bid" price={b.price} quantity={b.quantity} maxQty={maxQty} />
        ))}
      </div>
    </div>
  );
}

function Row({ price, quantity, type, maxQty }) {
  return (
    <div className={`row ${type}`}>
      {/* depth bar */}
      <div
        className="depth"
        style={{
          width: `${(quantity / maxQty) * 100}%`,
        }}
      />
      <span className="price">₹ {price.toFixed(2)}</span>
      <span className="qty">{quantity.toFixed(2)}</span>
    </div>
  );
}