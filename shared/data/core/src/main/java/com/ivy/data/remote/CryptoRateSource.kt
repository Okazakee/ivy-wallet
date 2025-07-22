package com.ivy.data.remote

interface CryptoRateSource {
    /**
     * Fetches the real-time rate for the given crypto/fiat pair (e.g., BTC/USD).
     * @param crypto The cryptocurrency code (e.g., "BTC").
     * @param fiat The fiat currency code (e.g., "USD").
     * @return The rate as Double, or null if unavailable.
     */
    suspend fun fetchRate(crypto: String, fiat: String): Double?
} 