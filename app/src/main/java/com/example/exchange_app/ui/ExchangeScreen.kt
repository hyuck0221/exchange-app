package com.example.exchange_app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import com.example.exchange_app.ui.icons.AppLogo
import com.example.exchange_app.ui.icons.SettingsIcon
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeScreen(
    viewModel: ExchangeViewModel,
    onNavigateToSettings: () -> Unit
) {
    val krwAmount by viewModel.krwAmount.collectAsStateWithLifecycle()
    val foreignAmount by viewModel.foreignAmount.collectAsStateWithLifecycle()
    val targetCurrency by viewModel.targetCurrency.collectAsStateWithLifecycle()
    val targetUnit by viewModel.targetUnit.collectAsStateWithLifecycle()
    val targetSymbol by viewModel.targetSymbol.collectAsStateWithLifecycle()
    val exchangeRate by viewModel.exchangeRate.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()

    val selectedCurrencyInfo = availableCurrencies.find { it.currencyCode == targetCurrency }
    val foreignLabel = selectedCurrencyInfo?.let { "${it.countryName} (${it.unit} ${it.symbol})" } ?: targetCurrency

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppLogo,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("환율 계산기", style = MaterialTheme.typography.titleMedium)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = SettingsIcon,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Foreign Currency Input
            CurrencyInput(
                label = foreignLabel,
                value = foreignAmount,
                onValueChange = { viewModel.updateForeign(it) },
                koreanUnit = if (targetCurrency == "KRW") "" else viewModel.formatKoreanUnit(foreignAmount)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // KRW Input
            CurrencyInput(
                label = "대한민국 (원 ₩)",
                value = krwAmount,
                onValueChange = { viewModel.updateKrw(it) },
                koreanUnit = viewModel.formatKoreanUnit(krwAmount)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reset Button
            TextButton(
                onClick = { viewModel.clearAmounts() },
                modifier = Modifier.align(Alignment.End)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("초기화", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "1 $targetCurrency = ${DecimalFormat("#,###.##").format(exchangeRate)} KRW",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun CurrencyInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    koreanUnit: String = ""
) {
    val decimalFormat = remember { DecimalFormat("#,###.##########") }
    val formattedValue = remember(value) {
        val cleanValue = value.replace(",", "")
        if (cleanValue.isEmpty()) ""
        else {
            val parsed = cleanValue.toDoubleOrNull()
            if (parsed != null) {
                if (cleanValue.endsWith(".")) value // Keep trailing dot
                else decimalFormat.format(parsed)
            } else value
        }
    }

    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = formattedValue,
                onValueChange = { newValue ->
                    val cleanNewValue = newValue.replace(",", "")
                    if (cleanNewValue.isEmpty() || cleanNewValue.toDoubleOrNull() != null || cleanNewValue.endsWith(".")) {
                        onValueChange(cleanNewValue)
                    }
                },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
            
            if (koreanUnit.isNotEmpty()) {
                Text(
                    text = koreanUnit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                )
            }
        }
    }
}
