package com.example.directcurrencyconverter.ui.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.directcurrencyconverter.R
import com.example.directcurrencyconverter.adapters.CurrencySpinnerAdapter
import com.example.directcurrencyconverter.data.local.CurrencyDatabase
import com.example.directcurrencyconverter.data.local.CurrencyEntity
import com.example.directcurrencyconverter.data.remote.ApiClient
import com.example.directcurrencyconverter.data.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChartFragment : Fragment() {
    private lateinit var spinnerBaseCurrency: Spinner
    private lateinit var btnLoadRates: Button
    private lateinit var ratesTextView: TextView
    private lateinit var repository: CurrencyRepository
    private var currencies: List<CurrencyEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)

        spinnerBaseCurrency = view.findViewById(R.id.spinnerBaseCurrency)
            ?: throw IllegalStateException("Spinner not found")
        btnLoadRates =
            view.findViewById(R.id.btnLoadChart) ?: throw IllegalStateException("Button not found")
        ratesTextView = view.findViewById(R.id.ratesTextView)
            ?: throw IllegalStateException("TextView not found")

        setupRepository()
        loadCurrencies()

        btnLoadRates.setOnClickListener {
            loadCurrentRates()
        }

        return view
    }

    private fun setupRepository() {
        val frankfurterApi = ApiClient.createFrankfurterApi()
        val exchangeRateApi = ApiClient.createExchangeRateApi()
        val dao = CurrencyDatabase.getDatabase(requireContext()).currencyDao()
        repository = CurrencyRepository(frankfurterApi, exchangeRateApi, dao)
    }

    private fun loadCurrencies() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val availableCurrencies = repository.checkSupportedCurrencies()
                if (availableCurrencies.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(), "Нет доступных валют",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                currencies = repository.getCurrencies(forceRefresh = true)
                if (currencies.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(), "Список валют пуст",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    val adapter = CurrencySpinnerAdapter(requireContext(), currencies)
                    spinnerBaseCurrency.adapter = adapter

                    val usdPosition = currencies.indexOfFirst { it.code == "USD" }
                    if (usdPosition != -1) {
                        spinnerBaseCurrency.setSelection(usdPosition)
                    } else {
                        Toast.makeText(
                            requireContext(), "USD не найдена в списке",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(), "Ошибка загрузки валют: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadCurrentRates() {
        val baseCurrency = (spinnerBaseCurrency.selectedItem as? CurrencyEntity)?.code ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currencies = repository.getCurrencies(forceRefresh = true)
                val baseRate = currencies.find { it.code == baseCurrency }?.rate ?: 1.0

                val spannableBuilder = SpannableStringBuilder()
                val header = "Текущие курсы относительно $baseCurrency:\n\n"
                spannableBuilder.append(header)
                spannableBuilder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    header.length,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableBuilder.setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.textColorPrimary
                        )
                    ),
                    0,
                    header.length,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Список нужных валют с их флагами
                val targetCurrencies = listOf(
                    Triple("USD", "🇺🇸", "Доллар США"),
                    Triple("EUR", "🇪🇺", "Евро"),
                    Triple("RUB", "🇷🇺", "Российский рубль"),
                    Triple("GBP", "🇬🇧", "Фунт стерлингов"),
                    Triple("CNY", "🇨🇳", "Китайский юань"),
                    Triple("KZT", "🇰🇿", "Казахстанский тенге")
                )

                targetCurrencies.forEach { (code, flag, name) ->
                    if (code != baseCurrency) {
                        val currency = currencies.find { it.code == code }
                        if (currency != null) {
                            val convertedRate = currency.rate / baseRate
                            val rateText = "$flag $name (${currency.code}): " +
                                    "${String.format("%.4f", convertedRate)}\n"
                            val start = spannableBuilder.length
                            spannableBuilder.append(rateText)

                            // Стиль для названия валюты
                            spannableBuilder.setSpan(
                                ForegroundColorSpan(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.textColorPrimary
                                    )
                                ),
                                start,
                                start + flag.length + 1 + name.length,
                                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            // Стиль для кода валюты
                            spannableBuilder.setSpan(
                                ForegroundColorSpan(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.currency_code
                                    )
                                ),
                                start + flag.length + 1 + name.length + 1,
                                start + flag.length + 1 + name.length + 1 +
                                        currency.code.length,
                                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            // Стиль для курса
                            val rateStart = start + flag.length + 1 + name.length + 1 +
                                    currency.code.length + 2
                            val rateEnd = start + rateText.length - 1
                            val color = if (convertedRate > 1.0) {
                                ContextCompat.getColor(requireContext(), R.color.rate_higher)
                            } else {
                                ContextCompat.getColor(requireContext(), R.color.rate_lower)
                            }
                            spannableBuilder.setSpan(
                                ForegroundColorSpan(color),
                                rateStart,
                                rateEnd,
                                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            spannableBuilder.append("------------------------\n")
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    ratesTextView.text = spannableBuilder
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(), "Ошибка загрузки курсов: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}