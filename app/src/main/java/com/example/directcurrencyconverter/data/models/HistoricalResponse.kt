package com.example.directcurrencyconverter.data.models

import com.google.gson.annotations.SerializedName

data class HistoricalResponse(
    @SerializedName("base")
    val base: String,

    @SerializedName("rates")
    val rates: Map<String, Map<String, Double>>,

    @SerializedName("start_date")
    val startDate: String,

    @SerializedName("end_date")
    val endDate: String
)