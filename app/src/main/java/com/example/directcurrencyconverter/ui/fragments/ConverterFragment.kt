package com.example.directcurrencyconverter.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.directcurrencyconverter.R
import com.example.directcurrencyconverter.adapters.CurrencySpinnerAdapter
import com.example.directcurrencyconverter.data.local.CurrencyDatabase
import com.example.directcurrencyconverter.data.local.CurrencyEntity
import com.example.directcurrencyconverter.data.remote.ApiClient
import com.example.directcurrencyconverter.data.repository.CurrencyRepository
import kotlinx.coroutines.launch

class ConverterFragment : Fragment() {

    private lateinit var etAmount: EditText
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var btnConvert: Button
    private lateinit var tvResult: TextView
    private lateinit var swapButton: ImageButton
    private lateinit var repository: CurrencyRepository

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_converter, container, false)

        // Инициализация компонентов
        etAmount = view.findViewById(R.id.etAmount)
        spinnerFrom = view.findViewById(R.id.spinnerFrom)
        spinnerTo = view.findViewById(R.id.spinnerTo)
        btnConvert = view.findViewById(R.id.btnConvert)
        tvResult = view.findViewById(R.id.tvResult)
        swapButton = view.findViewById(R.id.ivSwap)

        setupRepository()
        loadCurrencies()
        setupConvertButton()
        setupSwapButton()

        return view
    }

    private fun setupRepository() {
        val frankfurterApi = ApiClient.createFrankfurterApi()
        val exchangeRateApi = ApiClient.createExchangeRateApi()
        val dao = CurrencyDatabase.getDatabase(requireContext()).currencyDao()
        repository = CurrencyRepository(frankfurterApi, exchangeRateApi, dao)
    }

    private fun setupSwapButton() {
        swapButton.setOnClickListener {
            val fromPos = spinnerFrom.selectedItemPosition
            val toPos = spinnerTo.selectedItemPosition
            spinnerFrom.setSelection(toPos)
            spinnerTo.setSelection(fromPos)
        }
    }

    private fun loadCurrencies() {
        lifecycleScope.launch {
            try {
                showLoading(true)

                val availableCurrencies = repository.checkSupportedCurrencies()
                if (availableCurrencies.isEmpty()) {
                    showMessage("Нет доступных валют")
                    return@launch
                }

                val currencies = repository.getCurrencies()
                updateSpinners(currencies)

            } catch (e: Exception) {
                Log.e("ConverterFragment", "Ошибка загрузки", e)
                showMessage("Ошибка: ${e.localizedMessage}")
                loadCachedCurrencies()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun setupConvertButton() {
        btnConvert.setOnClickListener {
            if (isOnline()) {
                convertCurrency()
            } else {
                showMessage("Нет интернет-соединения")
            }
        }
    }

    private fun convertCurrency() {
        val amountText = etAmount.text.toString()
        if (amountText.isEmpty()) {
            showMessage("Введите сумму")
            return
        }

        val amount = amountText.toDoubleOrNull() ?: run {
            showMessage("Некорректная сумма")
            return
        }

        val fromCurrency = (spinnerFrom.selectedItem as? CurrencyEntity)?.code
        val toCurrency = (spinnerTo.selectedItem as? CurrencyEntity)?.code

        if (fromCurrency == null || toCurrency == null) {
            showMessage("Выберите валюты")
            return
        }

        if (fromCurrency == toCurrency) {
            showMessage("Выберите разные валюты")
            return
        }

        lifecycleScope.launch {
            try {
                showLoading(true)

                val currencies = repository.getCurrencies()

                Log.d("DEBUG", "Все курсы: ${currencies.map { "${it.code}:${it.rate}" }}")

                val fromRate = currencies.find { it.code == fromCurrency }?.rate ?: run {
                    showMessage("Курс для $fromCurrency не найден")
                    return@launch
                }

                val toRate = currencies.find { it.code == toCurrency }?.rate ?: run {
                    showMessage("Курс для $toCurrency не найден")
                    return@launch
                }

                Log.d(
                    "DEBUG", "Конвертация: $amount $fromCurrency ($fromRate)" +
                            " → $toCurrency ($toRate)"
                )

                val result = (amount / fromRate) * toRate
                tvResult.text = "%.2f".format(result)

            } catch (e: Exception) {
                Log.e("ConverterFragment", "Ошибка", e)
                showMessage("Ошибка конвертации")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        return connectivityManager.activeNetworkInfo?.isConnected == true
    }

    private fun updateSpinners(currencies: List<CurrencyEntity>) {
        val adapter = CurrencySpinnerAdapter(requireContext(), currencies)
        spinnerFrom.adapter = adapter
        spinnerTo.adapter = adapter

        val usdPosition = currencies.indexOfFirst { it.code == "USD" }
        if (usdPosition != -1) spinnerFrom.setSelection(usdPosition)
    }

    private fun loadCachedCurrencies() {
        lifecycleScope.launch {
            try {
                val cached = repository.getCurrencies(forceRefresh = false)
                if (cached.isNotEmpty()) {
                    updateSpinners(cached)
                    showMessage("Используются сохраненные данные")
                }
            } catch (e: Exception) {
                Log.e("ConverterFragment", "Ошибка загрузки кеша", e)
            }
        }
    }

    private fun showMessage(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
    }

    private fun showLoading(show: Boolean) {
        btnConvert.isEnabled = !show
    }
}