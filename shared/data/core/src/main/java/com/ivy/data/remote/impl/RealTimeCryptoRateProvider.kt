package com.ivy.data.remote.impl

import com.ivy.data.remote.CryptoRateSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealTimeCryptoRateProvider @Inject constructor(
    private val coinbase: CoinbaseCryptoRateSource,
    private val coinDesk: CoinDeskCryptoRateSource,
    private val coinGecko: CoinGeckoCryptoRateSource,
    private val bitstamp: BitstampCryptoRateSource,
    private val kraken: KrakenCryptoRateSource,
) {
    private val sources: List<CryptoRateSource> = listOf(
        coinbase,
        coinDesk,
        coinGecko,
        bitstamp,
        kraken,
    )

    suspend fun getRate(crypto: String, fiat: String): Double? {
        // Handle SATS by using BTC rate divided by 100M
        if (crypto.uppercase() == "SATS") {
            val btcRate = getBtcRate(fiat)
            return btcRate?.div(100_000_000.0)
        }
        
        // For other cryptos, try all sources
        for (source in sources) {
            val rate = source.fetchRate(crypto, fiat)
            if (rate != null) return rate
        }
        return null
    }
    
    private suspend fun getBtcRate(fiat: String): Double? {
        for (source in sources) {
            val rate = source.fetchRate("BTC", fiat)
            if (rate != null) return rate
        }
        return null
    }
} 