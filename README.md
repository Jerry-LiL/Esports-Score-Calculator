# LiL Ranker

Android app for running esports tournaments: set up the days and matches, enter each match by rank or kills, add penalties, and see the leaderboard. Access is by one-time keys checked against a small Node + MongoDB backend.

## Stack

- Android: Kotlin, Room, LiveData/ViewModel, coroutines, Retrofit, Material 3
- Backend: Node, Express, MongoDB

## Backend

Needs Node and a running MongoDB.

```bash
cd backend
npm install
node create-sample-keys.js   # adds a few test keys
npm start                    # port 3000
```

`./start-backend.sh` does the same and starts mongod if it isn't running. `./test-backend.sh` hits the endpoints with curl.

Endpoints:

```text
GET  /health
POST /api/validate-key   { key, deviceId }  redeems a key, one use per key
POST /api/create-key     { key }
GET  /api/keys
```

## App

Open the project in Android Studio and run it, or use `./build.sh`.

The backend URL is `BASE_URL` in `data/api/RetrofitClient.kt` (currently the Render deployment). Use `http://10.0.2.2:3000/` for a local backend from the emulator, or your machine's IP from a phone.

The session length is `SESSION_DURATION` in `util/SessionManager.kt`. It's 2 minutes right now for testing. The timer works offline, and tournament data is kept when the session ends.

## Notes

- Points come from placement plus kills, minus penalties, with a day filter on the leaderboard.
- Team aliases group two slots (e.g. the same clan); each match uses whichever scored higher.
- `USER_GUIDE_ENGLISH.html` and `USER_GUIDE_PERSIAN.html` are user guides for the app.

## TODO

- Set a real session length before release
- Protect the create-key and list-keys endpoints
