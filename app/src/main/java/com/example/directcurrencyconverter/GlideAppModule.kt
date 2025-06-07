package com.example.directcurrencyconverter

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper
import com.bumptech.glide.module.AppGlideModule
import java.io.File

@GlideModule
class AppGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDiskCache {
            DiskLruCacheWrapper.create(
                File(context.cacheDir, "flags_cache"), // Папка для кэша
                500 * 1024 * 1024 // 500 MB
            )
        }
    }
}