package com.ivy.domain.usecase.transaction

import arrow.core.Either
import arrow.core.raise.either
import com.ivy.data.model.Transaction
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.repository.ExchangeRatesRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.remote.impl.RealTimeCryptoRateProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class StoreTransactionRateUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val exchangeRatesRepository: ExchangeRatesRepository,
    private val realTimeCryptoRateProvider: RealTimeCryptoRateProvider,
) {
    /**
     * Fetches and stores the current exchange rate for a transaction if it involves crypto currencies.
     * This ensures historical accuracy by storing the rate at the time of transaction creation.
     */
    suspend fun storeRateForTransaction(transaction: Transaction): Either<String, Transaction> = either {
        val fromAccount = transaction.getFromAccount()
        val toAccount = transaction.getToAccount()
        
        // Only store rates for crypto transactions (BTC, SATS) or cross-currency transfers
        val shouldStoreRate = when {
            fromAccount.asset.code in listOf("BTC", "SATS") -> true
            toAccount?.asset.code in listOf("BTC", "SATS") -> true
            fromAccount.asset != toAccount?.asset -> true
            else -> false
        }
        
        if (!shouldStoreRate) {
            return@either transaction
        }
        
        // Determine the target currency for rate storage
        val targetCurrency = when {
            fromAccount.asset.code in listOf("BTC", "SATS") -> {
                // For crypto transactions, store rate to base currency (usually USD)
                AssetCode.USD
            }
            toAccount?.asset.code in listOf("BTC", "SATS") -> {
                // For crypto transactions, store rate to base currency (usually USD)
                AssetCode.USD
            }
            else -> {
                // For cross-currency transfers, store rate to the destination currency
                toAccount?.asset ?: fromAccount.asset
            }
        }
        
        // Try to get real-time rate first
        val rate = when {
            fromAccount.asset.code == "BTC" -> {
                realTimeCryptoRateProvider.getRate("BTC", targetCurrency.code)
            }
            fromAccount.asset.code == "SATS" -> {
                // For SATS, get BTC rate and divide by 100M
                realTimeCryptoRateProvider.getRate("BTC", targetCurrency.code)?.let { btcRate ->
                    btcRate / 100_000_000.0
                }
            }
            else -> {
                // For other currencies, try to get from exchange rates repository
                val rates = exchangeRatesRepository.findAll().first()
                rates.find { 
                    it.baseCurrency == fromAccount.asset && it.currency == targetCurrency 
                }?.rate?.value
            }
        }
        
        if (rate != null) {
            // Update the transaction with the exchange rate information
            val updatedTransaction = when (transaction) {
                is com.ivy.data.model.Expense -> transaction.copy(
                    metadata = transaction.metadata.copy(
                        exchangeRateAtTime = rate,
                        exchangeRateCurrency = targetCurrency.code
                    )
                )
                is com.ivy.data.model.Income -> transaction.copy(
                    metadata = transaction.metadata.copy(
                        exchangeRateAtTime = rate,
                        exchangeRateCurrency = targetCurrency.code
                    )
                )
                is com.ivy.data.model.Transfer -> transaction.copy(
                    metadata = transaction.metadata.copy(
                        exchangeRateAtTime = rate,
                        exchangeRateCurrency = targetCurrency.code
                    )
                )
            }
            
            updatedTransaction
        } else {
            // If no rate is available, return the original transaction
            transaction
        }
    }
} 