# 🎮 LiL Ranker - Tournament Management App with MongoDB Authentication

A complete Android tournament management application for esports with MongoDB-based key authentication, session management, and auto-logout features.

## 🌟 Features

### Tournament Management
- ✅ Configure tournament settings (days, matches per day, teams, scoring)
- ✅ Enter match results with rank-based or kill-based entry
- ✅ Real-time leaderboard with day filtering
- ✅ Penalty system for rule violations
- ✅ Intelligent reset functionality
- ✅ Data persistence with Room database

### Authentication System (NEW!)
- ✅ MongoDB-based key validation
- ✅ One-time use access keys
- ✅ Session management with configurable duration
- ✅ Auto-logout after session expiry
- ✅ Offline session tracking (no internet bypass!)
- ✅ Data preservation across sessions
- ✅ Dark theme login screen

## 📋 Requirements

- **Android:** Android 10+ (API 29+)
- **Backend:** Node.js v14+, MongoDB
- **Development:** Android Studio, Java 17

## 🚀 Quick Start

### 1. Start Backend Server

```bash
cd /home/jerry/Desktop/aa
./start-backend.sh
```

This will:
- Check/start MongoDB
- Install dependencies
- Create test keys
- Start server on port 3000

### 2. Run Android App

1. Open project in Android Studio
2. Build and run (Shift+F10)

## 📁 Project Structure

```
aa/
├── app/                           # Android application
│   ├── src/main/
│   │   ├── java/com/lilranker/tournament/
│   │   │   ├── data/
│   │   │   │   ├── api/          # Retrofit API services
│   │   │   │   ├── dao/          # Room database DAOs
│   │   │   │   ├── database/     # Room database
│   │   │   │   ├── model/        # Data models
│   │   │   │   └── repository/   # Data repositories
│   │   │   ├── ui/               # Activities and adapters
│   │   │   │   ├── LoginActivity.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ConfigActivity.kt
│   │   │   │   ├── MatchEntryActivity.kt
│   │   │   │   └── LeaderboardActivity.kt
│   │   │   └── util/             # Utilities
│   │   │       ├── SessionManager.kt
│   │   │       └── SessionMonitor.kt
│   │   └── res/                  # Resources
│   └── build.gradle.kts
│
├── backend/                       # Node.js + MongoDB backend
│   ├── server.js                 # Express server
│   ├── create-sample-keys.js     # Key generator
│   ├── package.json
│   └── .env
│
├── start-backend.sh              # Backend startup script
├── test-backend.sh               # API testing script
│
└── Documentation/
    ├── MONGODB_AUTH_IMPLEMENTATION.md
    ├── QUICK_START_AUTH.md
    ├── AUTH_IMPLEMENTATION_SUMMARY.md
    ├── AUTH_VISUAL_GUIDE.md
    └── TESTING_CHECKLIST.md
```

## 🔐 Authentication System

### How It Works

1. **User opens app** → LoginActivity appears
2. **User enters key** → Backend validates via MongoDB
3. **Valid & unused?** → Mark as used, save session
4. **Navigate to home** → Start session timer
5. **Auto-logout** → After 2 minutes (configurable to 30 days)
6. **Data preserved** → Tournament data survives logout

### Key Features

- **One-time use**: Each key can only be redeemed once
- **Device binding**: Key tied to specific device
- **Offline protection**: Timer works without internet
- **Data isolation**: Auth and tournament data separate

### Session Duration

**Current (Testing):** 2 minutes  
**Production:** Change to 30 days

Edit `SessionManager.kt`:
```kotlin
const val SESSION_DURATION = 30L * 24 * 60 * 60 * 1000 // 30 days
```

## 🛠️ Configuration

### For Android Emulator (Default)
No changes needed! Uses `http://10.0.2.2:3000/`

### For Real Device
1. Find computer IP: `hostname -I`
2. Edit `RetrofitClient.kt`:
```kotlin
private const val BASE_URL = "http://YOUR_IP:3000/"
```

## 📊 Backend API

### Endpoints

```
GET  /health                - Health check
POST /api/validate-key      - Validate access key
POST /api/create-key        - Create new key (admin)
GET  /api/keys              - List all keys (admin)
```

### Create New Key

```bash
curl -X POST http://localhost:3000/api/create-key \
  -H "Content-Type: application/json" \
  -d '{"key":"NEW-KEY-2025-001"}'
```

## 🧪 Testing

### Test Backend

```bash
./test-backend.sh
```

### Manual API Test

```bash
curl -X POST http://localhost:3000/api/validate-key \
  -H "Content-Type: application/json" \
  -d '{"key":"TEST-KEY-2025-001","deviceId":"test-device"}'
```

### Full Testing Checklist

See `TESTING_CHECKLIST.md` for comprehensive testing guide.

## 🎨 UI Theme

- **Background:** AMOLED Black (#000000)
- **Primary:** Neon Red (#FF0000)
- **Secondary:** Gray (#888888)
- **Text:** White (#FFFFFF)
- **Dark theme optimized for esports**

## 📱 App Screens

1. **LoginActivity** - Key-based authentication
2. **MainActivity** - Home with navigation
3. **ConfigActivity** - Tournament settings
4. **MatchEntryActivity** - Enter match results
5. **LeaderboardActivity** - Rankings and scores

## 🔧 Troubleshooting

### Backend not starting?
```bash
sudo systemctl start mongod
cd backend && npm install
npm start
```

### Can't connect from app?
- Emulator: Use `10.0.2.2:3000`
- Real device: Use your computer's IP
- Check firewall settings

### Key already used?
Create new key:
```bash
cd backend
node -e "
const {MongoClient} = require('mongodb');
MongoClient.connect('mongodb://localhost:27017').then(client => {
  client.db('lilranker_auth').collection('keys').insertOne({
    key: 'KEY-' + Date.now(),
    isUsed: false,
    usedBy: null,
    usedAt: null,
    createdAt: Date.now()
  }).then(() => console.log('✅ Key created'));
});
"
```

## 📖 Documentation

- **`MONGODB_AUTH_IMPLEMENTATION.md`** - Complete implementation guide
- **`QUICK_START_AUTH.md`** - Quick reference
- **`AUTH_IMPLEMENTATION_SUMMARY.md`** - Feature summary
- **`AUTH_VISUAL_GUIDE.md`** - Visual diagrams and flows
- **`TESTING_CHECKLIST.md`** - Testing procedures
- **`backend/README.md`** - Backend documentation

## 🚢 Production Deployment

### Pre-production Checklist

- [ ] Change session duration to 30 days
- [ ] Update BASE_URL to production server
- [ ] Enable ProGuard/R8
- [ ] Add HTTPS to backend
- [ ] Secure admin endpoints
- [ ] Set up MongoDB replica set
- [ ] Configure monitoring
- [ ] Set up backups

## 🤝 Contributing

This is a private tournament management application. For questions or issues, refer to the documentation files.

## 📄 License

Copyright © 2025 LiL Ranker. All rights reserved.

## 🎯 Development Timeline

- ✅ Tournament management features
- ✅ Room database integration
- ✅ MVVM architecture
- ✅ Dark theme with neon accents
- ✅ MongoDB authentication (NEW!)
- ✅ Session management (NEW!)
- ✅ Auto-logout system (NEW!)

## 💡 Technical Stack

### Android
- Kotlin
- MVVM Architecture
- Room Database
- LiveData & ViewModel
- Coroutines
- Retrofit
- Material Design 3

### Backend
- Node.js
- Express.js
- MongoDB
- RESTful API

## 📞 Support

For help:

1. Check documentation in root directory
2. Review `QUICK_START_AUTH.md`
3. Run `./test-backend.sh` to verify setup
4. Check Android Logcat for errors
5. Check backend terminal logs

## ✨ Credits

Built with ❤️ for competitive gaming tournaments.

**Key Technologies:**
- Android Jetpack
- Room Persistence Library
- Retrofit HTTP Client
- MongoDB Database
- Express.js Framework

---

**Ready to manage your tournaments! 🎮**

Start with: `./start-backend.sh` then run the app!
