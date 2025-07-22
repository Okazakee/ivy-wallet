package com.ivy.data.remote

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRateCache @Inject constructor() {
    private val expiryMillis: Long = 5 * 60 * 1000 // 5 minutes
    
    private data class CacheKey(val crypto: String, val fiat: String)
    private data class CacheEntry(val rate: Double, val timestamp: Long)

    private val cache = mutableMapOf<CacheKey, CacheEntry>()

    fun get(crypto: String, fiat: String): Double? {
        val key = CacheKey(crypto.uppercase(), fiat.uppercase())
        val entry = cache[key]
        val now = System.currentTimeMillis()
        return if (entry != null && now - entry.timestamp <= expiryMillis) {
            entry.rate
        } else {
            cache.remove(key)
            null
        }
    }

    fun set(crypto: String, fiat: String, rate: Double) {
        val key = CacheKey(crypto.uppercase(), fiat.uppercase())
        cache[key] = CacheEntry(rate, System.currentTimeMillis())
    }

    fun clear() {
        cache.clear()
    }
} 