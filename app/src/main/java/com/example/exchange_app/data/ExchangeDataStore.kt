package com.example.exchange_app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "exchange_settings")

class ExchangeDataStore(private val context: Context) {
    companion object {
        val TARGET_CURRENCY = stringPreferencesKey("target_currency")
        val EXCHANGE_RATE = doublePreferencesKey("exchange_rate")
        val TARGET_UNIT = stringPreferencesKey("target_unit")
        val TARGET_SYMBOL = stringPreferencesKey("target_symbol")
        val TARGET_COUNTRY_NAME = stringPreferencesKey("target_country_name")
        val LAST_UPDATED = longPreferencesKey("last_updated")
        val IS_INITIAL_SETUP_COMPLETE = booleanPreferencesKey("is_initial_setup_complete")
        val IS_RATE_LOCKED = booleanPreferencesKey("is_rate_locked")
        val LAST_KRW_AMOUNT = stringPreferencesKey("last_krw_amount")
        val LAST_FOREIGN_AMOUNT = stringPreferencesKey("last_foreign_amount")
        val IS_TIP_ENABLED = booleanPreferencesKey("is_tip_enabled")
    }

    val isInitialSetupCompleteFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_INITIAL_SETUP_COMPLETE] ?: false
    }

    val isRateLockedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_RATE_LOCKED] ?: false
    }

    val exchangeRateFlow: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[EXCHANGE_RATE] ?: 0.0
    }

    val targetCurrencyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TARGET_CURRENCY] ?: ""
    }

    val targetUnitFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TARGET_UNIT] ?: ""
    }

    val targetSymbolFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TARGET_SYMBOL] ?: ""
    }

    val targetCountryNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TARGET_COUNTRY_NAME] ?: ""
    }

    val lastKrwAmountFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_KRW_AMOUNT] ?: ""
    }

    val lastForeignAmountFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_FOREIGN_AMOUNT] ?: ""
    }

    val isTipEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_TIP_ENABLED] ?: false
    }

    suspend fun saveExchangeRate(
        currency: String, 
        rate: Double,
        unit: String? = null,
        symbol: String? = null,
        countryName: String? = null,
        isLocked: Boolean? = null
    ) {
        context.dataStore.edit { preferences ->
            preferences[TARGET_CURRENCY] = currency
            preferences[EXCHANGE_RATE] = rate
            unit?.let { preferences[TARGET_UNIT] = it }
            symbol?.let { preferences[TARGET_SYMBOL] = it }
            countryName?.let { preferences[TARGET_COUNTRY_NAME] = it }
            isLocked?.let { preferences[IS_RATE_LOCKED] = it }
            preferences[LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun setRateLocked(locked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_RATE_LOCKED] = locked
        }
    }

    suspend fun clearSettings() {
        context.dataStore.edit { preferences ->
            preferences[TARGET_CURRENCY] = ""
            preferences[EXCHANGE_RATE] = 0.0
            preferences[TARGET_UNIT] = ""
            preferences[TARGET_SYMBOL] = ""
            preferences[TARGET_COUNTRY_NAME] = ""
            preferences[IS_RATE_LOCKED] = false
        }
    }

    suspend fun setInitialSetupComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_INITIAL_SETUP_COMPLETE] = complete
        }
    }

    suspend fun saveAmounts(krw: String, foreign: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_KRW_AMOUNT] = krw
            preferences[LAST_FOREIGN_AMOUNT] = foreign
        }
    }

    suspend fun setTipEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_TIP_ENABLED] = enabled
        }
    }
}
