@echo off
echo ========================================
echo Auto-Tips Plugin Build Script
echo ========================================
echo.

echo [1/3] Cleaning previous build...
call gradlew clean
if %errorlevel% neq 0 (
    echo Error: Clean failed
    pause
    exit /b %errorlevel%
)
echo.

echo [2/3] Running tests...
call gradlew test
if %errorlevel% neq 0 (
    echo Warning: Some tests failed, but continuing...
)
echo.

echo [3/3] Building plugin...
call gradlew buildPlugin
if %errorlevel% neq 0 (
    echo Error: Build failed
    pause
    exit /b %errorlevel%
)
echo.

echo ========================================
echo Build completed successfully!
echo ========================================
echo.
echo Plugin file location:
echo build\distributions\auto-tips-1.0.0.zip
echo.
echo Next steps:
echo 1. Test the plugin locally: gradlew runIde
echo 2. Upload to JetBrains Marketplace
echo 3. See PUBLISH_GUIDE.md for details
echo.
pause
