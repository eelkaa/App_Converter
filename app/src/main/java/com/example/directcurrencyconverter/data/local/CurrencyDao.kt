package com.example.directcurrencyconverter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies")
    suspend fun getCurrencies(): List<CurrencyEntity>

    //COALESCE возвращает 0 если lastUpdated NULL
    @Query("SELECT COALESCE(MAX(lastUpdated), 0) FROM currencies")
    suspend fun getLastUpdateTime(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(currencies: List<CurrencyEntity>)

    @Query("DELETE FROM currencies")
    suspend fun clearCurrencies()
}