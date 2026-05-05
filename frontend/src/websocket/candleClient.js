/**
 * Candle WebSocket Client
 * Handles real-time candle/OHLC updates via WebSocket
 */

export class CandleClient {
  constructor(asset, interval, onUpdate, onError) {
    this.asset = asset;
    this.interval = interval;
    this.onUpdate = onUpdate;
    this.onError = onError;
    this.ws = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 1000;
  }

  connect() {
    const wsUrl = process.env.REACT_APP_WS_URL || "ws://localhost:8080/api/ws";
    const url = `${wsUrl}/candles`;

    try {
      this.ws = new WebSocket(url);

      this.ws.onopen = () => {
        console.log(
          `✓ Connected to Candle WebSocket for ${this.asset} (${this.interval})`,
        );
        this.reconnectAttempts = 0;
        // Send subscription message after connection
        this.send({
          type: "SUBSCRIBE_CANDLES",
          payload: {
            asset: this.asset,
            interval: this.interval,
          },
        });
      };

      this.ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          // Backend sends: { type: 'CANDLE_UPDATE', payload: { open, high, low, close, volume } }
          if (
            message.type === "CANDLE_UPDATE" &&
            message.payload &&
            this.onUpdate
          ) {
            this.onUpdate(message.payload);
          }
        } catch (err) {
          console.error("Failed to parse candle data:", err);
          if (this.onError) {
            this.onError("Invalid candle data format");
          }
        }
      };

      this.ws.onerror = (error) => {
        console.error("Candle WebSocket error:", error);
        if (this.onError) {
          this.onError("WebSocket connection error");
        }
      };

      this.ws.onclose = () => {
        console.log("Candle WebSocket disconnected");
        this.attemptReconnect();
      };
    } catch (err) {
      console.error("Failed to create Candle WebSocket:", err);
      if (this.onError) {
        this.onError("Failed to connect to candle stream");
      }
    }
  }

  attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay =
        this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
      console.log(
        `Reconnecting to candle stream in ${delay}ms... (Attempt ${this.reconnectAttempts})`,
      );
      setTimeout(() => this.connect(), delay);
    } else {
      console.error("Max reconnection attempts reached for candle stream");
      if (this.onError) {
        this.onError("Failed to maintain WebSocket connection");
      }
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  send(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.log("📤 Sending WebSocket message:", message);
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn("WebSocket not ready, cannot send:", message);
    }
  }

  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN;
  }
}
