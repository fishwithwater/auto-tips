#!/bin/bash

echo "========================================"
echo "Auto-Tips Plugin Build Script"
echo "========================================"
echo ""

echo "[1/3] Cleaning previous build..."
./gradlew clean
if [ $? -ne 0 ]; then
    echo "Error: Clean failed"
    exit 1
fi
echo ""

echo "[2/3] Running tests..."
./gradlew test
if [ $? -ne 0 ]; then
    echo "Warning: Some tests failed, but continuing..."
fi
echo ""

echo "[3/3] Building plugin..."
./gradlew buildPlugin
if [ $? -ne 0 ]; then
    echo "Error: Build failed"
    exit 1
fi
echo ""

echo "========================================"
echo "Build completed successfully!"
echo "========================================"
echo ""
echo "Plugin file location:"
echo "build/distributions/auto-tips-1.0.0.zip"
echo ""
echo "Next steps:"
echo "1. Test the plugin locally: ./gradlew runIde"
echo "2. Upload to JetBrains Marketplace"
echo "3. See PUBLISH_GUIDE.md for details"
echo ""
