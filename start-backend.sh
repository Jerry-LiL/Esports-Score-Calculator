#!/bin/bash

echo "🚀 LiL Ranker Authentication Setup"
echo "=================================="
echo ""

# Check if MongoDB is running
echo "📊 Checking MongoDB..."
if ! pgrep -x mongod > /dev/null; then
    echo "⚠️  MongoDB is not running!"
    echo "Starting MongoDB..."
    sudo systemctl start mongod 2>/dev/null || {
        echo "❌ Failed to start MongoDB. Please start it manually."
        echo "Try: sudo systemctl start mongod"
        exit 1
    }
fi

echo "✅ MongoDB is running"
echo ""

# Navigate to backend directory
cd backend || {
    echo "❌ Backend directory not found!"
    exit 1
}

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing backend dependencies..."
    npm install
    echo "✅ Dependencies installed"
    echo ""
fi

# Create sample keys
echo "🔑 Creating sample keys..."
node create-sample-keys.js
echo ""

# Start the server
echo "🚀 Starting backend server..."
echo "   Server will run on http://localhost:3000"
echo "   Press Ctrl+C to stop"
echo ""
echo "=================================="
echo ""

npm start
