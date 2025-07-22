package com.ivy.data.remote.impl

import com.ivy.data.remote.CryptoRateCache
import com.ivy.data.remote.CryptoRateSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject

class CoinDeskCryptoRateSource @Inject constructor(
    private val httpClient: HttpClient,
    private val cache: CryptoRateCache
) : CryptoRateSource {
    override suspend fun fetchRate(crypto: String, fiat: String): Double? {
        cache.get(crypto, fiat)?.let { return it }
        try {
            val url = "https://min-api.cryptocompare.com/data/price?fsym=${crypto.uppercase()}&tsyms=${fiat.uppercase()}"
            val response: Map<String, Double> = httpClient.get(url).body()
            val rate = response[fiat.uppercase()]
            if (rate != null && rate >= 0) {
                cache.set(crypto, fiat, rate)
                return rate
            }
        } catch (_: Exception) {}
        return null
    }
} 