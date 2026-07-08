package com.bthr.backtest.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bthr.backtest.model.Candle
import com.bthr.backtest.model.ChartSettings
import com.bthr.backtest.model.ChartType
import com.bthr.backtest.model.Indicator
import com.bthr.backtest.model.Timeframe
import com.bthr.backtest.model.shouldDrawForTimeframe
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ChartDrawerCandles {

    fun drawSessions(
        drawScope: DrawScope,
        allCandles: List<Candle>,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        mainH: Float,
        indicator: Indicator.Sessions,
        normalizeY: (Float) -> Float,
        textMeasurer: TextMeasurer,
        timeframe: Timeframe = Timeframe.H1,
        exchangeTimeZone: TimeZone? = null
    ) = with(drawScope) {
        // Vérifier si l'indicateur doit être visible selon le timeframe
        if (!indicator.isVisible || allCandles.isEmpty() || chartW <= 0) return@with
        
        // Vérifier la visibilité selon le timeframe sélectionné
        val isTimeframeVisible = when(timeframe) {
            Timeframe.M1 -> indicator.timeframeVisibility.showMinute1
            Timeframe.M3 -> indicator.timeframeVisibility.showMinute1
            Timeframe.M5 -> indicator.timeframeVisibility.showMinute5
            Timeframe.M10 -> indicator.timeframeVisibility.showMinute5
            Timeframe.M15 -> indicator.timeframeVisibility.showMinute15
            Timeframe.M30 -> indicator.timeframeVisibility.showMinute30
            Timeframe.M45 -> indicator.timeframeVisibility.showMinute45
            Timeframe.H1 -> indicator.timeframeVisibility.showHour1
            Timeframe.H2 -> indicator.timeframeVisibility.showHour2
            Timeframe.H3 -> indicator.timeframeVisibility.showHour3
            Timeframe.H4 -> indicator.timeframeVisibility.showHour4
            Timeframe.D1 -> indicator.timeframeVisibility.showDaily
            Timeframe.W1 -> indicator.timeframeVisibility.showWeekly
            Timeframe.MN1 -> indicator.timeframeVisibility.showMonthly
        }
        
        if (!isTimeframeVisible) return@with

        val effectiveTimeZone = if (!indicator.useExchangeTimezone) {
            val offsetMillis = indicator.utcOffset * 60 * 60 * 1000
            val ids = TimeZone.getAvailableIDs(offsetMillis)
            if (ids.isNotEmpty()) TimeZone.getTimeZone(ids[0]) else TimeZone.getTimeZone("UTC")
        } else {
            exchangeTimeZone ?: TimeZone.getTimeZone("UTC")
        }
        val cal = Calendar.getInstance(effectiveTimeZone)
        
        fun parseTimeToMinutes(timeStr: String): Int {
            return try {
                val parts = timeStr.split(":")
                val h = parts[0].toInt()
                val m = if (parts.size > 1) parts[1].toInt() else 0
                h * 60 + m
            } catch (e: Exception) { 0 }
        }

        data class SessionInfo(val name: String, val startMin: Int, val endMin: Int, val color: Color, val thickness: Float, val style: Int)
        val sessions = listOfNotNull(
            if (indicator.showSydney) SessionInfo("Sydney", parseTimeToMinutes(indicator.sydneyStart), parseTimeToMinutes(indicator.sydneyEnd), indicator.sydneyColor, indicator.sydneyThickness, indicator.sydneyStyle) else null,
            if (indicator.showTokyo) SessionInfo("Tokyo", parseTimeToMinutes(indicator.tokyoStart), parseTimeToMinutes(indicator.tokyoEnd), indicator.tokyoColor, indicator.tokyoThickness, indicator.tokyoStyle) else null,
            if (indicator.showLondon) SessionInfo("London", parseTimeToMinutes(indicator.londonStart), parseTimeToMinutes(indicator.londonEnd), indicator.londonColor, indicator.londonThickness, indicator.londonStyle) else null,
            if (indicator.showNewYork) SessionInfo("New York", parseTimeToMinutes(indicator.newYorkStart), parseTimeToMinutes(indicator.newYorkEnd), indicator.newYorkColor, indicator.newYorkThickness, indicator.newYorkStyle) else null
        )

        val visibleEndIdx = (startIdx + (chartW / candleW).toInt() + 1).coerceAtMost(allCandles.size)
        
        sessions.forEachIndexed { sessionIndex, session ->
            val processedIndices = mutableSetOf<Int>()
            
            for (i in startIdx until visibleEndIdx) {
                if (i in processedIndices) continue
                
                val candle = allCandles[i]
                cal.timeInMillis = candle.timestamp
                val currentMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                
                val isInSession = if (session.startMin < session.endMin) {
                    currentMin in session.startMin until session.endMin
                } else {
                    currentMin >= session.startMin || currentMin < session.endMin
                }
                
                if (isInSession) {
                    var blockStart = i
                    while (blockStart > 0) {
                        val prevCandle = allCandles[blockStart - 1]
                        cal.timeInMillis = prevCandle.timestamp
                        val m = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                        val ok = if (session.startMin < session.endMin) m in session.startMin until session.endMin else m >= session.startMin || m < session.endMin
                        if (!ok) break
                        blockStart--
                    }
                    
                    var blockEnd = i
                    while (blockEnd < allCandles.size - 1) {
                        val nextCandle = allCandles[blockEnd + 1]
                        cal.timeInMillis = nextCandle.timestamp
                        val m = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                        val ok = if (session.startMin < session.endMin) m in session.startMin until session.endMin else m >= session.startMin || m < session.endMin
                        if (!ok) break
                        blockEnd++
                    }
                    
                    var sessionMin = Float.MAX_VALUE
                    var sessionMax = Float.MIN_VALUE
                    for (k in blockStart..blockEnd) {
                        sessionMin = min(sessionMin, allCandles[k].low)
                        sessionMax = max(sessionMax, allCandles[k].high)
                        processedIndices.add(k)
                    }
                    
                    drawSessionBox(this, blockStart, blockEnd, sessionMin, sessionMax, session.name, session.color, session.thickness, session.style, indicator, totalCandles, scrollOffset, candleW, chartW, mainH, normalizeY, textMeasurer, sessionIndex)
                }
            }
        }
    }

    private fun drawSessionBox(
        drawScope: DrawScope,
        startIdx: Int,
        endIdx: Int,
        minP: Float,
        maxP: Float,
        name: String,
        color: Color,
        thickness: Float,
        style: Int,
        indicator: Indicator.Sessions,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        mainH: Float,
        normalizeY: (Float) -> Float,
        textMeasurer: TextMeasurer,
        sessionIndex: Int = 0
    ) = with(drawScope) {
        val xStart = chartW - (candleW / 2f) - ((totalCandles - 1 - startIdx - scrollOffset) * candleW) - candleW/2f
        val xEnd = chartW - (candleW / 2f) - ((totalCandles - 1 - endIdx - scrollOffset) * candleW) + candleW/2f
        
        if (xEnd < 0 || xStart > chartW) return@with

        val yTop = normalizeY(maxP)
        val yBottom = normalizeY(minP)

        if (indicator.showBackground) {
            drawRect(
                color = indicator.backgroundColor,
                topLeft = Offset(xStart, yTop),
                size = Size(xEnd - xStart, yBottom - yTop)
            )
        }

        val pe = when(style) { 1 -> PathEffect.dashPathEffect(floatArrayOf(10f, 6f)); 2 -> PathEffect.dashPathEffect(floatArrayOf(0f, 12f)); else -> null }

        drawRect(
            color = color,
            topLeft = Offset(xStart, yTop),
            size = Size(xEnd - xStart, yBottom - yTop),
            style = Stroke(width = thickness.dp.toPx(), pathEffect = pe, cap = if (style == 2) StrokeCap.Round else StrokeCap.Butt)
        )

        if (indicator.showLabels) {
            val style = TextStyle(color = color, fontSize = indicator.labelTextSize.sp)
            val layout = textMeasurer.measure(name, style)
            val labelX = xStart + (xEnd - xStart - layout.size.width) / 2f
            if (labelX + layout.size.width > 0 && labelX < chartW) {
                // SESSIONS paires (0=Sydney, 2=London) : label en haut
                // SESSIONS impaires (1=Tokyo, 3=NewYork) : label en bas
                val labelY = if (sessionIndex % 2 == 0) {
                    // En haut du rectangle
                    (yTop - layout.size.height - 2.dp.toPx()).coerceAtLeast(2.dp.toPx())
                } else {
                    // En bas du rectangle
                    (yBottom + 2.dp.toPx()).coerceAtMost(mainH - layout.size.height - 2.dp.toPx())
                }
                drawText(layout, topLeft = Offset(labelX, labelY))
            }
        }
    }

    fun drawCandles(
        drawScope: DrawScope,
        visibleCandles: List<Candle>,
        allCandles: List<Candle>,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        bodyW: Float,
        chartW: Float,
        mainH: Float,
        volumeIndicator: Indicator.Volume?,
        volumeMaValues: List<Float?>?,
        settings: ChartSettings,
        normalizeY: (Float) -> Float,
        timeframe: Timeframe = Timeframe.H1
    ) = with(drawScope) {
        if (visibleCandles.isEmpty() || chartW <= 0) {
            return@with
        }
        val maxVol = visibleCandles.let { candles ->
            if (candles.isEmpty()) 1f
            else {
                val sorted = candles.sortedBy { it.volume }
                val idx = (sorted.size * 0.95).toInt().coerceIn(0, sorted.size - 1)
                sorted[idx].volume.coerceAtLeast(1f)
            }
        }
        val volumeVisible = volumeIndicator?.shouldDrawForTimeframe(timeframe) == true
        
        // Dessiner le volume si activé
        if (volumeVisible) {
            val endIdx = startIdx + visibleCandles.size
            val normVolY: (Float) -> Float = { mainH - (it / maxVol) * (mainH * 0.15f) }
            if (volumeIndicator.showVolumeMa && volumeMaValues != null) ChartUtils.drawIndicatorPath(this, volumeMaValues, startIdx, endIdx, scrollOffset, candleW, chartW, volumeIndicator.volumeMaColor, thickness = volumeIndicator.volumeMaThickness, style = volumeIndicator.volumeMaStyle, normY = normVolY)
        }

        val showVolume = volumeVisible && volumeIndicator!!.showVolume
        val isVerySmall = candleW < 1.5.dp.toPx() 
        
        // Sélectionner le type de rendu selon settings.chartType
        when (settings.chartType) {
            ChartType.CANDLESTICK -> drawCandlestickBars(
                visibleCandles, allCandles, startIdx, totalCandles, scrollOffset,
                candleW, bodyW, chartW, mainH, showVolume, maxVol, isVerySmall, settings, normalizeY
            )
            ChartType.LINE -> drawLineChart(
                visibleCandles, allCandles, startIdx, totalCandles, scrollOffset,
                candleW, chartW, mainH, showVolume, maxVol, settings, normalizeY
            )
            ChartType.OHLC_BARS -> drawOhlcBars(
                visibleCandles, allCandles, startIdx, totalCandles, scrollOffset,
                candleW, chartW, mainH, showVolume, maxVol, isVerySmall, settings, normalizeY
            )
            ChartType.HEIKIN_ASHI -> drawHeikinAshiCandles(
                visibleCandles, allCandles, startIdx, totalCandles, scrollOffset,
                candleW, bodyW, chartW, mainH, showVolume, maxVol, isVerySmall, settings, normalizeY
            )
        }
    }

    // ────────────────────────────────────────────────
    // Rendu: Bougies Japonaises (Candlestick)
    // ────────────────────────────────────────────────
    private fun DrawScope.drawCandlestickBars(
        visibleCandles: List<Candle>,
        allCandles: List<Candle>,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        bodyW: Float,
        chartW: Float,
        mainH: Float,
        showVolume: Boolean,
        maxVol: Float,
        isVerySmall: Boolean,
        settings: ChartSettings,
        normalizeY: (Float) -> Float
    ) {
        val pathUp = Path()
        val pathDown = Path()
        var lastX = -1000f
        val minXGap = 1.0f 

        visibleCandles.forEachIndexed { index, candle ->
            val absIdx = startIdx + index
            val x = chartW - (candleW / 2f) - ((totalCandles - 1 - absIdx - scrollOffset) * candleW)
            if (x < -candleW) return@forEachIndexed
            if (x > chartW + candleW) return@forEachIndexed

            // Volume bars
            if (showVolume) {
                val volH = (candle.volume / maxVol) * (mainH * 0.15f)
                val isGrowing = candle.close >= candle.open
                drawRect(settings.volumeUpColor.copy(alpha = 0.5f).takeIf { isGrowing } ?: settings.volumeDownColor.copy(alpha = 0.5f), Offset(x - bodyW / 2, mainH - volH), Size(bodyW, volH))
            }

            // Determiner si la bougie est haussiere ou baissiere
            val isUp = if (settings.colorizeBarsBasedOnPrevClose && absIdx > 0) {
                candle.close >= allCandles[absIdx - 1].close
            } else {
                candle.close >= candle.open
            }
            val color = if (isUp) settings.upColor else settings.downColor

            if (isVerySmall) {
                if (abs(x - lastX) < minXGap && index != 0 && index != visibleCandles.size - 1) return@forEachIndexed
                lastX = x
                val highY = normalizeY(candle.high)
                val lowY = normalizeY(candle.low)
                val targetPath = if (isUp) pathUp else pathDown
                targetPath.moveTo(x, highY)
                targetPath.lineTo(x, lowY)
            } else {
                val highY = normalizeY(candle.high); val lowY = normalizeY(candle.low)
                val bTop = min(normalizeY(candle.open), normalizeY(candle.close))
                val bBottom = max(normalizeY(candle.open), normalizeY(candle.close))
                
                val minHeight = 2f
                val bodyHeight = max(bBottom - bTop, minHeight)
                val adjustedBTop = if (bodyHeight == minHeight && (bBottom - bTop) < minHeight) {
                    (bTop + bBottom) / 2f - minHeight / 2f
                } else {
                    bTop
                }

                if (settings.wickEnabled) {
                    val wickColor = if (isUp) settings.upWickColor else settings.downWickColor
                    drawLine(wickColor, Offset(x, highY), Offset(x, adjustedBTop), 1f)
                    drawLine(wickColor, Offset(x, adjustedBTop + bodyHeight), Offset(x, lowY), 1f)
                }
                if (settings.bodyEnabled) drawRect(color, Offset(x - bodyW / 2, adjustedBTop), Size(bodyW, bodyHeight))
                if (settings.bordersEnabled) {
                    val borderColor = if (isUp) settings.upBorderColor else settings.downBorderColor
                    drawRect(borderColor, Offset(x - bodyW / 2, adjustedBTop), Size(bodyW, bodyHeight), style = Stroke(1f))
                }
            }
        }
        
        if (isVerySmall) {
            val strokeW = max(bodyW, 1f)
            drawPath(pathUp, settings.upColor, style = Stroke(width = strokeW))
            drawPath(pathDown, settings.downColor, style = Stroke(width = strokeW))
        }
    }

    // ────────────────────────────────────────────────
    // Rendu: Ligne simple (Line Chart)
    // ────────────────────────────────────────────────
    private fun DrawScope.drawLineChart(
        visibleCandles: List<Candle>,
        allCandles: List<Candle>,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        mainH: Float,
        showVolume: Boolean,
        maxVol: Float,
        settings: ChartSettings,
        normalizeY: (Float) -> Float
    ) {
        val linePath = Path()
        var firstPoint = true

        // Fonction helper pour obtenir le prix selon la source sélectionnée
        fun getPrice(candle: Candle): Float {
            return when (settings.linePriceSource) {
                "Ouverture" -> candle.open
                "Haut" -> candle.high
                "Bas" -> candle.low
                "Clôture" -> candle.close
                "(H + L)/2" -> (candle.high + candle.low) / 2f
                "(H + L + C)/3" -> (candle.high + candle.low + candle.close) / 3f
                "(O + H + L + C)/4" -> (candle.open + candle.high + candle.low + candle.close) / 4f
                else -> candle.close  // Par défaut: Clôture
            }
        }

        visibleCandles.forEachIndexed { index, candle ->
            val absIdx = startIdx + index
            val x = chartW - (candleW / 2f) - ((totalCandles - 1 - absIdx - scrollOffset) * candleW)
            if (x < -candleW) return@forEachIndexed
            if (x > chartW + candleW) return@forEachIndexed

            // Volume bars (optionnel pour line chart)
            if (showVolume) {
                val volH = (candle.volume / maxVol) * (mainH * 0.15f)
                val isGrowing = candle.close >= candle.open
                drawRect(settings.volumeUpColor.copy(alpha = 0.3f).takeIf { isGrowing } ?: settings.volumeDownColor.copy(alpha = 0.3f), Offset(x - candleW / 4, mainH - volH), Size(candleW / 2, volH))
            }

            val price = getPrice(candle)
            val priceY = normalizeY(price)
            if (firstPoint) {
                linePath.moveTo(x, priceY)
                firstPoint = false
            } else {
                linePath.lineTo(x, priceY)
            }
        }

        // Dessiner la ligne principale selon le style
        if (settings.lineStyle == "Dégradé") {
            // Créer un dégradé horizontal de gauche à droite
            val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(settings.lineGradientStartColor, settings.lineGradientEndColor),
                startX = 0f,
                endX = chartW
            )
            drawPath(linePath, brush, style = Stroke(width = settings.lineWidth.dp.toPx()))
        } else {
            // Style uni
            drawPath(linePath, settings.lineColor, style = Stroke(width = settings.lineWidth.dp.toPx()))
        }
    }

    // ────────────────────────────────────────────────
    // Rendu: Barres OHLC traditionnelles
    // ────────────────────────────────────────────────
    private fun DrawScope.drawOhlcBars(
        visibleCandles: List<Candle>,
        allCandles: List<Candle>,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        mainH: Float,
        showVolume: Boolean,
        maxVol: Float,
        isVerySmall: Boolean,
        settings: ChartSettings,
        normalizeY: (Float) -> Float
    ) {
        visibleCandles.forEachIndexed { index, candle ->
            val absIdx = startIdx + index
            val x = chartW - (candleW / 2f) - ((totalCandles - 1 - absIdx - scrollOffset) * candleW)
            if (x < -candleW) return@forEachIndexed
            if (x > chartW + candleW) return@forEachIndexed

            // Volume bars
            if (showVolume) {
                val volH = (candle.volume / maxVol) * (mainH * 0.15f)
                val isGrowing = candle.close >= candle.open
                drawRect(settings.volumeUpColor.copy(alpha = 0.5f).takeIf { isGrowing } ?: settings.volumeDownColor.copy(alpha = 0.5f), Offset(x - candleW / 4, mainH - volH), Size(candleW / 2, volH))
            }

            // Determiner couleur selon mode
            val isUp = if (settings.colorizeBarsBasedOnPrevClose && absIdx > 0) {
                candle.close >= allCandles[absIdx - 1].close
            } else {
                candle.close >= candle.open
            }
            
            // Utiliser la couleur OHLC configurée
            val barColor = settings.ohlcBarsColor

            val highY = normalizeY(candle.high)
            val lowY = normalizeY(candle.low)
            val openY = normalizeY(candle.open)
            val closeY = normalizeY(candle.close)
            
            // Épaisseur des lignes: s'adapte par paliers au zoom horizontal pour une lisibilité optimale
            val strokeWidth = if (settings.ohlcThinBars) {
                // Mode "Barres fines": épaisseur fixe de 1.2f
                1.2f
            } else {
                // Mode normal: épaisseur adaptative par paliers de zoom
                // candleW ≈ largeur d'une bougie en pixels
                // Plus le zoom est élevé (candleW grand), plus les barres sont épaisses
                when {
                    candleW < 3f   -> 0.8f   // Dézoom extrême : barres très fines
                    candleW < 5f   -> 1.0f   // Dézoom fort
                    candleW < 8f   -> 1.4f   // Dézoom modéré
                    candleW < 12f  -> 1.8f   // Zoom léger
                    candleW < 18f  -> 2.5f   // Zoom normal
                    candleW < 28f  -> 3.2f   // Zoom moyen
                    candleW < 40f  -> 4.0f   // Zoom élevé
                    candleW < 60f  -> 5.0f   // Zoom fort
                    candleW < 90f  -> 6.5f   // Zoom très fort
                    else           -> 9.0f   // Zoom extrême
                }
            }
            
            // Largeur des traits horizontaux (open/close): proportionnelle à l'épaisseur verticale
            val horizontalTickWidth = strokeWidth * 1.6f

            // Ligne verticale (high-low)
            drawLine(barColor, Offset(x, highY), Offset(x, lowY), strokeWidth = strokeWidth)

            // Tick gauche (open) - plus large que la ligne verticale
            drawLine(barColor, Offset(x - candleW / 4, openY), Offset(x, openY), strokeWidth = horizontalTickWidth)

            // Tick droit (close) - plus large que la ligne verticale
            drawLine(barColor, Offset(x, closeY), Offset(x + candleW / 4, closeY), strokeWidth = horizontalTickWidth)
        }
    }

    // ────────────────────────────────────────────────
    // Rendu: Heikin-Ashi
    // ────────────────────────────────────────────────
    private fun DrawScope.drawHeikinAshiCandles(
        visibleCandles: List<Candle>,
        allCandles: List<Candle>,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        bodyW: Float,
        chartW: Float,
        mainH: Float,
        showVolume: Boolean,
        maxVol: Float,
        isVerySmall: Boolean,
        settings: ChartSettings,
        normalizeY: (Float) -> Float
    ) {
        val pathUp = Path()
        val pathDown = Path()
        var lastX = -1000f
        val minXGap = 1.0f 

        // Calculer les valeurs Heikin-Ashi
        val haCandles = mutableListOf<Candle>()
        for (i in allCandles.indices) {
            val c = allCandles[i]
            if (i == 0) {
                // Premiere bougie HA = moyenne de OHLC
                val haOpen = (c.open + c.close) / 2f
                val haClose = (c.open + c.high + c.low + c.close) / 4f
                val haHigh = maxOf(c.high, haOpen, haClose)
                val haLow = minOf(c.low, haOpen, haClose)
                haCandles.add(Candle(timestamp = c.timestamp, open = haOpen, high = haHigh, low = haLow, close = haClose, volume = c.volume))
            } else {
                val prevHa = haCandles[i - 1]
                val haOpen = (prevHa.open + prevHa.close) / 2f
                val haClose = (c.open + c.high + c.low + c.close) / 4f
                val haHigh = maxOf(c.high, haOpen, haClose)
                val haLow = minOf(c.low, haOpen, haClose)
                haCandles.add(Candle(timestamp = c.timestamp, open = haOpen, high = haHigh, low = haLow, close = haClose, volume = c.volume))
            }
        }

        visibleCandles.forEachIndexed { index, candle ->
            val absIdx = startIdx + index
            val x = chartW - (candleW / 2f) - ((totalCandles - 1 - absIdx - scrollOffset) * candleW)
            if (x < -candleW) return@forEachIndexed
            if (x > chartW + candleW) return@forEachIndexed

            // Obtenir la bougie HA correspondante
            val haCandle = haCandles.getOrNull(absIdx) ?: candle

            // Volume bars
            if (showVolume) {
                val volH = (candle.volume / maxVol) * (mainH * 0.15f)
                val isGrowing = haCandle.close >= haCandle.open
                drawRect(settings.volumeUpColor.copy(alpha = 0.5f).takeIf { isGrowing } ?: settings.volumeDownColor.copy(alpha = 0.5f), Offset(x - bodyW / 2, mainH - volH), Size(bodyW, volH))
            }

            // Couleur basee sur HA open/close
            val isUp = haCandle.close >= haCandle.open
            val color = if (isUp) settings.upColor else settings.downColor

            if (isVerySmall) {
                if (abs(x - lastX) < minXGap && index != 0 && index != visibleCandles.size - 1) return@forEachIndexed
                lastX = x
                val highY = normalizeY(haCandle.high)
                val lowY = normalizeY(haCandle.low)
                val targetPath = if (isUp) pathUp else pathDown
                targetPath.moveTo(x, highY)
                targetPath.lineTo(x, lowY)
            } else {
                val highY = normalizeY(haCandle.high); val lowY = normalizeY(haCandle.low)
                val bTop = min(normalizeY(haCandle.open), normalizeY(haCandle.close))
                val bBottom = max(normalizeY(haCandle.open), normalizeY(haCandle.close))
                
                val minHeight = 2f
                val bodyHeight = max(bBottom - bTop, minHeight)
                val adjustedBTop = if (bodyHeight == minHeight && (bBottom - bTop) < minHeight) {
                    (bTop + bBottom) / 2f - minHeight / 2f
                } else {
                    bTop
                }

                if (settings.wickEnabled) {
                    val wickColor = if (isUp) settings.upWickColor else settings.downWickColor
                    drawLine(wickColor, Offset(x, highY), Offset(x, adjustedBTop), 1f)
                    drawLine(wickColor, Offset(x, adjustedBTop + bodyHeight), Offset(x, lowY), 1f)
                }
                if (settings.bodyEnabled) drawRect(color, Offset(x - bodyW / 2, adjustedBTop), Size(bodyW, bodyHeight))
                if (settings.bordersEnabled) {
                    val borderColor = if (isUp) settings.upBorderColor else settings.downBorderColor
                    drawRect(borderColor, Offset(x - bodyW / 2, adjustedBTop), Size(bodyW, bodyHeight), style = Stroke(1f))
                }
            }
        }
        
        if (isVerySmall) {
            val strokeW = max(bodyW, 1f)
            drawPath(pathUp, settings.upColor, style = Stroke(width = strokeW))
            drawPath(pathDown, settings.downColor, style = Stroke(width = strokeW))
        }
    }

    fun drawSelectionHandles(
        drawScope: DrawScope,
        totalCount: Int,
        startIdx: Int,
        endIdx: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        normalizeY: (Float) -> Float,
        getValue: (Int) -> Float?
    ) = with(drawScope) {
        if (totalCount < 2 || chartW <= 0 || candleW <= 0) return@with
        val handleColor = Color(0xFF2196F3); val radius = 3.2.dp.toPx(); val step = 10
        var i = totalCount - 2
        while (i >= 0) {
            if (i in startIdx until endIdx) {
                val v = getValue(i); if (v != null) {
                    val x = chartW - (candleW / 2f) - ((totalCount - 1 - i - scrollOffset) * candleW)
                    if (x in 0f..chartW) {
                        val y = normalizeY(v)
                        drawCircle(color = Color.White, radius = radius, center = Offset(x, y))
                        drawCircle(color = handleColor, radius = radius, center = Offset(x, y), style = Stroke(width = 1.1.dp.toPx()))
                    }
                }
            }
            if (i < startIdx) break
            i -= step
        }
    }
}
