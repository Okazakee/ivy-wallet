package com.ivy.data.remote.impl

import com.ivy.data.remote.CryptoRateCache
import com.ivy.data.remote.CryptoRateSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject

class KrakenCryptoRateSource @Inject constructor(
    private val httpClient: HttpClient,
    private val cache: CryptoRateCache
) : CryptoRateSource {
    override suspend fun fetchRate(crypto: String, fiat: String): Double? {
        cache.get(crypto, fiat)?.let { return it }
        try {
            // Kraken uses XXBTZUSD for BTC/USD, etc.
            val pair = "X${crypto.uppercase()}Z${fiat.uppercase()}"
            val url = "https://api.kraken.com/0/public/Ticker?pair=$pair"
            val response: KrakenResponse = httpClient.get(url).body()
            val rate = response.result[pair]?.c?.getOrNull(0)?.toDoubleOrNull()
            if (rate != null && rate >= 0) {
                cache.set(crypto, fiat, rate)
                return rate
            }
        } catch (_: Exception) {}
        return null
    }

    private data class KrakenResponse(val result: Map<String, Ticker>) {
        data class Ticker(val c: List<String>)
    }
} 