import { useState, useEffect, useRef } from "react";
import { OrderBookClient } from "../websocket/orderBookClient";

/**
 * Custom hook for real-time order book via WebSocket
 * Automatically connects on mount and disconnects on unmount
 */
export function useOrderBook(asset) {
  const [orderBook, setOrderBook] = useState({ bids: [], asks: [] });
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState("");
  const clientRef = useRef(null);

  useEffect(() => {
    // Create and connect client
    const client = new OrderBookClient(
      asset,
      (data) => {
        setOrderBook(data);
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

    // Cleanup on unmount or asset change
    return () => {
      if (clientRef.current) {
        clientRef.current.disconnect();
      }
    };
  }, [asset]);

  return {
    orderBook,
    isConnected,
    error,
    refresh: () => {
      if (clientRef.current && clientRef.current.isConnected()) {
        clientRef.current.send({ action: "refresh" });
      }
    },
  };
}
