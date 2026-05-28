@echo off
REM DineSplit Performance Optimization Verification Script (Windows)

echo =========================================
echo DineSplit Optimization Verification
echo =========================================
echo.

REM Check if Gradle wrapper exists
if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found. Please run from project root.
    exit /b 1
)

echo Step 1: Cleaning build artifacts...
call gradlew.bat clean --quiet
if errorlevel 1 (
    echo ERROR: Clean failed
    exit /b 1
)

echo.
echo Step 2: Building with optimizations enabled...
call gradlew.bat :app:assembleDebug --quiet
if errorlevel 1 (
    echo ERROR: Debug build failed
    exit /b 1
)

echo SUCCESS: Debug build completed
echo.

echo Step 3: Checking for optimization files...
if exist "app\src\main\java\com\example\dinesplit\core\di\CoilConfiguration.kt" (
    echo    FOUND: CoilConfiguration.kt
) else (
    echo    MISSING: CoilConfiguration.kt
)

if exist "app\src\main\java\com\example\dinesplit\core\application\DineSplitApplication.kt" (
    echo    FOUND: DineSplitApplication.kt
) else (
    echo    MISSING: DineSplitApplication.kt
)

if exist "app\src\main\java\com\example\dinesplit\ui\components\OptimizedAsyncImage.kt" (
    echo    FOUND: OptimizedAsyncImage.kt
) else (
    echo    MISSING: OptimizedAsyncImage.kt
)

echo.
echo Step 4: Checking gradle configuration...
findstr /m "isMinifyEnabled = true" app\build.gradle.kts >nul
if errorlevel 0 (
    echo    ENABLED: Minification
)

findstr /m "shrinkResources = true" app\build.gradle.kts >nul
if errorlevel 0 (
    echo    ENABLED: Resource shrinking
)

echo.
echo =========================================
echo Verification Complete!
echo =========================================
echo.
echo Next steps:
echo   1. Test on low-RAM device/emulator
echo   2. Monitor memory with Android Profiler
echo   3. Check for ANRs
echo   4. Verify image loading
echo.

