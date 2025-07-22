package com.ivy.wallet.domain.action.exchange

import arrow.core.Option
import com.ivy.data.db.dao.read.ExchangeRatesDao
import com.ivy.data.remote.impl.RealTimeCryptoRateProvider
import com.ivy.frp.action.FPAction
import com.ivy.frp.then
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import com.ivy.wallet.domain.pure.exchange.exchange
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

class ExchangeAct @Inject constructor(
    private val exchangeRatesDao: ExchangeRatesDao,
    private val realTimeCryptoRateProvider: RealTimeCryptoRateProvider,
) : FPAction<ExchangeAct.Input, Option<BigDecimal>>() {
    override suspend fun Input.compose(): suspend () -> Option<BigDecimal> = suspend {
        // Try real-time rate for BTC
        if (data.fromCurrency.fold({ false }) { it == "BTC" } && data.toCurrency != "BTC") {
            Timber.d("Attempting real-time BTC/${data.toCurrency} rate")
            val realTimeRate = realTimeCryptoRateProvider.getRate("BTC", data.toCurrency)
            if (realTimeRate != null) {
                Timber.d("Using real-time rate: $realTimeRate")
                val convertedAmount = amount * BigDecimal.valueOf(realTimeRate)
                return@compose Option.Some(convertedAmount)
            } else {
                Timber.d("Real-time rate failed, falling back to database")
            }
        }
        
        // Fallback to database rates
        exchange(
            data = data,
            amount = amount,
            getExchangeRate = exchangeRatesDao::findByBaseCurrencyAndCurrency then {
                it?.toLegacyDomain()
            }
        )
    }

    data class Input(
        val data: ExchangeData,
        val amount: BigDecimal
    )
}

fun actInput(
    data: ExchangeData,
    amount: BigDecimal
): ExchangeAct.Input = ExchangeAct.Input(
    data = data,
    amount = amount
)
