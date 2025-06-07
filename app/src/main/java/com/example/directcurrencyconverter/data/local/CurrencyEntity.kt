package com.example.directcurrencyconverter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey
    val code: String,      // "USD", "EUR" и т.д.
    val currency: String,  // Полное название
    val rate: Double,
    val lastUpdated: Long = System.currentTimeMillis()
)