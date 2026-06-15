package com.example.projekt_10.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class Trap(
    val x: Float,
    val baseY: Float,
    val width: Float
)
data class Platform(
    val x: Float,
    val baseY: Float,
    val width: Float,
    val isStartingPlatform: Boolean = false
)

data class Coin(
    val x: Float,
    val baseY: Float,
    val size: Float
)

data class BackgroundObject(
    val drawableId: Int,
    val x: Float,
    val baseY: Float,
    val size: Float,
    val speedMultiplier: Float
)

class GameState {
    var circleX by mutableStateOf(0f)
    var isCirclePositionInitialized by mutableStateOf(false)
    val platforms = mutableStateListOf<Platform>()
    var backgroundOffsetY by mutableStateOf(0f)
    var worldOffsetY by mutableStateOf(0f)
    var nextPlatformSpawnBaseY by mutableStateOf(Float.NaN)
    var jumpOffset by mutableStateOf(0f)
    var velocityY by mutableStateOf(0f)
    var isJumping by mutableStateOf(false)
    var hasStarted by mutableStateOf(false)
    var isGameOver by mutableStateOf(false)
    var deathFallOffset by mutableStateOf(0f)
    var hasPassedPlatform by mutableStateOf(false)
    var gameTimeMillis by mutableLongStateOf(0L)
    var gameStartTime by mutableLongStateOf(0L)
    var facingRight by mutableStateOf(true)
    val traps = mutableStateListOf<Trap>()
    var nextTrapSpawnBaseY by mutableStateOf(Float.NaN)
    val coins = mutableStateListOf<Coin>()
    var nextCoinSpawnBaseY by mutableFloatStateOf(Float.NaN)
    var score by mutableIntStateOf(0)
    var collectedCoins by mutableIntStateOf(0)
    var hasCreatedStartingPlatform by mutableStateOf(false)
    var restartCounter by mutableIntStateOf(0)
}