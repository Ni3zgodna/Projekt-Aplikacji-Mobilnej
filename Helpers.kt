package com.example.projekt_10.game
import kotlin.math.ceil

fun calculateBackgroundOffsets(
    screenHeight: Float,
    imageHeight: Float,
    offsetY: Float,
    overlap: Float
): List<Float> {

    val tileStep = imageHeight - overlap
    if (tileStep <= 0f) {
        return listOf(screenHeight - imageHeight + offsetY)
    }

    val baseY = screenHeight - imageHeight + offsetY
    val tileCount = ceil(screenHeight / tileStep).toInt() + 3

    return List(tileCount) { index ->
        baseY - (index * tileStep)
    }
}

fun updatePlayerPosition(
    currentX: Float,
    dragAmount: Float,
    minX: Float,
    maxX: Float
): Float {
    val newX = currentX + dragAmount
    return newX.coerceIn(minX, maxX)
}
