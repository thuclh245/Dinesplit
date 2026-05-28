package com.example.dinesplit.core.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Coil image loader configuration optimized for memory-constrained environments
 */
object CoilConfiguration {
    fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(200)  // Smooth transition instead of immediate swap
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.15)  // Use 15% of available memory for cache
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024L)  // 100MB max
                    .build()
            }
            .respectCacheHeaders(false)  // Ignore cache headers to reduce network calls
            .build()
    }
}

