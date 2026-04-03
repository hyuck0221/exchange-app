package com.example.exchange_app.ui

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exchange_app.ui.icons.AppLogo
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ExchangeViewModel,
    onBack: () -> Unit
) {
    val currentCurrency by viewModel.targetCurrency.collectAsStateWithLifecycle()
    val currentRate by viewModel.exchangeRate.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLocked by viewModel.isRateLocked.collectAsStateWithLifecycle()
    val isFetchUIReady by viewModel.isFetchUIReady.collectAsStateWithLifecycle()
    val isTipEnabled by viewModel.isTipEnabled.collectAsStateWithLifecycle()

    var rateInput by remember(currentRate) { 
        mutableStateOf(if (currentRate > 0) String.format("%.2f", currentRate) else "") 
    }
    
    LaunchedEffect(currentRate) {
        rateInput = if (currentRate > 0) String.format("%.2f", currentRate) else ""
    }
    
    var mainDropdownExpanded by remember { mutableStateOf(false) }
    var fetchDropdownExpanded by remember { mutableStateOf(false) }
    var selectedFetchCurrency by remember { mutableStateOf<CurrencyCode?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var selectedDate by remember { 
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) 
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val calendarSelected = Calendar.getInstance()
            calendarSelected.set(year, month, dayOfMonth)
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendarSelected.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            Text("설정", style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text("Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Target Currency Selector (Fixed 4)
                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentInfo = viewModel.fixedCurrencies.find { it.currencyCode == currentCurrency }
                    val displayText = if (currentCurrency.isEmpty()) "통화를 선택해주세요" 
                                     else currentInfo?.let { "${it.countryName} (${it.currencyCode})" } ?: currentCurrency
                    
                    OutlinedTextField(
                        value = displayText,
                        onValueChange = {},
                        label = { Text("기준 통화 선택") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLocked,
                        trailingIcon = {
                            if (!isLocked) {
                                IconButton(onClick = { mainDropdownExpanded = true }) {
                                    Text("▼", fontSize = 12.sp)
                                }
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = mainDropdownExpanded,
                        onDismissRequest = { mainDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        viewModel.fixedCurrencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text("${currency.countryName} (${currency.currencyCode})") },
                                onClick = {
                                    viewModel.switchCurrency(currency)
                                    mainDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Exchange Rate Input
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { newValue ->
                        rateInput = newValue
                        val rateValue = newValue.toDoubleOrNull()
                        if (rateValue != null) {
                            viewModel.updateManualRate(rateValue)
                        }
                    },
                    label = { Text("환율 (1 외화 = ? KRW)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    readOnly = isLocked,
                    enabled = !isLocked,
                    suffix = { Text("KRW") }
                )

                // 3. Unlock Button
                if (isLocked) {
                    TextButton(
                        onClick = { viewModel.unlockAndClear() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("수기로 수정하기 (잠금 해제 및 초기화)", color = MaterialTheme.colorScheme.error)
                    }
                }

                // 4. Tip Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "팁 계산 기능",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "외화 입력 시 팁 %를 추가하여 원화에 포함",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isTipEnabled,
                            onCheckedChange = { viewModel.setTipEnabled(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 5. API Fetch Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!isFetchUIReady) {
                            Text(
                                text = "실시간 환율 정보를 불러옵니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.prepareFetch() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            ) {
                                Text("환율 정보 불러오기")
                            }
                        } else {
                            Text(
                                text = "불러올 정보 선택",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Fetch Currency Selector
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedFetchCurrency?.let { "${it.countryName} (${it.currencyCode})" } ?: "불러올 통화 선택",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { fetchDropdownExpanded = true }) {
                                            Text("▼", fontSize = 12.sp)
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = fetchDropdownExpanded,
                                    onDismissRequest = { fetchDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.7f).heightIn(max = 300.dp)
                                ) {
                                    availableCurrencies.forEach { currency ->
                                        DropdownMenuItem(
                                            text = { Text("${currency.countryName} (${currency.currencyCode})") },
                                            onClick = {
                                                selectedFetchCurrency = currency
                                                fetchDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Date Selector
                            OutlinedTextField(
                                value = selectedDate,
                                onValueChange = {},
                                label = { Text("환율 기준 날짜") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { datePickerDialog.show() }) {
                                        Text("📅", fontSize = 16.sp)
                                    }
                                }
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.cancelFetch() }, 
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("취소")
                                }
                                Button(
                                    onClick = { 
                                        selectedFetchCurrency?.let { 
                                            viewModel.fetchLatestExchangeRate(it, selectedDate) 
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = selectedFetchCurrency != null && !isLoading
                                ) {
                                    Text("불러오기")
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("처리 중...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
