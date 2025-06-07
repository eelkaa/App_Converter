package com.example.directcurrencyconverter.data.models

import com.google.gson.annotations.SerializedName

data class CurrencyResponse(
    @SerializedName("base")
    val baseCode: String, // Переименовано в camelCase

    @SerializedName("rates")
    val rates: Map<String, Double>, // Упрощаем название

    @SerializedName("date")
    val date: String? = null, // Реальное поле из API

    // Доп поля для совместимости
    val result: String = "success",
    val errorType: String? = null
)