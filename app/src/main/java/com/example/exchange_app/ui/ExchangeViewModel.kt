package com.example.exchange_app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchange_app.data.ExchangeDataStore
import com.example.exchange_app.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*

// API Data Models
data class CurrencyResponse(val payload: List<CurrencyCode>)
data class CurrencyCode(
    val currencyCode: String,
    val countryName: String,
    val unit: String,
    val symbol: String
)

data class ExchangeResponse(val payload: ExchangePayload)
data class ExchangePayload(val countries: List<ExchangeRate>)
data class ExchangeRate(
    val currencyCode: String,
    val amount: Double
)

interface ExchangeApi {
    @GET("/api/money/exchange/currency-code")
    suspend fun getCurrencyCodes(
        @Header("X-Api-Key") apiKey: String = BuildConfig.EXCHANGE_API_KEY
    ): CurrencyResponse

    @GET("/api/money/exchange")
    suspend fun getExchangeRates(
        @Query("baseAmount") baseAmount: Int = 1,
        @Query("baseCurrencyCode") baseCurrencyCode: String,
        @Query("date") date: String,
        @Header("X-Api-Key") apiKey: String = BuildConfig.EXCHANGE_API_KEY
    ): ExchangeResponse
}

enum class SetupMode { NONE, API, MANUAL }

class ExchangeViewModel(private val dataStore: ExchangeDataStore) : ViewModel() {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://apisis.dev")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(ExchangeApi::class.java)

    // DataStore Flows
    val exchangeRate = dataStore.exchangeRateFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val targetCurrency = dataStore.targetCurrencyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val targetUnit = dataStore.targetUnitFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val targetSymbol = dataStore.targetSymbolFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val isInitialSetupComplete = dataStore.isInitialSetupCompleteFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isRateLocked = dataStore.isRateLockedFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isTipEnabled = dataStore.isTipEnabledFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // UI States
    private val _availableCurrencies = MutableStateFlow<List<CurrencyCode>>(emptyList())
    val availableCurrencies: StateFlow<List<CurrencyCode>> = _availableCurrencies

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage

    private val _krwAmount = MutableStateFlow("")
    val krwAmount: StateFlow<String> = _krwAmount

    private val _foreignAmount = MutableStateFlow("")
    val foreignAmount: StateFlow<String> = _foreignAmount

    private val _tipPercent = MutableStateFlow("")
    val tipPercent: StateFlow<String> = _tipPercent

    private val _tipAmountKrw = MutableStateFlow(0.0)
    val tipAmountKrw: StateFlow<Double> = _tipAmountKrw

    private val _krwAmountWithTip = MutableStateFlow("")
    val krwAmountWithTip: StateFlow<String> = _krwAmountWithTip

    // Control visibility of fetch UI in Settings
    private val _isFetchUIReady = MutableStateFlow(false)
    val isFetchUIReady: StateFlow<Boolean> = _isFetchUIReady

    // Setup Flow States
    private val _setupStep = MutableStateFlow(1) 
    val setupStep: StateFlow<Int> = _setupStep

    private val _setupMode = MutableStateFlow(SetupMode.NONE)
    val setupMode: StateFlow<SetupMode> = _setupMode

    private val _pendingRate = MutableStateFlow("0.00")
    val pendingRate: StateFlow<String> = _pendingRate

    private val _pendingCurrency = MutableStateFlow<CurrencyCode?>(null)
    val pendingCurrency: StateFlow<CurrencyCode?> = _pendingCurrency

    val fixedCurrencies = listOf(
        CurrencyCode("USD", "미국", "달러", "$"),
        CurrencyCode("JPY", "일본", "엔", "¥"),
        CurrencyCode("VND", "베트남", "동", "₫"),
        CurrencyCode("CNH", "중국", "위안", "¥")
    )

    init {
        viewModelScope.launch {
            _krwAmount.value = dataStore.lastKrwAmountFlow.first()
            _foreignAmount.value = dataStore.lastForeignAmountFlow.first()
        }
    }

    fun prepareFetch() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getCurrencyCodes()
                _availableCurrencies.value = response.payload
                _isFetchUIReady.value = true
            } catch (e: Exception) {
                _errorMessage.emit("인터넷 연결을 확인해 주세요.")
                _isFetchUIReady.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startInitialSetup(useApi: Boolean) {
        if (useApi) {
            prepareFetch() // Reuse prepareFetch logic
            _setupMode.value = SetupMode.API
            _setupStep.value = 2
        } else {
            _setupMode.value = SetupMode.MANUAL
            _availableCurrencies.value = fixedCurrencies
            _setupStep.value = 2
        }
    }

    fun selectPendingCurrency(currency: CurrencyCode?) {
        if (currency == null) return
        _pendingCurrency.value = currency
        _pendingRate.value = getMockRate(currency.currencyCode)
    }

    private fun getMockRate(code: String): String = when (code) {
        "USD" -> "1450.00"
        "JPY" -> "9.50"
        "VND" -> "0.06"
        "CNH", "CNY" -> "200.00"
        else -> "1.00"
    }

    fun goToSetupStep1() {
        _setupStep.value = 1
        _setupMode.value = SetupMode.NONE
        _pendingCurrency.value = null
        _pendingRate.value = "0.00"
        _isFetchUIReady.value = false
        viewModelScope.launch { dataStore.setRateLocked(false) }
    }

    fun updatePendingRate(rate: String) {
        if (!isRateLocked.value) {
            _pendingRate.value = rate
        }
    }

    fun finalizeSetup() {
        val currency = _pendingCurrency.value ?: return
        val rateValue = _pendingRate.value.toDoubleOrNull() ?: 0.0
        if (rateValue <= 0.0) return

        viewModelScope.launch {
            dataStore.saveExchangeRate(
                currency.currencyCode,
                String.format("%.2f", rateValue).toDouble(),
                currency.unit,
                currency.symbol,
                currency.countryName,
                isLocked = isRateLocked.value
            )
            dataStore.setInitialSetupComplete(true)
        }
    }

    fun fetchLatestExchangeRate(currency: CurrencyCode, date: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetchDate = date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val response = api.getExchangeRates(baseCurrencyCode = currency.currencyCode, date = fetchDate)
                val krwRate = response.payload.countries.find { it.currencyCode == "KRW" }?.amount
                
                if (krwRate != null) {
                    val formattedRate = String.format("%.2f", krwRate)
                    if (!isInitialSetupComplete.value) {
                        _pendingCurrency.value = currency
                        _pendingRate.value = formattedRate
                        dataStore.setRateLocked(true)
                    } else {
                        dataStore.saveExchangeRate(
                            currency.currencyCode,
                            formattedRate.toDouble(),
                            currency.unit,
                            currency.symbol,
                            currency.countryName,
                            isLocked = true
                        )
                        updateKrw(_krwAmount.value)
                        _isFetchUIReady.value = false // Hide UI after success in Settings
                    }
                } else {
                    _errorMessage.emit("환율 정보를 가져올 수 없습니다.")
                }
            } catch (e: Exception) {
                _errorMessage.emit("인터넷 연결을 확인해 주세요.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun unlockAndClear() {
        viewModelScope.launch {
            dataStore.clearSettings()
            _pendingCurrency.value = null
            _pendingRate.value = "0.00"
            _krwAmount.value = ""
            _foreignAmount.value = ""
            _isFetchUIReady.value = false
            dataStore.saveAmounts("", "")
            if (_setupStep.value == 2 && _setupMode.value == SetupMode.API) {
                // Keep mode API but cleared
            } else {
                _setupMode.value = SetupMode.MANUAL
            }
        }
    }

    fun cancelFetch() {
        _isFetchUIReady.value = false
    }

    fun updateTipPercent(percent: String) {
        val clean = percent.replace(",", "")
        if (clean.isEmpty()) {
            _tipPercent.value = ""
            recalculateTip()
            return
        }
        val value = clean.toDoubleOrNull() ?: return
        if (value > 100) return
        _tipPercent.value = clean
        recalculateTip()
    }

    private fun recalculateTip() {
        val foreignValue = _foreignAmount.value.replace(",", "").toDoubleOrNull()
        val rate = exchangeRate.value
        val tipPct = _tipPercent.value.toDoubleOrNull() ?: 0.0

        if (foreignValue == null || rate <= 0.0 || tipPct <= 0.0 || !isTipEnabled.value) {
            _tipAmountKrw.value = 0.0
            _krwAmountWithTip.value = _krwAmount.value
            return
        }

        val tipForeign = foreignValue * (tipPct / 100.0)
        val tipKrw = tipForeign * rate
        _tipAmountKrw.value = tipKrw
        val baseKrw = _krwAmount.value.replace(",", "").toDoubleOrNull() ?: 0.0
        _krwAmountWithTip.value = String.format("%.0f", baseKrw + tipKrw)
    }

    fun setTipEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setTipEnabled(enabled)
            if (!enabled) {
                _tipPercent.value = ""
                _tipAmountKrw.value = 0.0
                _krwAmountWithTip.value = _krwAmount.value
            }
        }
    }

    fun updateKrw(amount: String) {
        val cleanAmount = amount.replace(",", "")
        _krwAmount.value = cleanAmount
        val rate = exchangeRate.value
        if (cleanAmount.isEmpty() || rate <= 0.0) {
            _foreignAmount.value = ""
        } else {
            val krwValue = cleanAmount.toDoubleOrNull()
            if (krwValue != null) {
                _foreignAmount.value = String.format("%.2f", krwValue / rate)
            }
        }
        recalculateTip()
        persistAmounts()
    }

    fun updateForeign(amount: String) {
        val cleanAmount = amount.replace(",", "")
        _foreignAmount.value = cleanAmount
        val rate = exchangeRate.value
        if (cleanAmount.isEmpty() || rate <= 0.0) {
            _krwAmount.value = ""
        } else {
            val foreignValue = cleanAmount.toDoubleOrNull()
            if (foreignValue != null) {
                _krwAmount.value = String.format("%.0f", foreignValue * rate)
            }
        }
        recalculateTip()
        persistAmounts()
    }

    private fun persistAmounts() {
        viewModelScope.launch {
            dataStore.saveAmounts(_krwAmount.value, _foreignAmount.value)
        }
    }

    fun clearAmounts() {
        _krwAmount.value = ""
        _foreignAmount.value = ""
        _tipPercent.value = ""
        _tipAmountKrw.value = 0.0
        _krwAmountWithTip.value = ""
        persistAmounts()
    }

    fun switchCurrency(currency: CurrencyCode) {
        if (isRateLocked.value) return
        viewModelScope.launch {
            dataStore.saveExchangeRate(
                currency.currencyCode,
                getMockRate(currency.currencyCode).toDouble(),
                currency.unit,
                currency.symbol,
                currency.countryName,
                isLocked = false
            )
            updateKrw(_krwAmount.value)
        }
    }

    fun updateManualRate(rate: Double) {
        if (isRateLocked.value) return
        viewModelScope.launch {
            dataStore.saveExchangeRate(
                targetCurrency.value,
                String.format("%.2f", rate).toDouble(),
                isLocked = false
            )
            updateKrw(_krwAmount.value)
        }
    }

    fun formatKoreanUnit(amountStr: String): String {
        val amount = amountStr.replace(",", "").toDoubleOrNull() ?: return ""
        if (amount == 0.0) return ""
        return when {
            amount >= 100_000_000 -> {
                val eok = amount / 100_000_000
                val man = (amount % 100_000_000) / 10_000
                if (man >= 1) String.format("%.0f억 %.0f만", eok, man)
                else String.format("%.0f억", eok)
            }
            amount >= 10_000 -> {
                val man = amount / 10_000
                String.format("%.1f만", man).replace(".0", "")
            }
            else -> ""
        }
    }
}
