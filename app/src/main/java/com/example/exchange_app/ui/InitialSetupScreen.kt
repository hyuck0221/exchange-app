package com.example.exchange_app.ui

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
fun InitialSetupScreen(
    viewModel: ExchangeViewModel
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val setupStep by viewModel.setupStep.collectAsStateWithLifecycle()
    val setupMode by viewModel.setupMode.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val pendingCurrency by viewModel.pendingCurrency.collectAsStateWithLifecycle()
    val pendingRate by viewModel.pendingRate.collectAsStateWithLifecycle()
    val isLocked by viewModel.isRateLocked.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (setupStep == 2) {
                        TopAppBar(
                            title = { Text(if (setupMode == SetupMode.API) "불러오기 설정" else "수기 입력 설정") },
                            navigationIcon = {
                                IconButton(onClick = { viewModel.goToSetupStep1() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                                }
                            }
                        )
                    }
                },
                containerColor = Color.Transparent
            ) { padding ->
                if (setupStep == 1) {
                    StepOneModeSelection(padding, viewModel, isLoading)
                } else {
                    StepTwoCurrencySelection(
                        padding = padding, 
                        viewModel = viewModel, 
                        setupMode = setupMode,
                        currencies = availableCurrencies,
                        selectedCurrency = pendingCurrency, 
                        currentRate = pendingRate, 
                        isLocked = isLocked,
                        isLoading = isLoading
                    )
                }
            }
        }

        if (isLoading) {
            LoadingOverlay("데이터를 처리 중...")
        }
    }
}

@Composable
fun StepOneModeSelection(
    padding: PaddingValues,
    viewModel: ExchangeViewModel,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = AppLogo,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color.Unspecified
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(text = "환율 계산기 설정", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "앱을 사용하기 전에 환율 정보를 설정해야 합니다.\n어떤 방식으로 시작하시겠습니까?",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { viewModel.startInitialSetup(useApi = true) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(16.dp),
            enabled = !isLoading
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("실시간 환율 불러오기", fontWeight = FontWeight.Bold)
                Text("인터넷 연결 필요", style = MaterialTheme.typography.labelSmall)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { viewModel.startInitialSetup(useApi = false) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(16.dp),
            enabled = !isLoading
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("나중에 설정 (수기 입력)", fontWeight = FontWeight.Bold)
                Text("인터넷 연결이 어려울 때", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepTwoCurrencySelection(
    padding: PaddingValues,
    viewModel: ExchangeViewModel,
    setupMode: SetupMode,
    currencies: List<CurrencyCode>,
    selectedCurrency: CurrencyCode?,
    currentRate: String,
    isLocked: Boolean,
    isLoading: Boolean
) {
    var mainExpanded by remember { mutableStateOf(false) }
    var selectedFetchTarget by remember { mutableStateOf<CurrencyCode?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var selectedDate by remember { 
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) 
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (setupMode == SetupMode.MANUAL) {
            // --- Manual Mode UI ---
            Text(text = "수기 정보 입력", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // Manual Currency Selector (Fixed 4)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCurrency?.let { "${it.countryName} (${it.currencyCode})" } ?: "통화를 선택해주세요",
                    onValueChange = {},
                    label = { Text("기준 통화") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { mainExpanded = true }) {
                            Text("▼", fontSize = 12.sp)
                        }
                    }
                )
                DropdownMenu(
                    expanded = mainExpanded,
                    onDismissRequest = { mainExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    viewModel.fixedCurrencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text("${currency.countryName} (${currency.currencyCode})") },
                            onClick = {
                                viewModel.selectPendingCurrency(currency)
                                mainExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Rate Input
            OutlinedTextField(
                value = currentRate,
                onValueChange = { viewModel.updatePendingRate(it) },
                label = { Text("환율 (1 ${selectedCurrency?.currencyCode ?: "외화"} = ? KRW)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                suffix = { Text("KRW") }
            )
        } else {
            // --- API Mode UI ---
            Text(text = "실시간 정보로 채우기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            if (!isLocked) {
                // Before Fetching
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedFetchTarget?.let { "${it.countryName} (${it.currencyCode})" } ?: "불러올 통화 선택",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { mainExpanded = true }) {
                                Text("▼", fontSize = 12.sp)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = mainExpanded,
                        onDismissRequest = { mainExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f).heightIn(max = 300.dp)
                    ) {
                        currencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text("${currency.countryName} (${currency.currencyCode})") },
                                onClick = {
                                    selectedFetchTarget = currency
                                    mainExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { selectedFetchTarget?.let { viewModel.fetchLatestExchangeRate(it, selectedDate) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedFetchTarget != null && !isLoading
                ) {
                    Text("실시간 환율 불러오기")
                }
            } else {
                // After Fetching (Locked Results)
                OutlinedTextField(
                    value = selectedCurrency?.let { "${it.countryName} (${it.currencyCode})" } ?: "",
                    onValueChange = {},
                    label = { Text("불러온 통화") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentRate,
                    onValueChange = {},
                    label = { Text("불러온 환율") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text("KRW") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { viewModel.unlockAndClear() }) {
                    Text("수기로 수정하기 (잠금 해제 및 초기화)", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Start Button (Common)
        Button(
            onClick = { viewModel.finalizeSetup() },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(16.dp),
            enabled = !isLoading && selectedCurrency != null && (currentRate.toDoubleOrNull() ?: 0.0) > 0
        ) {
            Text("시작하기", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun LoadingOverlay(text: String) {
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
            Text(text, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
