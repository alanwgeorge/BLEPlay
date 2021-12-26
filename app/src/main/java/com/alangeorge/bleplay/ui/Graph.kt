package com.alangeorge.bleplay.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt

@ExperimentalComposeUiApi
@Composable
fun Graph(modifier: Modifier = Modifier, points: List<Float>, avgLine: Boolean = true) {
    val gradientColor = MaterialTheme.colors.primary
    var touchX by remember { mutableStateOf(-1f) }
    if (points.isNotEmpty()) {
        Canvas(
            modifier = modifier.pointerInteropFilter {
                touchX = it.rawX
                true
            }
        ) {
            val gradient = Brush.verticalGradient(
                colors = listOf(gradientColor, Color.Transparent)
            )

            val textPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = Color.Black.toArgb()
                textSize = 36f
            }

            val canvasHeight = size.height
            val canvasWidth = size.width
            val distanceXBetweenPoints = canvasWidth / (points.size - 1)

            val startX = 0f
            val startY = points.firstOrNull()?.let {
                canvasHeight - (canvasHeight * it)
            } ?: 0f

            val linePath = Path().apply {
                moveTo(startX, startY)
            }

            val fillPath = Path().apply {
                moveTo(startX, startY)
            }


            points.reduceIndexed { index, p1, p2 ->
                val x1 = ((index - 1) * distanceXBetweenPoints)
                val x2 = x1 + distanceXBetweenPoints
                val y1 = canvasHeight - (canvasHeight * p1)
                val y2 = canvasHeight - (canvasHeight * p2)

                val controlX1 = x1 + (distanceXBetweenPoints / 3)
                val controlY1 = y1

                val controlX2 = x2 - (distanceXBetweenPoints / 3)
                val controlY2 = y2

                linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x2, y2)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x2, y2)

                if ((x1..x2).contains(touchX)) {
                    drawLine(
                        color = gradientColor,
                        strokeWidth = 5f,
                        start = Offset(x = x1, y = y1),
                        end = Offset(x = x1, y = canvasHeight)
                    )
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(
                            "${p1.times(100).roundToInt()}",
                            x1,
                            y1 - 20,
                            textPaint
                        )
                    }
                }

                p2
            }

            with(fillPath) {
                lineTo(canvasWidth, canvasHeight)
                lineTo(0f, canvasHeight)
                lineTo(startX, startY)
            }

            drawPath(path = fillPath, brush = gradient, style = Fill)

            drawPath(path = linePath, color = Color.Gray, style = Stroke(width = 5f))

            val pointAverage = points.average().toFloat()
            val avgY = canvasHeight - (canvasHeight * pointAverage)

            // average line and text
            if (avgLine) {
                drawLine(color = Color.Red, alpha = .33f, strokeWidth = 5f, start = Offset(x = 0f, y = avgY), end = Offset(x = canvasWidth, y = avgY))
                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        "${pointAverage.times(100).roundToInt()}",
                        0f,
                        avgY - 5,
                        textPaint
                    )
                }
            }
        }
    }
}

fun generateSomeGraphPoints(number: Int = 50, variance: Int = 7) =
    (1..number).runningFold(Random.nextFloat()) { previous, _ ->
        val previousInt = (previous * 100).toInt()

        val limitLow = (previousInt - variance).coerceIn(1, 100)
        val limitHigh = (previousInt + variance).coerceIn(1, 100)

        Random.nextInt(limitLow..limitHigh) / 100f
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun GraphPreview() {
    Graph(modifier = Modifier.fillMaxWidth().height(300.dp), points = generateSomeGraphPoints())
}