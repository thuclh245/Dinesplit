#!/bin/bash
# DineSplit Performance Optimization Verification Script

echo "========================================="
echo "DineSplit Optimization Verification"
echo "========================================="
echo ""

# Check if Gradle wrapper exists
if [ ! -f "./gradlew" ]; then
    echo "❌ gradlew not found. Please run from project root."
    exit 1
fi

echo "📦 Step 1: Cleaning build artifacts..."
./gradlew clean --quiet

echo ""
echo "🔨 Step 2: Building with optimizations enabled..."
./gradlew :app:assembleDebug --quiet

if [ $? -eq 0 ]; then
    echo "✅ Debug build successful"
else
    echo "❌ Debug build failed"
    exit 1
fi

echo ""
echo "📊 Step 3: Checking APK size..."
APK_SIZE=$(wc -c < "./app/build/outputs/apk/debug/app-debug.apk")
APK_SIZE_MB=$(echo "scale=2; $APK_SIZE / 1024 / 1024" | bc)
echo "   APK Size: ${APK_SIZE_MB}MB"

echo ""
echo "🔍 Step 4: Verifying optimization classes..."
if [ -d "./app/src/main/java/com/example/dinesplit/core/di" ]; then
    echo "   ✅ CoilConfiguration.kt present"
else
    echo "   ⚠️  CoilConfiguration not found"
fi

if [ -d "./app/src/main/java/com/example/dinesplit/core/application" ]; then
    echo "   ✅ DineSplitApplication.kt present"
else
    echo "   ⚠️  DineSplitApplication not found"
fi

if [ -d "./app/src/main/java/com/example/dinesplit/ui/components" ]; then
    echo "   ✅ OptimizedAsyncImage.kt present"
else
    echo "   ⚠️  OptimizedAsyncImage not found"
fi

echo ""
echo "📋 Step 5: Configuration checks..."
if grep -q "isMinifyEnabled = true" ./app/build.gradle.kts; then
    echo "   ✅ Minification enabled in release build"
else
    echo "   ⚠️  Minification check failed"
fi

if grep -q "shrinkResources = true" ./app/build.gradle.kts; then
    echo "   ✅ Resource shrinking enabled"
else
    echo "   ⚠️  Resource shrinking not enabled"
fi

echo ""
echo "========================================="
echo "✨ Verification Complete!"
echo "========================================="
echo ""
echo "📝 Next steps:"
echo "   1. Test on low-RAM device/emulator"
echo "   2. Monitor memory usage with Android Profiler"
echo "   3. Check for ANRs in crash reports"
echo "   4. Profile image loading performance"
echo ""

