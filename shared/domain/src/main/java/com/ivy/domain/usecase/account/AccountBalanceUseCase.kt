package com.ivy.domain.usecase.account

import arrow.core.None
import arrow.core.Option
import com.ivy.data.model.AccountId
import com.ivy.data.model.Value
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.NonZeroDouble
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.remote.impl.RealTimeCryptoRateProvider
import com.ivy.domain.usecase.BalanceBuilder
import com.ivy.domain.usecase.exchange.ExchangeUseCase
import javax.inject.Inject

@Suppress("UnusedPrivateProperty", "UnusedParameter")
class AccountBalanceUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountStatsUseCase: AccountStatsUseCase,
    private val exchangeUseCase: ExchangeUseCase,
    private val accountRepository: com.ivy.data.repository.AccountRepository,
    private val realTimeCryptoRateProvider: RealTimeCryptoRateProvider,
) {
    /**
     * @return none balance if the balance is zero or exchange to [outCurrency]
     * failed for all assets
     */
    suspend fun calculate(
        account: AccountId,
        outCurrency: AssetCode,
    ): ExchangedAccountBalance {
        val acc = accountRepository.findById(account)
        if (acc != null && acc.asset.code == "BTC" && outCurrency.code != "BTC") {
            // Try real-time BTC rate
            val realTimeRate = realTimeCryptoRateProvider.getRate("BTC", outCurrency.code)
            val balance = calculate(account)
            val btcAmount = balance[AssetCode.Companion.exactName.let { AssetCode.unsafe("BTC") }]
            val nonZeroAmount = if (realTimeRate != null && btcAmount != null) {
                com.ivy.data.model.primitive.NonZeroDouble.from(btcAmount.value * realTimeRate).getOrNull()
            } else null
            if (nonZeroAmount != null) {
                val value = com.ivy.data.model.Value(
                    amount = nonZeroAmount,
                    asset = outCurrency
                )
                return ExchangedAccountBalance(
                    balance = arrow.core.Some(value),
                    exchangeErrors = emptySet()
                )
            }
            // fallback to existing logic if real-time fails
        }
        val balance = calculate(account)
        return if (balance.isEmpty()) {
            ExchangedAccountBalance.NoneBalance
        } else {
            val exchangeResult = exchangeUseCase.convert(values = balance, to = outCurrency)
            ExchangedAccountBalance(
                balance = exchangeResult.exchanged,
                exchangeErrors = exchangeResult.exchangeErrors
            )
        }
    }

    /**
     * Calculates the all-time balance for an account
     * in all assets that it have. **Note:** the balance can be negative.
     */
    suspend fun calculate(
        account: AccountId,
    ): Map<AssetCode, NonZeroDouble> {
        val accountStats = accountStatsUseCase.calculate(
            account = account,
            transactions = transactionRepository.findAll()
        )

        return BalanceBuilder().run {
            processDeposits(
                incomes = accountStats.income.values,
                transfersIn = accountStats.transfersIn.values
            )
            processWithdrawals(
                expenses = accountStats.expense.values,
                transfersOut = accountStats.transfersOut.values
            )
            build()
        }
    }
}

data class ExchangedAccountBalance(
    val balance: Option<Value>,
    val exchangeErrors: Set<AssetCode>,
) {
    companion object {
        val NoneBalance = ExchangedAccountBalance(
            balance = None,
            exchangeErrors = emptySet()
        )
    }
}