package com.example.directcurrencyconverter.data.remote

import com.example.directcurrencyconverter.data.models.CurrencyResponse
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CurrencyApi {
    @GET("latest")
    suspend fun getLatestCurrencies(
        @Query("from") baseCurrency: String = "EUR",
        @Query("to") targets: String? = null,
        @Query("amount") amount: Double? = null
    ): CurrencyResponse

    @GET("currencies")
    suspend fun getAvailableCurrencies(): Map<String, String>

    companion object {
        fun create(): CurrencyApi {
            return ApiClient.createFrankfurterApi()
        }
    }
}

interface ExchangeRateApi {
    @GET("v6/{apiKey}/latest/{baseCurrency}")
    suspend fun getExchangeRates(
        @Path("apiKey") apiKey: String,
        @Path("baseCurrency") baseCurrency: String
    ): ExchangeRateResponse

    companion object {
        fun create(): ExchangeRateApi {
            return ApiClient.createExchangeRateApi()
        }
    }
}

data class ExchangeRateResponse(
    @SerializedName("result")
    val result: String,

    @SerializedName("conversion_rates")
    val rates: Map<String, Double>
)

object ApiClient {
    private val logging by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    fun createFrankfurterApi(): CurrencyApi {
        return Retrofit.Builder()
            .baseUrl("https://api.frankfurter.app/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApi::class.java)
    }

    fun createExchangeRateApi(): ExchangeRateApi {
        return Retrofit.Builder()
            .baseUrl("https://v6.exchangerate-api.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExchangeRateApi::class.java)
    }
}