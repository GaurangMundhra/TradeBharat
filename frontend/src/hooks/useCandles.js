import { useState, useEffect, useRef } from "react";
import { CandleClient } from "../websocket/candleClient";

/**
 * Custom hook for real-time candles via WebSocket
 * Automatically connects on mount and disconnects on unmount
 */
export function useCandles(asset, interval = "1m") {
  const [candles, setCandles] = useState([]);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState("");
  const clientRef = useRef(null);

  useEffect(() => {
    // Create and connect client
    const client = new CandleClient(
      asset,
      interval,
      (data) => {
        // Handle single candle or array of candles
        if (Array.isArray(data)) {
          setCandles(data);
        } else if (data && data.candles) {
          setCandles(data.candles);
        }
        setIsConnected(true);
        setError("");
      },
      (err) => {
        setError(err);
        setIsConnected(false);
      },
    );

    client.connect();
    clientRef.current = client;

    // Cleanup on unmount or asset/interval change
    return () => {
      if (clientRef.current) {
        clientRef.current.disconnect();
      }
    };
  }, [asset, interval]);

  return {
    candles,
    isConnected,
    error,
    refresh: () => {
      if (clientRef.current && clientRef.current.isConnected()) {
        clientRef.current.send({ action: "refresh" });
      }
    },
  };
}
