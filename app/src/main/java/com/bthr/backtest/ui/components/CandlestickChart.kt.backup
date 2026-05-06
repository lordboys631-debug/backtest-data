@file:Suppress("ASSIGNED_VALUE_IS_NEVER_READ", "UnusedAssignment", "UNUSED_VALUE")

package com.bthr.backtest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bthr.backtest.model.Candle
import com.bthr.backtest.model.ChartSettings
import com.bthr.backtest.model.DrawingTool
import com.bthr.backtest.model.Indicator
import com.bthr.backtest.model.SimpleTrendLine
import com.bthr.backtest.util.ChartDrawer
import com.bthr.backtest.util.ChartUtils
import com.bthr.backtest.util.DrawingCoordinateMapper
import com.bthr.backtest.util.DrawingManager
import com.bthr.backtest.ui.components.DragHandle
import com.bthr.backtest.ui.components.getFavoritesBarColors
import com.bthr.backtest.util.DrawingState
import com.bthr.backtest.util.DrawingUseMode
import com.bthr.backtest.util.IndicatorCalculators
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// Simple drawing state enum for trend line drawing
private enum class SimpleDrawingState {
    NONE,
    PLACING_FIRST_POINT,
    PLACING_SECOND_POINT
}

@Suppress("UNUSED_VALUE", "UnusedAssignment")
@Composable
fun CandlestickChart(
    allCandles: List<Candle>,
    modifier: Modifier = Modifier,
    indicators: List<Indicator> = emptyList(),
    scrollOffset: Float = 0f,
    displayCount: Float = 60f,
    symbol: String = "",
    timeframe: String = "",
    onZoom: (Float) -> Unit = {},
    onPan: (Float) -> Unit = {},
    onSettingsRequest: () -> Unit = {},
    onOpenChartSettings: () -> Unit = {},
    onIndicatorSettingsRequest: (Indicator) -> Unit = {},
    onIndicatorToggleVisibility: (Indicator) -> Unit = {},
    onIndicatorRemove: (Indicator) -> Unit = {},
    onResetHorizontalZoom: () -> Unit = {},
    settings: ChartSettings = ChartSettings(),
    timeZone: TimeZone = TimeZone.getDefault(),
    secondaryTimeZone: TimeZone? = null,
    showDualTimezone: Boolean = false,
    favoriteTools: Set<String> = emptySet(),
    @Suppress("UNUSED_PARAMETER")
    onFavoriteToolsChange: (Set<String>) -> Unit = {},
    activeDrawingTool: DrawingTool = DrawingTool.NONE,
    drawingUseMode: DrawingUseMode = DrawingUseMode.REPEAT,
    onDrawingUseModeChange: (DrawingUseMode) -> Unit = {},
    onDrawingToolUsed: () -> Unit = {},
    activateDrawingModeTrigger: Int = 0,
    selectedSimpleTool: DrawingTool = DrawingTool.NONE,
    isIntentionalDrawingActivation: Boolean = false,
    simpleTrendLines: List<SimpleTrendLine> = emptyList(),
    arrows: List<SimpleTrendLine> = emptyList(),
    extendedLines: List<SimpleTrendLine> = emptyList(),
    rays: List<SimpleTrendLine> = emptyList(),
    onLinesChanged: ((List<SimpleTrendLine>, List<SimpleTrendLine>, List<SimpleTrendLine>, List<SimpleTrendLine>) -> Unit)? = null,
    onActivateSimpleDrawingMode: ((DrawingTool) -> Unit)? = null,
    onFavoritesBarToolSelected: ((DrawingTool) -> Unit)? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    
    val colorScheme = MaterialTheme.colorScheme
    val textStyle = TextStyle(color = colorScheme.onBackground, fontSize = settings.scaleTextSize.sp)
    val crosshairTextStyle = TextStyle(color = Color.White, fontSize = 10.sp) 
    
    val currentOnZoom by rememberUpdatedState(onZoom)
    val currentOnPan by rememberUpdatedState(onPan)
    val currentDisplayCount by rememberUpdatedState(displayCount)
    val currentOnSettingsRequest by rememberUpdatedState(onSettingsRequest)

    var crosshairPosition by remember { mutableStateOf<Offset?>(null) }
    var crosshairDragOffset by remember { mutableStateOf(Offset.Zero) }
    var isLongPressing by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }
    var selectedIndicatorId by remember { mutableStateOf<String?>(null) }

    // Simple drawing state for trend line
    var simpleDrawingState by remember { mutableStateOf(SimpleDrawingState.NONE) }
    var firstPoint by remember { mutableStateOf<Pair<Int, Float>?>(null) }
    var firstPointScreen by remember { mutableStateOf<Offset?>(null) }
    
    // Create mutable local copies of persistent lines
    var localSimpleTrendLines by remember { mutableStateOf(simpleTrendLines) }
    var localArrows by remember { mutableStateOf(arrows) }
    var localExtendedLines by remember { mutableStateOf(extendedLines) }
    var localRays by remember { mutableStateOf(rays) }
    var localMeasurements by remember { mutableStateOf<List<com.bthr.backtest.model.Drawing.Measure>>(emptyList()) }
    
    // Force initial sync to ensure all lines are loaded properly
    LaunchedEffect(Unit) {
        android.util.Log.d("LineSync", "Force initial sync - Simple: ${simpleTrendLines.size}, Extended: ${extendedLines.size}, Arrows: ${arrows.size}, Rays: ${rays.size}")
        localSimpleTrendLines = simpleTrendLines
        localArrows = arrows
        localExtendedLines = extendedLines
        localRays = rays
    }
    
    // Update local state when parameters change
    LaunchedEffect(simpleTrendLines) { 
        android.util.Log.d("LineSync", "Updating localSimpleTrendLines: ${simpleTrendLines.size} items")
        localSimpleTrendLines = simpleTrendLines 
    }
    LaunchedEffect(arrows) { 
        android.util.Log.d("LineSync", "Updating localArrows: ${arrows.size} items")
        localArrows = arrows 
    }
    LaunchedEffect(extendedLines) { 
        android.util.Log.d("LineSync", "Updating localExtendedLines: ${extendedLines.size} items")
        localExtendedLines = extendedLines 
    }
    LaunchedEffect(rays) { 
        android.util.Log.d("LineSync", "Updating localRays: ${rays.size} items")
        localRays = rays 
    }
    var selectedSimpleTrendLineIndex by remember { mutableStateOf<Int?>(null) }
    var isDraggingSimpleTrendLine by remember { mutableStateOf(false) }
    var dragStartOffset by remember { mutableStateOf<Offset?>(null) }
    var originalTrendLinePosition by remember { mutableStateOf<SimpleTrendLine?>(null) }
    var selectedEndpoint by remember { mutableStateOf<Int?>(null) } // 0 = start point, 1 = end point

    // Extended lines (same structure as simple trend lines but with extend mode)
    // Use persistent lines from parameters instead of local state
    var selectedExtendedTrendLineIndex by remember { mutableStateOf<Int?>(null) }
    var isDraggingExtendedTrendLine by remember { mutableStateOf(false) }
    var originalExtendedTrendLinePosition by remember { mutableStateOf<SimpleTrendLine?>(null) }

    // Arrows (same structure as simple trend lines but with arrow drawing)
    // Use persistent lines from parameters instead of local state
    var selectedArrowIndex by remember { mutableStateOf<Int?>(null) }
    var isDraggingArrow by remember { mutableStateOf(false) }
    var originalArrowPosition by remember { mutableStateOf<SimpleTrendLine?>(null) }

    // Rays (ray line extends infinitely from origin through direction point)
    // Use persistent lines from parameters instead of local state
    var selectedRayIndex by remember { mutableStateOf<Int?>(null) }
    var isDraggingRay by remember { mutableStateOf(false) }
    var originalRayPosition by remember { mutableStateOf<SimpleTrendLine?>(null) }

    // Create a local mutable state for intentional activation that can be modified
    var localIntentionalActivation by remember { mutableStateOf(isIntentionalDrawingActivation) }
    
    // Update local state when parameter changes
    LaunchedEffect(isIntentionalDrawingActivation) {
        localIntentionalActivation = isIntentionalDrawingActivation
    }

    // Function to update persistent lines
    fun updatePersistentLines() {
        onLinesChanged?.invoke(localSimpleTrendLines, localArrows, localExtendedLines, localRays)
    }

    // ── Drawing Manager (replaces old isTrendLineMode / isFibMode scattered state) ──
    val drawingManager = remember { DrawingManager() }
    var effectiveDrawingUseMode by remember { mutableStateOf(drawingUseMode) }

    LaunchedEffect(drawingUseMode) {
        effectiveDrawingUseMode = drawingUseMode
        drawingManager.setUseMode(drawingUseMode)
    }

    // Activate tool from external menu
    LaunchedEffect(activeDrawingTool, effectiveDrawingUseMode) {
        drawingManager.setUseMode(effectiveDrawingUseMode)
        if (activeDrawingTool != DrawingTool.NONE) {
            drawingManager.setTool(activeDrawingTool)
        } else {
            drawingManager.cancelDrawing()
        }
    }

    // Trigger simple drawing mode when parent increments the trigger
    LaunchedEffect(activateDrawingModeTrigger) {
        if (activateDrawingModeTrigger > 0 && selectedSimpleTool != DrawingTool.NONE && isIntentionalDrawingActivation) {
            android.util.Log.d("SimpleDrawing", "LaunchedEffect triggered - activating simple drawing mode")
            android.util.Log.d("SimpleDrawing", "Selected tool to set: ${selectedSimpleTool.name}")
            android.util.Log.d("SimpleDrawing", "Trigger value: $activateDrawingModeTrigger")
            android.util.Log.d("SimpleDrawing", "Is intentional activation: $localIntentionalActivation")
            
            simpleDrawingState = SimpleDrawingState.PLACING_FIRST_POINT
            firstPoint = null
            firstPointScreen = null
            // Set the active tool based on what was selected in DrawingToolsMenu
            drawingManager.setTool(selectedSimpleTool)
            android.util.Log.d("SimpleDrawing", "Tool set in drawingManager: ${drawingManager.activeTool.name}")
            
            // Reset the intentional activation flag after using it
            localIntentionalActivation = false
        } else if (activateDrawingModeTrigger > 0) {
            android.util.Log.d("SimpleDrawing", "LaunchedEffect triggered but conditions not met - ignoring")
            android.util.Log.d("SimpleDrawing", "Trigger value: $activateDrawingModeTrigger")
            android.util.Log.d("SimpleDrawing", "Selected tool: ${selectedSimpleTool.name}")
            android.util.Log.d("SimpleDrawing", "Is intentional activation: $localIntentionalActivation")
        }
    }
    
    // Handle tool selection from DrawingToolsMenu
    onActivateSimpleDrawingMode?.let { onActivate ->
        // Create a wrapper callback that marks activation as intentional
        val wrappedCallback = { tool: DrawingTool ->
            android.util.Log.d("DrawingToolsMenu", "Tool selected from menu: ${tool.name}")
            android.util.Log.d("DrawingToolsMenu", "Marking as intentional activation")
            localIntentionalActivation = true
            onActivate(tool)
        }
        
        // Use the wrapped callback instead of the original
        // This will be passed to DrawingToolsMenu
    }
    
    // Set up the callback for intentional drawing activation
    LaunchedEffect(Unit) {
        // This callback will be used by DrawingToolsMenu to mark intentional activations
        // The actual assignment needs to be handled differently since onActivateSimpleDrawingMode is a parameter
    }
    
    // Handle tool selection from DrawingToolsMenu (no longer needed with direct parameter passing)

    // Favorites bar draggable state (px, null = not yet initialized)
    var favoritesBarPosition by remember { mutableStateOf<Offset?>(null) }
    var favoritesBarSizePx by remember { mutableStateOf(Offset(200f, 40f)) }
    var isDraggingFavoritesBar by remember { mutableStateOf(false) }

    var isAutoHeight by remember { mutableStateOf(true) }
    var manualMinPrice by remember { mutableFloatStateOf(0f) }
    var manualMaxPrice by remember { mutableFloatStateOf(100f) }

    val indicatorHeights = remember { mutableStateMapOf<String, Float>() }
    val defaultHeightPx = with(density) { 60.dp.toPx() }
    val minIndicatorHeightPx = with(density) { 30.dp.toPx() }
    val minimizedHeightPx = with(density) { 20.dp.toPx() }

    val volumeIndicator = remember(indicators) { indicators.filterIsInstance<Indicator.Volume>().firstOrNull() }
    val bottomIndicators = indicators.filter { (it is Indicator.RSI || it is Indicator.MACD || it is Indicator.Stochastic || it is Indicator.ATR) }
    val overlayIndicators = indicators.filter { it is Indicator.SMA || it is Indicator.EMA || it is Indicator.HMA || it is Indicator.VWAP || it is Indicator.BollingerBands || it is Indicator.ATRBands || it is Indicator.Supertrend || it is Indicator.Alligator || it is Indicator.Ichimoku || it is Indicator.Sessions || it is Indicator.Ribbon }
    val sessionsIndicator = remember(indicators) { indicators.filterIsInstance<Indicator.Sessions>().firstOrNull() }

    val indicatorAutoHeight = remember { mutableStateMapOf<String, Boolean>() }
    val indicatorRanges = remember { mutableStateMapOf<String, Pair<Float, Float>>() }

    LaunchedEffect(bottomIndicators) {
        bottomIndicators.forEach { indicator ->
            if (!indicatorHeights.containsKey(indicator.id)) indicatorHeights[indicator.id] = defaultHeightPx
            if (!indicatorAutoHeight.containsKey(indicator.id)) indicatorAutoHeight[indicator.id] = true
        }
    }

    var draggingSeparatorIdx by remember { mutableStateOf(-1) }
    var dragArea by remember { mutableStateOf(0) } 
    var draggingIndicatorId by remember { mutableStateOf<String?>(null) }
    var expandedIndicatorId by remember { mutableStateOf<String?>(null) }
    val showIndicatorLabels by remember { mutableStateOf(true) }
    var overlayCollapsed by remember { mutableStateOf(false) }

    val timeFormatter = remember(timeZone) { SimpleDateFormat("HH:mm\ndd MMM", Locale.getDefault()).apply { this.timeZone = timeZone } }
    val crosshairTimeFormatter = remember(timeZone) { 
        SimpleDateFormat("EEE dd MMM yy HH:mm", Locale.getDefault()).apply { this.timeZone = timeZone } 
    }
    
    // Formatters pour le fuseau horaire secondaire
    val secondaryTimeFormatter = remember(secondaryTimeZone) { 
        secondaryTimeZone?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).apply { this.timeZone = it } }
    }
    val secondaryCrosshairTimeFormatter = remember(secondaryTimeZone) { 
        secondaryTimeZone?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).apply { this.timeZone = it } }
    }

    val endIdx = (allCandles.size - scrollOffset.toInt()).coerceIn(0, allCandles.size)
    val startIdx = (endIdx - ceil(displayCount).toInt() - 10).coerceAtLeast(0)
    val visibleCandles = if (allCandles.isEmpty()) emptyList() else allCandles.subList(startIdx, endIdx)

    if (visibleCandles.isNotEmpty()) {
        val maxP = visibleCandles.maxOfOrNull { it.high } ?: 100f
        val minP = visibleCandles.minOfOrNull { it.low } ?: 0f
        if (isAutoHeight) {
            SideEffect { manualMinPrice = minP; manualMaxPrice = maxP }
        }
    }

    val smaValues = remember(allCandles, indicators) { 
        indicators.filterIsInstance<Indicator.SMA>().associate { 
            it.id to IndicatorCalculators.calculateSMA(
                allCandles, it.period, it.source)
        } 
    }
    val emaValues = remember(allCandles, indicators) { 
        indicators.filterIsInstance<Indicator.EMA>().associate { 
            it.id to IndicatorCalculators.calculateEMA(allCandles, it.period, it.source) 
        } 
    }
    val hmaValues = remember(allCandles, indicators) {
        indicators.filterIsInstance<Indicator.HMA>().associate {
            it.id to IndicatorCalculators.calculateHMA(allCandles, it.period, it.source)
        }
    }
    val vwapValues = remember(allCandles, indicators) {
        indicators.filterIsInstance<Indicator.VWAP>().associate {
            it.id to IndicatorCalculators.calculateVWAP(allCandles, it.anchor, it.source, it.bandMult1, it.bandMult2, it.bandMult3)
        }
    }
    val bbValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.BollingerBands>().associate { it.id to IndicatorCalculators.calculateBollingerBands(allCandles, it.period, it.stdDev) } }
    val atrBandsValues = remember(allCandles, indicators) { 
        indicators.filterIsInstance<Indicator.ATRBands>().associate { 
            it.id to IndicatorCalculators.calculateATRBands(allCandles, it.period, it.multiplier, it.source, it.showTPBands, it.tpScaleFactor) 
        } 
    }
    val stValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.Supertrend>().associate { it.id to IndicatorCalculators.calculateSupertrend(allCandles, it.period, it.multiplier) } }
    val alligatorValues = remember(allCandles, indicators) { 
        indicators.filterIsInstance<Indicator.Alligator>().associate { 
            it.id to IndicatorCalculators.calculateAlligator(allCandles, it.jawPeriod, it.jawOffset, it.teethPeriod, it.teethOffset, it.lipsPeriod, it.lipsOffset) 
        } 
    }
    val ichimokuValues = remember(allCandles, indicators) {
        indicators.filterIsInstance<Indicator.Ichimoku>().associate {
            it.id to IndicatorCalculators.calculateIchimoku(allCandles, it.tenkanPeriod, it.kijunPeriod, it.senkouBPeriod, it.displacement)
        }
    }
    val ribbonValues = remember(allCandles, indicators) {
        indicators.filterIsInstance<Indicator.Ribbon>().associate {
            it.id to IndicatorCalculators.calculateRibbon(allCandles, it.isExponential, it.source, it.refPeriod)
        }
    }
    
    val rsiValues = remember(allCandles, indicators) { 
        indicators.filterIsInstance<Indicator.RSI>().associate { 
            it.id to IndicatorCalculators.calculateRSI(allCandles, it.period, it.source) 
        } 
    }
    val rsiMaValues = remember(rsiValues, indicators) {
        indicators.filterIsInstance<Indicator.RSI>().associate { rsi ->
            val vals = rsiValues[rsi.id] ?: emptyList()
            rsi.id to if (rsi.showMa) {
                if (rsi.maType == "EMA") IndicatorCalculators.calculateEMAFromValues(vals, rsi.maPeriod)
                else IndicatorCalculators.calculateSMAFromValues(vals, rsi.maPeriod)
            } else emptyList()
        }
    }
    
    val macdValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.MACD>().associate { it.id to IndicatorCalculators.calculateMACD(allCandles, it.fastPeriod, it.slowPeriod, it.signalPeriod) } }
    val stochValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.Stochastic>().associate { it.id to IndicatorCalculators.calculateStochastic(allCandles, it.kPeriod, it.kSmoothing, it.dPeriod) } }
    val atrValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.ATR>().associate { it.id to IndicatorCalculators.calculateWilderATR(allCandles, it.period) } }

    val volumeMaValues = remember(allCandles, volumeIndicator) {
        volumeIndicator?.let { IndicatorCalculators.calculateVolumeMA(allCandles, it.maLength, it.maType) }
    }
    val smoothedMaValues = remember(allCandles, volumeIndicator) {
        volumeIndicator?.let { IndicatorCalculators.calculateVolumeMA(allCandles, it.smoothingLength, it.smoothingLine) }
    }

    LaunchedEffect(visibleCandles, bottomIndicators, rsiValues, rsiMaValues, macdValues, stochValues, atrValues) {
        bottomIndicators.forEach { ind ->
            if (indicatorAutoHeight[ind.id] != false) {
                val vals = when (ind) {
                    is Indicator.RSI -> {
                        val rsi = rsiValues[ind.id] ?: emptyList()
                        val rsiMa = rsiMaValues[ind.id] ?: emptyList()
                        val vRsi = if (rsi.isEmpty()) emptyList() else rsi.subList(startIdx.coerceIn(0, rsi.size), endIdx.coerceIn(0, rsi.size))
                        val vMa = if (rsiMa.isEmpty()) emptyList() else rsiMa.subList(startIdx.coerceIn(0, rsiMa.size), endIdx.coerceIn(0, rsiMa.size))
                        vRsi + vMa
                    }
                    is Indicator.MACD -> {
                        macdValues[ind.id]?.let {
                            val vMacd = if (it.macdLine.isEmpty()) emptyList() else it.macdLine.subList(startIdx.coerceIn(0, it.macdLine.size), endIdx.coerceIn(0, it.macdLine.size))
                            val vSignal = if (it.signalLine.isEmpty()) emptyList() else it.signalLine.subList(startIdx.coerceIn(0, it.signalLine.size), endIdx.coerceIn(0, it.signalLine.size))
                            val vHist = if (it.histogram.isEmpty()) emptyList() else it.histogram.let { h -> h.subList(startIdx.coerceIn(0, h.size), endIdx.coerceIn(0, h.size)) }
                            vMacd + vSignal + vHist
                        } ?: emptyList()
                    }
                    is Indicator.Stochastic -> {
                        stochValues[ind.id]?.let {
                            val vK = if (it.k.isEmpty()) emptyList() else it.k.subList(startIdx.coerceIn(0, it.k.size), endIdx.coerceIn(0, it.k.size))
                            val vD = if (it.d.isEmpty()) emptyList() else it.d.subList(startIdx.coerceIn(0, it.d.size), endIdx.coerceIn(0, it.d.size))
                            vK + vD
                        } ?: emptyList()
                    }
                    is Indicator.ATR -> {
                        val atr = atrValues[ind.id] ?: emptyList()
                        if (atr.isEmpty()) emptyList() else atr.subList(startIdx.coerceIn(0, atr.size), endIdx.coerceIn(0, atr.size))
                    }
                    else -> emptyList()
                }
                val visibleVals = vals.filterNotNull()
                if (visibleVals.isNotEmpty()) {
                    var minV = visibleVals.min(); var maxV = visibleVals.max()
                    val diff = (maxV - minV).coerceAtLeast(0.001f)
                    minV -= diff * 0.1f; maxV += diff * 0.1f
                    if (ind is Indicator.RSI || ind is Indicator.Stochastic) {
                        minV = minV.coerceAtMost(20f).coerceAtLeast(0f)
                        maxV = maxV.coerceAtLeast(80f).coerceAtMost(100f)
                    }
                    indicatorRanges[ind.id] = Pair(minV, maxV)
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val priceWidthPx = with(density) { 55.dp.toPx() }
        val timeHeightPx = with(density) { 35.dp.toPx() }

        // Helper to build a DrawingCoordinateMapper from current chart state
        fun buildMapper(
            chartWidthPx: Float, mainH: Float,
            totalCandles: Int, scrollOffset: Float, displayCount: Float, marginRightBars: Float,
            manualMinPrice: Float, manualMaxPrice: Float,
            topMarginPx: Float, bottomMarginPx: Float
        ): DrawingCoordinateMapper {
            val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
            val range = rawRange * 1.1f
            val minP = (manualMaxPrice + manualMinPrice) / 2f - range / 2f
            val maxP = minP + range
            return DrawingCoordinateMapper(
                chartWidthPx = chartWidthPx,
                chartHeightPx = mainH,
                totalCandleCount = totalCandles,
                scrollOffset = scrollOffset,
                displayCount = displayCount,
                marginRightBars = marginRightBars,
                minPrice = minP,
                maxPrice = maxP,
                topMarginPx = topMarginPx,
                bottomMarginPx = bottomMarginPx
            )
        }
        val chartWidthPx = w - priceWidthPx
        val plotsHeightPx = h - timeHeightPx
        val labelBgColor = colorScheme.background

        // Snap crosshair X to the nearest candle center so horizontal drag moves bar-to-bar.
        fun snapXToCandleCenter(rawX: Float): Float {
            // Pas de clampage supérieur : on autorise rawX > chartWidthPx pour les barres virtuelles futures
            val clampedX = rawX.coerceAtLeast(0f)
            if (allCandles.isEmpty()) return clampedX.coerceAtMost(chartWidthPx)

            val localCandleW = if (currentDisplayCount > 0) {
                chartWidthPx / (currentDisplayCount + settings.marginRightBars)
            } else 0f
            if (localCandleW <= 0f) return clampedX.coerceAtMost(chartWidthPx)

            val rawIdx = round(
                allCandles.size - 1 - scrollOffset -
                    (chartWidthPx - clampedX - localCandleW / 2f) / localCandleW
            ).toInt()

            // Autoriser les indices virtuels futurs (au-delà de la dernière bougie)
            // mais pas avant la première bougie
            val maxVirtualIdx = allCandles.size - 1 + (currentDisplayCount + settings.marginRightBars).toInt()
            val snappedIdx = rawIdx.coerceIn(0, maxVirtualIdx)

            val snappedX = chartWidthPx - (localCandleW / 2f) -
                ((allCandles.size - 1 - snappedIdx - scrollOffset) * localCandleW)

            // Si le snap tombe au-delà du bord droit du chart, déclencher un pan
            // pour créer de la marge droite (le chart scrolle avec le viseur)
            if (snappedX > chartWidthPx && snappedIdx > allCandles.size - 1) {
                val overshoot = snappedX - chartWidthPx
                val candlesOver = overshoot / localCandleW
                currentOnPan(-candlesOver)
            }

            return snappedX.coerceIn(0f, chartWidthPx)
        }

        fun updateCrosshairFromRaw(rawX: Float, rawY: Float) {
            crosshairPosition = Offset(
                snapXToCandleCenter(rawX),
                rawY.coerceIn(0f, h)
            )
        }

        fun candleIndexAtX(x: Float): Int {
            if (currentDisplayCount <= 0f) return -1
            val candleW = chartWidthPx / (currentDisplayCount + settings.marginRightBars)
            if (candleW <= 0f) return -1
            return round(allCandles.size - 1 - scrollOffset - (chartWidthPx - x - candleW / 2f) / candleW).toInt()
        }

        // Helper function to convert timestamp to X position
        fun timestampToX(timestamp: Long): Float {
            if (allCandles.isEmpty()) return 0f
            
            val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
            if (candleW <= 0f) return 0f
            
            // Find the candle index for this timestamp
            val targetIndex = allCandles.indexOfFirst { it.timestamp >= timestamp }
            
            return if (targetIndex == -1) {
                // Timestamp is beyond the last candle - extrapolate
                val lastCandle = allCandles.last()
                val lastX = chartWidthPx - (candleW / 2f) - ((allCandles.size - 1 - (allCandles.size - 1) - scrollOffset) * candleW)
                
                if (allCandles.size > 1) {
                    val avgTimePerBar = (allCandles.last().timestamp - allCandles[allCandles.size - 2].timestamp).toLong()
                    val timeDiff = timestamp - allCandles.last().timestamp
                    val barsAfter = timeDiff.toFloat() / avgTimePerBar.toFloat()
                    val extrapolatedIndex = (allCandles.size - 1) + barsAfter
                    // Use same formula as candleIdxToX but with extrapolated index
                    chartWidthPx - (allCandles.size - 1 - extrapolatedIndex - scrollOffset) * candleW - candleW / 2f
                } else {
                    // Only one candle, simple extrapolation
                    val extrapolatedIndex = (allCandles.size - 1) + 1f
                    chartWidthPx - (allCandles.size - 1 - extrapolatedIndex - scrollOffset) * candleW - candleW / 2f
                }
            } else {
                // Timestamp is within existing candles - use exact same formula as candleIdxToX
                val actualIndex = targetIndex.coerceIn(0, allCandles.size - 1).toFloat()
                chartWidthPx - (allCandles.size - 1 - actualIndex - scrollOffset) * candleW - candleW / 2f
            }
        }

        // Helper function to convert X position to timestamp
        fun xToTimestamp(x: Float): Long {
            val index = candleIndexAtX(x)
            return if (index in allCandles.indices) {
                allCandles[index].timestamp
            } else if (allCandles.isNotEmpty()) {
                // Extrapolate timestamp beyond existing candles
                val candleW = chartWidthPx / (currentDisplayCount + settings.marginRightBars)
                if (candleW <= 0f) return allCandles.last().timestamp
                
                if (index < 0) {
                    // Before first candle - extrapolate backwards
                    val firstCandle = allCandles.first()
                    val firstX = timestampToX(firstCandle.timestamp)
                    val barsBefore = (firstX - x) / candleW
                    val avgTimePerBar = if (allCandles.size > 1) {
                        (allCandles[1].timestamp - allCandles[0].timestamp).toLong()
                    } else 3600000L // 1 hour default
                    firstCandle.timestamp - (barsBefore * avgTimePerBar).toLong()
                } else {
                    // After last candle - extrapolate forwards
                    val lastCandle = allCandles.last()
                    val lastX = timestampToX(lastCandle.timestamp)
                    val barsAfter = (x - lastX) / candleW
                    val avgTimePerBar = if (allCandles.size > 1) {
                        (allCandles.last().timestamp - allCandles[allCandles.size - 2].timestamp).toLong()
                    } else 3600000L // 1 hour default
                    lastCandle.timestamp + (barsAfter * avgTimePerBar).toLong()
                }
            } else {
                System.currentTimeMillis()
            }
        }

        fun currentBottomIndicatorsHeight(): Float = bottomIndicators.fold(0f) { acc, indicator ->
            acc + if (indicator.isVisible) {
                indicatorHeights[indicator.id] ?: defaultHeightPx
            } else {
                minimizedHeightPx
            }
        }

        fun currentMainHeight(): Float = plotsHeightPx - currentBottomIndicatorsHeight()

        fun routeDrawingDrag(pointerPos: Offset, dragAmount: Offset) {
            val mainHLocal = currentMainHeight()
            val mapper = buildMapper(
                chartWidthPx = chartWidthPx,
                mainH = mainHLocal,
                totalCandles = allCandles.size,
                scrollOffset = scrollOffset,
                displayCount = displayCount,
                marginRightBars = settings.marginRightBars.toFloat(),
                manualMinPrice = manualMinPrice,
                manualMaxPrice = manualMaxPrice,
                topMarginPx = mainHLocal * (settings.marginTopPercent / 100f),
                bottomMarginPx = mainHLocal * (settings.marginBottomPercent / 100f)
            )
            drawingManager.onCursorMove(pointerPos)
            drawingManager.onDragUpdate(pointerPos, dragAmount, mapper)
        }

        fun clearChartSelection() {
            isSelected = false
            selectedIndicatorId = null
            selectedSimpleTrendLineIndex = null
            selectedExtendedTrendLineIndex = null
            selectedArrowIndex = null
            selectedRayIndex = null
            selectedEndpoint = null
        }

        fun isNearEndpoint(tap: Offset, lineIndex: Int, endpointIndex: Int, candleW: Float, mainH: Float, topM: Float, botM: Float, normY: (Float) -> Float): Boolean {
            android.util.Log.d("DragDebug", "isNearEndpoint called: lineIndex=$lineIndex, endpointIndex=$endpointIndex")
            android.util.Log.d("DragDebug", "Tap position: $tap")
            if (lineIndex !in localSimpleTrendLines.indices) {
                android.util.Log.d("DragDebug", "Line index out of bounds")
                return false
            }
            val line = localSimpleTrendLines[lineIndex]
            val startTimestamp = line.timestamp1
            val startPrice = line.price1
            val endTimestamp = line.timestamp2
            val endPrice = line.price2
            
            val point = if (endpointIndex == 0) {
                val x = timestampToX(startTimestamp)
                val y = normY(startPrice)
                Offset(x, y)
            } else {
                val x = timestampToX(endTimestamp)
                val y = normY(endPrice)
                Offset(x, y)
            }
            
            android.util.Log.d("DragDebug", "Endpoint $endpointIndex position: $point")
            val distance = sqrt((tap.x - point.x) * (tap.x - point.x) + (tap.y - point.y) * (tap.y - point.y))
            val threshold = 80f // Higher threshold for more lenient endpoint selection
            android.util.Log.d("DragDebug", "Distance to endpoint: $distance, threshold: $threshold")
            val result = distance <= threshold
            android.util.Log.d("DragDebug", "isNearEndpoint result: $result")
            return result
        }

        fun isNearExtendedEndpoint(tap: Offset, lineIndex: Int, endpointIndex: Int, candleW: Float, mainH: Float, topM: Float, botM: Float, normY: (Float) -> Float): Boolean {
            if (lineIndex !in localExtendedLines.indices) return false
            val line = localExtendedLines[lineIndex]
            val startTimestamp = line.timestamp1
            val startPrice = line.price1
            val endTimestamp = line.timestamp2
            val endPrice = line.price2
            
            val point = if (endpointIndex == 0) {
                val x = timestampToX(startTimestamp)
                val y = normY(startPrice)
                Offset(x, y)
            } else {
                val x = timestampToX(endTimestamp)
                val y = normY(endPrice)
                Offset(x, y)
            }
            
            val distance = sqrt((tap.x - point.x) * (tap.x - point.x) + (tap.y - point.y) * (tap.y - point.y))
            val threshold = 80f // Higher threshold for more lenient endpoint selection
            
            // Debug logs for extended endpoint detection
            android.util.Log.d("ExtendedEndpointDetect", "Line $lineIndex, endpoint $endpointIndex: point($point), tap($tap)")
            android.util.Log.d("ExtendedEndpointDetect", "Distance: $distance, threshold: $threshold, result: ${distance <= threshold}")
            
            return distance <= threshold
        }

        fun calculatePointToLineDistance(point: Offset, lineStart: Offset, lineEnd: Offset): Float {
            val lineLength = sqrt((lineEnd.x - lineStart.x) * (lineEnd.x - lineStart.x) + (lineEnd.y - lineStart.y) * (lineEnd.y - lineStart.y))
            if (lineLength == 0f) return sqrt((point.x - lineStart.x) * (point.x - lineStart.x) + (point.y - lineStart.y) * (point.y - lineStart.y))
            
            val t = ((point.x - lineStart.x) * (lineEnd.x - lineStart.x) + (point.y - lineStart.y) * (lineEnd.y - lineStart.y)) / (lineLength * lineLength)
            val clampedT = t.coerceIn(0f, 1f)
            
            val projectionX = lineStart.x + clampedT * (lineEnd.x - lineStart.x)
            val projectionY = lineStart.y + clampedT * (lineEnd.y - lineStart.y)
            
            return sqrt((point.x - projectionX) * (point.x - projectionX) + (point.y - projectionY) * (point.y - projectionY))
        }

        fun isNearArrow(tap: Offset, lineIndex: Int, candleW: Float, mainH: Float, topM: Float, botM: Float, normY: (Float) -> Float): Boolean {
            if (lineIndex !in localArrows.indices) return false
            val line = localArrows[lineIndex]
            val startTimestamp = line.timestamp1
            val startPrice = line.price1
            val endTimestamp = line.timestamp2
            val endPrice = line.price2
            
            val startX = timestampToX(startTimestamp)
            val startY = normY(startPrice)
            val endX = timestampToX(endTimestamp)
            val endY = normY(endPrice)
            
            // Debug log for arrow detection coordinates
            android.util.Log.d("ArrowDetect", "Detecting arrow $lineIndex at: start($startX, $startY), end($endX, $endY), tap($tap)")
            
            val distance = calculatePointToLineDistance(tap, Offset(startX, startY), Offset(endX, endY))
            val threshold = 50f // Distance threshold for line detection (same as other trend lines for consistency)
            android.util.Log.d("ArrowDetect", "Distance: $distance, threshold: $threshold, result: ${distance <= threshold}")
            return distance <= threshold
        }

        fun isNearArrowEndpoint(tap: Offset, lineIndex: Int, endpointIndex: Int, candleW: Float, mainH: Float, topM: Float, botM: Float, normY: (Float) -> Float): Boolean {
            if (lineIndex !in localArrows.indices) return false
            val line = localArrows[lineIndex]
            val startTimestamp = line.timestamp1
            val startPrice = line.price1
            val endTimestamp = line.timestamp2
            val endPrice = line.price2
            
            val point = if (endpointIndex == 0) {
                val x = timestampToX(startTimestamp)
                val y = normY(startPrice)
                Offset(x, y)
            } else {
                val x = timestampToX(endTimestamp)
                val y = normY(endPrice)
                Offset(x, y)
            }
            
            val distance = sqrt((tap.x - point.x) * (tap.x - point.x) + (tap.y - point.y) * (tap.y - point.y))
            val threshold = 80f // Higher threshold for more lenient endpoint selection
            return distance <= threshold
        }

        fun isNearSimpleTrendLine(tap: Offset, lineIndex: Int, candleW: Float, mainH: Float, topM: Float, botM: Float, normY: (Float) -> Float): Boolean {
            if (lineIndex !in localSimpleTrendLines.indices) return false
            val line = localSimpleTrendLines[lineIndex]
            val startTimestamp = line.timestamp1
            val startPrice = line.price1
            val endTimestamp = line.timestamp2
            val endPrice = line.price2
            
            val startX = timestampToX(startTimestamp)
            val startY = normY(startPrice)
            val endX = timestampToX(endTimestamp)
            val endY = normY(endPrice)
            
            // Debug logs for trend line detection
            android.util.Log.d("TrendLineDetect", "Line $lineIndex: start($startX, $startY), end($endX, $endY)")
            android.util.Log.d("TrendLineDetect", "Touch position: ($tap)")
            
            // Calculate distance from point to line segment
            val lineLength = sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY))
            if (lineLength == 0f) return false
            
            // Project point onto line
            val t = ((tap.x - startX) * (endX - startX) + (tap.y - startY) * (endY - startY)) / (lineLength * lineLength)
            val clampedT = t.coerceIn(0f, 1f)
            val closestX = startX + clampedT * (endX - startX)
            val closestY = startY + clampedT * (endY - startY)
            
            val distance = sqrt((tap.x - closestX) * (tap.x - closestX) + (tap.y - closestY) * (tap.y - closestY))
            val threshold = 50f // Increased threshold for easier line selection on mobile
            
            return distance <= threshold
        }

        fun isNearExtendedTrendLine(tap: Offset, lineIndex: Int, candleW: Float, mainH: Float, topM: Float, botM: Float, normY: (Float) -> Float): Boolean {
            if (lineIndex !in localExtendedLines.indices) return false
            val line = localExtendedLines[lineIndex]
            val startTimestamp = line.timestamp1
            val startPrice = line.price1
            val endTimestamp = line.timestamp2
            val endPrice = line.price2
            
            val startX = timestampToX(startTimestamp)
            val startY = normY(startPrice)
            val endX = timestampToX(endTimestamp)
            val endY = normY(endPrice)
            
            // Debug logs for extended trend line detection
            android.util.Log.d("ExtendedTrendLineDetect", "Line $lineIndex: startTimestamp=$startTimestamp, startPrice=$startPrice, endTimestamp=$endTimestamp, endPrice=$endPrice")
            android.util.Log.d("ExtendedTrendLineDetect", "Converted coords: start($startX, $startY), end($endX, $endY)")
            android.util.Log.d("ExtendedTrendLineDetect", "Touch position: ($tap)")
            android.util.Log.d("ExtendedTrendLineDetect", "Chart bounds: mainH=$mainH, topM=$topM, botM=$botM, effectiveMainH=${mainH - topM - botM}")
            
            // SIMPLIFIED: Use same logic as simple trend lines that WORK!
            val lineLength = sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY))
            if (lineLength == 0f) return false
            
            // Project point onto line segment (same as working simple trend lines)
            val t = ((tap.x - startX) * (endX - startX) + (tap.y - startY) * (endY - startY)) / (lineLength * lineLength)
            val clampedT = t.coerceIn(0f, 1f)
            val closestX = startX + clampedT * (endX - startX)
            val closestY = startY + clampedT * (endY - startY)
            
            android.util.Log.d("ExtendedTrendLineDetect", "Projection t: $t, closest($closestX, $closestY)")
            
            val distance = sqrt((tap.x - closestX) * (tap.x - closestX) + (tap.y - closestY) * (tap.y - closestY))
            val threshold = 50f // Increased threshold for easier line selection on mobile
            
            android.util.Log.d("ExtendedTrendLineDetect", "Distance: $distance, threshold: $threshold, result: ${distance <= threshold}")
            
            return distance <= threshold
        }

        fun isNearRay(tap: Offset, lineIndex: Int, candleW: Float, mainH: Float, topM: Float, botM: Float, normY: (Float) -> Float): Boolean {
            if (lineIndex !in localRays.indices) return false
            val line = localRays[lineIndex]
            val startTimestamp = line.timestamp1
            val startPrice = line.price1
            val endTimestamp = line.timestamp2
            val endPrice = line.price2

            val startX = timestampToX(startTimestamp)
            val startY = normY(startPrice)
            val endX = timestampToX(endTimestamp)
            val endY = normY(endPrice)

            // Debug logs for ray detection
            android.util.Log.d("RayDetect", "Ray $lineIndex: start($startX, $startY), end($endX, $endY)")
            android.util.Log.d("RayDetect", "Touch position: ($tap)")

            // Calculate ray direction
            val dirX = endX - startX
            val dirY = endY - startY
            val lineLength = sqrt(dirX * dirX + dirY * dirY)
            if (lineLength < 0.001f) return false

            // For ray: calculate where the ray exits the visible screen area
            // This defines the VISIBLE portion of the ray for hit testing
            val unitDirX = dirX / lineLength
            val unitDirY = dirY / lineLength

            // Find ray intersection with screen boundaries
            var maxT = 1f // At least include the direction point

            // Check right edge
            if (abs(unitDirX) > 0.001f) {
                val tRight = (chartWidthPx - startX) / (unitDirX * lineLength)
                if (tRight > maxT) maxT = tRight.toFloat()
                // Check left edge
                val tLeft = -startX / (unitDirX * lineLength)
                if (tLeft > maxT) maxT = tLeft.toFloat()
            }
            // Check bottom edge
            if (abs(unitDirY) > 0.001f) {
                val tBottom = (mainH - startY) / (unitDirY * lineLength)
                if (tBottom > maxT) maxT = tBottom.toFloat()
                // Check top edge
                val tTop = -startY / (unitDirY * lineLength)
                if (tTop > maxT) maxT = tTop.toFloat()
            }

            // Clamp to reasonable bounds (don't extend too far beyond screen)
            maxT = maxT.coerceAtMost(5f) // Limit extension to 5x the P1-P2 distance

            // Use same logic as simple trend lines that WORKS!
            val t = ((tap.x - startX) * dirX + (tap.y - startY) * dirY) / (lineLength * lineLength)
            val clampedT = t.coerceIn(0f, 1f)
            val closestX = startX + clampedT * dirX
            val closestY = startY + clampedT * dirY
            val perpDistance = sqrt((tap.x - closestX) * (tap.x - closestX) + (tap.y - closestY) * (tap.y - closestY))
            val perpThreshold = 50f // Same as extended trend lines

            val distance = perpDistance // Distance to segment (same as simple trend lines)
            val threshold = 50f // Same as extended trend lines

            android.util.Log.d("RayDetect", "t: $t, clampedT: $clampedT, closest($closestX, $closestY)")
            android.util.Log.d("RayDetect", "Distance: $distance, threshold: $threshold, result: ${distance <= threshold}")

            return distance <= threshold
        }

        fun isNearRayEndpoint(tap: Offset, lineIndex: Int, endpointIndex: Int, candleW: Float, mainH: Float, topM: Float, botM: Float, normY: (Float) -> Float): Boolean {
            if (lineIndex !in localRays.indices) return false
            val line = localRays[lineIndex]
            val startTimestamp = line.timestamp1
            val startPrice = line.price1
            val endTimestamp = line.timestamp2
            val endPrice = line.price2
            
            val point = if (endpointIndex == 0) {
                val x = timestampToX(startTimestamp)
                val y = normY(startPrice)
                Offset(x, y)
            } else {
                val x = timestampToX(endTimestamp)
                val y = normY(endPrice)
                Offset(x, y)
            }
            
            val distance = sqrt((tap.x - point.x) * (tap.x - point.x) + (tap.y - point.y) * (tap.y - point.y))
            val threshold = 100f // More forgiving for touch detection
            return distance <= threshold
        }

        // Simple drawing mode activation/deactivation functions
        fun activateSimpleDrawingMode() {
            simpleDrawingState = SimpleDrawingState.PLACING_FIRST_POINT
            firstPoint = null
            firstPointScreen = null
            // Show crosshair if not already visible
            if (crosshairPosition == null) {
                isLongPressing = true
                val mainHLocal = currentMainHeight()
                crosshairPosition = Offset(chartWidthPx / 2f, mainHLocal / 2f)
                crosshairDragOffset = Offset.Zero
            }
        }

        fun deactivateSimpleDrawingMode() {
            android.util.Log.d("SimpleDrawing", "Deactivating simple drawing mode and drawingManager")
            simpleDrawingState = SimpleDrawingState.NONE
            firstPoint = null
            firstPointScreen = null
            // Hide crosshair when exiting drawing mode
            isLongPressing = false
            crosshairPosition = null
            // Also deactivate the drawingManager to stop the blue crosshair
            drawingManager.cancelDrawing()
        }

        fun isOverlayIndicatorHit(ind: Indicator, idx: Int, priceAtY: Float, searchThreshold: Float): Boolean =
            when (ind) {
                is Indicator.SMA -> smaValues[ind.id]?.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false
                is Indicator.EMA -> emaValues[ind.id]?.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false
                is Indicator.HMA -> hmaValues[ind.id]?.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false
                is Indicator.VWAP -> vwapValues[ind.id]?.let { v ->
                    (v.vwap.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (if (ind.showBands) {
                            (v.upper1.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                                (v.lower1.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false)
                        } else false)
                } ?: false
                is Indicator.BollingerBands -> bbValues[ind.id]?.let { bb ->
                    (bb.middle.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (bb.upper.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (bb.lower.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.ATRBands -> atrBandsValues[ind.id]?.let { ab ->
                    (ab.upper.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (ab.lower.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (ab.tpUpper.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (ab.tpLower.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.Alligator -> alligatorValues[ind.id]?.let { res ->
                    (res.jaw.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.teeth.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.lips.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.Ichimoku -> ichimokuValues[ind.id]?.let { res ->
                    (res.tenkanSen.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.kijunSen.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.senkouSpanA.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.senkouSpanB.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.chikouSpan.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.Supertrend -> stValues[ind.id]?.values?.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false
                is Indicator.Ribbon -> ribbonValues[ind.id]?.let { res ->
                    res.mas.any { ma -> ma.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false }
                } ?: false
                is Indicator.Sessions -> false
                else -> false
            }

        fun isBottomIndicatorHit(ind: Indicator, idx: Int, valAtY: Float, searchThreshold: Float): Boolean =
            when (ind) {
                is Indicator.RSI ->
                    (rsiValues[ind.id]?.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false) ||
                        (rsiMaValues[ind.id]?.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false)
                is Indicator.MACD -> macdValues[ind.id]?.let { res ->
                    (res.macdLine.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false) ||
                        (res.signalLine.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false) ||
                        (res.histogram.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.Stochastic -> stochValues[ind.id]?.let { res ->
                    (res.k.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false) ||
                        (res.d.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.ATR -> atrValues[ind.id]?.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false
                else -> false
            }

        fun handleBottomIndicatorSettingsTap(ind: Indicator, idx: Int, valAtY: Float, rV: Float): Boolean {
            if (isBottomIndicatorHit(ind, idx, valAtY, rV * 0.05f)) {
                onIndicatorSettingsRequest(ind)
                return true
            }
            return false
        }

        fun handleMainCanvasDrag(change: PointerInputChange, dragAmount: Offset) {
            if (change.isConsumed) {
                return
            }

            // Handle dragging selected simple trendline or endpoint
            if (selectedSimpleTrendLineIndex != null && simpleDrawingState == SimpleDrawingState.NONE) {
                android.util.Log.d("DragDebug", "Selected simple trend line index: $selectedSimpleTrendLineIndex")
                android.util.Log.d("DragDebug", "Simple drawing state: $simpleDrawingState")
                android.util.Log.d("DragDebug", "Is dragging simple trend line: $isDraggingSimpleTrendLine")
                // Reduce log frequency for better performance
                val mainHLocal = currentMainHeight()
                val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                val topM = mainHLocal * (settings.marginTopPercent / 100f)
                val botM = mainHLocal * (settings.marginBottomPercent / 100f)
                val effectiveMainH = mainHLocal - topM - botM
                val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                val range = rawRange * 1.1f
                val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                val normY: (Float) -> Float = { topM + effectiveMainH - ((it - minP) / range * effectiveMainH) }
                
                // Initialize drag state on first drag event
                if (!isDraggingSimpleTrendLine) {
                    val index = selectedSimpleTrendLineIndex!!
                    android.util.Log.d("DragDebug", "Initializing drag for line at index: $index")
                    if (index in localSimpleTrendLines.indices) {
                        val line = localSimpleTrendLines[index]
                        android.util.Log.d("DragDebug", "Line found, locked status: ${line.isLocked}")
                        // Check if line is locked - if so, don't allow dragging
                        if (line.isLocked) {
                            android.util.Log.d("DragDebug", "Line is locked, drag not allowed")
                            return@handleMainCanvasDrag
                        }
                        
                        // Detect if dragging on endpoint or line body
                        android.util.Log.d("TrendLineDrag", "Checking drag detection at position: ${change.position}")
                        if (isNearEndpoint(change.position, index, 0, candleW, mainHLocal, topM, botM, normY)) {
                            android.util.Log.d("TrendLineDrag", "Start endpoint detected - starting drag")
                            isDraggingSimpleTrendLine = true
                            dragStartOffset = change.position
                            originalTrendLinePosition = localSimpleTrendLines[index]
                            selectedEndpoint = 0 // Dragging start endpoint
                        } else if (isNearEndpoint(change.position, index, 1, candleW, mainHLocal, topM, botM, normY)) {
                            android.util.Log.d("TrendLineDrag", "End endpoint detected - starting drag")
                            isDraggingSimpleTrendLine = true
                            dragStartOffset = change.position
                            originalTrendLinePosition = localSimpleTrendLines[index]
                            selectedEndpoint = 1 // Dragging end endpoint
                        } else if (isNearSimpleTrendLine(change.position, index, candleW, mainHLocal, topM, botM, normY)) {
                            android.util.Log.d("TrendLineDrag", "Line body detected - starting drag")
                            isDraggingSimpleTrendLine = true
                            dragStartOffset = change.position
                            originalTrendLinePosition = localSimpleTrendLines[index]
                            selectedEndpoint = null // Dragging whole line
                        } else {
                            // More lenient: if line is selected, allow drag to start anywhere within 100px
                            // This helps with fast drags where finger might not be exactly on line
                            val startTimestamp = line.timestamp1
                            val startPrice = line.price1
                            val endTimestamp = line.timestamp2
                            val endPrice = line.price2
                            
                            val startX = timestampToX(startTimestamp)
                            val startY = normY(startPrice)
                            val endX = timestampToX(endTimestamp)
                            val endY = normY(endPrice)
                            
                            // Check if finger is within 100px of either endpoint or line body
                            val distToStart = sqrt((change.position.x - startX) * (change.position.x - startX) + (change.position.y - startY) * (change.position.y - startY))
                            val distToEnd = sqrt((change.position.x - endX) * (change.position.x - endX) + (change.position.y - endY) * (change.position.y - endY))
                            
                            android.util.Log.d("TrendLineDrag", "Distance check - distToStart: $distToStart, distToEnd: $distToEnd")
                            if (distToStart <= 100f) {
                                android.util.Log.d("TrendLineDrag", "Start endpoint within 100px - starting drag")
                                isDraggingSimpleTrendLine = true
                                dragStartOffset = change.position
                                originalTrendLinePosition = localSimpleTrendLines[index]
                                selectedEndpoint = 0
                            } else if (distToEnd <= 100f) {
                                android.util.Log.d("TrendLineDrag", "End endpoint within 100px - starting drag")
                                isDraggingSimpleTrendLine = true
                                dragStartOffset = change.position
                                originalTrendLinePosition = localSimpleTrendLines[index]
                                selectedEndpoint = 1
                            } else {
                                // Check distance to line segment
                                val lineLength = sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY))
                                if (lineLength > 0f) {
                                    val t = ((change.position.x - startX) * (endX - startX) + (change.position.y - startY) * (endY - startY)) / (lineLength * lineLength)
                                    val clampedT = t.coerceIn(0f, 1f)
                                    val closestX = startX + clampedT * (endX - startX)
                                    val closestY = startY + clampedT * (endY - startY)
                                    val distToLine = sqrt((change.position.x - closestX) * (change.position.x - closestX) + (change.position.y - closestY) * (change.position.y - closestY))
                                    
                                    android.util.Log.d("TrendLineDrag", "Line distance check - distToLine: $distToLine")
                                    if (distToLine <= 100f) {
                                        android.util.Log.d("TrendLineDrag", "Line within 100px - starting drag")
                                        isDraggingSimpleTrendLine = true
                                        dragStartOffset = change.position
                                        originalTrendLinePosition = localSimpleTrendLines[index]
                                        selectedEndpoint = null
                                    }
                                }
                            }
                        }
                    }
                }
                
                // If drag is active, update line position
                if (isDraggingSimpleTrendLine) {
                    android.util.Log.d("DragDebug", "Simple trend line drag detected")
                    android.util.Log.d("DragDebug", "Drag position: ${change.position}")
                    
                    // Calculate total displacement from drag start
                    val totalDragX = change.position.x - (dragStartOffset?.x ?: 0f)
                    val totalDragY = change.position.y - (dragStartOffset?.y ?: 0f)
                    
                    android.util.Log.d("DragDebug", "Total drag X: $totalDragX, Total drag Y: $totalDragY")
                    android.util.Log.d("DragDebug", "Drag start offset: $dragStartOffset")
                    
                    // Allow both horizontal and vertical movement
                    val priceDelta = (totalDragY / effectiveMainH) * range
                    
                    // Calculate smooth horizontal movement based on actual X position
                    val currentX = change.position.x
                    val originalX = dragStartOffset?.x ?: currentX
                    val horizontalMovement = currentX - originalX
                    
                    android.util.Log.d("DragDebug", "Price delta: $priceDelta, Horizontal movement: $horizontalMovement")
                    android.util.Log.d("DragDebug", "Candle width: $candleW, Effective height: $effectiveMainH, Range: $range")
                    
                    // Update the selected trendline from original position
                    val index = selectedSimpleTrendLineIndex!!
                    originalTrendLinePosition?.let { originalLine ->
                        val startTimestamp = originalLine.timestamp1
                        val startPrice = originalLine.price1
                        val endTimestamp = originalLine.timestamp2
                        val endPrice = originalLine.price2
                        
                        android.util.Log.d("DragDebug", "Simple trend line - Original timestamps: start=$startTimestamp, end=$endTimestamp")
                        
                        // Calculate new timestamps based on smooth horizontal movement
                        val (newStartTimestamp, newEndTimestamp) = if (selectedEndpoint == null) {
                            // Move whole line - apply horizontal movement to both endpoints
                            val startX = timestampToX(startTimestamp)
                            val endX = timestampToX(endTimestamp)
                            val newStartX = startX + horizontalMovement
                            val newEndX = endX + horizontalMovement
                            
                            Pair(xToTimestamp(newStartX), xToTimestamp(newEndX))
                        } else if (selectedEndpoint == 0) {
                            // Move only start point
                            val startX = timestampToX(startTimestamp)
                            val newStartX = startX + horizontalMovement
                            
                            Pair(xToTimestamp(newStartX), endTimestamp)
                        } else {
                            // Move only end point
                            val endX = timestampToX(endTimestamp)
                            val newEndX = endX + horizontalMovement
                            
                            Pair(startTimestamp, xToTimestamp(newEndX))
                        }
                        
                        android.util.Log.d("DragDebug", "Simple trend line - New timestamps: newStart=$newStartTimestamp, newEnd=$newEndTimestamp")
                        
                        val updatedLine = if (selectedEndpoint == null) {
                            // Move whole line
                            originalLine.copy(
                                timestamp1 = newStartTimestamp,
                                price1 = startPrice - priceDelta,
                                timestamp2 = newEndTimestamp,
                                price2 = endPrice - priceDelta
                            )
                        } else if (selectedEndpoint == 0) {
                            // Move only start point
                            originalLine.copy(
                                timestamp1 = newStartTimestamp,
                                price1 = startPrice - priceDelta
                            )
                        } else {
                            // Move only end point
                            originalLine.copy(
                                timestamp2 = newEndTimestamp,
                                price2 = endPrice - priceDelta
                            )
                        }
                        
                        android.util.Log.d("DragDebug", "Updated line timestamps: ${updatedLine.timestamp1} -> ${updatedLine.timestamp2}")
                        android.util.Log.d("DragDebug", "Updated line prices: ${updatedLine.price1} -> ${updatedLine.price2}")
                        
                        localSimpleTrendLines = localSimpleTrendLines.toMutableList().also { it[index] = updatedLine }
                        // Don't call updatePersistentLines during drag for better performance
                        // updatePersistentLines() will be called when drag ends
                        android.util.Log.d("TrendLineDrag", "Line updated locally during drag")
                    }
                }
                
                change.consume()
                return
            }

            // Handle dragging selected extended trendline or endpoint
            if (selectedExtendedTrendLineIndex != null && simpleDrawingState == SimpleDrawingState.NONE) {
                // Reduce log frequency for better performance
                val mainHLocal = currentMainHeight()
                val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                val topM = mainHLocal * (settings.marginTopPercent / 100f)
                val botM = mainHLocal * (settings.marginBottomPercent / 100f)
                val effectiveMainH = mainHLocal - topM - botM
                val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                val range = rawRange * 1.1f
                val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                val normY: (Float) -> Float = { topM + effectiveMainH - ((it - minP) / range * effectiveMainH) }
                
                // Initialize drag state on first drag event
                if (!isDraggingExtendedTrendLine) {
                    val index = selectedExtendedTrendLineIndex!!
                    if (index in localExtendedLines.indices) {
                        val line = localExtendedLines[index]
                        // Check if line is locked - if so, don't allow dragging
                        if (line.isLocked) return@handleMainCanvasDrag
                        
                        // Detect if dragging on endpoint or line body
                        android.util.Log.d("TrendLineDrag", "Extended: Checking drag detection at position: ${change.position}")
                        if (isNearExtendedEndpoint(change.position, index, 0, candleW, mainHLocal, topM, botM, normY)) {
                            android.util.Log.d("TrendLineDrag", "Extended: Start endpoint detected - starting drag")
                            isDraggingExtendedTrendLine = true
                            dragStartOffset = change.position
                            originalExtendedTrendLinePosition = localExtendedLines[index]
                            selectedEndpoint = 0 // Dragging start endpoint
                        } else if (isNearExtendedEndpoint(change.position, index, 1, candleW, mainHLocal, topM, botM, normY)) {
                            android.util.Log.d("TrendLineDrag", "Extended: End endpoint detected - starting drag")
                            isDraggingExtendedTrendLine = true
                            dragStartOffset = change.position
                            originalExtendedTrendLinePosition = localExtendedLines[index]
                            selectedEndpoint = 1 // Dragging end endpoint
                        } else if (isNearExtendedTrendLine(change.position, index, candleW, mainHLocal, topM, botM, normY)) {
                            android.util.Log.d("TrendLineDrag", "Extended: Line body detected - starting drag")
                            isDraggingExtendedTrendLine = true
                            dragStartOffset = change.position
                            originalExtendedTrendLinePosition = localExtendedLines[index]
                            selectedEndpoint = null // Dragging whole line
                        } else {
                            // More lenient: if line is selected, allow drag to start anywhere within 100px
                            val startTimestamp = line.timestamp1
                            val startPrice = line.price1
                            val endTimestamp = line.timestamp2
                            val endPrice = line.price2
                            
                            val startX = timestampToX(startTimestamp)
                            val startY = normY(startPrice)
                            val endX = timestampToX(endTimestamp)
                            val endY = normY(endPrice)
                            
                            val distance = calculatePointToLineDistance(change.position, Offset(startX, startY), Offset(endX, endY))
                            if (distance < 100f) {
                                android.util.Log.d("TrendLineDrag", "Extended: Lenient drag allowed - distance: $distance")
                                isDraggingExtendedTrendLine = true
                                dragStartOffset = change.position
                                originalExtendedTrendLinePosition = localExtendedLines[index]
                                selectedEndpoint = null // Dragging whole line
                            } else {
                                android.util.Log.d("TrendLineDrag", "Extended: Too far from line - not dragging")
                                return@handleMainCanvasDrag
                            }
                        }
                    }
                }
                
                // Handle ongoing drag
                if (isDraggingExtendedTrendLine) {
                    // Calculate total displacement from drag start (same as simple trend lines)
                    val totalDragX = change.position.x - (dragStartOffset?.x ?: 0f)
                    val totalDragY = change.position.y - (dragStartOffset?.y ?: 0f)
                    
                    // Allow both horizontal and vertical movement (same as simple trend lines)
                    val priceDelta = (totalDragY / effectiveMainH) * range
                    
                    // Calculate smooth horizontal movement based on actual X position (same as simple trend lines)
                    val currentX = change.position.x
                    val originalX = dragStartOffset?.x ?: currentX
                    val horizontalMovement = currentX - originalX
                    
                    // Update the selected extended trendline from original position
                    val index = selectedExtendedTrendLineIndex!!
                    originalExtendedTrendLinePosition?.let { originalLine ->
                        val startTimestamp = originalLine.timestamp1
                        val startPrice = originalLine.price1
                        val endTimestamp = originalLine.timestamp2
                        val endPrice = originalLine.price2
                        
                        // Calculate new timestamps based on smooth horizontal movement (same as simple trend lines)
                        val (newStartTimestamp, newEndTimestamp) = if (selectedEndpoint == null) {
                            // Move whole line - apply horizontal movement to both endpoints
                            val startX = timestampToX(startTimestamp)
                            val endX = timestampToX(endTimestamp)
                            val newStartX = startX + horizontalMovement
                            val newEndX = endX + horizontalMovement
                            
                            Pair(xToTimestamp(newStartX), xToTimestamp(newEndX))
                        } else if (selectedEndpoint == 0) {
                            // Move only start point
                            val startX = timestampToX(startTimestamp)
                            val newStartX = startX + horizontalMovement
                            
                            Pair(xToTimestamp(newStartX), endTimestamp)
                        } else {
                            // Move only end point
                            val endX = timestampToX(endTimestamp)
                            val newEndX = endX + horizontalMovement
                            
                            Pair(startTimestamp, xToTimestamp(newEndX))
                        }
                        
                        val updatedLine = if (selectedEndpoint == null) {
                            // Move whole line
                            originalLine.copy(
                                timestamp1 = newStartTimestamp,
                                price1 = startPrice - priceDelta,
                                timestamp2 = newEndTimestamp,
                                price2 = endPrice - priceDelta
                            )
                        } else if (selectedEndpoint == 0) {
                            // Move only start point
                            originalLine.copy(
                                timestamp1 = newStartTimestamp,
                                price1 = startPrice - priceDelta
                            )
                        } else {
                            // Move only end point
                            originalLine.copy(
                                timestamp2 = newEndTimestamp,
                                price2 = endPrice - priceDelta
                            )
                        }
                        
                        // Update the extended trendline in the list
                        localExtendedLines = localExtendedLines.toMutableList().also { 
                            it[index] = updatedLine 
                        }
                        // Don't call updatePersistentLines during drag for better performance
                        // updatePersistentLines() will be called when drag ends
                    }
                }
                
                change.consume()
                return
            }

            // Handle dragging selected arrow or endpoint
            if (selectedArrowIndex != null && simpleDrawingState == SimpleDrawingState.NONE) {
                // Reduce log frequency for better performance
                val mainHLocal = currentMainHeight()
                val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                val topM = mainHLocal * (settings.marginTopPercent / 100f)
                val botM = mainHLocal * (settings.marginBottomPercent / 100f)
                val effectiveMainH = mainHLocal - topM - botM
                val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                val range = rawRange * 1.1f
                val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                val normY: (Float) -> Float = { topM + effectiveMainH - ((it - minP) / range * effectiveMainH) }
                
                // Initialize drag state on first drag event
                if (!isDraggingArrow) {
                    val index = selectedArrowIndex!!
                    if (index in localArrows.indices) {
                        val line = localArrows[index]
                        // Check if line is locked - if so, don't allow dragging
                        if (line.isLocked) return@handleMainCanvasDrag
                        
                        // Detect if dragging on endpoint or line body
                        android.util.Log.d("TrendLineDrag", "Arrow: Checking drag detection at position: ${change.position}")
                        if (isNearArrowEndpoint(change.position, index, 0, candleW, mainHLocal, topM, botM, normY)) {
                            android.util.Log.d("TrendLineDrag", "Arrow: Start endpoint detected - starting drag")
                            isDraggingArrow = true
                            dragStartOffset = change.position
                            originalArrowPosition = localArrows[index]
                            selectedEndpoint = 0 // Dragging start endpoint
                        } else if (isNearArrowEndpoint(change.position, index, 1, candleW, mainHLocal, topM, botM, normY)) {
                            android.util.Log.d("TrendLineDrag", "Arrow: End endpoint detected - starting drag")
                            isDraggingArrow = true
                            dragStartOffset = change.position
                            originalArrowPosition = localArrows[index]
                            selectedEndpoint = 1 // Dragging end endpoint
                        } else if (isNearArrow(change.position, index, candleW, mainHLocal, topM, botM, normY)) {
                            android.util.Log.d("TrendLineDrag", "Arrow: Line body detected - starting drag")
                            isDraggingArrow = true
                            dragStartOffset = change.position
                            originalArrowPosition = localArrows[index]
                            selectedEndpoint = null // Dragging whole line
                        } else {
                            // More lenient: if line is selected, allow drag to start anywhere within 100px
                            val startTimestamp = line.timestamp1
                            val startPrice = line.price1
                            val endTimestamp = line.timestamp2
                            val endPrice = line.price2
                            
                            val startX = timestampToX(startTimestamp)
                            val startY = normY(startPrice)
                            val endX = timestampToX(endTimestamp)
                            val endY = normY(endPrice)
                            
                            val distance = calculatePointToLineDistance(change.position, Offset(startX, startY), Offset(endX, endY))
                            if (distance < 100f) {
                                android.util.Log.d("TrendLineDrag", "Arrow: Lenient drag allowed - distance: $distance")
                                isDraggingArrow = true
                                dragStartOffset = change.position
                                originalArrowPosition = localArrows[index]
                                selectedEndpoint = null // Dragging whole line
                            } else {
                                android.util.Log.d("TrendLineDrag", "Arrow: Too far from line - not dragging")
                                return@handleMainCanvasDrag
                            }
                        }
                    }
                }
                
                // Handle ongoing drag
                if (isDraggingArrow) {
                    // Calculate total displacement from drag start (same as simple trend lines)
                    val totalDragX = change.position.x - (dragStartOffset?.x ?: 0f)
                    val totalDragY = change.position.y - (dragStartOffset?.y ?: 0f)
                    
                    // Allow both horizontal and vertical movement (same as simple trend lines)
                    val priceDelta = (totalDragY / effectiveMainH) * range
                    
                    // Calculate smooth horizontal movement based on actual X position (same as simple trend lines)
                    val currentX = change.position.x
                    val originalX = dragStartOffset?.x ?: currentX
                    val horizontalMovement = currentX - originalX
                    
                    // Update the selected arrow from original position
                    val index = selectedArrowIndex!!
                    originalArrowPosition?.let { originalLine ->
                        val startTimestamp = originalLine.timestamp1
                        val startPrice = originalLine.price1
                        val endTimestamp = originalLine.timestamp2
                        val endPrice = originalLine.price2
                        
                        // Calculate new timestamps based on smooth horizontal movement (same as simple trend lines)
                        val (newStartTimestamp, newEndTimestamp) = if (selectedEndpoint == null) {
                            // Move whole line - apply horizontal movement to both endpoints
                            val startX = timestampToX(startTimestamp)
                            val endX = timestampToX(endTimestamp)
                            val newStartX = startX + horizontalMovement
                            val newEndX = endX + horizontalMovement
                            
                            Pair(xToTimestamp(newStartX), xToTimestamp(newEndX))
                        } else if (selectedEndpoint == 0) {
                            // Move only start point
                            val startX = timestampToX(startTimestamp)
                            val newStartX = startX + horizontalMovement
                            
                            Pair(xToTimestamp(newStartX), endTimestamp)
                        } else {
                            // Move only end point
                            val endX = timestampToX(endTimestamp)
                            val newEndX = endX + horizontalMovement
                            
                            Pair(startTimestamp, xToTimestamp(newEndX))
                        }
                        
                        val updatedLine = if (selectedEndpoint == null) {
                            // Move whole line
                            originalLine.copy(
                                timestamp1 = newStartTimestamp,
                                price1 = startPrice - priceDelta,
                                timestamp2 = newEndTimestamp,
                                price2 = endPrice - priceDelta
                            )
                        } else if (selectedEndpoint == 0) {
                            // Move only start point
                            originalLine.copy(
                                timestamp1 = newStartTimestamp,
                                price1 = startPrice - priceDelta
                            )
                        } else {
                            // Move only end point
                            originalLine.copy(
                                timestamp2 = newEndTimestamp,
                                price2 = endPrice - priceDelta
                            )
                        }
                        
                        // Update the arrow in the list
                        localArrows = localArrows.toMutableList().also { 
                            it[index] = updatedLine 
                        }
                        // Don't call updatePersistentLines during drag for better performance
                        // updatePersistentLines() will be called when drag ends
                    }
                }
                
                change.consume()
                return
            }

            // Handle dragging selected ray or endpoint
            if (selectedRayIndex != null && simpleDrawingState == SimpleDrawingState.NONE) {
                // Reduce log frequency for better performance
                val mainHLocal = currentMainHeight()
                val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                val topM = mainHLocal * (settings.marginTopPercent / 100f)
                val botM = mainHLocal * (settings.marginBottomPercent / 100f)
                val effectiveMainH = mainHLocal - topM - botM
                val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                val range = rawRange * 1.1f
                val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                val normY: (Float) -> Float = { topM + effectiveMainH - ((it - minP) / range * effectiveMainH) }

                // Initialize drag state on first drag event - STRICT CHECK
                if (!isDraggingRay) {
                    val index = selectedRayIndex!!
                    if (index in localRays.indices) {
                        val line = localRays[index]
                        // Check if line is locked - if so, don't allow dragging
                        if (line.isLocked) {
                            android.util.Log.d("TrendLineDrag", "Ray: Line is locked - not dragging")
                            return@handleMainCanvasDrag
                        }

                        // Calculate actual endpoint positions for debugging
                        val startTimestamp = line.timestamp1
                        val startPrice = line.price1
                        val endTimestamp = line.timestamp2
                        val endPrice = line.price2
                        val startX = timestampToX(startTimestamp)
                        val startY = normY(startPrice)
                        val endX = timestampToX(endTimestamp)
                        val endY = normY(endPrice)

                        // Calculate distances for debugging
                        val distToP1 = sqrt((change.position.x - startX) * (change.position.x - startX) + (change.position.y - startY) * (change.position.y - startY))
                        val distToP2 = sqrt((change.position.x - endX) * (change.position.x - endX) + (change.position.y - endY) * (change.position.y - endY))

                        android.util.Log.d("TrendLineDrag", "Ray: Touch=${change.position}, P1=($startX, $startY), P2=($endX, $endY)")
                        android.util.Log.d("TrendLineDrag", "Ray: Distances - distToP1=$distToP1 (threshold=100), distToP2=$distToP2 (threshold=100)")

                        // STRICT detection: only start drag if touching EXACT hit zones
                        val nearP1 = isNearRayEndpoint(change.position, index, 0, candleW, mainHLocal, topM, botM, normY)
                        val nearP2 = isNearRayEndpoint(change.position, index, 1, candleW, mainHLocal, topM, botM, normY)
                        val nearLine = isNearRay(change.position, index, candleW, mainHLocal, topM, botM, normY)

                        android.util.Log.d("TrendLineDrag", "Ray: Hit test at ${change.position} - nearP1=$nearP1, nearP2=$nearP2, nearLine=$nearLine")

                        when {
                            nearP1 -> {
                                android.util.Log.d("TrendLineDrag", "Ray: HIT P1 zone - starting endpoint drag")
                                isDraggingRay = true
                                dragStartOffset = change.position
                                originalRayPosition = localRays[index]
                                selectedEndpoint = 0
                            }
                            nearP2 -> {
                                android.util.Log.d("TrendLineDrag", "Ray: HIT P2 zone - starting endpoint drag")
                                isDraggingRay = true
                                dragStartOffset = change.position
                                originalRayPosition = localRays[index]
                                selectedEndpoint = 1
                            }
                            nearLine -> {
                                android.util.Log.d("TrendLineDrag", "Ray: HIT LINE zone - starting line drag")
                                isDraggingRay = true
                                dragStartOffset = change.position
                                originalRayPosition = localRays[index]
                                selectedEndpoint = null
                            }
                            else -> {
                                // STRICT: Not touching any hit zone - completely ignore this drag
                                android.util.Log.d("TrendLineDrag", "Ray: NO HIT - ignoring drag completely")
                                return@handleMainCanvasDrag
                            }
                        }
                    }
                }

                // Handle ongoing drag with RAY CONSTRAINTS
                if (isDraggingRay) {
                    // Calculate total displacement from drag start (same as simple trend lines)
                    val totalDragX = change.position.x - (dragStartOffset?.x ?: 0f)
                    val totalDragY = change.position.y - (dragStartOffset?.y ?: 0f)

                    // Allow both horizontal and vertical movement (same as simple trend lines)
                    val priceDelta = (totalDragY / effectiveMainH) * range
                    
                    // Calculate smooth horizontal movement based on actual X position (same as simple trend lines)
                    val currentX = change.position.x
                    val originalX = dragStartOffset?.x ?: currentX
                    val horizontalMovement = currentX - originalX

                    // Update the selected ray from original position
                    val index = selectedRayIndex!!
                    originalRayPosition?.let { originalLine ->
                        val startTimestamp = originalLine.timestamp1
                        val startPrice = originalLine.price1
                        val endTimestamp = originalLine.timestamp2
                        val endPrice = originalLine.price2

                        // Calculate new timestamps based on smooth horizontal movement (same as simple trend lines)
                        val (newStartTimestamp, newEndTimestamp) = if (selectedEndpoint == null) {
                            // Move whole line - apply horizontal movement to both endpoints
                            val startX = timestampToX(startTimestamp)
                            val endX = timestampToX(endTimestamp)
                            val newStartX = startX + horizontalMovement
                            val newEndX = endX + horizontalMovement
                            
                            Pair(xToTimestamp(newStartX), xToTimestamp(newEndX))
                        } else if (selectedEndpoint == 0) {
                            // Move only start point
                            val startX = timestampToX(startTimestamp)
                            val newStartX = startX + horizontalMovement
                            
                            Pair(xToTimestamp(newStartX), endTimestamp)
                        } else {
                            // Move only end point
                            val endX = timestampToX(endTimestamp)
                            val newEndX = endX + horizontalMovement
                            
                            Pair(startTimestamp, xToTimestamp(newEndX))
                        }

                        // Calculate new position based on drag delta
                        var updatedLine = when (selectedEndpoint) {
                            null -> {
                                // Move whole line - both points move together (no constraints needed)
                                originalLine.copy(
                                    timestamp1 = newStartTimestamp,
                                    price1 = startPrice - priceDelta,
                                    timestamp2 = newEndTimestamp,
                                    price2 = endPrice - priceDelta,
                                    color = originalLine.color,
                                    strokeWidth = originalLine.strokeWidth,
                                    lineStyle = originalLine.lineStyle,
                                    isLocked = originalLine.isLocked
                                )
                            }
                            0 -> {
                                // Moving origin point (p1) - FLUID, no constraints during drag
                                // Constraint only applied at final position if needed
                                val newP1Price = startPrice - priceDelta

                                originalLine.copy(
                                    timestamp1 = newStartTimestamp,
                                    price1 = newP1Price,
                                    color = originalLine.color,
                                    strokeWidth = originalLine.strokeWidth,
                                    lineStyle = originalLine.lineStyle,
                                    isLocked = originalLine.isLocked
                                )
                            }
                            else -> {
                                // Moving direction point (p2) - FLUID, no constraints during drag
                                val newP2Price = endPrice - priceDelta

                                originalLine.copy(
                                    timestamp2 = newEndTimestamp,
                                    price2 = newP2Price,
                                    color = originalLine.color,
                                    strokeWidth = originalLine.strokeWidth,
                                    lineStyle = originalLine.lineStyle,
                                    isLocked = originalLine.isLocked
                                )
                            }
                        }

                        // Update the ray in the list
                        localRays = localRays.toMutableList().also {
                            it[index] = updatedLine
                        }
                        // Don't call updatePersistentLines during drag for better performance
                        // updatePersistentLines() will be called when drag ends
                    }
                }

                change.consume()
                return
            }

            // In simple drawing mode, only update crosshair position
            if (simpleDrawingState != SimpleDrawingState.NONE) {
                if (crosshairPosition != null) {
                    // Initialize drag offset on first drag event for simple drawing mode
                    if (crosshairDragOffset == Offset.Zero) {
                        crosshairDragOffset = Offset(crosshairPosition!!.x - change.position.x, crosshairPosition!!.y - change.position.y)
                    }
                    val rawX = change.position.x + crosshairDragOffset.x
                    updateCrosshairFromRaw(rawX, change.position.y + crosshairDragOffset.y)
                }
                change.consume()
                return
            }

            when (dragArea) {
                -10 -> {
                    routeDrawingDrag(change.position, dragAmount)
                }
                0 -> {
                    // Skip DrawingManager for trend line and arrow tools - use simple drawing mode instead
                    if (drawingManager.activeTool != DrawingTool.NONE && drawingManager.activeTool != DrawingTool.TREND_LINE && drawingManager.activeTool != DrawingTool.ARROW) {
                        // Use snapped position for fluid movement like main crosshair
                        val rawX = change.position.x + crosshairDragOffset.x
                        val snappedX = snapXToCandleCenter(rawX)
                        val snappedPosition = Offset(snappedX, change.position.y + crosshairDragOffset.y)
                        drawingManager.onCursorMove(snappedPosition)
                    } else if (isLongPressing) {
                        val rawX = change.position.x + crosshairDragOffset.x
                        updateCrosshairFromRaw(rawX, change.position.y + crosshairDragOffset.y)
                    }
                }
                1 -> {
                    // Effacer la sélection lors du glissement sur la barre de prix
                    clearChartSelection()
                    
                    isAutoHeight = false
                    val zoom = (1f - (dragAmount.y / plotsHeightPx) * 2f).coerceIn(0.1f, 10f)
                    val range = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.1f)
                    val center = (manualMaxPrice + manualMinPrice) / 2f
                    manualMinPrice = center - (range / zoom) / 2f
                    manualMaxPrice = center + (range / zoom) / 2f
                }
                2 -> {
                    // Effacer la sélection lors du glissement sur la barre de temps
                    clearChartSelection()
                    
                    currentOnZoom((1f - (dragAmount.x / chartWidthPx) * 8f).coerceIn(0.15f, 100f))
                }
                3 -> {
                    // Effacer la sélection lors du glissement sur la grille
                    clearChartSelection()
                    
                    val candleW = if (currentDisplayCount > 0)
                        chartWidthPx / (currentDisplayCount + settings.marginRightBars)
                    else 0f

                    if (candleW > 0) {
                        currentOnPan(dragAmount.x / candleW)
                    }

                    val range = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.1f)
                    val priceDelta = (dragAmount.y / plotsHeightPx) * range
                    manualMinPrice += priceDelta
                    manualMaxPrice += priceDelta
                }
                4 -> {
                    val dy = dragAmount.y
                    val minChartH = with(density) { 60.dp.toPx() }

                    val idxBelow = (draggingSeparatorIdx until bottomIndicators.size).firstOrNull { bottomIndicators[it].isVisible }
                    val idxAbove = (draggingSeparatorIdx - 1 downTo 0).firstOrNull { bottomIndicators[it].isVisible }

                    if (idxBelow != null && idxAbove != null) {
                        val idAbove = bottomIndicators[idxAbove].id
                        val idBelow = bottomIndicators[idxBelow].id
                        val hAbove = indicatorHeights[idAbove] ?: defaultHeightPx
                        val hBelow = indicatorHeights[idBelow] ?: defaultHeightPx
                        val limitedDy = dy.coerceIn(minIndicatorHeightPx - hAbove, hBelow - minIndicatorHeightPx)
                        indicatorHeights[idAbove] = hAbove + limitedDy
                        indicatorHeights[idBelow] = hBelow - limitedDy
                    } else if (idxBelow != null) {
                        val idBelow = bottomIndicators[idxBelow].id
                        val hBelow = indicatorHeights[idBelow] ?: defaultHeightPx
                        val othersH = bottomIndicators.indices.filter { it != idxBelow }.sumOf { idx ->
                            val ind = bottomIndicators[idx]
                            if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx).toDouble() else minimizedHeightPx.toDouble()
                        }.toFloat()
                        val mHLimit = (plotsHeightPx - othersH - minChartH).coerceAtLeast(minIndicatorHeightPx)
                        indicatorHeights[idBelow] = (hBelow - dy).coerceIn(minIndicatorHeightPx, mHLimit)
                    } else if (idxAbove != null) {
                        val idAbove = bottomIndicators[idxAbove].id
                        val hAbove = indicatorHeights[idAbove] ?: defaultHeightPx
                        val othersH = bottomIndicators.indices.filter { it != idxAbove }.sumOf { idx ->
                            val ind = bottomIndicators[idx]
                            if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx).toDouble() else minimizedHeightPx.toDouble()
                        }.toFloat()
                        val mHLimit = (plotsHeightPx - othersH - minChartH).coerceAtLeast(minIndicatorHeightPx)
                        indicatorHeights[idAbove] = (hAbove + dy).coerceIn(minIndicatorHeightPx, mHLimit)
                    }
                }
                5 -> {
                    draggingIndicatorId?.let { id ->
                        indicatorAutoHeight[id] = false
                        val indH = indicatorHeights[id] ?: defaultHeightPx
                        val zoom = (1f - (dragAmount.y / indH) * 2f).coerceIn(0.1f, 10f)
                        val range = indicatorRanges[id] ?: Pair(0f, 100f)
                        val rV = (range.second - range.first).coerceAtLeast(0.001f)
                        val center = (range.second + range.first) / 2f
                        indicatorRanges[id] = Pair(center - (rV / zoom) / 2f, center + (rV / zoom) / 2f)
                    }
                }
                6 -> {
                    val candleW = if (currentDisplayCount > 0)
                        chartWidthPx / (currentDisplayCount + settings.marginRightBars)
                    else 0f

                    if (candleW > 0) {
                        currentOnPan(dragAmount.x / candleW)
                    }

                    draggingIndicatorId?.let { id ->
                        indicatorAutoHeight[id] = false
                        val indH = indicatorHeights[id] ?: defaultHeightPx
                        val range = indicatorRanges[id] ?: Pair(0f, 100f)
                        val rV = (range.second - range.first).coerceAtLeast(0.001f)
                        val vDelta = (dragAmount.y / indH) * rV
                        indicatorRanges[id] = Pair(range.first + vDelta, range.second + vDelta)
                    }
                }
            }
            change.consume()
        }

        fun handleMainCanvasTap(offset: Offset) {
            android.util.Log.d("DrawingDebug", "handleMainCanvasTap called at offset: $offset")
            android.util.Log.d("DrawingDebug", "simpleDrawingState: $simpleDrawingState, activeTool: ${drawingManager.activeTool}")
            expandedIndicatorId = null
            val mainHLocal = currentMainHeight()
            
            // Masquer les mesures lors d'un clic sur la grille
            if (offset.x <= chartWidthPx && offset.y <= mainHLocal) {
                localMeasurements = emptyList()
                // Vider aussi les mesures du drawingManager
                drawingManager.removeMeasures()
                android.util.Log.d("MeasureDebug", "Measures hidden due to grid tap")
            }

            // Priorité 1 : simple drawing mode taps (placement des points)
            if (simpleDrawingState != SimpleDrawingState.NONE) {
                android.util.Log.d("SimpleDrawing", "Drawing mode active: $simpleDrawingState, tool: ${drawingManager.activeTool}")
                val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                val topM = mainHLocal * (settings.marginTopPercent / 100f)
                val botM = mainHLocal * (settings.marginBottomPercent / 100f)
                val effectiveMainH = mainHLocal - topM - botM
                val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                val range = rawRange * 1.1f
                val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                val denormY: (Float) -> Float = { minP + ((effectiveMainH - (it - topM)) / effectiveMainH * range) }

                when (simpleDrawingState) {
                    SimpleDrawingState.PLACING_FIRST_POINT -> {
                        // Place first point at crosshair center
                        crosshairPosition?.let { crosshair ->
                            val index = if (candleW > 0) {
                                round(allCandles.size - 1 - scrollOffset - (chartWidthPx - crosshair.x - candleW / 2f) / candleW).toInt()
                            } else -1
                            val price = denormY(crosshair.y)
                            firstPoint = Pair(index, price)
                            // Store screen coordinates to keep the point fixed during drawing
                            firstPointScreen = crosshair
                            simpleDrawingState = SimpleDrawingState.PLACING_SECOND_POINT
                        }
                    }
                    SimpleDrawingState.PLACING_SECOND_POINT -> {
                        // Place second point and finalize line
                        crosshairPosition?.let { crosshair ->
                            val index = if (candleW > 0) {
                                round(allCandles.size - 1 - scrollOffset - (chartWidthPx - crosshair.x - candleW / 2f) / candleW).toInt()
                            } else -1
                            val price = denormY(crosshair.y)
                            firstPoint?.let { (startIndex, startPrice) ->
                                // Convert screen positions to timestamps for persistence across timeframes
                                android.util.Log.d("DrawingDebug", "Creating line - startIndex: $startIndex, endIndex: $index")
                                android.util.Log.d("DrawingDebug", "First point screen: $firstPointScreen, Crosshair: $crosshair")
                                android.util.Log.d("DrawingDebug", "All candles size: ${allCandles.size}, indices range: 0..${allCandles.indices.lastOrNull()}")
                                
                                val startTimestamp: Long = if (startIndex in allCandles.indices && startIndex >= 0) {
                                    android.util.Log.d("DrawingDebug", "Using existing candle timestamp for start: ${allCandles[startIndex].timestamp}")
                                    allCandles[startIndex].timestamp
                                } else {
                                    // Use xToTimestamp to extrapolate timestamp beyond existing candles
                                    val screenPos = firstPointScreen
                                    if (screenPos != null) {
                                        android.util.Log.d("DrawingDebug", "Extrapolating start timestamp from X: ${screenPos.x}")
                                        val timestamp = xToTimestamp(screenPos.x)
                                        android.util.Log.d("DrawingDebug", "Final start timestamp: $timestamp")
                                        timestamp
                                    } else {
                                        android.util.Log.d("DrawingDebug", "Using System.currentTimeMillis() as fallback for start")
                                        val timestamp = System.currentTimeMillis()
                                        android.util.Log.d("DrawingDebug", "Final start timestamp: $timestamp")
                                        timestamp
                                    }
                                }
                                val endTimestamp: Long = if (index in allCandles.indices && index >= 0) {
                                    android.util.Log.d("DrawingDebug", "Using existing candle timestamp for end: ${allCandles[index].timestamp}")
                                    allCandles[index].timestamp
                                } else {
                                    // Use xToTimestamp to extrapolate timestamp beyond existing candles
                                    android.util.Log.d("DrawingDebug", "Extrapolating end timestamp from X: ${crosshair.x}")
                                    val timestamp: Long = xToTimestamp(crosshair.x)
                                    android.util.Log.d("DrawingDebug", "Final end timestamp: $timestamp")
                                    timestamp
                                }
                                
                                // Check which tool is active and add to appropriate list
                                if (drawingManager.activeTool == DrawingTool.EXTENDED_LINE) {
                                    android.util.Log.d("SimpleDrawing", "Creating extended line, deactivating drawing mode")
                                    // Add to extended trend lines list
                                    localExtendedLines = localExtendedLines + SimpleTrendLine(startTimestamp, startPrice, endTimestamp, price)
                                    updatePersistentLines()
                                    // Auto-select the newly created extended trendline
                                    selectedExtendedTrendLineIndex = localExtendedLines.size - 1
                                    selectedSimpleTrendLineIndex = null
                                    selectedEndpoint = null
                                } else if (drawingManager.activeTool == DrawingTool.RAY) {
                                    android.util.Log.d("SimpleDrawing", "Creating ray, deactivating drawing mode")
                                    // Add to rays list
                                    localRays = localRays + SimpleTrendLine(startTimestamp, startPrice, endTimestamp, price)
                                    updatePersistentLines()
                                    // Auto-select the newly created ray
                                    selectedRayIndex = localRays.size - 1
                                    selectedSimpleTrendLineIndex = null
                                    selectedExtendedTrendLineIndex = null
                                    selectedArrowIndex = null
                                    selectedEndpoint = null
                                } else if (drawingManager.activeTool == DrawingTool.ARROW) {
                                    android.util.Log.d("SimpleDrawing", "Creating arrow, deactivating drawing mode")
                                    // Add to arrows list
                                    localArrows = localArrows + SimpleTrendLine(startTimestamp, startPrice, endTimestamp, price)
                                    updatePersistentLines()
                                    // Auto-select the newly created arrow
                                    selectedArrowIndex = localArrows.size - 1
                                    selectedSimpleTrendLineIndex = null
                                    selectedExtendedTrendLineIndex = null
                                    selectedRayIndex = null
                                    selectedEndpoint = null
                                } else if (drawingManager.activeTool == DrawingTool.MEASURE) {
                                    android.util.Log.d("SimpleDrawing", "Creating measure, deactivating drawing mode")
                                    // Add to measures list using Drawing.Measure
                                    val p1 = com.bthr.backtest.model.AnchorPoint(startIndex, startPrice)
                                    val p2 = com.bthr.backtest.model.AnchorPoint(index, price)
                                    val measureTool = com.bthr.backtest.model.Drawing.Measure(p1, p2)
                                    // Add measure to completedDrawings using the DrawingManager
                                    drawingManager.addDrawing(measureTool)
                                    updatePersistentLines()
                                    // Auto-select the newly created measure
                                    selectedSimpleTrendLineIndex = null
                                    selectedExtendedTrendLineIndex = null
                                    selectedArrowIndex = null
                                    selectedRayIndex = null
                                    selectedEndpoint = null
                                } else {
                                    android.util.Log.d("SimpleDrawing", "Creating simple line, deactivating drawing mode")
                                    android.util.Log.d("DrawingDebug", "Adding line to localSimpleTrendLines: startTimestamp=$startTimestamp, startPrice=$startPrice, endTimestamp=$endTimestamp, price=$price")
                                    android.util.Log.d("DrawingDebug", "Current localSimpleTrendLines size: ${localSimpleTrendLines.size}")
                                    // Add to simple trend lines list (default behavior)
                                    val newLine = SimpleTrendLine(startTimestamp, startPrice, endTimestamp, price)
                                    localSimpleTrendLines = localSimpleTrendLines + newLine
                                    android.util.Log.d("DrawingDebug", "New localSimpleTrendLines size: ${localSimpleTrendLines.size}")
                                    android.util.Log.d("DrawingDebug", "New line added at index: ${localSimpleTrendLines.size - 1}")
                                    updatePersistentLines()
                                    // Auto-select the newly created trendline
                                    selectedSimpleTrendLineIndex = localSimpleTrendLines.size - 1
                                    android.util.Log.d("DrawingDebug", "Selected simple trend line index: $selectedSimpleTrendLineIndex")
                                    selectedExtendedTrendLineIndex = null
                                    selectedArrowIndex = null
                                    selectedEndpoint = null
                                }
                            }
                        }
                        // Reset drawing mode
                        android.util.Log.d("SimpleDrawing", "Calling deactivateSimpleDrawingMode()")
                        deactivateSimpleDrawingMode()
                        android.util.Log.d("SimpleDrawing", "Drawing mode deactivated, new state: $simpleDrawingState")
                    }
                    SimpleDrawingState.NONE -> {}
                }
                return
            }

            // Priorité 2 : Check for all line types with equal priority (no more simple lines priority!)
            if (simpleDrawingState == SimpleDrawingState.NONE && !isLongPressing) {
                val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                val topM = mainHLocal * (settings.marginTopPercent / 100f)
                val botM = mainHLocal * (settings.marginBottomPercent / 100f)
                val effectiveMainH = mainHLocal - topM - botM
                val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                val range = rawRange * 1.1f
                val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                val normY: (Float) -> Float = { topM + effectiveMainH - ((it - minP) / range * effectiveMainH) }
                
                // Check ALL line types with equal priority (reverse order for topmost)
                var hitFound = false
                
                // Debug: Log current state of all line lists
                android.util.Log.d("LineDebug", "Line lists state - Simple: ${localSimpleTrendLines.size}, Extended: ${localExtendedLines.size}, Arrows: ${localArrows.size}, Rays: ${localRays.size}")
                
                // Check simple trend lines - body first, then endpoints
                for (i in localSimpleTrendLines.indices.reversed()) {
                    if (isNearSimpleTrendLine(offset, i, candleW, mainHLocal, topM, botM, normY)) {
                        clearChartSelection()
                        selectedSimpleTrendLineIndex = i
                        hitFound = true
                        return
                    }
                }
                
                // Check endpoints for simple trend lines (only if no body hit)
                if (!hitFound) {
                    for (i in localSimpleTrendLines.indices.reversed()) {
                        if (isNearEndpoint(offset, i, 0, candleW, mainHLocal, topM, botM, normY) ||
                            isNearEndpoint(offset, i, 1, candleW, mainHLocal, topM, botM, normY)) {
                            clearChartSelection()
                            selectedSimpleTrendLineIndex = i
                            hitFound = true
                            return
                        }
                    }
                }
                
                // Check extended trend lines - body first, then endpoints
                if (!hitFound) {
                    for (i in localExtendedLines.indices.reversed()) {
                        if (isNearExtendedTrendLine(offset, i, candleW, mainHLocal, topM, botM, normY)) {
                            clearChartSelection()
                            selectedExtendedTrendLineIndex = i
                            hitFound = true
                            return
                        }
                    }
                }
                
                // Check endpoints for extended trend lines (only if no body hit)
                if (!hitFound) {
                    for (i in localExtendedLines.indices.reversed()) {
                        if (isNearExtendedEndpoint(offset, i, 0, candleW, mainHLocal, topM, botM, normY) ||
                            isNearExtendedEndpoint(offset, i, 1, candleW, mainHLocal, topM, botM, normY)) {
                            clearChartSelection()
                            selectedExtendedTrendLineIndex = i
                            hitFound = true
                            return
                        }
                    }
                }
                
                // Check arrows - body first, then endpoints
                if (!hitFound) {
                    for (i in localArrows.indices.reversed()) {
                        if (isNearArrow(offset, i, candleW, mainHLocal, topM, botM, normY)) {
                            clearChartSelection()
                            selectedArrowIndex = i
                            hitFound = true
                            return
                        }
                    }
                }
                
                // Check endpoints for arrows (only if no body hit)
                if (!hitFound) {
                    for (i in localArrows.indices.reversed()) {
                        if (isNearArrowEndpoint(offset, i, 0, candleW, mainHLocal, topM, botM, normY) ||
                            isNearArrowEndpoint(offset, i, 1, candleW, mainHLocal, topM, botM, normY)) {
                            clearChartSelection()
                            selectedArrowIndex = i
                            hitFound = true
                            return
                        }
                    }
                }

                // Check rays - body first, then endpoints
                if (!hitFound) {
                    for (i in localRays.indices.reversed()) {
                        if (isNearRay(offset, i, candleW, mainHLocal, topM, botM, normY)) {
                            clearChartSelection()
                            selectedRayIndex = i
                            hitFound = true
                            return
                        }
                    }
                }
                
                // Check endpoints for rays (only if no body hit)
                if (!hitFound) {
                    for (i in localRays.indices.reversed()) {
                        if (isNearRayEndpoint(offset, i, 0, candleW, mainHLocal, topM, botM, normY) ||
                            isNearRayEndpoint(offset, i, 1, candleW, mainHLocal, topM, botM, normY)) {
                            clearChartSelection()
                            selectedRayIndex = i
                            hitFound = true
                            return
                        }
                    }
                }

                // Deselect if tapping elsewhere
                if (!hitFound) {
                    clearChartSelection()
                }
            }

            // Priorité 3 : si le viseur est actif en mode normal, un tap dans la grille le ferme
            if (isLongPressing && simpleDrawingState == SimpleDrawingState.NONE) {
                val tIndH2 = bottomIndicators.sumOf {
                    if (it.isVisible) (indicatorHeights[it.id] ?: defaultHeightPx).toDouble() else minimizedHeightPx.toDouble()
                }.toFloat()
                val mainHForTap = plotsHeightPx - tIndH2
                val inGrid = offset.x <= chartWidthPx && offset.y <= mainHForTap
                if (inGrid) {
                    isLongPressing = false
                    crosshairPosition = null
                }
                return
            }

            // Priorité 3 : outils de dessin
            if ((drawingManager.activeTool != DrawingTool.NONE || drawingManager.drawingState is DrawingState.Selected) && offset.x <= chartWidthPx && offset.y <= mainHLocal) {
                val topM = mainHLocal * (settings.marginTopPercent / 100f)
                val botM = mainHLocal * (settings.marginBottomPercent / 100f)
                val mapper = buildMapper(
                    chartWidthPx = chartWidthPx,
                    mainH = mainHLocal,
                    totalCandles = allCandles.size,
                    scrollOffset = scrollOffset,
                    displayCount = displayCount,
                    marginRightBars = settings.marginRightBars.toFloat(),
                    manualMinPrice = manualMinPrice,
                    manualMaxPrice = manualMaxPrice,
                    topMarginPx = topM,
                    bottomMarginPx = botM
                )
                if (drawingManager.onTap(offset, mapper, allCandles)) {
                    onDrawingToolUsed()
                }
            } else {
                val tIndH = bottomIndicators.sumOf {
                    if (it.isVisible) (indicatorHeights[it.id] ?: defaultHeightPx).toDouble() else minimizedHeightPx.toDouble()
                }.toFloat()
                val mH = plotsHeightPx - tIndH

                if (offset.x <= chartWidthPx) {
                    if (offset.y <= mH) {
                        val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                        if (candleW > 0) {
                            val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                            val range = rawRange * 1.1f
                            val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                            val topMarginPx = (mH * (settings.marginTopPercent / 100f))
                            val bottomMarginPx = (mH * (settings.marginBottomPercent / 100f))
                            val effectiveMainH = mH - topMarginPx - bottomMarginPx
                            val denormY: (Float) -> Float = { minP + ((effectiveMainH - (it - topMarginPx)) / effectiveMainH * range) }

                            val idx = round(allCandles.size - 1 - scrollOffset - (chartWidthPx - offset.x - candleW / 2f) / candleW).toInt()

                            var foundInd = false
                            val searchThreshold = range * 0.03f
                            val priceAtY = denormY(offset.y)

                            for (ind in overlayIndicators) {
                                if (!ind.isVisible) continue
                                val found = isOverlayIndicatorHit(ind, idx, priceAtY, searchThreshold)
                                if (found) {
                                    selectedIndicatorId = ind.id
                                    isSelected = false
                                    foundInd = true
                                    break
                                }
                            }

                            if (!foundInd && idx in allCandles.indices) {
                                val candle = allCandles[idx]
                                if (priceAtY in candle.low..candle.high) {
                                    isSelected = true
                                    selectedIndicatorId = null
                                } else {
                                    clearChartSelection()
                                }
                            } else if (!foundInd) {
                                clearChartSelection()
                            }
                        }
                    } else {
                        var curYPos = mH
                        var found = false
                        for (ind in bottomIndicators) {
                            val indH = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                            if (offset.y in curYPos..curYPos + indH) {
                                if (ind.isVisible) {
                                    val rangeInd = indicatorRanges[ind.id] ?: Pair(0f, 100f)
                                    val rV = (rangeInd.second - rangeInd.first).coerceAtLeast(0.001f)
                                    val denormIndY: (Float) -> Float = { rangeInd.first + (curYPos + indH - it) / indH * rV }
                                    val valAtY = denormIndY(offset.y)
                                    val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                                    val idx = if (candleW > 0) round(allCandles.size - 1 - scrollOffset - (chartWidthPx - offset.x - candleW / 2f) / candleW).toInt() else -1
                                    val searchThreshold = rV * 0.05f

                                    if (isBottomIndicatorHit(ind, idx, valAtY, searchThreshold)) {
                                        selectedIndicatorId = ind.id
                                        isSelected = false
                                        found = true
                                        break
                                    }
                                }
                            }
                            curYPos += indH
                        }
                        if (!found) {
                            clearChartSelection()
                        }
                    }
                } else {
                    // Ne pas effacer la sélection sur la barre de prix pour permettre la sélection
                    // clearChartSelection()
                }
            }
        }

        val crosshairLabelBgColor = if (settings.backgroundColor.luminance() > 0.5f) Color(0xFF212121) else Color(0xFFEEEEEE)

        val totalIndH = bottomIndicators.sumOf {
            if (it.isVisible) (indicatorHeights[it.id] ?: defaultHeightPx).toDouble()
            else minimizedHeightPx.toDouble()
        }.toFloat()
        val mainH = plotsHeightPx - totalIndH

        // Initialize crosshair immediately when simple drawing mode is activated
        LaunchedEffect(simpleDrawingState) {
            if (simpleDrawingState == SimpleDrawingState.PLACING_FIRST_POINT && crosshairPosition == null) {
                isLongPressing = true
                val mainHLocal = currentMainHeight()
                crosshairPosition = Offset(chartWidthPx / 2f, mainHLocal / 2f)
                crosshairDragOffset = Offset.Zero
            }
        }

        // Initialize crosshair when drawingManager tool is activated
        LaunchedEffect(drawingManager.activeTool) {
            if (drawingManager.activeTool != DrawingTool.NONE && crosshairPosition == null) {
                val mainHLocal = currentMainHeight()
                crosshairPosition = Offset(chartWidthPx / 2f, mainHLocal / 2f)
                crosshairDragOffset = Offset.Zero
            }
        }

        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(false)
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.size > 1 && !isLongPressing) {
                                val zoom = event.calculateZoom()
                                if (abs(zoom - 1f) > 0.001f) {
                                    currentOnZoom(zoom)
                                    currentOnPan(currentDisplayCount * (1f - 1f / zoom) / 2f)
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(bottomIndicators, isLongPressing, drawingManager.activeTool, drawingManager.drawingState, selectedSimpleTrendLineIndex) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Masquer les mesures lors du glissement sur la grille
                            if (offset.x <= chartWidthPx && offset.y <= mainH) {
                                localMeasurements = emptyList()
                                drawingManager.removeMeasures()
                                android.util.Log.d("MeasureDebug", "Measures hidden due to grid drag start")
                            }
                            
                            // ── Drawing tool drag start ──────────────────────
                            if (drawingManager.activeTool != DrawingTool.NONE && offset.x <= chartWidthPx && offset.y <= mainH) {
                                // Initialize crosshair position and drag offset for drawing tools like main crosshair
                                if (crosshairPosition == null) {
                                    crosshairPosition = Offset(offset.x, offset.y)
                                    crosshairDragOffset = Offset.Zero
                                } else {
                                    crosshairDragOffset = Offset(crosshairPosition!!.x - offset.x, crosshairPosition!!.y - offset.y)
                                }
                                
                                val mapper = buildMapper(
                                    chartWidthPx = chartWidthPx,
                                    mainH = mainH,
                                    totalCandles = allCandles.size,
                                    scrollOffset = scrollOffset,
                                    displayCount = displayCount,
                                    marginRightBars = settings.marginRightBars.toFloat(),
                                    manualMinPrice = manualMinPrice,
                                    manualMaxPrice = manualMaxPrice,
                                    topMarginPx = mainH * (settings.marginTopPercent / 100f),
                                    bottomMarginPx = mainH * (settings.marginBottomPercent / 100f)
                                )
                                drawingManager.onDragStart(offset, mapper)
                                dragArea = -10  // consumed
                                return@detectDragGestures
                            }
                            if (drawingManager.drawingState is DrawingState.Selected && offset.x <= chartWidthPx && offset.y <= mainH) {
                                val mapper = buildMapper(
                                    chartWidthPx = chartWidthPx,
                                    mainH = mainH,
                                    totalCandles = allCandles.size,
                                    scrollOffset = scrollOffset,
                                    displayCount = displayCount,
                                    marginRightBars = settings.marginRightBars.toFloat(),
                                    manualMinPrice = manualMinPrice,
                                    manualMaxPrice = manualMaxPrice,
                                    topMarginPx = mainH * (settings.marginTopPercent / 100f),
                                    bottomMarginPx = mainH * (settings.marginBottomPercent / 100f)
                                )
                                drawingManager.onDragStart(offset, mapper)
                                dragArea = -10
                                return@detectDragGestures
                            }
                            if (isLongPressing) {
                                crosshairPosition?.let {
                                    crosshairDragOffset = Offset(it.x - offset.x, it.y - offset.y)
                                }
                                dragArea = 0
                                return@detectDragGestures
                            }
                            
                            // Handle crosshair drag offset for simple drawing mode
                            if (simpleDrawingState != SimpleDrawingState.NONE && crosshairPosition != null) {
                                crosshairPosition?.let {
                                    crosshairDragOffset = Offset(it.x - offset.x, it.y - offset.y)
                                }
                                dragArea = 0
                                return@detectDragGestures
                            }

                            var found = false
                            val tIndH = bottomIndicators.sumOf {
                                if (it.isVisible) (indicatorHeights[it.id] ?: defaultHeightPx).toDouble()
                                else minimizedHeightPx.toDouble()
                            }.toFloat()
                            var currentY = plotsHeightPx - tIndH

                            val sepRadius = with(density) { 15.dp.toPx() }
                            val anyVisible = bottomIndicators.any { it.isVisible }

                            if (anyVisible) {
                                for (i in 0 until bottomIndicators.size) {
                                    if (abs(offset.y - currentY) < sepRadius) {
                                        dragArea = 4; draggingSeparatorIdx = i; found = true; break
                                    }
                                    val ind = bottomIndicators[i]
                                    currentY += if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                                }
                            }

                            if (!found) {
                                currentY = plotsHeightPx - tIndH
                                for (ind in bottomIndicators) {
                                    val indH = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                                    if (offset.y in currentY..currentY + indH) {
                                        if (ind.isVisible) {
                                            if (offset.x > chartWidthPx) { dragArea = 5; draggingIndicatorId = ind.id }
                                            else { dragArea = 6; draggingIndicatorId = ind.id }
                                            found = true; break
                                        }
                                    }
                                    currentY += indH
                                }
                            }

                            if (!found) {
                                dragArea = when {
                                    offset.x > chartWidthPx -> 1
                                    offset.y > plotsHeightPx -> 2
                                    else -> 3
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            handleMainCanvasDrag(change, dragAmount)
                        },
                        onDragEnd = {
                            // Reset simple trendline drag state
                            if (isDraggingSimpleTrendLine) {
                                isDraggingSimpleTrendLine = false
                                dragStartOffset = null
                                originalTrendLinePosition = null
                                selectedEndpoint = null // Reset endpoint selection after drag
                                // Update persistent lines when drag ends for better performance
                                updatePersistentLines()
                                android.util.Log.d("TrendLineDrag", "Drag ended - persistent lines updated")
                            }
                            // Reset extended trendline drag state
                            if (isDraggingExtendedTrendLine) {
                                isDraggingExtendedTrendLine = false
                                dragStartOffset = null
                                originalExtendedTrendLinePosition = null
                                selectedEndpoint = null // Reset endpoint selection after drag
                                // Update persistent lines when drag ends for better performance
                                updatePersistentLines()
                                android.util.Log.d("TrendLineDrag", "Extended drag ended - persistent lines updated")
                            }
                            // Reset arrow drag state
                            if (isDraggingArrow) {
                                isDraggingArrow = false
                                dragStartOffset = null
                                originalArrowPosition = null
                                selectedEndpoint = null // Reset endpoint selection after drag
                                // Update persistent lines when drag ends for better performance
                                updatePersistentLines()
                                android.util.Log.d("TrendLineDrag", "Arrow drag ended - persistent lines updated")
                            }
                            // Reset ray drag state
                            if (isDraggingRay) {
                                isDraggingRay = false
                                dragStartOffset = null
                                originalRayPosition = null
                                selectedEndpoint = null // Reset endpoint selection after drag
                                // Update persistent lines when drag ends for better performance
                                updatePersistentLines()
                                android.util.Log.d("TrendLineDrag", "Ray drag ended - persistent lines updated")
                            }

                            if (dragArea == -10) {
                                if (drawingManager.onDragEnd()) {
                                    onDrawingToolUsed()
                                }
                            }
                            draggingSeparatorIdx = -1; draggingIndicatorId = null
                        }
                    )
                }
                .pointerInput(visibleCandles, allCandles, scrollOffset, displayCount, manualMinPrice, manualMaxPrice, mainH, chartWidthPx, indicators, drawingManager.activeTool, drawingManager.drawingState) {
                    detectTapGestures(
                        onTap = { offset -> 
                            android.util.Log.d("TapDebug", "Tap detected at offset: $offset")
                            android.util.Log.d("TapDebug", "Chart width: $chartWidthPx, offset.x > chartWidthPx: ${offset.x > chartWidthPx}")
                            handleMainCanvasTap(offset) 
                        },
                        onDoubleTap = { offset ->
                            val tIndH = currentBottomIndicatorsHeight()
                            val mainHeight = plotsHeightPx - tIndH

                            if (offset.x > chartWidthPx) {
                                if (offset.y <= mainHeight) {
                                    // Double-tap on price axis resets to auto-scale
                                    isAutoHeight = true
                                } else {
                                    var curYPos = mainHeight
                                    for (ind in bottomIndicators) {
                                        val indH = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                                        if (offset.y in curYPos..curYPos + indH) { 
                                            if (ind.isVisible) indicatorAutoHeight[ind.id] = true
                                            break 
                                        }
                                        curYPos += indH
                                    }
                                }
                            } else if (offset.y > plotsHeightPx) {
                                // Double-tap on time axis resets horizontal zoom
                                onResetHorizontalZoom()
                            } else if (offset.y <= mainHeight) {
                                val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                                if (candleW > 0) {
                                    val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                                    val range = rawRange * 1.1f
                                    val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                                    val topMarginPx = (mainHeight * (settings.marginTopPercent / 100f))
                                    val bottomMarginPx = (mainHeight * (settings.marginBottomPercent / 100f))
                                    val effectiveMainH = mainHeight - topMarginPx - bottomMarginPx
                                    val denormY: (Float) -> Float = { minP + ((effectiveMainH - (it - topMarginPx)) / effectiveMainH * range) }
                                    
                                    val idx = round(allCandles.size - 1 - scrollOffset - (chartWidthPx - offset.x - candleW / 2f) / candleW).toInt()
                                    val searchThreshold = range * 0.03f
                                    val priceAtY = denormY(offset.y)

                                    var foundInd = false
                                    for (ind in overlayIndicators) {
                                        if (!ind.isVisible) continue
                                        val found = isOverlayIndicatorHit(ind, idx, priceAtY, searchThreshold)
                                        if (found) {
                                            onIndicatorSettingsRequest(ind)
                                            foundInd = true
                                            break
                                        }
                                    }
                                    
                                    if (!foundInd && idx in allCandles.indices) {
                                        val candle = allCandles[idx]
                                        val threshold = (candle.high - candle.low) * 0.2f + (range * 0.02f)
                                        if (priceAtY in (candle.low - threshold)..(candle.high + threshold)) {
                                            currentOnSettingsRequest()
                                        }
                                    }
                                }
                            } else {
                                var curYPos = mainHeight
                                var idxBottom = 0
                                var handled = false
                                while (idxBottom < bottomIndicators.size && !handled) {
                                    val ind = bottomIndicators[idxBottom]
                                    val indH = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                                    if (offset.y in curYPos..curYPos + indH) {
                                        if (ind.isVisible) {
                                            val rangeInd = indicatorRanges[ind.id] ?: Pair(0f, 100f)
                                            val rV = (rangeInd.second - rangeInd.first).coerceAtLeast(0.001f)
                                            val denormIndY: (Float) -> Float = { rangeInd.first + (curYPos + indH - it) / indH * rV }
                                            val valAtY = denormIndY(offset.y)
                                            val idx = candleIndexAtX(offset.x)

                                            if (handleBottomIndicatorSettingsTap(ind, idx, valAtY, rV)) {
                                                handled = true
                                            }
                                        }
                                    }
                                    curYPos += indH
                                    idxBottom++
                                }
                            }
                        },
                        onLongPress = { offset ->
                            // Masquer les mesures lors du long clic sur la grille
                            if (offset.x <= chartWidthPx && offset.y <= mainH) {
                                localMeasurements = emptyList()
                                drawingManager.removeMeasures()
                                android.util.Log.d("MeasureDebug", "Measures hidden due to grid long press")
                            }
                            
                            // Désélectionner la ligne de tendance sur long click
                            if (selectedSimpleTrendLineIndex != null) {
                                selectedSimpleTrendLineIndex = null
                                selectedEndpoint = null
                                android.util.Log.d("TrendLineDeselect", "Deselected simple trend line on long press")
                            }
                            if (selectedExtendedTrendLineIndex != null) {
                                selectedExtendedTrendLineIndex = null
                                selectedEndpoint = null
                                android.util.Log.d("TrendLineDeselect", "Deselected extended trend line on long press")
                            }
                            if (selectedRayIndex != null) {
                                selectedRayIndex = null
                                selectedEndpoint = null
                                android.util.Log.d("TrendLineDeselect", "Deselected ray on long press")
                            }

                            if (offset.x <= chartWidthPx) {
                                isLongPressing = true
                                crosshairPosition = Offset(
                                    snapXToCandleCenter(offset.x),
                                    offset.y.coerceIn(0f, h)
                                )
                                crosshairDragOffset = Offset.Zero
                            }
                        }
                    )
                }
        ) {
            ChartDrawer.drawBackground(this, settings, w, h)
            
            val topMarginPx = (mainH * (settings.marginTopPercent / 100f))
            val bottomMarginPx = (mainH * (settings.marginBottomPercent / 100f))
            val effectiveMainH = mainH - topMarginPx - bottomMarginPx

            val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
            val range = rawRange * 1.1f
            val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
            
            val normY: (Float) -> Float = { topMarginPx + effectiveMainH - ((it - minP) / range * effectiveMainH) }
            val denormY: (Float) -> Float = { minP + ((effectiveMainH - (it - topMarginPx)) / effectiveMainH * range) }

            val candleW = if (displayCount > 0) chartWidthPx / (displayCount + settings.marginRightBars) else 0f
            val bodyW = candleW * 0.7f

            val visibleMaxP = denormY(0f)
            val visibleMinP = denormY(mainH)
            
            ChartDrawer.drawGridAndLabels(this, textMeasurer, chartWidthPx, priceWidthPx, mainH, visibleMaxP - visibleMinP, visibleMinP, normY, settings, textStyle)
            
            ChartDrawer.drawTimeLabels(this, allCandles, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, plotsHeightPx, displayCount, timeFormatter, settings, textStyle, textMeasurer, labelBgColor, timeframe, secondaryTimeFormatter, showDualTimezone)

            ChartDrawer.drawWatermark(this, textMeasurer, chartWidthPx, mainH, symbol, timeframe, settings)

            clipRect(right = chartWidthPx, bottom = mainH) {
                sessionsIndicator?.let { ChartDrawer.drawSessions(this, allCandles, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, mainH, it, normY, textMeasurer) }
                ChartDrawer.drawOverlayIndicators(this, overlayIndicators, allCandles, smaValues, emaValues, hmaValues, vwapValues, bbValues, atrBandsValues, stValues, alligatorValues, ichimokuValues, ribbonValues, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY)

                ChartDrawer.drawCandles(this, visibleCandles, allCandles, startIdx, allCandles.size, scrollOffset, candleW, bodyW, chartWidthPx, mainH, volumeIndicator, volumeMaValues, smoothedMaValues, settings, normY)
                
                if (isSelected) {
                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> (allCandles[idx].high + allCandles[idx].low) / 2f }
                }
                
                selectedIndicatorId?.let { selId ->
                    overlayIndicators.find { it.id == selId }?.let { ind ->
                        when (ind) {
                            is Indicator.SMA -> smaValues[ind.id]?.let { vals ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> vals.getOrNull(idx) }
                            }
                            is Indicator.EMA -> emaValues[ind.id]?.let { vals ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> vals.getOrNull(idx) }
                            }
                            is Indicator.HMA -> hmaValues[ind.id]?.let { vals ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> vals.getOrNull(idx) }
                            }
                            is Indicator.VWAP -> vwapValues[ind.id]?.let { res ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.vwap.getOrNull(idx) }
                                if (ind.showBands) {
                                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.upper1.getOrNull(idx) }
                                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.lower1.getOrNull(idx) }
                                }
                            }
                            is Indicator.BollingerBands -> bbValues[ind.id]?.let { bb ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> bb.middle.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> bb.upper.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> bb.lower.getOrNull(idx) }
                            }
                            is Indicator.ATRBands -> atrBandsValues[ind.id]?.let { ab ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> ab.upper.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> ab.lower.getOrNull(idx) }
                                if (ind.showTPBands) {
                                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> ab.tpUpper.getOrNull(idx) }
                                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> ab.tpLower.getOrNull(idx) }
                                }
                            }
                            is Indicator.Alligator -> alligatorValues[ind.id]?.let { res ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.jaw.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.teeth.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.lips.getOrNull(idx) }
                            }
                            is Indicator.Ichimoku -> ichimokuValues[ind.id]?.let { res ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.tenkanSen.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.kijunSen.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.senkouSpanA.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.senkouSpanB.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.chikouSpan.getOrNull(idx) }
                            }
                            is Indicator.Supertrend -> stValues[ind.id]?.let { st ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> st.values.getOrNull(idx) }
                            }
                            is Indicator.Ribbon -> ribbonValues[ind.id]?.let { res ->
                                res.mas.forEach { ma ->
                                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> ma.getOrNull(idx) }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }

            ChartDrawer.drawBottomIndicators(this, bottomIndicators, indicatorHeights, defaultHeightPx, rsiValues, rsiMaValues, macdValues, stochValues, atrValues, indicatorRanges, visibleCandles, allCandles.size, startIdx, endIdx, scrollOffset, candleW, bodyW, chartWidthPx, priceWidthPx, mainH, settings, textMeasurer, textStyle, crosshairTextStyle, draggingSeparatorIdx, dragArea, minimizedHeightPx)

            ChartDrawer.drawOverlayIndicatorLabels(this, overlayIndicators, smaValues, emaValues, hmaValues, vwapValues, bbValues, atrBandsValues, stValues, alligatorValues, ichimokuValues, ribbonValues, chartWidthPx, priceWidthPx, mainH, textMeasurer, crosshairTextStyle, normY)

            if (allCandles.isNotEmpty()) {
                ChartDrawer.drawLastPriceLabel(this, allCandles.last(), chartWidthPx, mainH, priceWidthPx, settings, crosshairTextStyle, textMeasurer, normY)
            }

            // Draw simple trend lines
            clipRect(right = chartWidthPx, bottom = mainH) {
                val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                val topM = mainH * (settings.marginTopPercent / 100f)
                val botM = mainH * (settings.marginBottomPercent / 100f)
                val effectiveMainH = mainH - topM - botM
                val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                val range = rawRange * 1.1f
                val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                val normY: (Float) -> Float = { topM + effectiveMainH - ((it - minP) / range * effectiveMainH) }

                // Draw completed simple trend lines
                localSimpleTrendLines.forEachIndexed { index, line ->
                    val startTimestamp = line.timestamp1
                    val startPrice = line.price1
                    val endTimestamp = line.timestamp2
                    val endPrice = line.price2
                    val startX = timestampToX(startTimestamp)
                    val startY = normY(startPrice)
                    val endX = timestampToX(endTimestamp)
                    val endY = normY(endPrice)

                    val isSelected = selectedSimpleTrendLineIndex == index
                    val isEndpointSelected = isSelected && selectedEndpoint != null
                    
                    // Determine line style based on lineStyle property
                    val pathEffect = when (line.lineStyle) {
                        1 -> PathEffect.dashPathEffect(floatArrayOf(4f * line.strokeWidth, 4f * line.strokeWidth))
                        2 -> PathEffect.dashPathEffect(floatArrayOf(0f, 4f * line.strokeWidth))
                        else -> null
                    }
                    
                    // Draw line with custom style from SimpleTrendLine
                    // Keep original color when selected
                    drawLine(
                        color = line.color,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isSelected) line.strokeWidth + 2f else line.strokeWidth,
                        pathEffect = pathEffect,
                        cap = StrokeCap.Round
                    )

                    // Draw bubbles at endpoints only when selected
                    if (isSelected) {
                        val bubbleSize = 25f
                        val bubbleBorderWidth = 4f
                        val bubbleBorderColor = Color(0xFF2196F3) // Blue border
                        val isDarkTheme = colorScheme.background.luminance() < 0.5f
                        val bubbleFillColor = if (isDarkTheme) Color.Black else Color.White
                        
                        // Draw bubble fill
                        drawCircle(bubbleFillColor, bubbleSize, Offset(startX, startY))
                        drawCircle(bubbleFillColor, bubbleSize, Offset(endX, endY))
                        
                        // Draw bubble border
                        drawCircle(color = bubbleBorderColor, radius = bubbleSize, center = Offset(startX, startY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = bubbleBorderWidth))
                        drawCircle(color = bubbleBorderColor, radius = bubbleSize, center = Offset(endX, endY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = bubbleBorderWidth))
                    }
                    
                }

                // Draw completed extended trend lines
                localExtendedLines.forEachIndexed { index, line ->
                    val startTimestamp = line.timestamp1
                    val startPrice = line.price1
                    val endTimestamp = line.timestamp2
                    val endPrice = line.price2
                    val startX = timestampToX(startTimestamp)
                    val startY = normY(startPrice)
                    val endX = timestampToX(endTimestamp)
                    val endY = normY(endPrice)

                    val isSelected = selectedExtendedTrendLineIndex == index
                    val isEndpointSelected = isSelected && selectedEndpoint != null
                    
                    // Determine line style based on lineStyle property
                    val pathEffect = when (line.lineStyle) {
                        1 -> PathEffect.dashPathEffect(floatArrayOf(4f * line.strokeWidth, 4f * line.strokeWidth))
                        2 -> PathEffect.dashPathEffect(floatArrayOf(0f, 4f * line.strokeWidth))
                        else -> null
                    }
                    
                    // Calculate extended line endpoints (extend infinitely in both directions)
                    val lineLength = sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY))
                    val extendedStartX: Float
                    val extendedStartY: Float
                    val extendedEndX: Float
                    val extendedEndY: Float
                        
                        // Extend line to cover entire visible area and beyond
                        // Calculate intersection with chart boundaries
                        
                        // For infinite extension, we need to find where the line intersects the chart boundaries
                        val boundaries = listOf(
                            0f, // left
                            chartWidthPx, // right
                            0f, // top
                            mainH // bottom
                        )
                        
                        // Calculate intersection points with all four boundaries
                        val intersections = mutableListOf<Offset>()
                        
                        // Left boundary (x = 0)
                        if (abs(dirX) > 0.001f) {
                            val t = -startX / dirX
                            val y = startY + dirY * t
                            if (y >= 0f && y <= mainH) {
                                intersections.add(Offset(0f, y))
                            }
                        }
                        
                        // Right boundary (x = chartWidthPx)
                        if (abs(dirX) > 0.001f) {
                            val t = (chartWidthPx - startX) / dirX
                            val y = startY + dirY * t
                            if (y >= 0f && y <= mainH) {
                                intersections.add(Offset(chartWidthPx, y))
                            }
                        }
                        
                        // Top boundary (y = 0)
                        if (abs(dirY) > 0.001f) {
                            val t = -startY / dirY
                            val x = startX + dirX * t
                            if (x >= 0f && x <= chartWidthPx) {
                                intersections.add(Offset(x, 0f))
                            }
                        }
                        
                        // Bottom boundary (y = mainH)
                        if (abs(dirY) > 0.001f) {
                            val t = (mainH - startY) / dirY
                            val x = startX + dirX * t
                            if (x >= 0f && x <= chartWidthPx) {
                                intersections.add(Offset(x, mainH))
                            }
                        }
                        
                        // If we found at least 2 intersections, use the furthest ones
                        android.util.Log.d("ExtendedLine", "Intersections found: ${intersections.size}")
                        if (intersections.size >= 2) {
                            // Sort intersections by distance from start point
                            intersections.sortBy { sqrt((it.x - startX) * (it.x - startX) + (it.y - startY) * (it.y - startY)) }
                            extendedStartX = intersections.first().x
                            extendedStartY = intersections.first().y
                            extendedEndX = intersections.last().x
                            extendedEndY = intersections.last().y
                            android.util.Log.d("ExtendedLine", "Using intersections - Start: ($extendedStartX, $extendedStartY), End: ($extendedEndX, $extendedEndY)")
                        } else {
                            // Fallback: extend far beyond chart bounds
                            val hugeExtension = maxOf(chartWidthPx, mainH) * 10f
                            extendedStartX = startX - dirX * hugeExtension
                            extendedStartY = startY - dirY * hugeExtension
                            extendedEndX = endX + dirX * hugeExtension
                            extendedEndY = endY + dirY * hugeExtension
                            android.util.Log.d("ExtendedLine", "Using fallback - Start: ($extendedStartX, $extendedStartY), End: ($extendedEndX, $extendedEndY)")
                        }
                    } else {
                        extendedStartX = startX
                        extendedStartY = startY
                        extendedEndX = endX
                        extendedEndY = endY
                    }
                    
                    if (lineLength > 0f) {
                        // Normalize direction vector
                        val dirX = (endX - startX) / lineLength
                        val dirY = (endY - startY) / lineLength
                        
                        // Extend line to cover entire visible area and beyond
                        // Calculate intersection with chart boundaries
                        
                        // For infinite extension, we need to find where line intersects with chart boundaries
                        val boundaries = listOf(
                            0f, // left
                            chartWidthPx, // right
                            0f, // top
                            mainH // bottom
                        )
                        
                        // Calculate intersection points with all four boundaries
                        val intersections = mutableListOf<Offset>()
                        
                        // Left boundary (x = 0)
                        if (abs(dirX) > 0.001f) {
                            val t = -startX / dirX
                            val y = startY + dirY * t
                            if (y >= 0f && y <= mainH) {
                                intersections.add(Offset(0f, y))
                            }
                        }
                        
                        // Right boundary (x = chartWidthPx)
                        if (abs(dirX) > 0.001f) {
                            val t = (chartWidthPx - startX) / dirX
                            val y = startY + dirY * t
                            if (y >= 0f && y <= mainH) {
                                intersections.add(Offset(chartWidthPx, y))
                            }
                        }
                        
                        // Top boundary (y = 0)
                        if (abs(dirY) > 0.001f) {
                            val t = -startY / dirY
                            val x = startX + dirX * t
                            if (x >= 0f && x <= chartWidthPx) {
                                intersections.add(Offset(x, 0f))
                            }
                        }
                        
                        // Bottom boundary (y = mainH)
                        if (abs(dirY) > 0.001f) {
                            val t = (mainH - startY) / dirY
                            val x = startX + dirX * t
                            if (x >= 0f && x <= chartWidthPx) {
                                intersections.add(Offset(x, mainH))
                            }
                        }
                        
                        // If we found at least 2 intersections, use the furthest ones
                        if (intersections.size >= 2) {
                            // Sort intersections by distance from start point
                            intersections.sortBy { sqrt((it.x - startX) * (it.x - startX) + (it.y - startY) * (it.y - startY)) }
                            extendedStartX = intersections.first().x
                            extendedStartY = intersections.first().y
                            extendedEndX = intersections.last().x
                            extendedEndY = intersections.last().y
                        } else {
                            // Fallback: extend far beyond chart bounds
                            val hugeExtension = maxOf(chartWidthPx, mainH) * 10f
                            extendedStartX = startX - dirX * hugeExtension
                            extendedStartY = startY - dirY * hugeExtension
                            extendedEndX = endX + dirX * hugeExtension
                            extendedEndY = endY + dirY * hugeExtension
                        }
                    } else {
                        extendedStartX = startX
                        extendedStartY = startY
                        extendedEndX = endX
                        extendedEndY = endY
                    }
                    
                    // Draw extended line with custom style from SimpleTrendLine
                    drawLine(
                        color = line.color,
                        start = Offset(extendedStartX, extendedStartY),
                        end = Offset(extendedEndX, extendedEndY),
                        strokeWidth = if (isSelected) line.strokeWidth + 2f else line.strokeWidth,
                        pathEffect = pathEffect,
                        cap = StrokeCap.Round
                    )

                    // Draw bubbles at original endpoints only when selected
                    if (isSelected) {
                        val bubbleSize = 25f
                        val bubbleBorderWidth = 4f
                        val bubbleBorderColor = Color(0xFF2196F3) // Blue border
                        val isDarkTheme = colorScheme.background.luminance() < 0.5f
                        val bubbleFillColor = if (isDarkTheme) Color.Black else Color.White
                        
                        // Draw bubble fill at original endpoints
                        drawCircle(bubbleFillColor, bubbleSize, Offset(startX, startY))
                        drawCircle(bubbleFillColor, bubbleSize, Offset(endX, endY))
                        
                        // Draw bubble border at original endpoints
                        drawCircle(color = bubbleBorderColor, radius = bubbleSize, center = Offset(startX, startY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = bubbleBorderWidth))
                        drawCircle(color = bubbleBorderColor, radius = bubbleSize, center = Offset(endX, endY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = bubbleBorderWidth))
                    }
                    
                }

                // Draw completed arrows
                localArrows.forEachIndexed { index, line ->
                    val startTimestamp = line.timestamp1
                    val startPrice = line.price1
                    val endTimestamp = line.timestamp2
                    val endPrice = line.price2
                    val startX = timestampToX(startTimestamp)
                    val startY = normY(startPrice)
                    val endX = timestampToX(endTimestamp)
                    val endY = normY(endPrice)
                    
                    // Debug log for arrow drawing coordinates
                    android.util.Log.d("ArrowDraw", "Drawing arrow $index at: start($startX, $startY), end($endX, $endY)")

                    val isSelected = selectedArrowIndex == index
                    val isEndpointSelected = isSelected && selectedEndpoint != null
                    
                    // Determine line style based on lineStyle property (comme les lignes de tendance normales)
                    val pathEffect = when (line.lineStyle) {
                        1 -> PathEffect.dashPathEffect(floatArrayOf(4f * line.strokeWidth, 4f * line.strokeWidth))
                        2 -> PathEffect.dashPathEffect(floatArrayOf(0f, 4f * line.strokeWidth))
                        else -> null
                    }
                    
                    // Draw arrowhead at end point - adapté au thickness
                    val arrowSize = (line.strokeWidth * 6f).coerceAtLeast(8f)   // Taille proportionnelle au thickness
                    val angle = atan2(endY - startY, endX - startX)
                    val arrowAngle1 = angle + PI / 4  // Angle standard (45°)
                    val arrowAngle2 = angle - PI / 4
                    
                    val arrowPoint1 = Offset(
                        endX - arrowSize * cos(arrowAngle1).toFloat(),
                        endY - arrowSize * sin(arrowAngle1).toFloat()
                    )
                    val arrowPoint2 = Offset(
                        endX - arrowSize * cos(arrowAngle2).toFloat(),
                        endY - arrowSize * sin(arrowAngle2).toFloat()
                    )
                    
                    // Draw arrow line with style - complete line to the end point
                    drawLine(
                        color = line.color,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isSelected) line.strokeWidth + 2f else line.strokeWidth,
                        cap = StrokeCap.Round,
                        pathEffect = pathEffect
                    )
                    
                    // Draw only the two sides of the triangle arrowhead (no base line)
                    drawLine(
                        color = line.color,
                        start = Offset(endX, endY),
                        end = arrowPoint1,
                        strokeWidth = line.strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = line.color,
                        start = Offset(endX, endY),
                        end = arrowPoint2,
                        strokeWidth = line.strokeWidth,
                        cap = StrokeCap.Round
                    )

                    // Draw bubbles at endpoints only when selected
                    if (isSelected) {
                        val bubbleSize = 25f
                        val bubbleBorderWidth = 4f
                        val bubbleBorderColor = Color(0xFF2196F3) // Blue border
                        val isDarkTheme = colorScheme.background.luminance() < 0.5f
                        val bubbleFillColor = if (isDarkTheme) Color.Black else Color.White
                        
                        // Draw bubble fill
                        drawCircle(bubbleFillColor, bubbleSize, Offset(startX, startY))
                        drawCircle(bubbleFillColor, bubbleSize, Offset(endX, endY))
                        
                        // Draw bubble border
                        drawCircle(color = bubbleBorderColor, radius = bubbleSize, center = Offset(startX, startY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = bubbleBorderWidth))
                        drawCircle(color = bubbleBorderColor, radius = bubbleSize, center = Offset(endX, endY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = bubbleBorderWidth))
                    }
                }

                // Draw completed rays
                localRays.forEachIndexed { index, line ->
                    val startTimestamp = line.timestamp1
                    val startPrice = line.price1
                    val endTimestamp = line.timestamp2
                    val endPrice = line.price2
                    val startX = timestampToX(startTimestamp)
                    val startY = normY(startPrice)
                    val endX = timestampToX(endTimestamp)
                    val endY = normY(endPrice)

                    val isSelected = selectedRayIndex == index

                    // Determine line style based on lineStyle property
                    val pathEffect = when (line.lineStyle) {
                        1 -> PathEffect.dashPathEffect(floatArrayOf(4f * line.strokeWidth, 4f * line.strokeWidth))
                        2 -> PathEffect.dashPathEffect(floatArrayOf(0f, 4f * line.strokeWidth))
                        else -> null
                    }

                    // Calculate ray line endpoints (extends from origin through direction point)
                    val lineLength = sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY))
                    val rayEndX: Float
                    val rayEndY: Float

                    if (lineLength > 0f) {
                        // Normalize direction vector
                        val dirX = (endX - startX) / lineLength
                        val dirY = (endY - startY) / lineLength

                        // Find intersection with chart boundaries in the forward direction only
                        val intersections = mutableListOf<Offset>()

                        // Right boundary (x = chartWidthPx)
                        if (abs(dirX) > 0.001f) {
                            val t = (chartWidthPx - startX) / dirX
                            if (t >= 0) {
                                val y = startY + dirY * t
                                if (y >= 0f && y <= mainH) {
                                    intersections.add(Offset(chartWidthPx, y))
                                }
                            }
                        }

                        // Left boundary (x = 0) - only if direction is leftward
                        if (abs(dirX) > 0.001f && dirX < 0) {
                            val t = -startX / dirX
                            if (t >= 0) {
                                val y = startY + dirY * t
                                if (y >= 0f && y <= mainH) {
                                    intersections.add(Offset(0f, y))
                                }
                            }
                        }

                        // Bottom boundary (y = mainH)
                        if (abs(dirY) > 0.001f) {
                            val t = (mainH - startY) / dirY
                            if (t >= 0) {
                                val x = startX + dirX * t
                                if (x >= 0f && x <= chartWidthPx) {
                                    intersections.add(Offset(x, mainH))
                                }
                            }
                        }

                        // Top boundary (y = 0)
                        if (abs(dirY) > 0.001f) {
                            val t = -startY / dirY
                            if (t >= 0) {
                                val x = startX + dirX * t
                                if (x >= 0f && x <= chartWidthPx) {
                                    intersections.add(Offset(x, 0f))
                                }
                            }
                        }

                        // Use the furthest intersection in the ray direction
                        if (intersections.isNotEmpty()) {
                            val furthest = intersections.maxByOrNull {
                                val dx = it.x - startX
                                val dy = it.y - startY
                                dx * dx + dy * dy
                            }
                            rayEndX = furthest?.x ?: (startX + dirX * maxOf(chartWidthPx, mainH))
                            rayEndY = furthest?.y ?: (startY + dirY * maxOf(chartWidthPx, mainH))
                        } else {
                            // Fallback: extend far in direction
                            val hugeExtension = maxOf(chartWidthPx, mainH) * 10f
                            rayEndX = startX + dirX * hugeExtension
                            rayEndY = startY + dirY * hugeExtension
                        }
                    } else {
                        rayEndX = endX
                        rayEndY = endY
                    }

                    // Draw ray line from origin through direction point
                    drawLine(
                        color = line.color,
                        start = Offset(startX, startY),
                        end = Offset(rayEndX, rayEndY),
                        strokeWidth = if (isSelected) line.strokeWidth + 2f else line.strokeWidth,
                        pathEffect = pathEffect,
                        cap = StrokeCap.Round
                    )

                    // Draw bubbles at endpoints only when selected
                    if (isSelected) {
                        val bubbleSize = 25f
                        val bubbleBorderWidth = 4f
                        val bubbleBorderColor = Color(0xFF2196F3) // Blue border
                        val isDarkTheme = colorScheme.background.luminance() < 0.5f
                        val bubbleFillColor = if (isDarkTheme) Color.Black else Color.White

                        // Draw bubble fill at origin point (p1)
                        drawCircle(bubbleFillColor, bubbleSize, Offset(startX, startY))
                        // Draw bubble fill at direction point (p2)
                        drawCircle(bubbleFillColor, bubbleSize, Offset(endX, endY))

                        // Draw bubble border at origin point
                        drawCircle(color = bubbleBorderColor, radius = bubbleSize, center = Offset(startX, startY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = bubbleBorderWidth))
                        // Draw bubble border at direction point
                        drawCircle(color = bubbleBorderColor, radius = bubbleSize, center = Offset(endX, endY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = bubbleBorderWidth))

                    }
                }

                // Draw preview line when placing second point
                if (simpleDrawingState == SimpleDrawingState.PLACING_SECOND_POINT && firstPoint != null && firstPointScreen != null && crosshairPosition != null) {
                    val (startIndex, startPrice) = firstPoint!!
                    val startX = if (candleW > 0) chartWidthPx - (candleW / 2f) - ((allCandles.size - 1 - startIndex - scrollOffset) * candleW) else 0f
                    val startY = normY(startPrice)

                    val endX = crosshairPosition!!.x
                    val endY = crosshairPosition!!.y

                    if (drawingManager.activeTool == DrawingTool.ARROW) {
                        // Draw preview arrow
                        drawLine(
                            color = Color.Red,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                        )

                        // Draw preview arrowhead - same style as final arrow
                        val arrowSize = 20f   // Taille beaucoup plus grande pour être très visible
                        val angle = atan2(endY - startY, endX - startX)
                        val arrowAngle1 = angle + PI / 4  // Angle standard (45°)
                        val arrowAngle2 = angle - PI / 4

                        val arrowPoint1 = Offset(
                            endX - arrowSize * cos(arrowAngle1).toFloat(),
                            endY - arrowSize * sin(arrowAngle1).toFloat()
                        )
                        val arrowPoint2 = Offset(
                            endX - arrowSize * cos(arrowAngle2).toFloat(),
                            endY - arrowSize * sin(arrowAngle2).toFloat()
                        )

                        // Draw only the two sides of the triangle arrowhead (no base line)
                        drawLine(
                            color = Color.Red,
                            start = Offset(endX, endY),
                            end = arrowPoint1,
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.Red,
                            start = Offset(endX, endY),
                            end = arrowPoint2,
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    } else if (drawingManager.activeTool == DrawingTool.RAY) {
                        // Draw preview ray - line extending from origin through cursor
                        val lineLength = sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY))
                        if (lineLength > 0f) {
                            val dirX = (endX - startX) / lineLength
                            val dirY = (endY - startY) / lineLength

                            // Extend ray to chart boundaries
                            val intersections = mutableListOf<Offset>()

                            if (abs(dirX) > 0.001f) {
                                val t = (chartWidthPx - startX) / dirX
                                if (t >= 0) {
                                    val y = startY + dirY * t
                                    if (y >= 0f && y <= mainH) intersections.add(Offset(chartWidthPx, y))
                                }
                                if (dirX < 0) {
                                    val t2 = -startX / dirX
                                    if (t2 >= 0) {
                                        val y2 = startY + dirY * t2
                                        if (y2 >= 0f && y2 <= mainH) intersections.add(Offset(0f, y2))
                                    }
                                }
                            }

                            if (abs(dirY) > 0.001f) {
                                val t = (mainH - startY) / dirY
                                if (t >= 0) {
                                    val x = startX + dirX * t
                                    if (x >= 0f && x <= chartWidthPx) intersections.add(Offset(x, mainH))
                                }
                                val t2 = -startY / dirY
                                if (t2 >= 0) {
                                    val x2 = startX + dirX * t2
                                    if (x2 >= 0f && x2 <= chartWidthPx) intersections.add(Offset(x2, 0f))
                                }
                            }

                            val rayEnd = if (intersections.isNotEmpty()) {
                                intersections.maxByOrNull {
                                    val dx = it.x - startX
                                    val dy = it.y - startY
                                    dx * dx + dy * dy
                                } ?: Offset(startX + dirX * maxOf(chartWidthPx, mainH), startY + dirY * maxOf(chartWidthPx, mainH))
                            } else {
                                val ext = maxOf(chartWidthPx, mainH) * 10f
                                Offset(startX + dirX * ext, startY + dirY * ext)
                            }

                            // Draw the ray line
                            drawLine(
                                color = Color.Red,
                                start = Offset(startX, startY),
                                end = rayEnd,
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                            )
                        } else {
                            // Fallback: just draw to cursor
                            drawLine(
                                color = Color.Red,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                            )
                        }
                    } else {
                        // Draw preview line for other tools
                        drawLine(
                            color = Color.Red,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                        )
                    }

                    // Draw bubble at first point
                    drawCircle(Color.Red, 6f, Offset(startX, startY))
                }
            }

            // Draw trend line if in drawing mode
            // (Now handled by DrawingManager via ChartDrawer.drawAllDrawings below)

              // ══ Draw all completed drawings + preview ══════════════════════
              clipRect(right = chartWidthPx, bottom = mainH) {
                  val mapper = buildMapper(
                      chartWidthPx = chartWidthPx,
                      mainH = mainH,
                      totalCandles = allCandles.size,
                      scrollOffset = scrollOffset,
                      displayCount = displayCount,
                      marginRightBars = settings.marginRightBars.toFloat(),
                      manualMinPrice = manualMinPrice,
                      manualMaxPrice = manualMaxPrice,
                      topMarginPx = mainH * (settings.marginTopPercent / 100f),
                      bottomMarginPx = mainH * (settings.marginBottomPercent / 100f)
                  )
                  val currentTimeframe = com.bthr.backtest.model.Timeframe.fromDisplayName(timeframe)
                  ChartDrawer.drawAllDrawings(
                      this, drawingManager.completedDrawings + localMeasurements, mapper,
                      drawingManager.selectedDrawingId,
                      drawingManager.drawingState,
                      textMeasurer, allCandles, density, activeDrawingTool, currentTimeframe
                  )
              }

              crosshairPosition?.let { p ->
                 ChartDrawer.drawCrosshair(
                     this, p, chartWidthPx, h, mainH, priceWidthPx, timeHeightPx,
                     settings, crosshairTextStyle, textMeasurer, allCandles,
                     scrollOffset, candleW, crosshairTimeFormatter,
                     denormalizeY = denormY, bottomIndicators, indicatorHeights,
                     indicatorRanges, defaultHeightPx, crosshairLabelBgColor,
                     timeframe = timeframe,
                     displayCount = displayCount,
                     secondaryCrosshairTimeFormatter = secondaryCrosshairTimeFormatter,
                     showDualTimezone = showDualTimezone,
                     isDrawingMode = drawingManager.activeTool != DrawingTool.NONE || simpleDrawingState != SimpleDrawingState.NONE
                 )
             }

            ChartDrawer.drawBorders(this, chartWidthPx, priceWidthPx, h, settings)
        }

        val candleW_hud = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
        val hoverIdx = crosshairPosition?.let { p ->
            if (candleW_hud > 0) (allCandles.size - 1 - scrollOffset - (chartWidthPx - p.x - candleW_hud / 2f) / candleW_hud).roundToInt().coerceIn(allCandles.indices) else null
        }
        
        if (crosshairPosition != null) {
            val displayedCandle = hoverIdx?.let { allCandles[it] } ?: allCandles.lastOrNull()

            displayedCandle?.let { candle ->
                val hudTextColor = colorScheme.onSecondaryContainer
                val isUp = candle.close >= candle.open
                val trendColor = if (isUp) Color(0xFF26A69A) else Color(0xFFEF5350)
                
                Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp)) {
                    @Suppress("DEPRECATION")
                    Text("O", color = hudTextColor.copy(0.6f), fontSize = 11.sp)
                    @Suppress("DEPRECATION")
                    Text(ChartUtils.formatPrice(candle.open), color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    @Suppress("DEPRECATION")
                    Text("H", color = hudTextColor.copy(0.6f), fontSize = 11.sp)
                    @Suppress("DEPRECATION")
                    Text(ChartUtils.formatPrice(candle.high), color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    @Suppress("DEPRECATION")
                    Text("L", color = hudTextColor.copy(0.6f), fontSize = 11.sp)
                    @Suppress("DEPRECATION")
                    Text(ChartUtils.formatPrice(candle.low), color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    @Suppress("DEPRECATION")
                    Text("C", color = hudTextColor.copy(0.6f), fontSize = 11.sp)
                    @Suppress("DEPRECATION")
                    Text(ChartUtils.formatPrice(candle.close), color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }

        val surfCol = colorScheme.surface
        val txtCol = colorScheme.onSecondaryContainer

        val allOverlay = (if (volumeIndicator != null) listOf(volumeIndicator) else emptyList()) + overlayIndicators

        if (allOverlay.isNotEmpty()) {
            val overlayTopPadding = if (crosshairPosition != null) 18.dp else 2.dp
            Column(modifier = Modifier.padding(start = 8.dp, top = overlayTopPadding)) {
                if (showIndicatorLabels) {
                    if (!overlayCollapsed) {
                        allOverlay.forEach { ind ->
                            val values = if (crosshairPosition != null && ind.isVisible) {
                                when (ind) {
                                    is Indicator.Volume -> {
                                        val v = hoverIdx?.let { allCandles.getOrNull(it)?.volume } ?: allCandles.lastOrNull()?.volume
                                        val c = hoverIdx?.let { allCandles.getOrNull(it) } ?: allCandles.lastOrNull()
                                        val color = if (c?.let { it.close >= it.open } == true) settings.upColor else settings.downColor
                                        v?.let { listOf(ChartUtils.formatVolume(it) to color) } ?: emptyList()
                                    }
                                    is Indicator.SMA -> {
                                        val list = smaValues[ind.id] ?: emptyList()
                                        val v = hoverIdx?.let { list.getOrNull(it) } ?: list.lastOrNull { it != null }
                                        v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to ind.color) } ?: emptyList()
                                    }
                                    is Indicator.EMA -> {
                                        val list = emaValues[ind.id] ?: emptyList()
                                        val v = hoverIdx?.let { list.getOrNull(it) } ?: list.lastOrNull { it != null }
                                        v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to ind.color) } ?: emptyList()
                                    }
                                    is Indicator.HMA -> {
                                        val list = hmaValues[ind.id] ?: emptyList()
                                        val v = hoverIdx?.let { list.getOrNull(it) } ?: list.lastOrNull { it != null }
                                        v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to ind.color) } ?: emptyList()
                                    }
                                    is Indicator.VWAP -> vwapValues[ind.id]?.let { res ->
                                        val idx = hoverIdx ?: res.vwap.indices.lastOrNull { res.vwap[it] != null }
                                        idx?.let { i ->
                                            val list = mutableListOf(
                                                String.format(Locale.getDefault(), "%.2f", res.vwap.getOrNull(i) ?: 0f) to ind.color
                                            )
                                            if (ind.showBands) {
                                                list.add(String.format(Locale.getDefault(), "%.2f", res.upper1.getOrNull(i) ?: 0f) to ind.upperColor)
                                                list.add(String.format(Locale.getDefault(), "%.2f", res.lower1.getOrNull(i) ?: 0f) to ind.lowerColor)
                                            }
                                            list
                                        }
                                    } ?: emptyList()
                                    is Indicator.BollingerBands -> bbValues[ind.id]?.let { bb ->
                                        val idx = hoverIdx ?: bb.middle.indices.lastOrNull { bb.middle[it] != null }
                                        idx?.let { i ->
                                            listOf(
                                                String.format(Locale.getDefault(), "%.2f", bb.upper.getOrNull(i) ?: 0f) to ind.upperColor,
                                                String.format(Locale.getDefault(), "%.2f", bb.middle.getOrNull(i) ?: 0f) to ind.middleColor,
                                                String.format(Locale.getDefault(), "%.2f", bb.lower.getOrNull(i) ?: 0f) to ind.lowerColor
                                            )
                                        }
                                    } ?: emptyList()
                                    is Indicator.ATRBands -> atrBandsValues[ind.id]?.let { ab ->
                                        val idx = hoverIdx ?: ab.upper.indices.lastOrNull { ab.upper[it] != null }
                                        idx?.let { i ->
                                            val res = mutableListOf(
                                                String.format(Locale.getDefault(), "%.2f", ab.upper.getOrNull(i) ?: 0f) to ind.upperColor,
                                                String.format(Locale.getDefault(), "%.2f", ab.lower.getOrNull(i) ?: 0f) to ind.lowerColor
                                            )
                                            if (ind.showTPBands) {
                                                res.add(String.format(Locale.getDefault(), "%.2f", ab.tpUpper.getOrNull(i) ?: 0f) to ind.tpUpperColor)
                                                res.add(String.format(Locale.getDefault(), "%.2f", ab.tpLower.getOrNull(i) ?: 0f) to ind.tpLowerColor)
                                            }
                                            res
                                        }
                                    } ?: emptyList()
                                    is Indicator.Alligator -> alligatorValues[ind.id]?.let { res ->
                                        val idx = hoverIdx ?: res.jaw.indices.lastOrNull { res.jaw[it] != null }
                                        idx?.let { i ->
                                            listOf(
                                                String.format(Locale.getDefault(), "%.2f", res.jaw.getOrNull(i) ?: 0f) to ind.jawColor,
                                                String.format(Locale.getDefault(), "%.2f", res.teeth.getOrNull(i) ?: 0f) to ind.teethColor,
                                                String.format(Locale.getDefault(), "%.2f", res.lips.getOrNull(i) ?: 0f) to ind.lipsColor
                                            )
                                        }
                                    } ?: emptyList()
                                    is Indicator.Ichimoku -> ichimokuValues[ind.id]?.let { res ->
                                        val idx = hoverIdx ?: res.tenkanSen.indices.lastOrNull { res.tenkanSen[it] != null }
                                        idx?.let { i ->
                                            listOf(
                                                String.format(Locale.getDefault(), "%.2f", res.tenkanSen.getOrNull(i) ?: 0f) to ind.tenkanColor,
                                                String.format(Locale.getDefault(), "%.2f", res.kijunSen.getOrNull(i) ?: 0f) to ind.kijunColor,
                                                String.format(Locale.getDefault(), "%.2f", res.senkouSpanA.getOrNull(i) ?: 0f) to ind.senkouAColor,
                                                String.format(Locale.getDefault(), "%.2f", res.senkouSpanB.getOrNull(i) ?: 0f) to ind.senkouBColor,
                                                String.format(Locale.getDefault(), "%.2f", res.chikouSpan.getOrNull(i) ?: 0f) to ind.chikouColor
                                            )
                                        }
                                    } ?: emptyList()
                                    is Indicator.Supertrend -> stValues[ind.id]?.let { st ->
                                        val idx = hoverIdx ?: st.values.indices.lastOrNull { st.values[it] != null }
                                        idx?.let { i ->
                                            val v = st.values.getOrNull(i)
                                            val up = st.isUp.getOrNull(i) ?: true
                                            v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to (if (up) ind.upColor else ind.downColor)) } ?: emptyList()
                                        }
                                    } ?: emptyList()
                                    is Indicator.Ribbon -> ribbonValues[ind.id]?.let { res ->
                                        val idx = hoverIdx ?: res.mas.firstOrNull()?.indices?.lastOrNull { res.mas.firstOrNull()?.get(it) != null }
                                        idx?.let { i ->
                                            val lastMa = res.mas.last()
                                            val lastV = lastMa.getOrNull(i) ?: 0f
                                            val refV = res.refMa.getOrNull(i) ?: 0f
                                            val diff = lastV - (lastMa.getOrNull(i - 1) ?: lastV)
                                            val color = when {
                                                diff >= 0 && lastV > refV -> Color(0xFF00FF00)
                                                diff < 0 && lastV > refV -> Color(0xFF800000)
                                                diff <= 0 && lastV < refV -> Color(0xFFFF0000)
                                                diff >= 0 && lastV < refV -> Color(0xFF008000)
                                                else -> Color(0xFF808080)
                                            }
                                            listOf(String.format(Locale.getDefault(), "%.2f", lastV) to color)
                                        }
                                    } ?: emptyList()
                                    is Indicator.Sessions -> emptyList()
                                    else -> emptyList()
                                }
                            } else emptyList()
                            
                            IndicatorLabelToolbar(
                                indicator = ind,
                                isExpanded = expandedIndicatorId == ind.id,
                                values = values,
                                textColor = txtCol,
                                surfaceColor = surfCol,
                                onToggleExpand = { expandedIndicatorId = if (expandedIndicatorId == ind.id) null else ind.id },
                                onToggleVisibility = { onIndicatorToggleVisibility(ind) },
                                onSettings = { onIndicatorSettingsRequest(ind) },
                                onRemove = { onIndicatorRemove(ind) }
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .height(20.dp)
                            .border(0.5.dp, txtCol.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .clickable { overlayCollapsed = !overlayCollapsed }
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            if (overlayCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = txtCol.copy(alpha = 0.6f),
                            modifier = Modifier.size(25.dp)
                        )
                        if (overlayCollapsed) {
                            @Suppress("DEPRECATION")
                            Text(
                                text = allOverlay.size.toString(),
                                color = txtCol.copy(alpha = 0.6f),
                                fontSize = 15.sp,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        if (bottomIndicators.isNotEmpty()) {
            var currentY = mainH
            bottomIndicators.forEach { ind ->
                val hInd = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                val transY = currentY + with(density) { 2.dp.toPx() }
                Column(modifier = Modifier.padding(start = 8.dp).graphicsLayer { translationY = transY }) {
                    if (showIndicatorLabels) {
                        val values = if (crosshairPosition != null && ind.isVisible) {
                            when (ind) {
                                is Indicator.RSI -> {
                                    val list = rsiValues[ind.id] ?: emptyList()
                                    val v = hoverIdx?.let { list.getOrNull(it) } ?: list.lastOrNull { it != null }
                                    v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to ind.color) } ?: emptyList()
                                }
                                is Indicator.MACD -> macdValues[ind.id]?.let { res ->
                                    val idx = hoverIdx ?: res.macdLine.indices.lastOrNull { res.macdLine[it] != null }
                                    idx?.let { i ->
                                        listOf(
                                            String.format(Locale.getDefault(), "%.2f", res.macdLine.getOrNull(i) ?: 0f) to ind.macdColor,
                                            String.format(Locale.getDefault(), "%.2f", res.signalLine.getOrNull(i) ?: 0f) to ind.signalColor,
                                            String.format(Locale.getDefault(), "%.2f", res.histogram.getOrNull(i) ?: 0f) to (if ((res.histogram.getOrNull(i) ?: 0f) >= 0) ind.histColorUp else ind.histColorDown)
                                        )
                                    }
                                } ?: emptyList()
                                is Indicator.Stochastic -> stochValues[ind.id]?.let { res ->
                                    val idx = hoverIdx ?: res.k.indices.lastOrNull { res.k[it] != null }
                                    idx?.let { i ->
                                        listOf(
                                            String.format(Locale.getDefault(), "%.2f", res.k.getOrNull(i) ?: 0f) to ind.kColor,
                                            String.format(Locale.getDefault(), "%.2f", res.d.getOrNull(i) ?: 0f) to ind.dColor
                                        )
                                    }
                                } ?: emptyList()
                                is Indicator.ATR -> {
                                    val list = atrValues[ind.id] ?: emptyList()
                                    val v = hoverIdx?.let { list.getOrNull(it) } ?: list.lastOrNull { it != null }
                                    v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to ind.color) } ?: emptyList()
                                }
                                else -> emptyList()
                            }
                        } else emptyList()
                        
                        IndicatorLabelToolbar(
                            indicator = ind,
                            isExpanded = expandedIndicatorId == ind.id,
                            values = values,
                            textColor = txtCol,
                            surfaceColor = surfCol,
                            onToggleExpand = { expandedIndicatorId = if (expandedIndicatorId == ind.id) null else ind.id },
                            onToggleVisibility = { onIndicatorToggleVisibility(ind) },
                            onSettings = { onIndicatorSettingsRequest(ind) },
                            onRemove = { onIndicatorRemove(ind) }
                        )
                    }
                }
                currentY += hInd
            }
        }

        // Settings icon under price bar (bottom-right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp)
        ) {
            IconButton(
                onClick = onOpenChartSettings,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Favorites Toolbar - Centered below top bar, clamped to screen bounds
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            val favoritesToolsList = DrawingTool.entries.filter { favoriteTools.contains(it.name) }
            if (favoritesToolsList.isNotEmpty()) {
                val isDarkChart = settings.backgroundColor.luminance() < 0.5f
                val colors = getDrawingToolsMenuColors(isDarkChart)

                // y=0 in chart coords = bottom of top app bar; small padding only
                val topBarMinY = with(density) { 8.dp.toPx() }

                val currentPos = favoritesBarPosition
                val barW = favoritesBarSizePx.x

                // Compute display position: initialize centered if not yet set
                val displayPos = currentPos ?: Offset((w - barW) / 2f, topBarMinY)

                // Calculate bar width based on content
                val dragHandleWidth = 30.dp
                val iconBoxWidth = 30.dp
                val totalIconWidth = iconBoxWidth * favoritesToolsList.size
                val spacerWidth = 2.dp
                val totalSpacerWidth = spacerWidth * (favoritesToolsList.size - 1)
                val calculatedBarWidth = dragHandleWidth + totalIconWidth + totalSpacerWidth + 16.dp // padding

                Box(
                    modifier = Modifier
                        .offset { IntOffset(displayPos.x.roundToInt(), displayPos.y.roundToInt()) }
                        .width(calculatedBarWidth)
                        .height(40.dp)
                        .onGloballyPositioned { coords ->
                            val newW = coords.size.width.toFloat()
                            val newH = coords.size.height.toFloat()
                            favoritesBarSizePx = Offset(newW, newH)
                            // Set centered initial position once size is known
                            if (favoritesBarPosition == null) {
                                favoritesBarPosition = Offset(
                                    ((w - newW) / 2f).coerceIn(0f, (w - newW).coerceAtLeast(0f)),
                                    topBarMinY
                                )
                            }
                        }
                        .pointerInput(w, h) {
                            detectDragGestures(
                                onDragStart = { isDraggingFavoritesBar = true },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val bW = favoritesBarSizePx.x
                                    val bH = favoritesBarSizePx.y
                                    val prev = favoritesBarPosition ?: Offset((w - bW) / 2f, topBarMinY)
                                    favoritesBarPosition = Offset(
                                        (prev.x + dragAmount.x).coerceIn(0f, (w - bW).coerceAtLeast(0f)),
                                        (prev.y + dragAmount.y).coerceIn(topBarMinY, (h - bH).coerceAtLeast(topBarMinY))
                                    )
                                },
                                onDragEnd = { isDraggingFavoritesBar = false },
                                onDragCancel = { isDraggingFavoritesBar = false }
                            )
                        }
                        .background(colors.bgColor, RoundedCornerShape(8.dp))
                        .border(0.5.dp, colors.borderColor, RoundedCornerShape(8.dp))
                        .padding(start = 8.dp, end = 4.dp, top = 0.dp, bottom = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drag Handle - 6 dots (2x3 grid)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            DragHandle(gripColor = colors.textColor.copy(0.5f), dotSize = 3f, spacing = 3f)
                        }

                        val context = LocalContext.current
                        favoritesToolsList.forEach { tool ->
                            // Encadrer chaque icône comme dans la barre de paramètres
                            Box(
                                modifier = Modifier
                                    .width(30.dp)
                                    .height(40.dp)
                                    .background(colors.bgColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .clickable(enabled = !isDraggingFavoritesBar) {
                                        if (tool == DrawingTool.TREND_LINE || tool == DrawingTool.EXTENDED_LINE || tool == DrawingTool.ARROW) {
                                            android.util.Log.d("FavoritesBar", "Tool selected: ${tool.name}")
                                            android.util.Log.d("FavoritesBar", "Activating simple drawing mode for: ${tool.name}")
                                            // Mark this as intentional activation
                                            localIntentionalActivation = true
                                            // Use the callback to activate simple drawing mode
                                            onFavoritesBarToolSelected?.invoke(tool)
                                        } else {
                                            // For other tools, use normal selection
                                            drawingManager.setTool(tool)
                                        }
                                    }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = tool.iconRes),
                                    contentDescription = null,
                                    tint = if (drawingManager.activeTool == tool) Color(0xFF2962FF) else colors.textColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Show settings bar when a simple trendline is selected
        if (selectedSimpleTrendLineIndex != null && selectedSimpleTrendLineIndex in localSimpleTrendLines.indices) {
            val selectedLine = localSimpleTrendLines[selectedSimpleTrendLineIndex!!]
            val isDarkTheme = colorScheme.background.luminance() < 0.5f
            SimpleTrendLineSettingsBar(
                selectedLine = selectedLine,
                chartWidthPx = chartWidthPx,
                screenWidth = w,
                screenHeight = h,
                isDarkTheme = isDarkTheme,
                onUpdate = { updatedLine ->
                    localSimpleTrendLines = localSimpleTrendLines.toMutableList().also { 
                        it[selectedSimpleTrendLineIndex!!] = updatedLine 
                    }
                    updatePersistentLines()
                },
                onDelete = {
                    android.util.Log.d("TrendLineDelete", "onDelete called, removing line at index: $selectedSimpleTrendLineIndex")
                    localSimpleTrendLines = localSimpleTrendLines.toMutableList().also { 
                        it.removeAt(selectedSimpleTrendLineIndex!!) 
                        android.util.Log.d("TrendLineDelete", "Line removed, new size: ${it.size}")
                    }
                    updatePersistentLines()
                    selectedSimpleTrendLineIndex = null
                    selectedEndpoint = null
                    android.util.Log.d("TrendLineDelete", "Selection cleared")
                }
            )
        }
        
        // Show settings bar when an extended trendline is selected
        if (selectedExtendedTrendLineIndex != null && selectedExtendedTrendLineIndex in localExtendedLines.indices) {
            val selectedLine = localExtendedLines[selectedExtendedTrendLineIndex!!]
            val isDarkTheme = colorScheme.background.luminance() < 0.5f
            SimpleTrendLineSettingsBar(
                selectedLine = selectedLine,
                chartWidthPx = chartWidthPx,
                screenWidth = w,
                screenHeight = h,
                isDarkTheme = isDarkTheme,
                onUpdate = { updatedLine ->
                    localExtendedLines = localExtendedLines.toMutableList().also { 
                        it[selectedExtendedTrendLineIndex!!] = updatedLine 
                    }
                    updatePersistentLines()
                },
                onDelete = {
                    android.util.Log.d("TrendLineDelete", "onDelete called for extended line, removing line at index: $selectedExtendedTrendLineIndex")
                    localExtendedLines = localExtendedLines.toMutableList().also { 
                        it.removeAt(selectedExtendedTrendLineIndex!!) 
                        android.util.Log.d("TrendLineDelete", "Extended line removed, new size: ${it.size}")
                    }
                    updatePersistentLines()
                    selectedExtendedTrendLineIndex = null
                    selectedEndpoint = null
                    android.util.Log.d("TrendLineDelete", "Extended line selection cleared")
                }
            )
        }
        
        // Show settings bar when an arrow is selected
        if (selectedArrowIndex != null && selectedArrowIndex in localArrows.indices) {
            val selectedLine = localArrows[selectedArrowIndex!!]
            val isDarkTheme = colorScheme.background.luminance() < 0.5f
            SimpleTrendLineSettingsBar(
                selectedLine = selectedLine,
                chartWidthPx = chartWidthPx,
                screenWidth = w,
                screenHeight = h,
                isDarkTheme = isDarkTheme,
                onUpdate = { updatedLine ->
                    localArrows = localArrows.toMutableList().also { 
                        it[selectedArrowIndex!!] = updatedLine 
                    }
                    updatePersistentLines()
                },
                onDelete = {
                    android.util.Log.d("TrendLineDelete", "onDelete called for arrow, removing line at index: $selectedArrowIndex")
                    localArrows = localArrows.toMutableList().also { 
                        it.removeAt(selectedArrowIndex!!) 
                        android.util.Log.d("TrendLineDelete", "Arrow removed, new size: ${it.size}")
                    }
                    updatePersistentLines()
                    selectedArrowIndex = null
                    selectedEndpoint = null
                    android.util.Log.d("TrendLineDelete", "Arrow selection cleared")
                }
            )
        }

        // Show settings bar when a ray is selected
        if (selectedRayIndex != null && selectedRayIndex in localRays.indices) {
            val selectedLine = localRays[selectedRayIndex!!]
            val isDarkTheme = colorScheme.background.luminance() < 0.5f
            SimpleTrendLineSettingsBar(
                selectedLine = selectedLine,
                chartWidthPx = chartWidthPx,
                screenWidth = w,
                screenHeight = h,
                isDarkTheme = isDarkTheme,
                onUpdate = { updatedLine ->
                    localRays = localRays.toMutableList().also {
                        it[selectedRayIndex!!] = updatedLine
                    }
                    updatePersistentLines()
                },
                onDelete = {
                    android.util.Log.d("TrendLineDelete", "onDelete called for ray, removing line at index: $selectedRayIndex")
                    localRays = localRays.toMutableList().also {
                        it.removeAt(selectedRayIndex!!)
                        android.util.Log.d("TrendLineDelete", "Ray removed, new size: ${it.size}")
                    }
                    updatePersistentLines()
                    selectedRayIndex = null
                    selectedEndpoint = null
                    android.util.Log.d("TrendLineDelete", "Ray selection cleared")
                }
            )
        }
    }
}

