/**
 * TRADING CHART UI - SETUP & INTEGRATION GUIDE
 *
 * This file provides step-by-step instructions for setting up the React trading chart
 */

// ============================================
// 1. INSTALLATION INSTRUCTIONS
// ============================================

/*
Step 1: Create React App (if not already created)
npx create-react-app frontend
cd frontend

Step 2: Install Required Dependencies
npm install lightweight-charts

Step 3: Optional - Install Additional Tools
npm install react-router-dom axios   (for routing and HTTP requests)

Step 4: Verify Node & npm versions
node --version  (should be 16.x or higher)
npm --version   (should be 7.x or higher)
*/

// ============================================
// 2. PACKAGE.JSON DEPENDENCIES
// ============================================

/*
Add to your package.json:

{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "lightweight-charts": "^4.1.0",
    "react-scripts": "5.0.1"
  },
  "scripts": {
    "start": "react-scripts start",
    "build": "react-scripts build",
    "test": "react-scripts test"
  }
}

After updating package.json, run:
npm install
*/

// ============================================
// 3. ENVIRONMENT VARIABLES
// ============================================

/*
Create .env file in project root:

REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_WS_URL=ws://localhost:8080/api/ws/candles

For production:
REACT_APP_API_URL=https://api.tradebharat.com/api
REACT_APP_WS_URL=wss://api.tradebharat.com/api/ws/candles
*/

// ============================================
// 4. FILE STRUCTURE
// ============================================

/*
frontend/
├── public/
│   └── index.html
├── src/
│   ├── components/
│   │   ├── CandleChart.jsx         (✅ PROVIDED)
│   │   └── CandleChart.css         (✅ PROVIDED)
│   ├── App.jsx                     (✅ PROVIDED)
│   ├── App.css                     (✅ PROVIDED)
│   ├── index.js
│   └── index.css
├── .env                            (✅ CREATE THIS)
├── package.json
└── README.md
*/

// ============================================
// 5. COMPONENT USAGE
// ============================================

/*
Basic Usage - In your App.jsx or any parent component:

import CandleChart from './components/CandleChart';

function MyComponent() {
  return (
    <CandleChart 
      defaultAsset="AAPL"
      interval="1m"
      limit={100}
    />
  );
}

Props:
- defaultAsset (string): Initial asset symbol (default: "AAPL")
- interval (string): Candle interval (default: "1m")
  Supported: "1m", "5m", "15m", "1h", "4h", "1d"
- limit (int): Number of historical candles to fetch (default: 100)
*/

// ============================================
// 6. API ENDPOINTS REQUIRED
// ============================================

/*
Your backend must provide:

1. REST API - Get Historical Candles
   GET /api/candles?asset=AAPL&interval=1m&limit=100
   
   Response Format:
   {
     "success": true,
     "message": "Retrieved 100 candles for AAPL (1m)",
     "data": [
       {
         "asset": "AAPL",
         "open": 300.00,
         "high": 305.50,
         "low": 298.75,
         "close": 302.00,
         "volume": 1500.0,
         "interval": "1m",
         "startTime": 1713561000000,
         "endTime": 1713561060000
       },
       ...
     ],
     "statusCode": 200,
     "timestamp": "2026-05-01T10:30:45.123456"
   }

2. WebSocket - Real-time Candle Updates
   URL: ws://localhost:8080/api/ws/candles
   
   Subscribe Message:
   {
     "type": "SUBSCRIBE_CANDLES",
     "payload": {
       "asset": "AAPL",
       "interval": "1m"
     }
   }
   
   Server Broadcasting:
   {
     "type": "CANDLE_UPDATE",
     "payload": {
       "asset": "AAPL",
       "open": 300.00,
       "high": 305.50,
       "low": 298.75,
       "close": 302.00,
       "volume": 1500.0,
       "interval": "1m",
       "startTime": 1713561000000,
       "endTime": 1713561060000
     }
   }
*/

// ============================================
// 7. KEY FEATURES IMPLEMENTED
// ============================================

/*
✅ Real-time Candlestick Chart
   - Uses TradingView's Lightweight Charts library
   - Optimized for trading: fast, lightweight, responsive

✅ Historical Data Fetching
   - Fetches last N candles via REST API on component mount
   - Converts API format to chart format
   - Loads data in reverse order (oldest first)

✅ Real-time WebSocket Updates
   - Connects to WebSocket on component mount
   - Subscribes to selected asset
   - Updates chart smoothly on each new candle
   - Detects same vs. new interval (update vs. append)

✅ Asset Switching
   - Dropdown to select different assets
   - Unsubscribes from old asset
   - Subscribes to new asset
   - Fetches new historical data

✅ Current Price Display
   - Shows last candle close price
   - Shows price change from previous update
   - Color-coded: green for up, red for down

✅ Connection Status Indicator
   - Shows live/offline status
   - Animated pulsing dot
   - Switches automatically on connect/disconnect

✅ Error Handling
   - Catches API fetch errors
   - Catches WebSocket errors
   - Automatic reconnection (3-second retry)
   - User-friendly error messages

✅ Loading State
   - Shows spinner while fetching data
   - Prevents asset changes during loading
   - Smooth loading overlay

✅ Responsive Design
   - Desktop: Full sidebar layout
   - Tablet: Sidebar below chart
   - Mobile: Single column, touch-friendly

✅ Accessible
   - ARIA-friendly
   - Keyboard navigation support
   - Focus states for all interactive elements
*/

// ============================================
// 8. DATA FLOW DIAGRAM
// ============================================

/*
User Opens App
    ↓
React Mounts CandleChart Component
    ├─ Initialize Chart with TradingView Lightweight Charts
    ├─ Call REST API: GET /api/candles?asset=AAPL&limit=100
    │  ↓
    │  Response: List of CandleDTO
    │  ↓
    │  Convert to chart format & Load into chart
    │
    └─ Connect to WebSocket
       ↓
       Send: SUBSCRIBE_CANDLES {asset: AAPL}
       ↓
       Ready for live updates

User Trades Occur
    ↓
Backend Matches Orders
    ↓
CandleService Updates Candle
    ↓
WebSocket Broadcasts CANDLE_UPDATE
    ↓
CandleChart Receives Message
    ├─ Extract Candle Data
    ├─ Check if Same Time Window (update) or New (append)
    └─ Update Chart with New Candle
       ↓
       Chart Re-renders Smoothly (via chart API, not React)
       ↓
       User Sees Real-time Update

User Changes Asset
    ↓
CandleChart.handleAssetChange()
    ├─ Unsubscribe from Old Asset
    ├─ Clear Chart Data & Historical Candles Map
    ├─ Call REST API for New Asset
    ├─ Load New Historical Data
    └─ Subscribe to New Asset
       ↓
       Ready for live updates on new asset
*/

// ============================================
// 9. RUNNING THE APPLICATION
// ============================================

/*
Terminal 1 - Start Backend (Spring Boot)
cd finsimx-backend
mvn spring-boot:run

Terminal 2 - Start Frontend (React)
cd frontend
npm start

Browser will open at:
http://localhost:3000

API calls to:
http://localhost:8080/api/candles

WebSocket connection to:
ws://localhost:8080/api/ws/candles
*/

// ============================================
// 10. TROUBLESHOOTING
// ============================================

/*
Issue: "Chart not displaying"
Solution:
  - Verify API endpoint is accessible: curl http://localhost:8080/api/candles?asset=AAPL
  - Check browser console for errors (F12)
  - Ensure .env file has correct API_URL
  - Verify Component is mounted and containerRef is set

Issue: "WebSocket connection fails"
Solution:
  - Check backend is running and WebSocket is enabled
  - Verify WS_URL in .env is correct (ws:// not http://)
  - Check CORS is enabled on backend
  - Monitor network tab (F12) for connection attempts

Issue: "No real-time updates"
Solution:
  - Execute trades to trigger candle updates
  - Check WebSocket connection status (green dot should be visible)
  - Monitor WebSocket messages in browser DevTools
  - Verify subscription message is being sent

Issue: "Memory usage growing"
Solution:
  - Component stores max 100-500 candles by default
  - Limit number of live updates per second
  - Consider implementing pagination for historical data

Issue: "Chart looks empty or misaligned"
Solution:
  - Clear browser cache and hard refresh (Ctrl+Shift+R)
  - Check container dimensions (must have width & height)
  - Verify lightweight-charts package is installed correctly
  - Try zooming chart or adjusting time scale
*/

// ============================================
// 11. PERFORMANCE OPTIMIZATION TIPS
// ============================================

/*
1. Limit Chart Redraws
   - Component only updates on WebSocket messages
   - Chart library handles most updates (not React)
   - No full component re-renders for data updates

2. Candle Deduplication
   - Uses candlesMapRef to track candles by timestamp
   - Prevents duplicate candles in chart
   - Efficient update vs append logic

3. Memory Management
   - Stores only recent candles in memory
   - Historical data cleared on asset switch
   - WebSocket listeners properly cleaned up on unmount

4. Lazy Loading
   - Initial data load (100 candles) is minimal
   - Only new candles streamed via WebSocket
   - Consider pagination for very old data

5. Network Optimization
   - Single REST API call on mount
   - WebSocket for streaming (lower latency)
   - Reconnection logic prevents connection spam
*/

// ============================================
// 12. EXTENDING THE COMPONENT
// ============================================

/*
Add More Features:

1. Add Volume Bar Chart
   candleSeries.setData(candles);
   const volumeSeries = chart.addHistogramSeries();
   volumeSeries.setData(volumeData);

2. Add Technical Indicators
   - Moving Averages (SMA, EMA)
   - RSI, MACD, Bollinger Bands
   - Add new series for each indicator

3. Add Multiple Intervals
   - Allow switching between 1m, 5m, 1h, 1d
   - Fetch new data on interval change
   - Maintain separate chart state per interval

4. Add Zoom & Pan Controls
   - Implement interactive legend
   - Add crosshair plugin
   - Track prices on hover

5. Save Chart State
   - Store viewing range in localStorage
   - Remember user's preferred interval
   - Remember selected asset

6. Export Chart as Image
   - Use canvas snapshots
   - Generate PNG/PDF reports
*/

// ============================================
// 13. DEPLOYMENT
// ============================================

/*
Build for Production:
npm run build

This creates optimized bundle in 'build/' folder

Deploy to:
- Vercel: vercel deploy
- Netlify: netlify deploy
- AWS S3: aws s3 sync build/ s3://bucket-name
- Docker: Create Dockerfile and build image

Environment Variables for Production:
REACT_APP_API_URL=https://api.tradebharat.com/api
REACT_APP_WS_URL=wss://api.tradebharat.com/api/ws/candles

(Note: Use wss:// for secure WebSocket on HTTPS)
*/

// ============================================
// 14. SUPPORT & DOCUMENTATION
// ============================================

/*
Lightweight Charts Documentation:
https://tradingview.github.io/lightweight-charts/

React Documentation:
https://react.dev/

WebSocket API:
https://developer.mozilla.org/en-US/docs/Web/API/WebSocket

For issues with the trading chart component:
1. Check console errors (F12)
2. Verify API endpoints
3. Check WebSocket connection
4. Monitor network requests
5. Test with different assets
*/

export default {};
