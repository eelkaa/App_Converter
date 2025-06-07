package com.example.directcurrencyconverter.data.repository

import android.util.Log
import com.example.directcurrencyconverter.data.local.CurrencyDao
import com.example.directcurrencyconverter.data.local.CurrencyEntity
import com.example.directcurrencyconverter.data.remote.CurrencyApi
import com.example.directcurrencyconverter.data.remote.ExchangeRateApi
import javax.inject.Inject

class CurrencyRepository @Inject constructor(
    private val frankfurterApi: CurrencyApi,
    private val exchangeRateApi: ExchangeRateApi,
    private val currencyDao: CurrencyDao
) {
    companion object {
        private const val CACHE_DURATION = 30 * 60 * 1000L
        private const val TAG = "CurrencyRepository"
        private const val EXCHANGE_RATE_API_KEY = "2b582970695a76cae6f754bf"
        //private const val EXCHANGE_RATE_API_KEY = "блаблабла"
    }

    suspend fun getCurrencies(forceRefresh: Boolean = false): List<CurrencyEntity> {
        return try {
            if (!forceRefresh) {
                getValidCache()?.let { return it }
            }

            val rates = try {
                exchangeRateApi.getExchangeRates(EXCHANGE_RATE_API_KEY, "EUR").rates
            } catch (e: Exception) {
                Log.w(TAG, "Using Frankfurter fallback", e)
                frankfurterApi.getLatestCurrencies().rates
            }

            // Создаем CurrencyEntity с правильными параметрами
            rates.map { (code, rate) ->
                CurrencyEntity(
                    code = code,
                    currency = code, // Просто используем код вместо названия
                    rate = rate,
                    lastUpdated = System.currentTimeMillis()
                )
            }.also { updateCache(it) }

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching currencies", e)
            handleError(e)
        }
    }

    suspend fun checkSupportedCurrencies(): Map<String, String> {
        return try {
            // Получаем валюты из обоих API
            val exchangeCurrencies = try {
                exchangeRateApi.getExchangeRates(EXCHANGE_RATE_API_KEY, "USD")
                    .rates.keys.associateWith { it }
            } catch (e: Exception) {
                emptyMap()
            }

            val frankfurterCurrencies = try {
                frankfurterApi.getAvailableCurrencies()
            } catch (e: Exception) {
                emptyMap()
            }

            // Объединяем результаты
            (exchangeCurrencies + frankfurterCurrencies).also { currencies ->
                Log.d(TAG, "Combined currencies: ${currencies.keys}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading currencies", e)
            emptyMap()
        }
    }

    suspend fun convert(amount: Double, from: String, to: String): Double {
        val currencies = getCurrencies()
        val fromRate = currencies.find { it.code == from }?.rate ?: 1.0
        val toRate = currencies.find { it.code == to }?.rate ?: 1.0
        return (amount / fromRate) * toRate
    }

    private suspend fun getValidCache(): List<CurrencyEntity>? {
        return try {
            val lastUpdate = currencyDao.getLastUpdateTime()
            val cachedCurrencies = currencyDao.getCurrencies()

            if (cachedCurrencies.isNotEmpty() &&
                System.currentTimeMillis() - lastUpdate < CACHE_DURATION
            ) {
                cachedCurrencies
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cache check failed", e)
            null
        }
    }

    private suspend fun updateCache(currencies: List<CurrencyEntity>) {
        try {
            currencyDao.clearCurrencies()
            currencyDao.insertCurrencies(currencies)
            Log.d(TAG, "Cache updated (${currencies.size} currencies)")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cache", e)
        }
    }

    private suspend fun handleError(e: Exception): List<CurrencyEntity> {
        return when (e) {
            is CurrencyApiException -> {
                Log.e(TAG, "API error: ${e.message}")
                val cached = currencyDao.getCurrencies()
                if (cached.isNotEmpty()) {
                    Log.w(TAG, "Using cached data")
                    cached
                } else {
                    throw e
                }
            }

            is java.net.UnknownHostException -> {
                Log.e(TAG, "No internet connection")
                val cached = currencyDao.getCurrencies()
                if (cached.isNotEmpty()) {
                    Log.w(TAG, "Using cache due to no internet")
                    cached
                } else {
                    throw NoInternetException("No internet and no cached data")
                }
            }

            else -> {
                Log.e(TAG, "Unknown error", e)
                val cached = currencyDao.getCurrencies()
                if (cached.isNotEmpty()) {
                    Log.w(TAG, "Using cache after error")
                    cached
                } else {
                    throw e
                }
            }
        }
    }
}

// Кастомные исключения
class CurrencyApiException(message: String) : Exception(message)
class NoInternetException(message: String) : Exception(message)