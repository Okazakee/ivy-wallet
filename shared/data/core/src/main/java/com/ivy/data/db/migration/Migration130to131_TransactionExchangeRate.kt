package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration130to131_TransactionExchangeRate : Migration(130, 131) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN exchangeRateAtTime REAL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN exchangeRateCurrency TEXT")
    }
} 