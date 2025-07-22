package com.ivy.data.remote.impl

import com.ivy.data.remote.CryptoRateCache
import com.ivy.data.remote.CryptoRateSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject

class CoinbaseCryptoRateSource @Inject constructor(
    private val httpClient: HttpClient,
    private val cache: CryptoRateCache
) : CryptoRateSource {
    override suspend fun fetchRate(crypto: String, fiat: String): Double? {
        // Check cache first
        cache.get(crypto, fiat)?.let { return it }
        try {
            val url = "https://api.coinbase.com/v2/prices/${crypto.uppercase()}-${fiat.uppercase()}/buy"
            val response: CoinbaseResponse = httpClient.get(url).body()
            val rate = response.data.amount.toDoubleOrNull()
            if (rate != null && rate >= 0) {
                cache.set(crypto, fiat, rate)
                return rate
            }
        } catch (_: Exception) {}
        return null
    }

    private data class CoinbaseResponse(val data: Data) {
        data class Data(val amount: String)
    }
} 