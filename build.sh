#!/bin/bash

# LiL Ranker Build Script
# Quick build script for future APK builds

echo "🎮 LiL Ranker - Build Script"
echo "=============================="
echo ""

# Set environment variables
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Navigate to project directory
cd "$(dirname "$0")"

echo "✅ Environment configured"
echo "   Java: $(java -version 2>&1 | head -1)"
echo "   Android SDK: $ANDROID_HOME"
echo ""

# Check if user wants to clean
if [ "$1" == "clean" ]; then
    echo "🧹 Cleaning previous build..."
    ./gradlew clean
    echo ""
fi

# Build the APK
echo "🔨 Building debug APK..."
echo ""

./gradlew assembleDebug --no-daemon

# Check build status
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo ""
    echo "📱 APK Location:"
    echo "   $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "📊 APK Size:"
    ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print "   " $5}'
    echo ""
    echo "🚀 To install on connected device:"
    echo "   adb install app/build/outputs/apk/debug/app-debug.apk"
    echo ""
else
    echo ""
    echo "❌ BUILD FAILED"
    echo ""
    echo "Check the error messages above and try:"
    echo "   ./build.sh clean"
    echo ""
    exit 1
fi
