/**
 * Order Book WebSocket Client
 * Handles real-time order book updates via WebSocket
 */

export class OrderBookClient {
  constructor(asset, onUpdate, onError) {
    this.asset = asset;
    this.onUpdate = onUpdate;
    this.onError = onError;
    this.ws = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 1000;
  }

  connect() {
    const wsUrl = process.env.REACT_APP_WS_URL || "ws://localhost:8080/api/ws";
    const url = `${wsUrl}/order-book`;

    try {
      this.ws = new WebSocket(url);

      this.ws.onopen = () => {
        console.log(`✓ Connected to Order Book WebSocket for ${this.asset}`);
        this.reconnectAttempts = 0;
        // Send subscription message after connection
        this.send({
          type: "SUBSCRIBE_ORDER_BOOK",
          payload: {
            asset: this.asset,
            depth: 10,
          },
        });
      };

      this.ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          // Backend sends: { type: 'ORDER_BOOK_UPDATE', payload: { bids, asks } }
          if (
            message.type === "ORDER_BOOK_UPDATE" &&
            message.payload &&
            this.onUpdate
          ) {
            this.onUpdate(message.payload);
          }
        } catch (err) {
          console.error("Failed to parse order book data:", err);
          if (this.onError) {
            this.onError("Invalid data format");
          }
        }
      };

      this.ws.onerror = (error) => {
        console.error("Order Book WebSocket error:", error);
        if (this.onError) {
          this.onError("WebSocket connection error");
        }
      };

      this.ws.onclose = () => {
        console.log("Order Book WebSocket disconnected");
        this.attemptReconnect();
      };
    } catch (err) {
      console.error("Failed to create WebSocket:", err);
      if (this.onError) {
        this.onError("Failed to connect to order book stream");
      }
    }
  }

  attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay =
        this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
      console.log(
        `Reconnecting to order book in ${delay}ms... (Attempt ${this.reconnectAttempts})`,
      );
      setTimeout(() => this.connect(), delay);
    } else {
      console.error("Max reconnection attempts reached for order book");
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
