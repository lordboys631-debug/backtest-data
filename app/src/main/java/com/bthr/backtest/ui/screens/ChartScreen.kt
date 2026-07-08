package com.bthr.backtest.ui.screens

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import com.bthr.backtest.ui.components.ChartSkeleton
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.bthr.backtest.R
import com.bthr.backtest.model.Candle
import com.bthr.backtest.model.Indicator
import com.bthr.backtest.model.loadChartSettings
import com.bthr.backtest.model.saveChartSettings
import com.bthr.backtest.model.ChartSettings
import com.bthr.backtest.util.ChartExportUtil
import com.bthr.backtest.util.FileDownloader
import com.bthr.backtest.model.Timeframe
import com.bthr.backtest.model.DrawingTool
import com.bthr.backtest.model.SimpleTrendLine
import com.bthr.backtest.model.TradeChartData
import com.bthr.backtest.model.TradeEntry
import com.bthr.backtest.ui.components.CandlestickChart
import com.bthr.backtest.ui.components.ChartSettingsDialog
import com.bthr.backtest.ui.components.IndicatorSettingsScreen
import com.bthr.backtest.ui.components.ObjectTreeScreen
import com.bthr.backtest.ui.components.DrawingToolsMenu
import com.bthr.backtest.ui.components.LotSizeDialog
import com.bthr.backtest.ui.components.DepositFundsDialog
import com.bthr.backtest.ui.components.InsufficientMarginDialog
import com.bthr.backtest.ui.components.ChartSidebar
import com.bthr.backtest.ui.components.ChartShareBottomSheet
import com.bthr.backtest.ui.components.DrawingToolsPanel
import com.bthr.backtest.ui.components.ManageFullScreen
import com.bthr.backtest.ui.components.rememberStopOutPrice
import com.bthr.backtest.ui.components.rememberChartUndoRedo
import com.bthr.backtest.ui.components.ChartTopBar
import com.bthr.backtest.ui.components.ChartTypeSelectorOverlay
import com.bthr.backtest.ui.components.IndicatorsMenu
import com.bthr.backtest.ui.components.TimeframeSelectorOverlay
import com.bthr.backtest.ui.components.TradesHistoryPanel
import com.bthr.backtest.ui.components.TradeDisplayData
import com.bthr.backtest.ui.components.getDrawingToolsMenuColors
import com.bthr.backtest.ui.components.AppIcon
import com.bthr.backtest.ui.components.DragHandle
import com.bthr.backtest.ui.theme.BacktestTheme
import com.bthr.backtest.util.CandleUtil
import com.bthr.backtest.util.CsvParser
import com.bthr.backtest.util.TickUtil
import com.bthr.backtest.util.IndicatorSerializer
import com.bthr.backtest.util.DrawingManager
import com.bthr.backtest.util.DrawingUseMode
import com.bthr.backtest.util.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    navController: NavController,
    context: Context = LocalContext.current,
    backtestDate: String? = null,
    backtestStartTime: String? = null,   // "HH:mm" ex: "09:00"
    sessionName: String? = null,
    initialAmount: String? = null,
    backtestTimeframe: String? = null,
    backtestSymbol: String? = null,
    onMenuClick: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    globalIsDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val sharedPrefs = remember { context.getSharedPreferences("chart_prefs", Context.MODE_PRIVATE) }
    val density = LocalDensity.current
    // Clef prefixee par session pour la persistance des parametres/indicateurs
    val sKey = sessionName?.trim()?.takeIf { it.isNotBlank() } ?: "default"

    // ── Config de session (spread, commission, leverage) ─────────
    val sessionConfig = remember(sessionName) {
        if (!sessionName.isNullOrBlank()) SessionRepository.loadSessionConfig(context, sessionName) else null
    }
    val spreadPts    = (sessionConfig?.get("spread")?.toFloatOrNull()     ?: 0f)   // spread en pts de prix
    val commPerLot   = (sessionConfig?.get("commission")?.toFloatOrNull() ?: 0f)   // commission USD/lot
    val leverageStr = sessionConfig?.get("leverage") ?: "1:100"
    val leverageRatio = if (leverageStr.contains("Illimite", ignoreCase = true)) {
        Float.POSITIVE_INFINITY
    } else {
        leverageStr.split(":").getOrNull(1)?.toFloatOrNull() ?: 100f
    }

    var chartSettings by remember {
        mutableStateOf(loadChartSettings(sharedPrefs, globalIsDarkTheme))
    }

    // Reload chart settings when theme changes
    LaunchedEffect(globalIsDarkTheme) {
        chartSettings = loadChartSettings(sharedPrefs, globalIsDarkTheme)
    }

    var activeIndicators by remember(sessionName) {
        val indicatorKey = "active_indicators_$sKey"
        val savedIndicators = try {
            val saved = sharedPrefs.getString(indicatorKey, null)
            android.util.Log.d("ChartScreen", "Initial load from prefs ($indicatorKey): saved=$saved")
            if (saved == null || saved.isEmpty()) {
                android.util.Log.d("ChartScreen", "Initial: no saved indicators for session $sKey")
                listOf(Indicator.Volume())
            } else if (saved == "[]") {
                android.util.Log.d("ChartScreen", "Initial: explicitly empty list for session $sKey")
                emptyList()
            } else {
                val deserialized = IndicatorSerializer.deserializeIndicators(saved)
                android.util.Log.d("ChartScreen", "Initial deserialized: count=${deserialized.size}")
                deserialized
            }
        } catch (e: Exception) {
            android.util.Log.e("ChartScreen", "Error in initial load", e)
            listOf(Indicator.Volume())
        }
        mutableStateOf<List<Indicator>>(savedIndicators)
    }

    // LaunchedEffect to verify loading on screen initialization
    LaunchedEffect(sessionName) {
        try {
            val indicatorKey = "active_indicators_$sKey"
            val savedIndicators = sharedPrefs.getString(indicatorKey, null)
            android.util.Log.d("ChartScreen", "LaunchedEffect verification ($indicatorKey): savedIndicators=$savedIndicators")
        } catch (e: Exception) {
            android.util.Log.e("ChartScreen", "Error in LaunchedEffect verification", e)
        }
    }

    var editingIndicator by remember { mutableStateOf<Indicator?>(null) }

    // Use global app theme to maintain status bar appearance
    BacktestTheme(darkTheme = globalIsDarkTheme) {
        val colorScheme = MaterialTheme.colorScheme
        var displayedCandles by remember { mutableStateOf<List<Candle>>(emptyList()) }
        // ── Zoom : chargé depuis SharedPreferences (par session) ──
        var zoomLevel by rememberSaveable(sessionName) {
            mutableFloatStateOf(sharedPrefs.getFloat("chart_zoom_level_$sKey", 1f))
        }
        var scrollOffset by remember { mutableFloatStateOf(0f) }

        // ── Hauteurs et offsets horizontaux des indicateurs : chargés par session ──
        val loadedIndicatorHeights = remember(sessionName) {
            try {
                val json = sharedPrefs.getString("indicator_heights_$sKey", null)
                if (json != null) {
                    val gson = com.google.gson.Gson()
                    val type = object : com.google.gson.reflect.TypeToken<Map<String, Float>>() {}.type
                    gson.fromJson<Map<String, Float>>(json, type) ?: emptyMap()
                } else emptyMap()
            } catch (e: Exception) { emptyMap<String, Float>() }
        }
        var indicatorHeightsMap by remember { mutableStateOf(loadedIndicatorHeights.toMutableMap()) }
        var indicatorRangesMap by remember { mutableStateOf(emptyMap<String, Pair<Float, Float>>().toMutableMap()) }

        var isLoading by remember { mutableStateOf(true) }
        var downloadProgress by remember { mutableIntStateOf(0) }
        var isDownloading by remember { mutableStateOf(false) }
        var loadingError by remember { mutableStateOf<String?>(null) }
        var loadingStep by remember { mutableStateOf("") }
        var isChartRendered by remember { mutableStateOf(false) }
        // Toujours afficher la dernière bougie à droite au chargement initial
        // (scrollOffset déjà à 0f via LaunchedEffect(selectedSymbol) et le bloc de chargement)

        // Save active indicators immediately when they change (par session)
        val indicatorKey = "active_indicators_$sKey"
        LaunchedEffect(activeIndicators, sessionName) {
            try {
                val serialized = IndicatorSerializer.serializeIndicators(activeIndicators)
                android.util.Log.d("ChartScreen", "Saving ${activeIndicators.size} indicators to $indicatorKey")
                sharedPrefs.edit().putString(indicatorKey, serialized).commit()
            } catch (e: Exception) {
                android.util.Log.e("ChartScreen", "Error saving indicators", e)
            }
        }

        // Additional safeguard: save when leaving the screen or app goes to background
        val latestActiveIndicators by rememberUpdatedState(activeIndicators)
        val latestSKey by rememberUpdatedState(sKey)
        DisposableEffect(Unit) {
            onDispose {
                try {
                    val serialized = IndicatorSerializer.serializeIndicators(latestActiveIndicators)
                    val key = "active_indicators_$latestSKey"
                    android.util.Log.d("ChartScreen", "Emergency save on dispose ($key): ${latestActiveIndicators.size} indicators")
                    sharedPrefs.edit().putString(key, serialized).commit()
                } catch (e: Exception) {
                    android.util.Log.e("ChartScreen", "Error in emergency save", e)
                }
            }
        }
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                    try {
                        val serialized = IndicatorSerializer.serializeIndicators(latestActiveIndicators)
                        val key = "active_indicators_$latestSKey"
                        android.util.Log.d("ChartScreen", "Save on stop ($key): ${latestActiveIndicators.size} indicators")
                        sharedPrefs.edit().putString(key, serialized).commit()
                    } catch (e: Exception) {
                        android.util.Log.e("ChartScreen", "Error saving on stop", e)
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        var showIndicatorsMenu by remember { mutableStateOf(false) }
        var showChartTypeMenu by remember { mutableStateOf(false) }
        var showDrawingToolsPanel by remember { mutableStateOf(false) }
        var drawingToolsSearchQuery by remember { mutableStateOf("") }
        var currentDrawingTool by remember { mutableStateOf(DrawingTool.NONE) }
        val initialDrawingUseMode = if (sharedPrefs.getString("drawing_use_mode", DrawingUseMode.SINGLE.name) == DrawingUseMode.SINGLE.name) {
            DrawingUseMode.SINGLE
        } else {
            DrawingUseMode.REPEAT
        }
        var drawingUseMode by rememberSaveable { mutableStateOf(initialDrawingUseMode) }
        var selectedTimeframe by rememberSaveable(sessionName) {
            val savedTf = sharedPrefs.getString("timeframe_$sKey", null)
            mutableStateOf(
                savedTf?.let { try { Timeframe.fromDisplayName(it) } catch (_: Exception) { null } }
                    ?: backtestTimeframe?.let { Timeframe.fromDisplayName(it) }
                    ?: Timeframe.H1
            )
        }
        var showTimeframeMenu by remember { mutableStateOf(false) }
        val selectedSymbol = backtestSymbol ?: "XAUUSD"
        var showSettingsMenu by remember { mutableStateOf(false) }
        var showChartSettingsDialog by remember { mutableStateOf(false) }
        var showMoreMenu by remember { mutableStateOf(false) }
        // ── Barre latérale gauche (toggle depuis la topbar) ──
        var showSidebar by remember {
            mutableStateOf(sharedPrefs.getBoolean("sidebar_open", false))
        }
        // Référence à l'activité pour contrôler les barres système
        val activity = LocalActivity.current
        // État pour contrôler le mode capture (masque toutes les barres)
        var isCapturing by remember { mutableStateOf(false) }
        // État pour contrôler le mode plein écran (masque barre de notification, navigation et "Open N")
        var isFullScreen by remember { mutableStateOf(false) }
        var showShareOptionsDialog by remember { mutableStateOf(false) }
        var showObjectTree by remember { mutableStateOf(false) }
        var chartBoundsInRoot by remember { mutableStateOf(android.graphics.Rect()) }

        var isPlaying by remember { mutableStateOf(false) }
        var isChartLocked by remember { mutableStateOf(false) }
        var isPencilLocked by remember { mutableStateOf(initialDrawingUseMode == DrawingUseMode.REPEAT) }
        var isStarActive by remember {
            mutableStateOf(sharedPrefs.getBoolean("show_favorites_bar", false))
        }
        var isMagnetActive by remember { mutableStateOf(sharedPrefs.getBoolean("magnet_active", false)) }
        // ── Capital : chargé depuis SharedPreferences (session) ──
        val sessionBalanceKey = "balance_${sessionName ?: "default"}"
        var currentBalance by remember {
            mutableFloatStateOf(
                sharedPrefs.getFloat(sessionBalanceKey, initialAmount?.toFloatOrNull() ?: 10000f)
            )
        }
        // Dépôts supplémentaires effectués pendant la session (sans recharger la nav)
        var extraCapital by remember { mutableFloatStateOf(0f) }
        // Dépôts individuels pour l'historique
        var capitalEvents by remember { mutableStateOf<List<Triple<Long, String, Float>>>(emptyList()) }  // (timestamp, type, montant)
        var replaySpeed by remember { mutableFloatStateOf(1f) } // 0.5x, 1x, 2x, 4x, 8x
        var playStep by remember { mutableIntStateOf(sharedPrefs.getInt("play_step_$sKey", selectedTimeframe.minutes)) }
        // Message "Fin des données" quand le replay atteint la dernière bougie
        var showEndOfData by remember { mutableStateOf(false) }
        val coroutineScopeEnd = rememberCoroutineScope()


        // TradeEntry provient de com.bthr.backtest.model.TradeEntry (importé)
        var tradeEntries by remember {
            mutableStateOf(
                if (!sessionName.isNullOrBlank())
                    SessionRepository.loadTrades(context, sessionName)
                else
                    emptyList()
            )
        }
        var currentLotSize by remember(sessionName) {
            mutableFloatStateOf(sharedPrefs.getFloat("lot_$sKey", 0.1f))
        }

        // ── Affichage des trades sur le chart (menu paramètres, persisté) ──
        var hideAll           by remember { mutableStateOf(false) }
        var showIndicators    by remember { mutableStateOf(sharedPrefs.getBoolean("show_indicators",      true)) }
        var showDrawings      by remember { mutableStateOf(sharedPrefs.getBoolean("show_drawings",        true)) }
        var showOpenOnChart   by remember { mutableStateOf(sharedPrefs.getBoolean("show_open_on_chart",   true)) }
        var showClosedOnChart by remember { mutableStateOf(sharedPrefs.getBoolean("show_closed_on_chart", false)) }
        var showTpSlOnChart   by remember { mutableStateOf(sharedPrefs.getBoolean("show_tpsl_on_chart",   true)) }
        var showStopOutLine   by remember { mutableStateOf(sharedPrefs.getBoolean("show_stop_out_line",   false)) }
        var showTradingBar    by remember { mutableStateOf(sharedPrefs.getBoolean("show_trading_bar",     true)) }
        var showReplayBar     by remember { mutableStateOf(sharedPrefs.getBoolean("show_replay_bar",      true)) }
        var stopOutTriggered  by remember { mutableStateOf(false) }

        // LaunchedEffect pour détecter quand isCapturing change
        LaunchedEffect(isCapturing) {
            if (isCapturing) {
                // Masquer les barres quand on entre en mode capture
                showTradingBar = false
                showReplayBar = false
            }
        }

        // Gestion du mode plein écran extraite dans ChartStateHelpers.kt
        ManageFullScreen(isFullScreen = isFullScreen, activity = activity)

        // ── Ligne Stop Out extraite dans ChartStateHelpers.kt ──
        val stopOutPrice by rememberStopOutPrice(tradeEntries, initialAmount, extraCapital, spreadPts, commPerLot)

        // ── Sauvegarde automatique à chaque changement de trades ──────
        LaunchedEffect(tradeEntries) {
            if (!sessionName.isNullOrBlank()) {
                SessionRepository.saveTrades(context, sessionName, tradeEntries)
            }
        }

        // ── Sauvegarde du capital en temps réel ─────────────────────
        LaunchedEffect(currentBalance) {
            sharedPrefs.edit().putFloat(sessionBalanceKey, currentBalance).apply()
        }

        // ── Sauvegarde du zoom ─────────────────────────────────────────
        LaunchedEffect(zoomLevel) {
            sharedPrefs.edit().putFloat("chart_zoom_level_$sKey", zoomLevel).apply()
        }

        // ── Sauvegarde du step de replay ────────────────────────────────
        LaunchedEffect(playStep) {
            sharedPrefs.edit().putInt("play_step_$sKey", playStep).apply()
        }

        // Système de replay minute par minute
        var allMinuteCandles: List<Candle> by remember { mutableStateOf(emptyList()) }
        var isMinuteReplayMode by remember { mutableStateOf(false) }
        var m1WindowStartTs by remember { mutableLongStateOf(0L) }
        var m1WindowEndTs by remember { mutableLongStateOf(0L) }
        var isLoadingMoreHistory by remember { mutableStateOf(false) }
        var isHistoryExhausted by remember { mutableStateOf(false) }
        var loadHistoryTrigger by remember { mutableIntStateOf(0) }
        var loadForwardTrigger by remember { mutableIntStateOf(0) }
        var visibleStartIdx by remember { mutableIntStateOf(0) }
        var visibleEndIdx by remember { mutableIntStateOf(0) }
        // Index M1 max à afficher (>= currentMinuteIndex). Sincé avec currentMinuteIndex
        // pendant le replay, et ajusté lors du chargement historique à gauche.
        var displayM1EndIndex by remember { mutableIntStateOf(0) }
        val coroutineScope = rememberCoroutineScope()

        // ── Positions sauvegardées : chargées SYNCHRONEMENT au démarrage ─────
        val initialSavedMinuteIndex = remember {
            if (!sessionName.isNullOrBlank())
                SessionRepository.loadMinuteIndex(context, sessionName) ?: -1
            else -1
        }
        val initialSavedMinuteTimestamp = remember {
            if (!sessionName.isNullOrBlank())
                SessionRepository.loadMinuteTimestamp(context, sessionName) ?: -1L
            else -1L
        }
        val initialSavedPosition = remember {
            if (!sessionName.isNullOrBlank())
                SessionRepository.loadReplayPosition(context, sessionName) ?: 0
            else 0
        }
        // currentMinuteIndex initialisé directement depuis la valeur sauvegardée
        var currentMinuteIndex by remember { mutableIntStateOf(maxOf(0, initialSavedMinuteIndex)) }
        var visibleCandleCount by remember { mutableIntStateOf(maxOf(1, initialSavedPosition)) }
        // hasAppliedInitialPosition = true si aucune position sauvegardée (calcul depuis date)
        var hasAppliedInitialPosition by remember {
            mutableStateOf(initialSavedMinuteIndex < 0 && initialSavedPosition <= 0)
        }

        // ── Sauvegarde de secours au dispose et onStop ───────────────────────
        val latestM1Candles by rememberUpdatedState(allMinuteCandles)
        val latestMinuteIndex by rememberUpdatedState(currentMinuteIndex)
        val latestCandleIndex by rememberUpdatedState(visibleCandleCount)
        val latestMinuteMode  by rememberUpdatedState(isMinuteReplayMode)
        val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
        DisposableEffect(lifecycle) {
            val save = {
                if (!sessionName.isNullOrBlank()) {
                    if (latestMinuteMode && latestMinuteIndex > 0) {
                        SessionRepository.saveMinuteIndex(context, sessionName, latestMinuteIndex)
                        if (latestMinuteIndex in latestM1Candles.indices) {
                            SessionRepository.saveMinuteTimestamp(context, sessionName, latestM1Candles[latestMinuteIndex].timestamp)
                        }
                    } else if (!latestMinuteMode && latestCandleIndex > 1) {
                        SessionRepository.saveReplayPosition(context, sessionName, latestCandleIndex)
                    }
                }
            }
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) save()
            }
            lifecycle.lifecycle.addObserver(observer)
            onDispose {
                save()
                lifecycle.lifecycle.removeObserver(observer)
            }
        }

        // ── Restaure currentMinuteIndex dès que allMinuteCandles est chargé ──
        LaunchedEffect(allMinuteCandles.size, isMinuteReplayMode) {
            if (isMinuteReplayMode && allMinuteCandles.isNotEmpty()) {
                if (initialSavedMinuteTimestamp >= 0L && !hasAppliedInitialPosition) {
                    val bestIdx = allMinuteCandles.indexOfLast { it.timestamp <= initialSavedMinuteTimestamp }
                    currentMinuteIndex = if (bestIdx >= 0) bestIdx
                        else initialSavedMinuteIndex.coerceIn(0, allMinuteCandles.size - 1)
                    hasAppliedInitialPosition = true
                } else if (initialSavedMinuteIndex >= 0 && !hasAppliedInitialPosition) {
                    currentMinuteIndex = initialSavedMinuteIndex.coerceIn(0, allMinuteCandles.size - 1)
                    hasAppliedInitialPosition = true
                }
                visibleCandleCount = displayedCandles.size
            }
        }

        // ── Sync visibleCandleCount en mode minute quand displayedCandles change ────
        // (après restauration de position, displayedCandles est recalculé, il faut re-sync)
        LaunchedEffect(displayedCandles.size, isMinuteReplayMode) {
            if (isMinuteReplayMode && displayedCandles.isNotEmpty()) {
                visibleCandleCount = displayedCandles.size
            }
        }

        // ── Restaure visibleCandleCount (mode non-minute) ────────────────────
        LaunchedEffect(displayedCandles.size) {
            if (displayedCandles.isEmpty() || isMinuteReplayMode) return@LaunchedEffect
            if (!hasAppliedInitialPosition) {
                if (initialSavedPosition > 0) {
                    // Mode standard : reprendre depuis la position sauvegardée
                    visibleCandleCount = initialSavedPosition.coerceIn(1, displayedCandles.size)
                    scrollOffset = 0f
                } else if (backtestDate != null) {
                    // Première ouverture : calcule la position depuis la date + heure de début
                    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val targetDate = java.time.LocalDate.parse(backtestDate, dateFormatter)
                    // Calcule le timestamp cible avec l'heure de départ
                    val targetTime = if (!backtestStartTime.isNullOrBlank() && backtestStartTime != "00:00") {
                        try {
                            val parts = backtestStartTime.split(":")
                            java.time.LocalTime.of(parts[0].toInt(), parts[1].toInt())
                        } catch (_: Exception) { java.time.LocalTime.MIDNIGHT }
                    } else java.time.LocalTime.MIDNIGHT
                    val targetTs = targetDate.atTime(targetTime)
                        .atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

                    val targetIndex = displayedCandles.indexOfFirst { candle ->
                        candle.timestamp >= targetTs
                    }
                    if (targetIndex >= 0) {
                        visibleCandleCount = targetIndex + 1
                        scrollOffset = 0f
                    }
                }
                hasAppliedInitialPosition = true
            }
        }

        // Sync displayM1EndIndex quand currentMinuteIndex avance
        LaunchedEffect(currentMinuteIndex) {
            if (currentMinuteIndex > displayM1EndIndex) {
                displayM1EndIndex = currentMinuteIndex
            }
        }

        // Détection des bords pour chargement paresseux
        val nearLeftEdge by remember {
            derivedStateOf {
                isMinuteReplayMode && !isLoading && displayedCandles.isNotEmpty() &&
                    !isHistoryExhausted && visibleStartIdx == 0 && scrollOffset > 0f
            }
        }
        val nearRightEdge by remember {
            derivedStateOf {
                isMinuteReplayMode && !isLoading && !isHistoryExhausted &&
                    allMinuteCandles.isNotEmpty() && m1WindowEndTs > 0L &&
                    currentMinuteIndex >= allMinuteCandles.size - 50
            }
        }

        LaunchedEffect(isPlaying, replaySpeed, isMinuteReplayMode, playStep, selectedTimeframe) {
            if (isPlaying) {
                while (isPlaying) {
                    val delayMs = when (replaySpeed) {
                        0.5f -> 1000L
                        1f -> 500L
                        2f -> 250L
                        4f -> 125L
                        8f -> 62L
                        else -> 500L
                    }
                    kotlinx.coroutines.delay(delayMs)

                    if (isMinuteReplayMode) {
                        // Mode minute replay
                        val stepMatchesTimeframe = playStep == selectedTimeframe.minutes
                        if (stepMatchesTimeframe) {
                            // Timeframe Sync: avancer à la fermeture de la bougie suivante
                            val tfMin = selectedTimeframe.minutes
                            val posInGroup = currentMinuteIndex % tfMin
                            val minutesToAdvance = if (posInGroup == tfMin - 1) {
                                // Déjà à la fin d'un groupe complet → sauter au groupe suivant
                                tfMin
                            } else {
                                // Avancer à la fin du groupe actuel
                                tfMin - 1 - posInGroup
                            }
                            val newIndex = (currentMinuteIndex + minutesToAdvance).coerceAtMost(allMinuteCandles.size - 1)
                            if (newIndex > currentMinuteIndex) {
                                currentMinuteIndex = newIndex
                                if (!sessionName.isNullOrBlank()) {
                                    SessionRepository.saveMinuteIndex(context, sessionName, currentMinuteIndex)
                                    if (currentMinuteIndex in allMinuteCandles.indices) {
                                        SessionRepository.saveMinuteTimestamp(context, sessionName, allMinuteCandles[currentMinuteIndex].timestamp)
                                    }
                                }
                            } else if (isLoadingMoreHistory || nearRightEdge) {
                                kotlinx.coroutines.delay(100)
                            } else {
                                isPlaying = false
                                showEndOfData = true
                                coroutineScopeEnd.launch {
                                    kotlinx.coroutines.delay(3000L)
                                    showEndOfData = false
                                }
                            }
                        } else {
                            // Avancer selon playStep
                            if (currentMinuteIndex < allMinuteCandles.size - 1) {
                                currentMinuteIndex = (currentMinuteIndex + playStep).coerceAtMost(allMinuteCandles.size - 1)
                                if (!sessionName.isNullOrBlank()) {
                                    SessionRepository.saveMinuteIndex(context, sessionName, currentMinuteIndex)
                                    if (currentMinuteIndex in allMinuteCandles.indices) {
                                        SessionRepository.saveMinuteTimestamp(context, sessionName, allMinuteCandles[currentMinuteIndex].timestamp)
                                    }
                                }
                            } else if (isLoadingMoreHistory || nearRightEdge) {
                                kotlinx.coroutines.delay(100)
                            } else {
                                isPlaying = false
                                showEndOfData = true
                                coroutineScopeEnd.launch {
                                    kotlinx.coroutines.delay(3000L)
                                    showEndOfData = false
                                }
                            }
                        }
                    } else {
                        // Mode standard: avancer d'une bougie complète
                        if (visibleCandleCount < displayedCandles.size) {
                            visibleCandleCount++
                            if (!sessionName.isNullOrBlank()) {
                                SessionRepository.saveReplayPosition(context, sessionName, visibleCandleCount)
                            }
                        } else if (isLoadingMoreHistory) {
                            kotlinx.coroutines.delay(100)
                        } else {
                            isPlaying = false
                            showEndOfData = true
                            coroutineScopeEnd.launch {
                                kotlinx.coroutines.delay(3000L)
                                showEndOfData = false
                            }
                        }
                    }
                }
            } else {
                // Replay mis en pause → rien à faire ici,
                // la position a déjà été sauvegardée lors du dernier visibleCandleCount++
            }
        }

        var lastTimeframe by remember { mutableStateOf(selectedTimeframe) }

        LaunchedEffect(visibleCandleCount, displayedCandles, tradeEntries, currentMinuteIndex) {
            // Skip auto-close stop-out when timeframe has just changed (candles fully recomputed)
            val timeframeChanged = selectedTimeframe != lastTimeframe
            if (timeframeChanged) lastTimeframe = selectedTimeframe

            // Determine current price — use minute-level source in minute replay mode
            // (timeframe-independent: same price regardless of selected TF)
            val midPrice: Float?
            val currentTimestamp: Long
            if (isMinuteReplayMode && currentMinuteIndex in allMinuteCandles.indices) {
                val m1 = allMinuteCandles[currentMinuteIndex]
                midPrice = m1.close
                currentTimestamp = m1.timestamp
            } else if (visibleCandleCount > 0 && visibleCandleCount <= displayedCandles.size) {
                val c = displayedCandles[visibleCandleCount - 1]
                midPrice = c.close
                currentTimestamp = c.timestamp
            } else {
                midPrice = null
                currentTimestamp = 0L
            }

            if (midPrice != null) {
                tradeEntries = tradeEntries.map { entry ->
                    if (entry.exitPrice == null) {
                        val closePrice = if (entry.type == "buy") {
                            midPrice
                        } else {
                            midPrice + spreadPts
                        }
                        
                        val rawProfit = if (entry.type == "buy") {
                            (closePrice - entry.entryPrice) * entry.lotSize * 100f
                        } else {
                            (entry.entryPrice - closePrice) * entry.lotSize * 100f
                        }
                        val commCost   = commPerLot * entry.lotSize
                        val profit = rawProfit - commCost
                        entry.copy(profit = profit)
                    } else {
                        entry
                    }
                }

                val totalProfit = tradeEntries.sumOf { (it.profit ?: 0f).toDouble() }.toFloat()
                if (!stopOutTriggered) {
                    currentBalance = maxOf(0f, (initialAmount?.toFloatOrNull() ?: 10000f) + extraCapital + totalProfit)
                }

                //  Auto-fermeture Stop Out : capital ≤ 0 ────────────────────
                if (!timeframeChanged && currentBalance <= 0f && tradeEntries.any { it.exitPrice == null }) {
                    tradeEntries = tradeEntries.map { entry ->
                        if (entry.exitPrice == null) {
                            val closePrice = if (entry.type == "buy") {
                                midPrice
                            } else {
                                midPrice + spreadPts
                            }
                            
                            val rawP = if (entry.type == "buy")
                                (closePrice - entry.entryPrice) * entry.lotSize * 100f
                            else
                                (entry.entryPrice - closePrice) * entry.lotSize * 100f
                            val commCost   = commPerLot * entry.lotSize
                            entry.copy(
                                exitPrice     = closePrice,
                                exitTimestamp = currentTimestamp.takeIf { it > 0L } ?: System.currentTimeMillis(),
                                profit        = rawP - commCost
                            )
                        } else entry
                    }
                    val recalculatedTotal = tradeEntries.sumOf { (it.profit ?: 0f).toDouble() }.toFloat()
                    currentBalance = maxOf(0f, (initialAmount?.toFloatOrNull() ?: 10000f) + extraCapital + recalculatedTotal)
                }
            }
        }

        var favoriteTools by remember {
            mutableStateOf(
                sharedPrefs.getString("favorite_tools", "")?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
            )
        }
        var favoriteIndicators by remember {
            mutableStateOf(
                sharedPrefs.getString("favorite_indicators", "")?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
            )
        }
        var activateDrawingModeTrigger by remember { mutableIntStateOf(0) }
        var cancelDrawingTrigger by remember { mutableIntStateOf(0) }
        var selectedSimpleTool by remember { mutableStateOf<DrawingTool>(DrawingTool.NONE) }
        var isIntentionalDrawingActivation by remember { mutableStateOf(false) }

        val onFavoritesBarToolSelected: (DrawingTool) -> Unit = { tool ->
            if (tool == DrawingTool.NONE) {
                selectedSimpleTool = DrawingTool.NONE
                currentDrawingTool = DrawingTool.NONE
            } else {
                if (isChartLocked) isChartLocked = false
                currentDrawingTool = tool
                isIntentionalDrawingActivation = true
                val simpleTools = setOf(
                    DrawingTool.TREND_LINE, DrawingTool.EXTENDED_LINE, DrawingTool.RAY,
                    DrawingTool.HORIZONTAL_LINE, DrawingTool.VERTICAL_LINE,
                    DrawingTool.CROSS_LINE, DrawingTool.ARROW,
                    DrawingTool.HORIZONTAL_RAY,
                    DrawingTool.TEXT, DrawingTool.ANCHORED_TEXT
                )
                if (tool in simpleTools) {
                    selectedSimpleTool = tool
                    activateDrawingModeTrigger++
                } else {
                    selectedSimpleTool = DrawingTool.NONE
                }
            }
        }

        var simpleTrendLines by remember { mutableStateOf<List<SimpleTrendLine>>(emptyList()) }
        var arrows by remember { mutableStateOf<List<SimpleTrendLine>>(emptyList()) }
        var extendedLines by remember { mutableStateOf<List<SimpleTrendLine>>(emptyList()) }
        var rays by remember { mutableStateOf<List<SimpleTrendLine>>(emptyList()) }

        val loadedDrawings = remember(sessionName) {
            com.bthr.backtest.util.DrawingPersistence.loadAll(context, sKey)
        }
        var completedDrawingsLoaded by remember { mutableStateOf(false) }

        // ── Système Undo/Redo extrait dans ChartStateHelpers.kt ──
        var lastDrawCounts by remember { mutableStateOf(intArrayOf(0, 0, 0, 0)) }
        var verticalMinPrice by remember { mutableFloatStateOf(0f) }
        var verticalMaxPrice by remember { mutableFloatStateOf(100f) }

        val drawingManager = remember { DrawingManager() }

        LaunchedEffect(sessionName) {
            simpleTrendLines = loadedDrawings.simpleTrendLines
            arrows = loadedDrawings.arrows
            extendedLines = loadedDrawings.extendedLines
            rays = loadedDrawings.rays
            if (loadedDrawings.completedDrawings.isNotEmpty()) {
                drawingManager.restoreCompletedDrawings(loadedDrawings.completedDrawings)
            }
            completedDrawingsLoaded = true
        }

        LaunchedEffect(simpleTrendLines, arrows, extendedLines, rays, drawingManager.completedDrawings) {
            if (!completedDrawingsLoaded) return@LaunchedEffect
            com.bthr.backtest.util.DrawingPersistence.saveAll(context, sKey,
                com.bthr.backtest.util.DrawingsState(
                    simpleTrendLines = simpleTrendLines,
                    arrows = arrows,
                    extendedLines = extendedLines,
                    rays = rays,
                    completedDrawings = drawingManager.completedDrawings
                )
            )
        }
        val undoRedo = rememberChartUndoRedo(
            getTrendLines = { simpleTrendLines }, setTrendLines = { simpleTrendLines = it },
            getArrows = { arrows }, setArrows = { arrows = it },
            getExtLines = { extendedLines }, setExtLines = { extendedLines = it },
            getRays = { rays }, setRays = { rays = it },
            getIndicators = { activeIndicators }, setIndicators = { activeIndicators = it },
            getVerticalMin = { verticalMinPrice }, setVerticalMin = { verticalMinPrice = it },
            getVerticalMax = { verticalMaxPrice }, setVerticalMax = { verticalMaxPrice = it },
            getTimeframe = { selectedTimeframe }, setTimeframe = { selectedTimeframe = it },
            getSettings = { chartSettings }, setSettings = { chartSettings = it },
            setLastDrawCounts = { lastDrawCounts = it },
            getCompletedDrawings = { drawingManager.completedDrawings },
            setCompletedDrawings = { drawingManager.restoreCompletedDrawings(it) },
            getIndicatorHeights = { indicatorHeightsMap },
            setIndicatorHeights = { indicatorHeightsMap = it.toMutableMap() },
            getIndicatorRanges = { indicatorRangesMap },
            setIndicatorRanges = { indicatorRangesMap = it.toMutableMap() }
        )
        val snapshotChartState = undoRedo.snapshotChartState
        val canUndo by undoRedo.canUndo
        val canRedo by undoRedo.canRedo
        val performUndo = undoRedo.performUndo
        val performRedo = undoRedo.performRedo

        // Connecter le callback onBeforeChange du DrawingManager au snapshot undo/redo
        val currentSnapshotChartState by rememberUpdatedState(snapshotChartState)
        LaunchedEffect(drawingManager) {
            drawingManager.onBeforeChange = { currentSnapshotChartState() }
        }

        LaunchedEffect(activateDrawingModeTrigger) {
            if (activateDrawingModeTrigger > 0) {
                kotlinx.coroutines.delay(100)
                isIntentionalDrawingActivation = false
            }
        }

        LaunchedEffect(selectedSymbol) {
            isLoading = true
            isDownloading = true
            downloadProgress = 0
            loadingError = null
            isHistoryExhausted = false
            isChartRendered = false
            scrollOffset = 0f
            loadingStep = "Connexion au serveur..."
            yield()
            try {
                // Calcul du timestamp de backtest avec date + heure de départ (fait AVANT le
                // téléchargement pour ne demander que la fenêtre réellement nécessaire)
                val backtestTs = if (backtestDate != null) {
                    val fmt  = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val date = java.time.LocalDate.parse(backtestDate, fmt)
                    // Parse l'heure de départ si disponible (format "HH:mm")
                    val time = if (!backtestStartTime.isNullOrBlank() && backtestStartTime != "00:00") {
                        try {
                            val parts = backtestStartTime.split(":")
                            java.time.LocalTime.of(parts[0].toInt(), parts[1].toInt())
                        } catch (_: Exception) { java.time.LocalTime.MIDNIGHT }
                    } else java.time.LocalTime.MIDNIGHT
                    date.atTime(time).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
                } else System.currentTimeMillis()
                // Fenêtre initiale : charge un maximum d'historique (≈10 ans) pour éviter les allers-retours
                val maxLookbackMs = 10L * 365 * 24 * 60 * 60 * 1000  // 10 ans
                // Fenêtre basée sur le nombre de bougies M1 max (~100K = 69 jours)
                // Quel que soit le timeframe, on évite de charger des millions de M1
                val maxM1CandlesToLoad = 100_000L
                val initialWindowBeforeMs = (maxM1CandlesToLoad * 60 * 1000L)
                    .coerceAtMost(maxLookbackMs)
                // ~2000 minutes (1.4 jours) de données après le point de départ
                val initialWindowAfterMs = (2000L * 60 * 1000L)
                    .coerceAtMost(365L * 24 * 60 * 60 * 1000L)
                val restoreTs = initialSavedMinuteTimestamp.takeIf { it >= 0L }
                val windowFromTs = (restoreTs ?: backtestTs) - initialWindowBeforeMs
                val windowToTs   = (restoreTs ?: backtestTs) + initialWindowAfterMs

                loadingStep = "Téléchargement des données..."
                yield()
                val binFile = withContext(Dispatchers.IO) {
                    FileDownloader.downloadPriorityWindow(context, windowFromTs, windowToTs) { progress ->
                        downloadProgress = progress
                    }
                }
                isDownloading = false
                loadingStep = "Préparation du graphique..."
                yield()

                if (binFile == null) {
                    throw Exception("Impossible de télécharger les données. Vérifiez votre connexion Internet.")
                }

                // Le reste de l'historique continue de se télécharger en arrière-plan (après 5s pour ne pas concurrencer l'affichage)
                delay(5000)
                FileDownloader.startBackgroundFullDownload(context)

                val loaded = withContext(Dispatchers.IO) {
                    val m1Ohlcv = CsvParser.loadCandlesDirectFromBin(binFile, windowFromTs, windowToTs)
                    if (m1Ohlcv.isNotEmpty()) {
                        val idx = if (backtestDate != null) {
                            val found = m1Ohlcv.indexOfFirst { it.timestamp >= backtestTs }
                            if (found >= 0) found else m1Ohlcv.size - 1
                        } else m1Ohlcv.size - 1
                        Pair(m1Ohlcv, idx)
                    } else {
                        throw Exception("Impossible de charger les données. Vérifiez votre connexion Internet ou réessayez.")
                    }
                }

                // Main thread: mutation d'état s'il n'y a pas d'erreur
                if (loaded.first.isNotEmpty()) {
                    loadingStep = "Agrégation des données..."
                    yield()
                    val agg = withContext(Dispatchers.Default) {
                        CandleUtil.aggregatePartial(loaded.first, loaded.second, selectedTimeframe)
                    }
                    allMinuteCandles = loaded.first
                    m1WindowStartTs = loaded.first.first().timestamp
                    m1WindowEndTs = loaded.first.last().timestamp
                    currentMinuteIndex = loaded.second
                    displayM1EndIndex = loaded.second
                    isMinuteReplayMode = true
                    displayedCandles = agg
                    // Toujours ouvrir à la dernière bougie (scrollOffset = 0)
                    scrollOffset = 0f
                    loadingError = null
                    isChartRendered = true
                } else {
                    loadingError = "Impossible de charger les données. Vérifiez votre connexion Internet ou réessayez."
                    displayedCandles = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("ChartScreen", "Erreur lors du chargement des donn\u00e9es", e)
                loadingError = e.message ?: "Erreur lors du téléchargement des données. Vérifiez votre connexion Internet ou réessayez."
                displayedCandles = emptyList()
            } finally {
                isLoading = false
            }
        }

        // ⚡ Cache pour l'agrégation incrémentale : évite de recalculer TOUT l'historique
        // à chaque tick du replay (voir CandleUtil.advancePartial). Invalidé automatiquement
        // dès que le timeframe, le mode includePartial, ou la taille de allMinuteCandles change.
        var lastAggIndex by remember { mutableStateOf(-1) }
        var lastAggTimeframe by remember { mutableStateOf<Timeframe?>(null) }
        var lastAggIncludePartial by remember { mutableStateOf(true) }
        var lastAggArraySize by remember { mutableStateOf(-1) }

        // Recalcule les bougies affichées quand l'index d'affichage ou timeframe change
        LaunchedEffect(displayM1EndIndex, selectedTimeframe, allMinuteCandles, playStep) {
            if (isMinuteReplayMode && allMinuteCandles.isNotEmpty()) {
                val stepMatchesTimeframe = playStep == selectedTimeframe.minutes
                val includePartial = !stepMatchesTimeframe

                val m1Ref = allMinuteCandles
                displayedCandles = withContext(Dispatchers.Default) {
                    CandleUtil.aggregatePartial(
                        m1Ref, displayM1EndIndex,
                        selectedTimeframe, includePartial
                    )
                }
                lastAggIndex = displayM1EndIndex
                lastAggTimeframe = selectedTimeframe
                lastAggIncludePartial = includePartial
                lastAggArraySize = allMinuteCandles.size
            }
        }

        LaunchedEffect(nearLeftEdge, loadHistoryTrigger) {
            if (!nearLeftEdge || isLoadingMoreHistory || m1WindowStartTs <= 0L) return@LaunchedEffect
            isLoadingMoreHistory = true
            coroutineScope.launch {
                try {
                    // Charge 30 jours de données M1 par chunk pour éviter de bloquer le main thread
                    val LOAD_CHUNK_MS = 30L * 24 * 60 * 60 * 1000
                    val newFromTs = m1WindowStartTs - LOAD_CHUNK_MS
                    val moreM1 = withContext(Dispatchers.IO) {
                        val binFile = FileDownloader.ensureYearAvailableForRange(context, newFromTs, m1WindowStartTs)
                        CsvParser.loadCandlesDirectFromBin(binFile, newFromTs, m1WindowStartTs)
                    }

                    if (moreM1.isNotEmpty()) {
                        isHistoryExhausted = false

                        val chunkM1StartTs = moreM1.first().timestamp
                        val chunkM1EndTs = moreM1.last().timestamp
                        val shift = moreM1.size

// Agrégation du chunk + merge avec displayedCandles : O(chunk) pas O(total)
                        val (combinedM1, mergedDisplayed) = withContext(Dispatchers.Default) {
                            val intervalMs = selectedTimeframe.minutes * 60 * 1000L

                            // 1) Construire la liste M1 combinée sans copier toute allMinuteCandles
                            val combined = ArrayList<Candle>(shift + allMinuteCandles.size).apply {
                                addAll(moreM1)
                                addAll(allMinuteCandles)
                            }

                            // 2) Agréger uniquement le nouveau chunk au timeframe cible
                            val chunkAgg = CandleUtil.aggregatePartial(moreM1, shift - 1, selectedTimeframe, false)

                            // 3) Fusionner avec les bougies affichées existantes
                            val merged = if (chunkAgg.isEmpty()) {
                                displayedCandles
                            } else if (displayedCandles.isEmpty()) {
                                chunkAgg
                            } else {
                                val lastChunkGroup = (chunkAgg.last().timestamp / intervalMs) * intervalMs
                                val firstDisplayedGroup = (displayedCandles.first().timestamp / intervalMs) * intervalMs
                                val result = ArrayList<Candle>(chunkAgg.size + displayedCandles.size)

                                if (lastChunkGroup == firstDisplayedGroup) {
                                    // Même barre de timeframe : fusionner la dernière du chunk avec la première affichée
                                    result.addAll(chunkAgg.subList(0, chunkAgg.size - 1))
                                    val lc = chunkAgg.last()
                                    val fe = displayedCandles.first()
                                    result.add(Candle(lastChunkGroup, lc.open,
                                        maxOf(lc.high, fe.high), minOf(lc.low, fe.low),
                                        fe.close, lc.volume + fe.volume))
                                    result.addAll(displayedCandles.subList(1, displayedCandles.size))
                                } else {
                                    result.addAll(chunkAgg)
                                    result.addAll(displayedCandles)
                                }
                                result
                            }
Pair(combined, merged)
                        }

                        // O(1) sur le main thread : juste des assignations de références
                        currentMinuteIndex += shift
                        displayM1EndIndex += shift
                        val MAX_M1 = 200_000
                        allMinuteCandles = if (combinedM1.size > MAX_M1) {
                            val excess = combinedM1.size - MAX_M1
                            currentMinuteIndex -= excess
                            displayM1EndIndex -= excess
                            ArrayList(combinedM1.drop(excess))
                        } else {
                            combinedM1
                        }
                        m1WindowStartTs = if (allMinuteCandles.isNotEmpty()) allMinuteCandles.first().timestamp else 0L
                        lastAggIndex = displayM1EndIndex
                        lastAggTimeframe = selectedTimeframe
                        lastAggIncludePartial = !(playStep == selectedTimeframe.minutes)
                        lastAggArraySize = allMinuteCandles.size
                        displayedCandles = mergedDisplayed
                    } else {
                        isHistoryExhausted = true
                    }
                } finally {
                    isLoadingMoreHistory = false
                }
            }
        }

        // Re-déclenchement progressif : remplir le vide automatiquement
        // visibleStartIdx == 0 → l'écran n'est pas rempli, on recharge
        // allMinuteCandles.size change après chaque chargement → clé unique
        LaunchedEffect(visibleStartIdx, allMinuteCandles.size) {
            if (!isHistoryExhausted && !isLoadingMoreHistory && isMinuteReplayMode
                && displayedCandles.isNotEmpty() && visibleStartIdx == 0 && scrollOffset > 0f
                && m1WindowStartTs > 0L) {
                loadHistoryTrigger++
            }
        }

        // Chargement paresseux VERS L'AVANT (replay uniquement)
        LaunchedEffect(nearRightEdge, loadForwardTrigger) {
            if (!nearRightEdge || isLoadingMoreHistory || m1WindowEndTs <= 0L) return@LaunchedEffect
            isLoadingMoreHistory = true
            coroutineScope.launch {
                try {
                    val tfMin = selectedTimeframe.minutes.coerceAtLeast(1)
                    val chunkBars = 300
                    val maxLookaheadMs = 90L * 24 * 60 * 60 * 1000  // 90 jours max
                    val chunkMs = (chunkBars.toLong() * tfMin * 60 * 1000L).coerceAtMost(maxLookaheadMs)
                    val newToTs = m1WindowEndTs + chunkMs
                    val moreM1 = withContext(Dispatchers.IO) {
                        val binFile = FileDownloader.ensureYearAvailableForRange(context, m1WindowEndTs, newToTs)
                        CsvParser.loadCandlesDirectFromBin(binFile, m1WindowEndTs + 1, newToTs)
                    }

                    if (moreM1.isNotEmpty()) {
                        allMinuteCandles = ArrayList<Candle>(allMinuteCandles.size + moreM1.size).apply {
                            addAll(allMinuteCandles)
                            addAll(moreM1)
                        }
                        m1WindowEndTs = moreM1.last().timestamp
                        // Plafonne le nombre de M1 pour éviter l'OOM (heap ~200MB max)
                        val MAX_M1 = 200_000
                        if (allMinuteCandles.size > MAX_M1) {
                            val excess = allMinuteCandles.size - MAX_M1
                            allMinuteCandles = ArrayList(allMinuteCandles.drop(excess))
                            currentMinuteIndex -= excess
                            displayM1EndIndex -= excess
                        }
                        if (nearRightEdge) loadForwardTrigger++
                    } else {
                        isHistoryExhausted = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChartScreen", "Forward load error", e)
                } finally {
                    isLoadingMoreHistory = false
                }
            }
        }

        if (showChartSettingsDialog) {
            ChartSettingsDialog(
                onDismiss = { showChartSettingsDialog = false },
                onApply = { newSettings ->
                    snapshotChartState()
                    chartSettings = newSettings
                    saveChartSettings(sharedPrefs, newSettings, globalIsDarkTheme)
                    showChartSettingsDialog = false
                },
                initialSettings = chartSettings,
                isDarkTheme = globalIsDarkTheme,
                referenceTimestamp = displayedCandles.lastOrNull()?.timestamp ?: System.currentTimeMillis()
            )
        }

        if (loadingError != null) {
            AlertDialog(
                onDismissRequest = { loadingError = null },
                title = { Text("Erreur de chargement") },
                text = { Text(loadingError ?: "Erreur inconnue") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            loadingError = null
                            navController.popBackStack()
                        }
                    ) {
                        Text("Retour")
                    }
                }
            )
        }

        // Bottom Sheet partage (extrait dans ChartShareBottomSheet.kt)
        ChartShareBottomSheet(
            visible = showShareOptionsDialog,
            onDismiss = { showShareOptionsDialog = false },
            onSaveImage = {
                showShareOptionsDialog = false
                isCapturing = true
                coroutineScope.launch {
                    kotlinx.coroutines.delay(800)
                    ChartExportUtil.saveAndNotifyChartImage(
                        context, selectedSymbol, sessionName,
                        selectedTimeframe.displayName, chartSettings.timezone, activeIndicators,
                        chartBoundsInRoot
                    )
                    isCapturing = false
                }
            },
            onShareImage = {
                showShareOptionsDialog = false
                isCapturing = true
                coroutineScope.launch {
                    kotlinx.coroutines.delay(800)
                    ChartExportUtil.shareChartImageDirect(
                        context, selectedSymbol, sessionName,
                        selectedTimeframe.displayName, chartSettings.timezone, activeIndicators,
                        chartBoundsInRoot
                    )
                    isCapturing = false
                }
            }
        )

        // Écran Arborescence d'objets
        if (showObjectTree) {
            ObjectTreeScreen(
                symbol = selectedSymbol,
                timeframe = selectedTimeframe.displayName,
                indicators = activeIndicators,
                onIndicatorVisibilityChange = { ind: Indicator, visible: Boolean ->
                    val newList = activeIndicators.map { if (it.id == ind.id) {
                        when (ind) {
                            is Indicator.RSI -> ind.copy(isVisible = visible)
                            is Indicator.MACD -> ind.copy(isVisible = visible)
                            is Indicator.SMA -> ind.copy(isVisible = visible)
                            is Indicator.EMA -> ind.copy(isVisible = visible)
                            is Indicator.HMA -> ind.copy(isVisible = visible)
                            is Indicator.WMA -> ind.copy(isVisible = visible)
                            is Indicator.BollingerBands -> ind.copy(isVisible = visible)
                            is Indicator.ATRBands -> ind.copy(isVisible = visible)
                            is Indicator.RSI -> ind.copy(isVisible = visible)
                            is Indicator.MACD -> ind.copy(isVisible = visible)
                            is Indicator.Stochastic -> ind.copy(isVisible = visible)
                            is Indicator.ATR -> ind.copy(isVisible = visible)
                            is Indicator.Supertrend -> ind.copy(isVisible = visible)
                            is Indicator.Alligator -> ind.copy(isVisible = visible)
                            is Indicator.Ichimoku -> ind.copy(isVisible = visible)
                            is Indicator.Sessions -> ind.copy(isVisible = visible)
                            is Indicator.VWAP -> ind.copy(isVisible = visible)
                            is Indicator.Ribbon -> ind.copy(isVisible = visible)
                            is Indicator.Volume -> ind.copy(isVisible = visible)
                            else -> it
                        }
                    } else it }
                    activeIndicators = newList
                    try {
                        val serialized = IndicatorSerializer.serializeIndicators(newList)
                        sharedPrefs.edit().putString(indicatorKey, serialized).commit()
                    } catch (_: Exception) {}
                },
                onIndicatorDelete = { ind: Indicator ->
                    activeIndicators = activeIndicators.filter { it.id != ind.id }
                    try {
                        val serialized = IndicatorSerializer.serializeIndicators(activeIndicators)
                        sharedPrefs.edit().putString(indicatorKey, serialized).commit()
                    } catch (_: Exception) {}
                },
                drawings = drawingManager.completedDrawings.toList(),
                onDrawingVisibilityChange = { drawing: com.bthr.backtest.model.Drawing, visible: Boolean ->
                    val updated = drawingManager.completedDrawings.map {
                        if (it.id == drawing.id) {
                            when (it) {
                                is com.bthr.backtest.model.Drawing.TrendLine -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.HorizontalLine -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.HorizontalRay -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.VerticalLine -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.CrossLine -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.ParallelChannel -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.FibRetracement -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.Rectangle -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.Circle -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.Arrow -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.Polyline -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.Path -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.Brush -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.TextLabel -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.AnchoredText -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.Measure -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.LongPosition -> it.copy(isVisible = visible)
                                is com.bthr.backtest.model.Drawing.ShortPosition -> it.copy(isVisible = visible)
                                else -> it
                            }
                        } else it
                    }
                    drawingManager.restoreCompletedDrawings(updated)
                },
                onDrawingLockChange = { drawing: com.bthr.backtest.model.Drawing, locked: Boolean ->
                    val updated = drawingManager.completedDrawings.map {
                        if (it.id == drawing.id) {
                            when (it) {
                                is com.bthr.backtest.model.Drawing.TrendLine -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.HorizontalLine -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.HorizontalRay -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.VerticalLine -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.CrossLine -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.ParallelChannel -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.FibRetracement -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.Rectangle -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.Circle -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.Arrow -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.Polyline -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.Path -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.Brush -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.TextLabel -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.AnchoredText -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.Measure -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.LongPosition -> it.copy(isLocked = locked)
                                is com.bthr.backtest.model.Drawing.ShortPosition -> it.copy(isLocked = locked)
                                else -> it
                            }
                        } else it
                    }
                    drawingManager.restoreCompletedDrawings(updated)
                },
                onDrawingDelete = { drawing: com.bthr.backtest.model.Drawing ->
                    drawingManager.restoreCompletedDrawings(drawingManager.completedDrawings.filter { it.id != drawing.id })
                },
                simpleLines = simpleTrendLines,
                onSimpleLineVisibilityChange = { i, visible ->
                    simpleTrendLines = simpleTrendLines.toMutableList().also { it[i] = it[i].copy(isVisible = visible) }
                },
                onSimpleLineLockChange = { i, locked ->
                    simpleTrendLines = simpleTrendLines.toMutableList().also { it[i] = it[i].copy(isLocked = locked) }
                },
                onSimpleLineDelete = { i ->
                    if (i < simpleTrendLines.size) simpleTrendLines = simpleTrendLines.toMutableList().also { it.removeAt(i) }
                },
                arrows = arrows,
                onArrowVisibilityChange = { i, visible ->
                    arrows = arrows.toMutableList().also { it[i] = it[i].copy(isVisible = visible) }
                },
                onArrowLockChange = { i, locked ->
                    arrows = arrows.toMutableList().also { it[i] = it[i].copy(isLocked = locked) }
                },
                onArrowDelete = { i ->
                    if (i < arrows.size) arrows = arrows.toMutableList().also { it.removeAt(i) }
                },
                extendedLines = extendedLines,
                onExtendedVisibilityChange = { i, visible ->
                    extendedLines = extendedLines.toMutableList().also { it[i] = it[i].copy(isVisible = visible) }
                },
                onExtendedLockChange = { i, locked ->
                    extendedLines = extendedLines.toMutableList().also { it[i] = it[i].copy(isLocked = locked) }
                },
                onExtendedDelete = { i ->
                    if (i < extendedLines.size) extendedLines = extendedLines.toMutableList().also { it.removeAt(i) }
                },
                rays = rays,
                onRayVisibilityChange = { i, visible ->
                    rays = rays.toMutableList().also { it[i] = it[i].copy(isVisible = visible) }
                },
                onRayLockChange = { i, locked ->
                    rays = rays.toMutableList().also { it[i] = it[i].copy(isLocked = locked) }
                },
                onRayDelete = { i ->
                    if (i < rays.size) rays = rays.toMutableList().also { it.removeAt(i) }
                },
                onDeleteAllDrawings = { includeLocked ->
                    if (includeLocked) {
                        drawingManager.restoreCompletedDrawings(emptyList())
                        simpleTrendLines = emptyList()
                        arrows = emptyList()
                        extendedLines = emptyList()
                        rays = emptyList()
                    } else {
                        drawingManager.restoreCompletedDrawings(
                            drawingManager.completedDrawings.filter { it.isLocked }
                        )
                        simpleTrendLines = simpleTrendLines.filter { it.isLocked }
                        arrows = arrows.filter { it.isLocked }
                        extendedLines = extendedLines.filter { it.isLocked }
                        rays = rays.filter { it.isLocked }
                    }
                },
                onDeleteAllIndicators = { _ ->
                    activeIndicators = emptyList()
                },
                onDeleteAll = { includeLocked ->
                    if (includeLocked) {
                        drawingManager.restoreCompletedDrawings(emptyList())
                        simpleTrendLines = emptyList()
                        arrows = emptyList()
                        extendedLines = emptyList()
                        rays = emptyList()
                    } else {
                        drawingManager.restoreCompletedDrawings(
                            drawingManager.completedDrawings.filter { it.isLocked }
                        )
                        simpleTrendLines = simpleTrendLines.filter { it.isLocked }
                        arrows = arrows.filter { it.isLocked }
                        extendedLines = extendedLines.filter { it.isLocked }
                        rays = rays.filter { it.isLocked }
                    }
                    activeIndicators = emptyList()
                },
                onDismiss = { showObjectTree = false },
                isDarkTheme = globalIsDarkTheme
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = { if (!isCapturing && !isFullScreen) {
                // TopBar extraite dans ChartTopBar.kt
                ChartTopBar(
                    sessionName = sessionName,
                    tradeEntries = tradeEntries,
                    currentBalance = currentBalance,
                    isFullScreen = isFullScreen,
                    onToggleFullScreen = { isFullScreen = !isFullScreen },
                    selectedSymbol = selectedSymbol,
                    selectedTimeframe = selectedTimeframe,
                    isSidebarOpen = showSidebar,
                    onToggleSidebar = {
                        showSidebar = !showSidebar
                        sharedPrefs.edit().putBoolean("sidebar_open", showSidebar).apply()
                    },
                    onShowTimeframeMenu = { showTimeframeMenu = true },
                    onShowIndicatorsMenu = { showIndicatorsMenu = true },
                    onShowChartTypeMenu = { showChartTypeMenu = true },
                    selectedChartType = chartSettings.chartType,
                    onHideDrawingTools = { showDrawingToolsPanel = false },
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = { performUndo() },
                    onRedo = { performRedo() },
                    chartSettings = chartSettings,
                    showOpenOnChart = showOpenOnChart,
                    showClosedOnChart = showClosedOnChart,
                    showTpSlOnChart = showTpSlOnChart,
                    showStopOutLine = showStopOutLine,
                    showTradingBar = showTradingBar,
                    showReplayBar = showReplayBar,
                    showFavBar = isStarActive,
                    hideAll = hideAll,
                    showIndicators = showIndicators,
                    showDrawings = showDrawings,
                    onHideAllChanged = { hideAll = it },
                    onShowIndicatorsChanged = { showIndicators = it; sharedPrefs.edit().putBoolean("show_indicators", it).apply() },
                    onShowDrawingsChanged = { showDrawings = it; sharedPrefs.edit().putBoolean("show_drawings", it).apply() },
                    onShowOpenChanged = { showOpenOnChart = it; sharedPrefs.edit().putBoolean("show_open_on_chart", it).apply() },
                    onShowClosedChanged = { showClosedOnChart = it; sharedPrefs.edit().putBoolean("show_closed_on_chart", it).apply() },
                    onShowTpSlChanged = { showTpSlOnChart = it; sharedPrefs.edit().putBoolean("show_tpsl_on_chart", it).apply() },
                    onShowStopOutChanged = { showStopOutLine = it; sharedPrefs.edit().putBoolean("show_stop_out_line", it).apply() },
                    onShowTradingBarChanged = { showTradingBar = it; sharedPrefs.edit().putBoolean("show_trading_bar", it).apply() },
                    onShowReplayBarChanged = { showReplayBar = it; sharedPrefs.edit().putBoolean("show_replay_bar", it).apply() },
                    onShowFavBarChanged = { isStarActive = it; sharedPrefs.edit().putBoolean("show_favorites_bar", it).apply() },
                    isDarkTheme = globalIsDarkTheme,
                    colorScheme = colorScheme
                )
            } }        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(innerPadding).navigationBarsPadding()) {
                    var chartAreaWidthPx  by remember { mutableFloatStateOf(0f) }
                    var chartAreaHeightPx by remember { mutableFloatStateOf(0f) }

                    // ── Conversion des TradeEntry → TradeDisplayData pour le panneau ──
                    val tradeDisplayList = remember(tradeEntries, displayedCandles, visibleCandleCount, capitalEvents) {
                        val tradeList = tradeEntries.map { entry ->
                            TradeDisplayData(
                                ticket      = entry.id,
                                openTime    = if (entry.openTimestamp > 0L)
                                    entry.openTimestamp
                                else if (entry.candleIndex < displayedCandles.size)
                                    displayedCandles[entry.candleIndex].timestamp
                                else System.currentTimeMillis(),
                                type        = entry.type,
                                size        = entry.lotSize,
                                item        = "$selectedSymbol",
                                openPrice   = entry.entryPrice,
                                slPrice     = entry.slPrice,
                                tpPrice     = entry.tpPrice,
                                closeTime   = entry.exitTimestamp,
                                closePrice  = entry.exitPrice,
                                commission  = -(commPerLot * entry.lotSize),   // négatif = coût
                                profit      = entry.profit ?: 0f,
                                dateActual  = System.currentTimeMillis(),
                                currentPrice = displayedCandles.getOrNull(visibleCandleCount - 1)?.close ?: entry.entryPrice
                            )
                        }
                        // Ajouter les dépôts comme entrées spéciales
                        val depositList = capitalEvents.map { (ts, type, amount) ->
                            TradeDisplayData(
                                ticket     = "dep_$ts",
                                openTime   = ts,
                                type       = type,
                                size       = 0f,
                                item       = "",
                                openPrice  = 0f,
                                slPrice    = null,
                                tpPrice    = null,
                                closeTime  = ts,
                                closePrice = 0f,
                                profit     = amount,
                                dateActual = ts
                            )
                        }
                        // Fusionner et trier par date
                        (tradeList + depositList).sortedBy { it.closeTime ?: it.openTime }
                    }

                    //  Lambda partagée pour fermer un trade par son ID ─
                    val closeTradeById: (String, Float?) -> Unit = { id, exitLevelPrice ->
                        val currentCandle = displayedCandles.getOrNull(visibleCandleCount - 1)
                        if (currentCandle != null) {
                            tradeEntries = tradeEntries.map { entry ->
                                if (entry.id == id && entry.exitPrice == null) {
                                    // Si un prix de niveau est fourni (TP/SL touché), appliquer le spread
                                    // Sinon, utiliser le prix du marché courant
                                    val closePrice = if (exitLevelPrice != null) {
                                        // TP/SL touché : le prix fourni est le mid, appliquer spread selon sens de sortie
                                        if (entry.type == "buy") {
                                            exitLevelPrice  // Sortie sur Bid = mid
                                        } else {
                                            exitLevelPrice + spreadPts  // Sortie sur Ask = mid + spread
                                        }
                                    } else {
                                        val midPrice = currentCandle.close
                                        if (entry.type == "buy") {
                                            midPrice  // Sortie sur Bid = close
                                        } else {
                                            midPrice + spreadPts  // Sortie sur Ask = close + spread
                                        }
                                    }
                                    
                                    val rawProfit = if (entry.type == "buy") {
                                        (closePrice - entry.entryPrice) * entry.lotSize * 100f
                                    } else {
                                        (entry.entryPrice - closePrice) * entry.lotSize * 100f
                                    }
                                    // Commission seulement (le spread est déjà intégré dans les prix)
                                    val commCost   = commPerLot * entry.lotSize
                                    val finalProfit = rawProfit - commCost
                                    entry.copy(
                                        exitPrice     = closePrice,
                                        exitTimestamp = currentCandle.timestamp.takeIf { it > 0L } ?: System.currentTimeMillis(),
                                        profit        = finalProfit
                                    )
                                } else entry
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onSizeChanged { size ->
                                chartAreaWidthPx  = size.width.toFloat()
                                chartAreaHeightPx = size.height.toFloat()
                            }
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInRoot()
                                chartBoundsInRoot = android.graphics.Rect(
                                    bounds.left.toInt(), bounds.top.toInt(),
                                    bounds.right.toInt(), bounds.bottom.toInt()
                                )
                            }
                    ) {
                        // ── Sidebar verticale gauche animée (50 dp) ──
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showSidebar && !isFullScreen && !isCapturing,
                            enter = slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(durationMillis = 250)
                            ),
                            exit = slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(durationMillis = 250)
                            ),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxHeight()
                                .zIndex(10f)
                        ) {
                            // Sidebar extraite dans ChartSideBar.kt
                            ChartSidebar(
                                showDrawingToolsPanel = showDrawingToolsPanel,
                                onShowDrawingToolsPanelChange = { showDrawingToolsPanel = it },
                                isMagnetActive = isMagnetActive,
                                onMagnetActiveChange = { isMagnetActive = it; sharedPrefs.edit().putBoolean("magnet_active", it).apply() },
                                isPencilLocked = isPencilLocked,
                                onPencilLockedChange = {
                                    isPencilLocked = it
                                    drawingUseMode = if (it) DrawingUseMode.REPEAT else DrawingUseMode.SINGLE
                                    sharedPrefs.edit().putString("drawing_use_mode", drawingUseMode.name).apply()
                                },
                                isChartLocked = isChartLocked,
                                onChartLockedChange = { isChartLocked = it },
                                onShareClick = { showShareOptionsDialog = true },
                                onLayersClick = { showObjectTree = true },
                                onCancelDrawing = {
                                    currentDrawingTool = DrawingTool.NONE
                                    selectedSimpleTool = DrawingTool.NONE
                                    isIntentionalDrawingActivation = false
                                    cancelDrawingTrigger++
                                },
                                onDrawingToolSelected = { tool ->
                                    if (isChartLocked) isChartLocked = false
                                    currentDrawingTool = tool
                                    isIntentionalDrawingActivation = true
                                    val simpleTools = setOf(
                                            DrawingTool.TREND_LINE, DrawingTool.EXTENDED_LINE, DrawingTool.RAY,
                                            DrawingTool.HORIZONTAL_LINE, DrawingTool.VERTICAL_LINE,
                                            DrawingTool.CROSS_LINE, DrawingTool.ARROW,
                                            DrawingTool.HORIZONTAL_RAY,
                                            DrawingTool.TEXT, DrawingTool.ANCHORED_TEXT
                                        )
                                        if (tool in simpleTools) {
                                            selectedSimpleTool = tool
                                            activateDrawingModeTrigger++
                                        } else {
                                            selectedSimpleTool = DrawingTool.NONE
                                        }
                                    },
                                activeDrawingTool = currentDrawingTool,
                                colorScheme = colorScheme,
                                favoriteTools = favoriteTools,
                                onFavoriteToolsChange = { newFavorites ->
                                    favoriteTools = newFavorites
                                    sharedPrefs.edit().putString("favorite_tools", newFavorites.joinToString(",")).apply()
                                }
                            )
                        }

                        // ── Outils de dessin — overlay sur le chart sous la barre session (zIndex > sidebar) ──
                        DrawingToolsPanel(
                            visible = showDrawingToolsPanel,
                            searchQuery = drawingToolsSearchQuery,
                            onSearchQueryChange = { drawingToolsSearchQuery = it },
                            favoriteTools = favoriteTools,
                            onFavoritesChange = { newFavorites ->
                                favoriteTools = newFavorites
                                sharedPrefs.edit().putString("favorite_tools", newFavorites.joinToString(",")).apply()
                            },
                            onToolSelected = { tool ->
                                if (isChartLocked) isChartLocked = false
                                currentDrawingTool = tool
                                showDrawingToolsPanel = false
                                isIntentionalDrawingActivation = true
                                val simpleTools = setOf(
                                    DrawingTool.TREND_LINE, DrawingTool.EXTENDED_LINE, DrawingTool.RAY,
                                    DrawingTool.HORIZONTAL_LINE, DrawingTool.VERTICAL_LINE,
                                    DrawingTool.CROSS_LINE, DrawingTool.ARROW,
                                    DrawingTool.HORIZONTAL_RAY,
                                    DrawingTool.TEXT, DrawingTool.ANCHORED_TEXT
                                )
                                if (tool in simpleTools) {
                                    selectedSimpleTool = tool
                                    activateDrawingModeTrigger++
                                } else {
                                    selectedSimpleTool = DrawingTool.NONE
                                }
                            },
                            onDismiss = { showDrawingToolsPanel = false },
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(150f)
                        )
                        val widthPx = chartAreaWidthPx
                        val priceWidthPx = with(density) { 55.dp.toPx() }
                        val chartWidthPx = (widthPx - priceWidthPx).coerceAtLeast(1f)

                        val minCandleWidthPx = 2f
                        val maxVisibleFromPx = (chartWidthPx / minCandleWidthPx).coerceAtLeast(10f)
                        val dynamicMaxVisibleCount = minOf(maxVisibleFromPx, displayedCandles.size.toFloat()).coerceAtLeast(10f)
                        val baseVisibleCount = 60f
                        val displayCount = (baseVisibleCount / zoomLevel).coerceIn(2f, dynamicMaxVisibleCount)

                        val maxScroll = (displayedCandles.size.toFloat() - 1f).coerceAtLeast(0f)
                        val minScroll = -(displayCount * 0.9f + chartSettings.marginRightBars)

                        // Ajuster scrollOffset lorsque displayCount change sans changement de zoom
                        // (ex: plein écran, redimensionnement) pour garder le centre visuel stable.
                        // NE PAS ajuster si scrollOffset == 0 (dernière bougie à droite) — le bord droit est l'ancre.
                        val prevDC = remember { mutableFloatStateOf(displayCount) }
                        val prevZL = remember { mutableFloatStateOf(zoomLevel) }
                        LaunchedEffect(displayCount) {
                            val zlChanged = abs(prevZL.floatValue - zoomLevel) > 0.001f
                            val dcChanged = abs(prevDC.floatValue - displayCount) > 0.1f
                            if (!zlChanged && dcChanged && prevDC.floatValue > 0 && !isLoadingMoreHistory && scrollOffset > 0f) {
                                scrollOffset = (scrollOffset + (displayCount - prevDC.floatValue) / 2f)
                                    .coerceIn(minScroll, maxScroll)
                            }
                            prevZL.floatValue = zoomLevel
                            prevDC.floatValue = displayCount
                        }

                        // ── États des barres flottantes (utilisés par les anciennes barres dans le Box interne ET les nouvelles après la sidebar) ─
                        val windowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
                        val initialTradingX = sharedPrefs.getFloat("trading_bar_x", 10f)
                        val initialTradingY = sharedPrefs.getFloat("trading_bar_y", 10f)
                        val initialNavX = sharedPrefs.getFloat("nav_bar_x", 10f)
                        val initialNavY = sharedPrefs.getFloat("nav_bar_y", with(density) { 60.dp.toPx() })
                        var tradingBarOffset by remember { mutableStateOf(Offset(initialTradingX, initialTradingY)) }
                        var navBarOffset by remember { mutableStateOf(Offset(initialNavX, initialNavY)) }
                        var lotSize by remember { mutableFloatStateOf(currentLotSize) }
                        var showSpeedMenu by remember { mutableStateOf(false) }
                        var showStepMenu  by remember { mutableStateOf(false) }
                        var showLotDialog by remember { mutableStateOf(false) }
                        var lotInputText  by remember { mutableStateOf("") }
                        var showInsufficientMarginDialog by remember { mutableStateOf(false) }
                        var requiredMarginForDialog       by remember { mutableFloatStateOf(0f) }
                        var showDepositFromDialog by remember { mutableStateOf(false) }
                        var depositFromDialogText by remember { mutableStateOf("") }
                        var tradingBarActualWidthPx by remember { mutableFloatStateOf(with(density) { 220.dp.toPx() }) }
                        var navBarActualWidthPx     by remember { mutableFloatStateOf(with(density) { 260.dp.toPx() }) }

                        // ─ Valeurs calculées pour les barres ─
                        val barHeightPx = with(density) { 36.dp.toPx() }
                        val screenWidthPx  = windowSize.width.toFloat()
                        val maxYPx = (chartAreaHeightPx - barHeightPx).coerceAtLeast(0f)
                        val constrainedTradingOffset = Offset(
                            tradingBarOffset.x.coerceIn(0f, (screenWidthPx - tradingBarActualWidthPx).coerceAtLeast(0f)),
                            tradingBarOffset.y.coerceIn(0f, maxYPx)
                        )
                        val constrainedNavOffset = Offset(
                            navBarOffset.x.coerceIn(0f, (screenWidthPx - navBarActualWidthPx).coerceAtLeast(0f)),
                            navBarOffset.y.coerceIn(0f, maxYPx)
                        )

                        if (isLoading && !isChartRendered) {
                            ChartSkeleton(
                                modifier = Modifier.fillMaxSize(),
                                settings = chartSettings,
                                loadingStep = loadingStep
                            )
                        } else {
                            Box(Modifier.fillMaxSize()) {
                                val candlesToDisplay = if (backtestDate != null && !isMinuteReplayMode) {
                                    displayedCandles.take(visibleCandleCount.coerceAtMost(displayedCandles.size))
                                } else {
                                    displayedCandles
                                }

                                val visibleCandlesRevealed = candlesToDisplay

                                CandlestickChart(
                                    allCandles = visibleCandlesRevealed,
                                    modifier = Modifier.fillMaxSize(),
                                    indicators = activeIndicators,
                                    scrollOffset = scrollOffset,
                                    displayCount = displayCount,
                                    symbol = selectedSymbol,
                                    timeframe = selectedTimeframe.displayName,
                                    onZoom = { factor ->
                                        zoomLevel = (zoomLevel * factor).coerceIn(baseVisibleCount / dynamicMaxVisibleCount, 30f)
                                    },
                                    onPan = { delta -> scrollOffset = (scrollOffset + delta).coerceIn(minScroll, maxScroll) },
                                    onSettingsRequest = { showChartSettingsDialog = true },
                                    onIndicatorSettingsRequest = { ind -> editingIndicator = ind },
                                    onIndicatorToggleVisibility = { ind ->
                                        snapshotChartState()
                                        val newList = activeIndicators.map {
                                            if (it.id == ind.id) {
                                                when(it) {
                                                    is Indicator.SMA -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.EMA -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.HMA -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.WMA -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.VWAP -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.BollingerBands -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.ATRBands -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Alligator -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Ichimoku -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.RSI -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.MACD -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Stochastic -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Volume -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.ATR -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Supertrend -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Sessions -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Ribbon -> it.copy(isVisible = !it.isVisible)
                                                }
                                            } else it
                                        }
                                        try {
                                            sharedPrefs.edit().putString(indicatorKey, IndicatorSerializer.serializeIndicators(newList)).commit()
                                        } catch (e: Exception) { android.util.Log.e("ChartScreen", "Error saving toggle visibility", e) }
                                        activeIndicators = newList
                                    },
                                    onIndicatorRemove = { ind ->
                                        snapshotChartState()
                                        val newList = activeIndicators.filter { it.id != ind.id }
                                        try {
                                            sharedPrefs.edit().putString(indicatorKey, IndicatorSerializer.serializeIndicators(newList)).commit()
                                        } catch (e: Exception) { android.util.Log.e("ChartScreen", "Error saving remove", e) }
                                        activeIndicators = newList
                                    },
                                    onResetHorizontalZoom = {
                                        zoomLevel = 1f
                                        scrollOffset = 0f
                                    },
                                    settings = chartSettings,
                                    activeDrawingTool = currentDrawingTool,
                                    drawingUseMode = drawingUseMode,
                                    onDrawingUseModeChange = { mode ->
                                        drawingUseMode = mode
                                        sharedPrefs.edit().putString("drawing_use_mode", mode.name).apply()
                                    },
                                    onDrawingToolUsed = { /* Ne pas réinitialiser l'outil pour permettre la sélection persistante */ },
                                    onOpenChartSettings = { showChartSettingsDialog = true },
                                    activateDrawingModeTrigger = activateDrawingModeTrigger,
                                    selectedSimpleTool = selectedSimpleTool,
                                    isIntentionalDrawingActivation = isIntentionalDrawingActivation,
                                    cancelDrawingTrigger = cancelDrawingTrigger,
                                    simpleTrendLines = simpleTrendLines,
                                    arrows = arrows,
                                    extendedLines = extendedLines,
                                    rays = rays,
                                    onLinesChanged = { simple, arrow, extended, ray ->
                                        // Détecter ajouts/suppressions de dessins pour Undo/Redo
                                        val newCounts = intArrayOf(simple.size, arrow.size, extended.size, ray.size)
                                        if (!lastDrawCounts.contentEquals(newCounts)) {
                                            snapshotChartState()
                                            lastDrawCounts = newCounts.copyOf()
                                        }
                                        simpleTrendLines = simple
                                        arrows = arrow
                                        extendedLines = extended
                                        rays = ray
                                    },
                                    onFavoritesBarToolSelected = onFavoritesBarToolSelected,
                                    tradeChartData = tradeEntries
                                        .filter { entry ->
                                            val isOpen = entry.exitPrice == null
                                            if (isOpen) showOpenOnChart && !hideAll else showClosedOnChart && !hideAll
                                        }
                                        .map { entry ->
                                            TradeChartData(
                                                id             = entry.id,
                                                type           = entry.type,
                                                entryPrice     = entry.entryPrice,
                                                tpPrice        = if (showTpSlOnChart && !hideAll) entry.tpPrice else null,
                                                slPrice        = if (showTpSlOnChart && !hideAll) entry.slPrice else null,
                                                lotSize        = entry.lotSize,
                                                profit         = entry.profit ?: 0f,
                                                isOpen         = entry.exitPrice == null,
                                                exitPrice      = entry.exitPrice,
                                                entryTimestamp = if (entry.openTimestamp > 0L) entry.openTimestamp
                                                else displayedCandles.getOrNull(entry.candleIndex)?.timestamp ?: 0L,
                                                exitTimestamp  = entry.exitTimestamp
                                            )
                                        },
                                    onTradeChartDataChanged = { updatedList ->
                                        tradeEntries = tradeEntries.map { entry ->
                                            val updated = updatedList.find { it.id == entry.id }
                                            if (updated != null) {
                                                entry.copy(
                                                    tpPrice = updated.tpPrice,
                                                    slPrice = updated.slPrice
                                                )
                                            } else entry
                                        }
                                    },
                                    showTpSlOnChart = showTpSlOnChart && !hideAll,
                                    stopOutPrice = if (showStopOutLine && !hideAll) stopOutPrice else null,
                                    initialIndicatorHeights = indicatorHeightsMap,
                                    onIndicatorHeightsChange = { heights ->
                                        try {
                                            val gson = com.google.gson.Gson()
                                            sharedPrefs.edit().putString("indicator_heights_$sKey", gson.toJson(heights)).apply()
                                        } catch (_: Exception) {}
                                    },
                                    initialIndicatorRanges = indicatorRangesMap,
                                    onIndicatorRangesChange = { /* persistence future */ },
                                    onIndicatorDragStart = { snapshotChartState() },
                                    onTradeClose = closeTradeById,
                                    onStopOut = {
                                        // Stop out : fermer TOUS les trades ouverts au prix stopOut et capital = 0
                                        val currentCandle = displayedCandles.getOrNull(visibleCandleCount - 1)
                                        val soPrice = stopOutPrice
                                        if (currentCandle != null && soPrice != null && tradeEntries.any { it.exitPrice == null }) {
                                            tradeEntries = tradeEntries.map { entry ->
                                                if (entry.exitPrice == null) {
                                                    // Fermer au prix stop out
                                                    val closePrice = if (entry.type == "buy") {
                                                        soPrice  // Sortie sur Bid = stopOutPrice
                                                    } else {
                                                        soPrice + spreadPts  // Sortie sur Ask = stopOutPrice + spread
                                                    }
                                                    val rawP = if (entry.type == "buy")
                                                        (closePrice - entry.entryPrice) * entry.lotSize * 100f
                                                    else
                                                        (entry.entryPrice - closePrice) * entry.lotSize * 100f
                                                    val commCost = commPerLot * entry.lotSize
                                                    entry.copy(
                                                        exitPrice = closePrice,
                                                        exitTimestamp = currentCandle.timestamp.takeIf { it > 0L } ?: System.currentTimeMillis(),
                                                        profit = rawP - commCost
                                                    )
                                                } else entry
                                            }
                                            // Forcer le capital à zéro (stop out = compte liquidé)
                                            currentBalance = 0f
                                            stopOutTriggered = true
                                        }
                                    },
                                    showStatusBar = !isCapturing,  // Masquer la barre de statut pendant la capture
                                    statusBarStartOffset = if (showSidebar && !isFullScreen && !isCapturing) 40.dp else 4.dp,
                                    onVerticalRangeEnd = { minP, maxP ->
                                        // Snapshot intelligent: une seule fois en fin de drag vertical
                                        snapshotChartState()
                                        verticalMinPrice = minP
                                        verticalMaxPrice = maxP
                                    },
                                    verticalMinPrice = verticalMinPrice,
                                    verticalMaxPrice = verticalMaxPrice,
                                    onDrawingDragEnd = {
                                        snapshotChartState()
                                    },
                                    sessionSpread = spreadPts,
                                    commPerLot = commPerLot,
                                    drawingManager = drawingManager,
                                    isMagnetActive = isMagnetActive,
                                    isChartLocked = isChartLocked,
                                    showIndicators = showIndicators && !hideAll,
                                    showDrawings = showDrawings && !hideAll,
                                    onVisibleRangeChanged = { s, e ->
                                        visibleStartIdx = s
                                        visibleEndIdx = e
                                    },
                                )

                                // ── Spinner discret pendant le chargement historique ──
                                if (isLoadingMoreHistory && !isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(start = 8.dp)
                                            .size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = colorScheme.primary
                                    )
                                }


                                // ── Icône quitter plein écran en haut à gauche du chart ──
                                if (isFullScreen) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(end = 8.dp, top = 8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { isFullScreen = false },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    colorScheme.surface.copy(alpha = 0.85f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_fullscreen_exit),
                                                contentDescription = "Quitter plein écran",
                                                tint = colorScheme.onSurface.copy(alpha = 0.9f),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                // ─ Valeurs calculées DÉPLACÉES après la sidebar (voir lignes 1689+) ─

                                Box(modifier = Modifier.fillMaxSize()) {
                                // ── BARRES DÉSACTIVÉES - déplacées après la sidebar (voir lignes 1669+) ──
                                if (false && showTradingBar && !isCapturing) Box(
                                        modifier = Modifier
                                            .offset { IntOffset(constrainedTradingOffset.x.roundToInt(), constrainedTradingOffset.y.roundToInt()) }
                                            .wrapContentWidth()
                                            .height(36.dp)
                                            .onSizeChanged { tradingBarActualWidthPx = it.width.toFloat() }
                                            .background(colorScheme.surface, RoundedCornerShape(6.dp))
                                            .border(0.5.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .zIndex(100f)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .wrapContentWidth()
                                                .fillMaxHeight()
                                                .padding(horizontal = 6.dp)
                                                .pointerInput(Unit) {
                                                    detectDragGestures(
                                                        onDrag = { change, dragAmount ->
                                                            tradingBarOffset = Offset(tradingBarOffset.x + dragAmount.x, tradingBarOffset.y + dragAmount.y)
                                                            sharedPrefs.edit()
                                                                .putFloat("trading_bar_x", tradingBarOffset.x)
                                                                .putFloat("trading_bar_y", tradingBarOffset.y)
                                                                .apply()
                                                            change.consume()
                                                        }
                                                    )
                                                }
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (visibleCandleCount > 0 && visibleCandleCount <= displayedCandles.size) {
                                                        val currentCandle = displayedCandles[visibleCandleCount - 1]
                                                        val closePrice = if (isMinuteReplayMode && currentMinuteIndex in allMinuteCandles.indices)
                                                            allMinuteCandles[currentMinuteIndex].close else currentCandle.close
                                                        val entryTimestamp = if (isMinuteReplayMode && currentMinuteIndex in allMinuteCandles.indices)
                                                            allMinuteCandles[currentMinuteIndex].timestamp else currentCandle.timestamp
                                                        // BUY: entrée sur Ask (ligne Ask = close + spread)
                                                        val entryPrice = closePrice + spreadPts
                                                        val reqMargin = (entryPrice * lotSize * 100f) / leverageRatio
                                                        if (currentBalance < reqMargin) {
                                                            requiredMarginForDialog = reqMargin
                                                            showInsufficientMarginDialog = true
                                                        } else {
                                                            val newEntry = TradeEntry(
                                                                type = "buy",
                                                                lotSize = lotSize,
                                                                entryPrice = entryPrice,
                                                                candleIndex = visibleCandleCount - 1,
                                                                openTimestamp = entryTimestamp.takeIf { it > 0L } ?: System.currentTimeMillis()
                                                            )
                                                            tradeEntries = tradeEntries + newEntry
                                                            currentLotSize = lotSize
                                                            sharedPrefs.edit().putFloat("lot_$sKey", lotSize).apply()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.height(28.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF2196F3)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                Text("Buy", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { lotSize = (lotSize - 0.01f).coerceAtLeast(0.01f) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = colorScheme.onSurface, modifier = Modifier.size(12.dp))
                                                }
                                                // Tap sur le texte → clavier numérique
                                                Text(
                                                    String.format(java.util.Locale.ROOT, "%.2f", lotSize),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier
                                                        .padding(horizontal = 4.dp)
                                                        .clickable {
                                                            lotInputText = String.format(java.util.Locale.ROOT, "%.2f", lotSize)
                                                            showLotDialog = true
                                                        }
                                                )
                                                IconButton(
                                                    onClick = { lotSize = (lotSize + 0.01f).coerceAtMost(10f) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = colorScheme.onSurface, modifier = Modifier.size(12.dp))
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    if (visibleCandleCount > 0 && visibleCandleCount <= displayedCandles.size) {
                                                        val currentCandle = displayedCandles[visibleCandleCount - 1]
                                                        val closePrice = if (isMinuteReplayMode && currentMinuteIndex in allMinuteCandles.indices)
                                                            allMinuteCandles[currentMinuteIndex].close else currentCandle.close
                                                        val entryTimestamp = if (isMinuteReplayMode && currentMinuteIndex in allMinuteCandles.indices)
                                                            allMinuteCandles[currentMinuteIndex].timestamp else currentCandle.timestamp
                                                        // SELL: entrée sur Bid (ligne Bid = close)
                                                        val entryPrice = closePrice
                                                        val reqMargin = (entryPrice * lotSize * 100f) / leverageRatio
                                                        if (currentBalance < reqMargin) {
                                                            requiredMarginForDialog = reqMargin
                                                            showInsufficientMarginDialog = true
                                                        } else {
                                                            val newEntry = TradeEntry(
                                                                type = "sell",
                                                                lotSize = lotSize,
                                                                entryPrice = entryPrice,
                                                                candleIndex = visibleCandleCount - 1,
                                                                openTimestamp = entryTimestamp.takeIf { it > 0L } ?: System.currentTimeMillis()
                                                            )
                                                            tradeEntries = tradeEntries + newEntry
                                                            currentLotSize = lotSize
                                                            sharedPrefs.edit().putFloat("lot_$sKey", lotSize).apply()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.height(28.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFF44336)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                Text("Sell", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }

                                    // ── Dialogs extraits dans ChartDialogs.kt ──
                                    LotSizeDialog(
                                        visible = showLotDialog,
                                        initialText = String.format(java.util.Locale.ROOT, "%.2f", lotSize),
                                        onDismiss = { showLotDialog = false },
                                        onConfirm = { lotSize = it }
                                    )

                                    DepositFundsDialog(
                                        visible = showDepositFromDialog,
                                        currentBalance = currentBalance,
                                        onDismiss = {
                                            showDepositFromDialog = false
                                            depositFromDialogText = ""
                                        },
                                        onConfirmDeposit = { amt ->
                                            extraCapital += amt
                                            currentBalance = maxOf(0f, currentBalance + amt)
                                            stopOutTriggered = false
                                            capitalEvents = capitalEvents + Triple(System.currentTimeMillis(), "deposit", amt)
                                            depositFromDialogText = ""
                                            if (!sessionName.isNullOrBlank()) {
                                                val closedPnl = tradeEntries
                                                    .filter { it.exitPrice != null }
                                                    .sumOf { (it.profit ?: 0f).toDouble() }.toFloat()
                                                val sessionDisplayCapital = maxOf(0f,
                                                    (initialAmount?.toFloatOrNull() ?: 10000f) + extraCapital - amt + closedPnl)
                                                val newSessionCapital = sessionDisplayCapital + amt
                                                val newBase = newSessionCapital - closedPnl
                                                SessionRepository.updateSessionAmount(context, sessionName, newBase)
                                            }
                                        }
                                    )

                                    InsufficientMarginDialog(
                                        visible = showInsufficientMarginDialog,
                                        requiredMargin = requiredMarginForDialog,
                                        currentBalance = currentBalance,
                                        onDismiss = { showInsufficientMarginDialog = false },
                                        onDepositRequested = {
                                            showInsufficientMarginDialog = false
                                            depositFromDialogText = ""
                                            showDepositFromDialog = true
                                        }
                                    )

                                // ── BARRE 2 DÉSACTIVÉE - déplacée après la sidebar (voir lignes 1669+) ──
                                if (false && showReplayBar && !isCapturing) Box(
                                        modifier = Modifier
                                            .offset { IntOffset(constrainedNavOffset.x.roundToInt(), constrainedNavOffset.y.roundToInt()) }
                                            .wrapContentWidth()
                                            .height(36.dp)
                                            .onSizeChanged { navBarActualWidthPx = it.width.toFloat() }
                                            .background(colorScheme.surface, RoundedCornerShape(6.dp))
                                            .border(0.5.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .zIndex(100f)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .wrapContentWidth()
                                                .fillMaxHeight()
                                                .padding(horizontal = 8.dp)
                                                .pointerInput(Unit) {
                                                    detectDragGestures(
                                                        onDrag = { change, dragAmount ->
                                                            navBarOffset = Offset(navBarOffset.x + dragAmount.x, navBarOffset.y + dragAmount.y)
                                                            sharedPrefs.edit()
                                                                .putFloat("nav_bar_x", navBarOffset.x)
                                                                .putFloat("nav_bar_y", navBarOffset.y)
                                                                .apply()
                                                            change.consume()
                                                        }
                                                    )
                                                }
                                        ) {
                                            // Play/Pause
                                            IconButton(
                                                onClick = { isPlaying = !isPlaying },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            // Next (avance selon playStep)
                                            IconButton(
                                                onClick = {
                                                    if (isMinuteReplayMode && currentMinuteIndex < allMinuteCandles.size - 1) {
                                                        currentMinuteIndex = (currentMinuteIndex + playStep).coerceAtMost(allMinuteCandles.size - 1)
                                                        if (!sessionName.isNullOrBlank()) {
                                                            SessionRepository.saveMinuteIndex(context, sessionName, currentMinuteIndex)
                                                            if (currentMinuteIndex in allMinuteCandles.indices) {
                                                                SessionRepository.saveMinuteTimestamp(context, sessionName, allMinuteCandles[currentMinuteIndex].timestamp)
                                                            }
                                                        }
                                                    } else if (!isMinuteReplayMode && visibleCandleCount < displayedCandles.size) {
                                                        visibleCandleCount++
                                                        if (!sessionName.isNullOrBlank()) {
                                                            SessionRepository.saveReplayPosition(context, sessionName, visibleCandleCount)
                                                        }
                                                    } else if (isHistoryExhausted) {
                                                        // Fin des données
                                                        showEndOfData = true
                                                        coroutineScopeEnd.launch {
                                                            kotlinx.coroutines.delay(3000L)
                                                            showEndOfData = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.SkipNext,
                                                    contentDescription = "Next",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            VerticalDivider(
                                                modifier = Modifier.height(20.dp),
                                                color = colorScheme.outline.copy(alpha = 0.3f)
                                            )

                                            // ── Sélecteur de Pas (Step) ancré ──────────────
                                            Box {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                        .border(0.5.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .clickable { showStepMenu = true }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Step: ", fontSize = 10.sp, color = Color.Black.copy(alpha = 0.6f))
                                                    Text(
                                                        when(playStep) {
                                                            1 -> "1m"; 2 -> "2m"; 3 -> "3m"; 5 -> "5m"
                                                            10 -> "10m"; 15 -> "15m"; 30 -> "30m"
                                                            60 -> "1hr"; 240 -> "4hr"; 1440 -> "1D"
                                                            else -> "${playStep}m"
                                                        },
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Black
                                                    )
                                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                                }
                                                DropdownMenu(
                                                    expanded = showStepMenu,
                                                    onDismissRequest = { showStepMenu = false },
                                                    modifier = Modifier
                                                        .background(colorScheme.surface)
                                                        .border(0.5.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                        .width(80.dp)
                                                ) {
                                                    listOf(
                                                        1 to "1m", 2 to "2m", 3 to "3m", 5 to "5m",
                                                        10 to "10m", 15 to "15m", 30 to "30m",
                                                        60 to "1hr", 240 to "4hr", 1440 to "1D"
                                                    ).forEach { (minutes, label) ->
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { playStep = minutes; showStepMenu = false }
                                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(label, fontSize = 11.sp,
                                                                color = if (playStep == minutes) Color.Black else colorScheme.onSurface)
                                                        }
                                                    }
                                                }
                                            }

                                            // ── Sélecteur de Vitesse (Speed) ancré ──────────
                                            Box {
                                                Box(
                                                    modifier = Modifier
                                                        .width(40.dp)
                                                        .height(26.dp)
                                                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                        .clickable { showSpeedMenu = true }
                                                        .padding(horizontal = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("${replaySpeed}x", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                                }
                                                DropdownMenu(
                                                    expanded = showSpeedMenu,
                                                    onDismissRequest = { showSpeedMenu = false },
                                                    modifier = Modifier
                                                        .background(colorScheme.surface)
                                                        .border(0.5.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                        .width(70.dp)
                                                ) {
                                                    listOf(0.5f, 1f, 2f, 4f, 8f).forEach { speed ->
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { replaySpeed = speed; showSpeedMenu = false }
                                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                                        ) {
                                                            Text("${speed}x", fontSize = 11.sp,
                                                                color = if (replaySpeed == speed) Color.Black else colorScheme.onSurface)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            // IndicatorsMenu et DrawingToolsPanel déplacés en overlay (zIndex 20f)
                            }

                            // ── Banner "Fin des données" ──────────────────────────
                            if (showEndOfData) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 50.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .background(
                                                Color(0xFF1565C0).copy(alpha = 0.92f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "Fin des donn\u00e9es \u2014 Plus de barres disponibles",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                }
                            }
                        }

                    }


                    // ── Panneau historique des trades (draggable) — en bas de la Column ──
                    if (!isCapturing && !isFullScreen) TradesHistoryPanel(
                        trades              = tradeDisplayList,
                        isDarkTheme         = globalIsDarkTheme,
                        modifier            = Modifier.padding(top = 1.dp).fillMaxWidth(),
                        initialBalance      = initialAmount?.toFloatOrNull() ?: 0f,
                        sessionName         = sessionName,
                        spreadPts           = spreadPts,
                        onCloseTrade = { id, price -> closeTradeById(id, price) },
                        onCloseAllTrades = {
                            val currentCandle = displayedCandles.getOrNull(visibleCandleCount - 1)
                            if (currentCandle != null) {
                                tradeEntries = tradeEntries.map { entry ->
                                    if (entry.exitPrice == null) {
                                        // BUY: entré sur Ask (close + spread), sort sur Bid (close)
                                        // SELL: entré sur Bid (close), sort sur Ask (close + spread)
                                        val midPrice = currentCandle.close
                                        val closePrice = if (entry.type == "buy") {
                                            midPrice  // Sortie sur Bid = close
                                        } else {
                                            midPrice + spreadPts  // Sortie sur Ask = close + spread
                                        }
                                        
                                        val rawProfit = if (entry.type == "buy") {
                                            (closePrice - entry.entryPrice) * entry.lotSize * 100f
                                        } else {
                                            (entry.entryPrice - closePrice) * entry.lotSize * 100f
                                        }
                                        // Commission seulement (le spread est déjà intégré dans les prix)
                                        val commCost   = commPerLot * entry.lotSize
                                        val finalProfit = rawProfit - commCost
                                        entry.copy(
                                            exitPrice     = closePrice,
                                            exitTimestamp = currentCandle.timestamp.takeIf { it > 0L } ?: System.currentTimeMillis(),
                                            profit        = finalProfit
                                        )
                                    } else entry
                                }
                            }
                        }
                    )
                }
            }
        }

        // ── BARRES FLOTTANTES (Trading Bar & Replay Bar) - placées APRÈS la sidebar pour apparaître au-dessus ──
        if (!isLoading) {
            val windowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
            val initialTradingX = sharedPrefs.getFloat("trading_bar_x", 10f)
            val initialTradingY = sharedPrefs.getFloat("trading_bar_y", 10f)
            val initialNavX = sharedPrefs.getFloat("nav_bar_x", 10f)
            val initialNavY = sharedPrefs.getFloat("nav_bar_y", with(density) { 60.dp.toPx() })
            var tradingBarOffset by remember { mutableStateOf(Offset(initialTradingX, initialTradingY)) }
            var navBarOffset by remember { mutableStateOf(Offset(initialNavX, initialNavY)) }
            var lotSize by remember { mutableFloatStateOf(currentLotSize) }
            var showSpeedMenu by remember { mutableStateOf(false) }
            var showStepMenu  by remember { mutableStateOf(false) }
            var showLotDialog by remember { mutableStateOf(false) }
            var lotInputText  by remember { mutableStateOf("") }
            var showInsufficientMarginDialog by remember { mutableStateOf(false) }
            var requiredMarginForDialog       by remember { mutableFloatStateOf(0f) }
            var showDepositFromDialog by remember { mutableStateOf(false) }
            var depositFromDialogText by remember { mutableStateOf("") }
            var tradingBarActualWidthPx by remember { mutableFloatStateOf(with(density) { 220.dp.toPx() }) }
            var navBarActualWidthPx     by remember { mutableFloatStateOf(with(density) { 260.dp.toPx() }) }
            var favoritesBarPosition by remember {
                val savedX = sharedPrefs.getFloat("fav_bar_pos_x", -1f)
                val savedY = sharedPrefs.getFloat("fav_bar_pos_y", -1f)
                mutableStateOf<Offset?>(
                    if (savedX >= 0f && savedY >= 0f) Offset(savedX, savedY) else null
                )
            }
            var favoritesBarSizePx by remember { mutableStateOf(Offset(200f, 40f)) }
            var isDraggingFavoritesBar by remember { mutableStateOf(false) }

            val barHeightPx = with(density) { 32.dp.toPx() }
            val screenWidthPx  = windowSize.width.toFloat()
            val screenHeightPx = windowSize.height.toFloat()
            
            // ─ Limites verticales pour les barres draggable ─
            val topbarHeightPx = with(density) { 68.dp.toPx() }
            val openBarHeightPx = with(density) { 45.dp.toPx() }

            val minYPx = if (isFullScreen) 0f else topbarHeightPx
            val maxYpx = if (isFullScreen)
                (screenHeightPx - barHeightPx).coerceAtLeast(minYPx)
            else
                (screenHeightPx - openBarHeightPx - barHeightPx).coerceAtLeast(minYPx)
            
            val constrainedTradingOffset = Offset(
                tradingBarOffset.x.coerceIn(0f, (screenWidthPx - tradingBarActualWidthPx).coerceAtLeast(0f)),
                tradingBarOffset.y.coerceIn(minYPx, maxYpx)
            )
            val constrainedNavOffset = Offset(
                navBarOffset.x.coerceIn(0f, (screenWidthPx - navBarActualWidthPx).coerceAtLeast(0f)),
                navBarOffset.y.coerceIn(minYPx, maxYpx)
            )

            Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
                // BARRE 1: TRADING (Buy/Lot/Sell) - masquée pendant la capture
                if (showTradingBar && !hideAll && !isCapturing) Box(
                    modifier = Modifier
                        .offset { IntOffset(constrainedTradingOffset.x.roundToInt(), constrainedTradingOffset.y.roundToInt()) }
                        .wrapContentWidth()
                        .height(32.dp)
                        .onSizeChanged { tradingBarActualWidthPx = it.width.toFloat() }
                        .background(colorScheme.surface, RoundedCornerShape(6.dp))
                        .border(0.5.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .wrapContentWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 6.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        tradingBarOffset = Offset(tradingBarOffset.x + dragAmount.x, tradingBarOffset.y + dragAmount.y)
                                        sharedPrefs.edit()
                                            .putFloat("trading_bar_x", tradingBarOffset.x)
                                            .putFloat("trading_bar_y", tradingBarOffset.y)
                                            .apply()
                                        change.consume()
                                    }
                                )
                            }
                    ) {
                        Button(
                            onClick = {
                                if (visibleCandleCount > 0 && visibleCandleCount <= displayedCandles.size) {
                                    val currentCandle = displayedCandles[visibleCandleCount - 1]
                                    val closePrice = if (isMinuteReplayMode && currentMinuteIndex in allMinuteCandles.indices)
                                        allMinuteCandles[currentMinuteIndex].close else currentCandle.close
                                    val entryTimestamp = if (isMinuteReplayMode && currentMinuteIndex in allMinuteCandles.indices)
                                        allMinuteCandles[currentMinuteIndex].timestamp else currentCandle.timestamp
                                    // BUY: entrée sur Ask (ligne Ask = close + spread)
                                    val entryPrice = closePrice + spreadPts
                                    val reqMargin = (entryPrice * lotSize * 100f) / leverageRatio
                                    if (currentBalance < reqMargin) {
                                        requiredMarginForDialog = reqMargin
                                        showInsufficientMarginDialog = true
                                    } else {
                                        val newEntry = TradeEntry(
                                            type = "buy",
                                            lotSize = lotSize,
                                            entryPrice = entryPrice,
                                            candleIndex = visibleCandleCount - 1,
                                            openTimestamp = entryTimestamp.takeIf { it > 0L } ?: System.currentTimeMillis()
                                        )
                                        tradeEntries = tradeEntries + newEntry
                                        currentLotSize = lotSize
                                        sharedPrefs.edit().putFloat("lot_$sKey", lotSize).apply()
                                    }
                                }
                            },
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Buy", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp)
                        ) {
                            IconButton(onClick = { lotSize = (lotSize - 0.01f).coerceAtLeast(0.01f) }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = colorScheme.onSurface, modifier = Modifier.size(12.dp))
                            }
                            Text(String.format(java.util.Locale.ROOT, "%.2f", lotSize), fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp).clickable {
                                    lotInputText = String.format(java.util.Locale.ROOT, "%.2f", lotSize)
                                    showLotDialog = true
                                })
                            IconButton(onClick = { lotSize = (lotSize + 0.01f).coerceAtMost(10f) }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = colorScheme.onSurface, modifier = Modifier.size(12.dp))
                            }
                        }

                        Button(
                            onClick = {
                                if (visibleCandleCount > 0 && visibleCandleCount <= displayedCandles.size) {
                                    val currentCandle = displayedCandles[visibleCandleCount - 1]
                                    val closePrice = if (isMinuteReplayMode && currentMinuteIndex in allMinuteCandles.indices)
                                        allMinuteCandles[currentMinuteIndex].close else currentCandle.close
                                    val entryTimestamp = if (isMinuteReplayMode && currentMinuteIndex in allMinuteCandles.indices)
                                        allMinuteCandles[currentMinuteIndex].timestamp else currentCandle.timestamp
                                    // SELL: entrée sur Bid (ligne Bid = close)
                                    val entryPrice = closePrice
                                    val reqMargin = (entryPrice * lotSize * 100f) / leverageRatio
                                    if (currentBalance < reqMargin) {
                                        requiredMarginForDialog = reqMargin
                                        showInsufficientMarginDialog = true
                                    } else {
                                        val newEntry = TradeEntry(
                                            type = "sell",
                                            lotSize = lotSize,
                                            entryPrice = entryPrice,
                                            candleIndex = visibleCandleCount - 1,
                                            openTimestamp = entryTimestamp.takeIf { it > 0L } ?: System.currentTimeMillis()
                                        )
                                        tradeEntries = tradeEntries + newEntry
                                        currentLotSize = lotSize
                                        sharedPrefs.edit().putFloat("lot_$sKey", lotSize).apply()
                                    }
                                }
                            },
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Sell", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // BARRE 2: NAVIGATION (Play/Pause/Next/Step) - masquée pendant la capture
                if (showReplayBar && !hideAll && !isCapturing) Box(
                    modifier = Modifier
                        .offset { IntOffset(constrainedNavOffset.x.roundToInt(), constrainedNavOffset.y.roundToInt()) }
                        .wrapContentWidth()
                        .height(32.dp)
                        .onSizeChanged { navBarActualWidthPx = it.width.toFloat() }
                        .background(colorScheme.surface, RoundedCornerShape(6.dp))
                        .border(0.5.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .wrapContentWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        navBarOffset = Offset(navBarOffset.x + dragAmount.x, navBarOffset.y + dragAmount.y)
                                        sharedPrefs.edit()
                                            .putFloat("nav_bar_x", navBarOffset.x)
                                            .putFloat("nav_bar_y", navBarOffset.y)
                                            .apply()
                                        change.consume()
                                    }
                                )
                            }
                    ) {
                        // Play/Pause
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Next (avance selon playStep / fermeture bougie)
                        IconButton(
                            onClick = {
                                if (isMinuteReplayMode && currentMinuteIndex < allMinuteCandles.size - 1) {
                                    val stepMatchesTimeframe = playStep == selectedTimeframe.minutes
                                    if (stepMatchesTimeframe) {
                                        // Avancer à la fermeture de la bougie suivante
                                        val tfMin = selectedTimeframe.minutes
                                        val posInGroup = currentMinuteIndex % tfMin
                                        val minutesToAdvance = if (posInGroup == tfMin - 1) tfMin else tfMin - 1 - posInGroup
                                        currentMinuteIndex = (currentMinuteIndex + minutesToAdvance).coerceAtMost(allMinuteCandles.size - 1)
                                    } else {
                                        currentMinuteIndex = (currentMinuteIndex + playStep).coerceAtMost(allMinuteCandles.size - 1)
                                    }
                                    if (!sessionName.isNullOrBlank()) {
                                        SessionRepository.saveMinuteIndex(context, sessionName, currentMinuteIndex)
                                        if (currentMinuteIndex in allMinuteCandles.indices) {
                                            SessionRepository.saveMinuteTimestamp(context, sessionName, allMinuteCandles[currentMinuteIndex].timestamp)
                                        }
                                    }
                                } else if (!isMinuteReplayMode && visibleCandleCount < displayedCandles.size) {
                                    visibleCandleCount++
                                    if (!sessionName.isNullOrBlank()) {
                                        SessionRepository.saveReplayPosition(context, sessionName, visibleCandleCount)
                                    }
                                } else if (isHistoryExhausted) {
                                    // Fin des données
                                    showEndOfData = true
                                    coroutineScopeEnd.launch {
                                        kotlinx.coroutines.delay(3000L)
                                        showEndOfData = false
                                    }
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(20.dp),
                            color = colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // ── Sélecteur de Pas (Step) ancré ──────────────
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .border(0.5.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .clickable { showStepMenu = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    when(playStep) {
                                        1 -> "1m"; 2 -> "2m"; 3 -> "3m"; 5 -> "5m"
                                        10 -> "10m"; 15 -> "15m"; 30 -> "30m"
                                        60 -> "1hr"; 240 -> "4hr"; 1440 -> "1D"
                                        else -> "${playStep}m"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = Color.Black)
                            }
                            DropdownMenu(
                                expanded = showStepMenu,
                                onDismissRequest = { showStepMenu = false },
                                modifier = Modifier
                                    .background(colorScheme.surface)
                                    .border(0.5.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .width(80.dp)
                            ) {
                                listOf(
                                    1 to "1m", 2 to "2m", 3 to "3m", 5 to "5m",
                                    10 to "10m", 15 to "15m", 30 to "30m",
                                    60 to "1hr", 240 to "4hr", 1440 to "1D"
                                ).forEach { (minutes, label) ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { playStep = minutes; showStepMenu = false }
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(label, fontSize = 11.sp,
                                            color = if (playStep == minutes) Color.Black else colorScheme.onSurface)
                                    }
                                }
                            }
                        }

                        // ─ Sélecteur de Vitesse (Speed) ancré ──────────
                        Box {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(26.dp)
                                    .border(0.5.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .clickable { showSpeedMenu = true }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${replaySpeed}x", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false },
                                modifier = Modifier
                                    .background(colorScheme.surface)
                                    .border(0.5.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .width(70.dp)
                            ) {
                                listOf(0.5f, 1f, 2f, 4f, 8f).forEach { speed ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { replaySpeed = speed; showSpeedMenu = false }
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("${speed}x", fontSize = 11.sp,
                                            color = if (replaySpeed == speed) Color.Black else colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── FAVORITES BAR (outils favoris flottants) ──
                val favoritesToolsList = DrawingTool.entries.filter { favoriteTools.contains(it.name) }
                if (isStarActive && !hideAll && favoritesToolsList.isNotEmpty()) {
                    val isDarkChart = chartSettings.backgroundColor.luminance() < 0.5f
                    val colors = getDrawingToolsMenuColors(isDarkChart)

                    val topConstraint = if (isCapturing || isFullScreen) 0f else topbarHeightPx

                    val currentPos = favoritesBarPosition
                    val barW = favoritesBarSizePx.x

                    val displayPos = currentPos ?: Offset((screenWidthPx - barW) / 2f, topConstraint)

                    val dragHandleWidth = 30.dp
                    val iconSize = 22.dp
                    val iconBoxWidth = iconSize + 5.dp
                    val iconBoxHeight = iconSize + 5.dp
                    val totalIconWidth = iconBoxWidth * favoritesToolsList.size
                    val spacerWidth = 3.dp
                    val totalSpacerWidth = spacerWidth * (favoritesToolsList.size - 1)
                    val calculatedBarWidth = dragHandleWidth + totalIconWidth + totalSpacerWidth + 12.dp

                    val maxBarWidthPx = screenWidthPx * 0.95f
                    val barWidthPx = with(density) { calculatedBarWidth.toPx() }
                    val isVerticalMode = barWidthPx > maxBarWidthPx

                    val verticalBarWidth = iconBoxWidth + 6.dp
                    val verticalBarHeight = 6.dp + dragHandleWidth + iconBoxHeight * favoritesToolsList.size + spacerWidth * (favoritesToolsList.size - 1) + 3.dp

                    val barWidth = if (isVerticalMode) verticalBarWidth else calculatedBarWidth
                    val barHeight = if (isVerticalMode) verticalBarHeight else 32.dp

                    val scrollState = rememberScrollState()
                    val verticalScrollState = rememberScrollState()

                    // Valeurs refraîchies pour le pointerInput à clé stable
                    val latestTopConstraint by rememberUpdatedState(topConstraint)
                    val latestScreenWidthPx by rememberUpdatedState(screenWidthPx)
                    val latestScreenHeightPx by rememberUpdatedState(screenHeightPx)
                    val latestOpenBarHeightPx by rememberUpdatedState(openBarHeightPx)

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(displayPos.x.roundToInt(), displayPos.y.roundToInt()) }
                            .width(barWidth)
                            .height(barHeight)
                            .onGloballyPositioned { coords ->
                                val newW = coords.size.width.toFloat()
                                val newH = coords.size.height.toFloat()
                                favoritesBarSizePx = Offset(newW, newH)
                                if (favoritesBarPosition == null) {
                                    favoritesBarPosition = Offset(
                                        ((screenWidthPx - newW) / 2f).coerceIn(0f, (screenWidthPx - newW).coerceAtLeast(0f)),
                                        topConstraint
                                    ).also { pos ->
                                        sharedPrefs.edit()
                                            .putFloat("fav_bar_pos_x", pos.x)
                                            .putFloat("fav_bar_pos_y", pos.y)
                                            .apply()
                                    }
                                    } else {
                                        val pos = favoritesBarPosition!!
                                        val bottomBound = if (isFullScreen) (screenHeightPx - newH).coerceAtLeast(topConstraint) else (screenHeightPx - openBarHeightPx - newH).coerceAtLeast(topConstraint)
                                        favoritesBarPosition = Offset(
                                            pos.x.coerceIn(0f, (screenWidthPx - newW).coerceAtLeast(0f)),
                                            pos.y.coerceIn(topConstraint, bottomBound)
                                        ).also { newPos ->
                                        sharedPrefs.edit()
                                            .putFloat("fav_bar_pos_x", newPos.x)
                                            .putFloat("fav_bar_pos_y", newPos.y)
                                            .apply()
                                    }
                                }
                            }
                            .background(colors.bgColor, RoundedCornerShape(8.dp))
                            .border(0.5.dp, colors.borderColor, RoundedCornerShape(8.dp))
                            .padding(
                                start = if (isVerticalMode) 3.dp else 8.dp,
                                end = if (isVerticalMode) 3.dp else 4.dp,
                                top = if (isVerticalMode) 6.dp else 0.dp,
                                bottom = if (isVerticalMode) 3.dp else 0.dp
                            )
                            .then(
                                if (isVerticalMode) Modifier.verticalScroll(verticalScrollState)
                                else if (barWidthPx > screenWidthPx) Modifier.horizontalScroll(scrollState)
                                else Modifier
                            )
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isDraggingFavoritesBar = true },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val bW = favoritesBarSizePx.x
                                        val bH = favoritesBarSizePx.y
                                        val currentTop = latestTopConstraint
                                        val currentScreenW = latestScreenWidthPx
                                        val currentScreenH = latestScreenHeightPx
                                        val bottomBound = if (isFullScreen) (currentScreenH - bH).coerceAtLeast(currentTop) else (currentScreenH - latestOpenBarHeightPx - bH).coerceAtLeast(currentTop)
                                        val prev = favoritesBarPosition ?: Offset((currentScreenW - bW) / 2f, currentTop)
                                        val newX = prev.x + dragAmount.x
                                        val newY = prev.y + dragAmount.y
                                        favoritesBarPosition = Offset(
                                            newX.coerceIn(0f, (currentScreenW - bW).coerceAtLeast(0f)),
                                            newY.coerceIn(currentTop, bottomBound)
                                        )
                                    },
                                    onDragEnd = {
                                        isDraggingFavoritesBar = false
                                        favoritesBarPosition?.let { pos ->
                                            sharedPrefs.edit()
                                                .putFloat("fav_bar_pos_x", pos.x)
                                                .putFloat("fav_bar_pos_y", pos.y)
                                                .apply()
                                        }
                                    },
                                    onDragCancel = { isDraggingFavoritesBar = false }
                                )
                            }
                    ) {
                        if (isVerticalMode) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(spacerWidth),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.rotate(90f)) {
                                    DragHandle(gripColor = colors.textColor.copy(0.5f), dotSize = 3f, spacing = 3f)
                                }

                                favoritesToolsList.forEach { tool ->
                                    val isToolActive = if (tool == DrawingTool.TREND_LINE || tool == DrawingTool.EXTENDED_LINE || tool == DrawingTool.ARROW || tool == DrawingTool.RAY || tool == DrawingTool.MEASURE || tool == DrawingTool.HORIZONTAL_LINE || tool == DrawingTool.HORIZONTAL_RAY || tool == DrawingTool.VERTICAL_LINE || tool == DrawingTool.CROSS_LINE || tool == DrawingTool.TEXT || tool == DrawingTool.ANCHORED_TEXT) {
                                        selectedSimpleTool == tool
                                    } else {
                                        drawingManager.activeTool == tool
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(iconBoxWidth)
                                            .height(iconBoxHeight)
                                            .clickable(enabled = !isDraggingFavoritesBar) {
                                                onFavoritesBarToolSelected(tool)
                                            }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AppIcon(
                                            resId = tool.iconRes,
                                            contentDescription = null,
                                            tint = if (isToolActive) Color(0xFF2196F3) else colors.textColor,
                                            size = iconSize
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(spacerWidth),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    DragHandle(gripColor = colors.textColor.copy(0.5f), dotSize = 3f, spacing = 3f)
                                }

                                favoritesToolsList.forEach { tool ->
                                    val isToolActive = if (tool == DrawingTool.TREND_LINE || tool == DrawingTool.EXTENDED_LINE || tool == DrawingTool.ARROW || tool == DrawingTool.RAY || tool == DrawingTool.MEASURE || tool == DrawingTool.HORIZONTAL_LINE || tool == DrawingTool.HORIZONTAL_RAY || tool == DrawingTool.VERTICAL_LINE || tool == DrawingTool.CROSS_LINE || tool == DrawingTool.TEXT || tool == DrawingTool.ANCHORED_TEXT) {
                                        selectedSimpleTool == tool
                                    } else {
                                        drawingManager.activeTool == tool
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(iconBoxWidth)
                                            .height(iconBoxHeight)
                                            .clickable(enabled = !isDraggingFavoritesBar) {
                                                onFavoritesBarToolSelected(tool)
                                            }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AppIcon(
                                            resId = tool.iconRes,
                                            contentDescription = null,
                                            tint = if (isToolActive) Color(0xFF2196F3) else colors.textColor,
                                            size = iconSize
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dialogs
                LotSizeDialog(visible = showLotDialog, initialText = String.format(java.util.Locale.ROOT, "%.2f", lotSize),
                    onDismiss = { showLotDialog = false }, onConfirm = { lotSize = it })
                DepositFundsDialog(visible = showDepositFromDialog, currentBalance = currentBalance,
                    onDismiss = { showDepositFromDialog = false; depositFromDialogText = "" },
                    onConfirmDeposit = { amt ->
                        extraCapital += amt
                        currentBalance = maxOf(0f, currentBalance + amt)
                        stopOutTriggered = false
                        capitalEvents = capitalEvents + Triple(System.currentTimeMillis(), "deposit", amt)
                        depositFromDialogText = ""
                        if (!sessionName.isNullOrBlank()) {
                            val closedPnl = tradeEntries.filter { it.exitPrice != null }.sumOf { (it.profit ?: 0f).toDouble() }.toFloat()
                            val sessionDisplayCapital = maxOf(0f, (initialAmount?.toFloatOrNull() ?: 10000f) + extraCapital - amt + closedPnl)
                            val newSessionCapital = sessionDisplayCapital + amt
                            val newBase = newSessionCapital - closedPnl
                            SessionRepository.updateSessionAmount(context, sessionName, newBase)
                        }
                    })
                InsufficientMarginDialog(visible = showInsufficientMarginDialog, requiredMargin = requiredMarginForDialog,
                    currentBalance = currentBalance, onDismiss = { showInsufficientMarginDialog = false },
                    onDepositRequested = { showInsufficientMarginDialog = false; depositFromDialogText = ""; showDepositFromDialog = true })
            }
        }

        // Menu Indicateurs - place APRES les barres pour apparaitre au-dessus
        if (showIndicatorsMenu) {
            IndicatorsMenu(
                onIndicatorSelected = { newInd ->
                    snapshotChartState()
                    val newList = activeIndicators + newInd
                    try {
                        val serialized = IndicatorSerializer.serializeIndicators(newList)
                        sharedPrefs.edit().putString(indicatorKey, serialized).commit()
                    } catch (e: Exception) {
                        android.util.Log.e("ChartScreen", "Error saving added indicator", e)
                    }
                    activeIndicators = newList
                },
                onDismissRequest = { showIndicatorsMenu = false },
                isDarkTheme = globalIsDarkTheme,
                favoriteIndicators = favoriteIndicators,
                onFavoritesChange = { newFavorites -> favoriteIndicators = newFavorites },
            )
        }

        // Menu Timeframe overlay (extrait dans TimeframeSelectorOverlay.kt)
        TimeframeSelectorOverlay(
            showTimeframeMenu = showTimeframeMenu,
            onShowTimeframeMenuChange = { showTimeframeMenu = it },
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelected = { tf ->
                snapshotChartState()
                selectedTimeframe = tf
                sharedPrefs.edit().putString("timeframe_$sKey", tf.displayName).apply()
                scrollOffset = 0f
            },
            colorScheme = colorScheme,
            modifier = Modifier.zIndex(150f)
        )

        // Menu Chart Type overlay
        ChartTypeSelectorOverlay(
            showChartTypeMenu = showChartTypeMenu,
            onShowChartTypeMenuChange = { showChartTypeMenu = it },
            selectedChartType = chartSettings.chartType,
            onChartTypeSelected = { chartType ->
                snapshotChartState()
                chartSettings = chartSettings.copy(chartType = chartType)
                saveChartSettings(sharedPrefs, chartSettings, globalIsDarkTheme)
            },
            colorScheme = colorScheme,
            modifier = Modifier.zIndex(160f)
        )

        // Écran plein écran pour les paramètres d'indicateur
        if (editingIndicator != null) {
            IndicatorSettingsScreen(
                indicator = editingIndicator!!,
                onDismiss = { editingIndicator = null },
                onSave = { updated ->
                    snapshotChartState()
                    val newList = activeIndicators.map { if (it.id == updated.id) updated else it }
                    try {
                        val serialized = IndicatorSerializer.serializeIndicators(newList)
                        sharedPrefs.edit().putString(indicatorKey, serialized).commit()
                    } catch (e: Exception) {
                        android.util.Log.e("ChartScreen", "Error saving edited indicator", e)
                    }
                    activeIndicators = newList
                    editingIndicator = null
                },
                isDarkTheme = globalIsDarkTheme
            )
        }
    }
}

