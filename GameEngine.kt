package com.example.projekt_10.game

import com.example.projekt_10.R
import kotlinx.coroutines.delay
import kotlin.collections.minusAssign
import kotlin.collections.plusAssign
import kotlin.collections.remove
import kotlin.compareTo
import kotlin.inc
import kotlin.random.Random
import kotlin.text.compareTo

class GameEngine(private val state: GameState) {

    private val gravity = 0.6f
    private val jumpForce = -35f
    private val baseWorldSpeed = 3.8f
    private val maxWorldSpeed = 8f
    private val maxPlatformsPerScreen = 7f

    suspend fun startGameLoop(
        imageHeightPx: Float,
        overlapPx: Float,

        screenWidthPx: Float,
        screenHeightPx: Float,

        circleSizePx: Float,
        ballBaseY: Float,
        platformHeightPx: Float
    ) {
        while (true) {

            maintainPlatforms(
                screenWidthPx,
                screenHeightPx,
                circleSizePx,
                ballBaseY,
                imageHeightPx,
                overlapPx
            )

            maintainTraps(
                screenWidthPx,
                screenHeightPx,
                circleSizePx
            )

            maintainCoins(
                screenWidthPx,
                screenHeightPx,
                circleSizePx
            )

            if (state.hasStarted) {
                if (state.gameStartTime == 0L) {
                    state.gameStartTime = System.currentTimeMillis()
                }

                if (!state.isGameOver) {

                    state.gameTimeMillis =
                        System.currentTimeMillis() - state.gameStartTime

                    updateJump()
                    updateBackground(imageHeightPx, overlapPx)

                    handlePlatformLanding(
                        ballBaseY = ballBaseY,
                        circleSizePx = circleSizePx,
                        platformHeightPx = platformHeightPx,
                        screenHeightPx = screenHeightPx
                    )

                    checkTrapCollision(
                        ballBaseY,
                        circleSizePx,
                        platformHeightPx
                    )

                    collectCoins(
                        ballBaseY,
                        circleSizePx
                    )

                } else {
                    updateFallingAfterDeath()
                }

                checkGameOver(
                    ballBaseY,
                    circleSizePx,
                    platformHeightPx
                )


            }

            delay(16)
        }
    }

    private fun handlePlatformLanding(
        ballBaseY: Float,
        circleSizePx: Float,
        platformHeightPx: Float,
        screenHeightPx: Float
    ) {
        if (state.velocityY <= 0f || state.isGameOver) return

        val ballLeft = state.circleX
        val ballRight = state.circleX + circleSizePx
        val ballBottom = ballBaseY + circleSizePx

        val worldMotionOffset = state.worldOffsetY - state.jumpOffset
        val landingTolerance = (state.velocityY + 6f).coerceIn(8f, 26f)


        val landingPlatform = state.platforms.firstOrNull { platform ->
            if (platform.isStartingPlatform && state.hasPassedPlatform) {
                return@firstOrNull false
            }

            val platformTop = platform.baseY + worldMotionOffset
            val platformBottom = platformTop + platformHeightPx

            if (platformTop < -platformHeightPx ||
                platformTop > screenHeightPx + platformHeightPx
            ) {
                return@firstOrNull false
            }

            val horizontalOverlap =
                ballRight > platform.x &&
                        ballLeft < (platform.x + platform.width)

            val touchesFromAbove =
                ballBottom >= (platformTop - landingTolerance) &&
                        ballBottom <= platformBottom

            horizontalOverlap && touchesFromAbove
        } ?: return

        val desiredJumpOffset = landingPlatform.baseY + state.worldOffsetY - ballBottom

        if (!landingPlatform.isStartingPlatform) {
            state.hasPassedPlatform = true
            state.platforms.removeAll { it.isStartingPlatform }
        }

        state.jumpOffset = desiredJumpOffset
        state.velocityY = 0f
        state.isJumping = false
    }

    private fun updateBackground(imageHeightPx: Float, overlapPx: Float) {
        val tileStep = imageHeightPx - overlapPx
        if (tileStep <= 0f) return

        val elapsedSeconds =
            state.gameTimeMillis / 1000f

        val speed =
            (baseWorldSpeed + elapsedSeconds * 0.03f)
                .coerceAtMost(maxWorldSpeed)

        state.worldOffsetY += speed
        state.backgroundOffsetY += speed

        while (state.backgroundOffsetY >= tileStep) {
            state.backgroundOffsetY -= tileStep
        }
        while (state.backgroundOffsetY < 0f) {
            state.backgroundOffsetY += tileStep
        }
    }

    private fun updateJump() {
        if (!state.isJumping) {
            state.velocityY = jumpForce
            state.isJumping = true
        }

        state.velocityY += gravity
        state.jumpOffset += state.velocityY

        if (state.jumpOffset >= 0f && !state.hasPassedPlatform) {
            state.jumpOffset = 0f
            state.velocityY = 0f
            state.isJumping = false
        }
    }

    private fun maintainPlatforms(
        screenWidthPx: Float,
        screenHeightPx: Float,
        circleSizePx: Float,
        ballBaseY: Float,
        imageHeightPx: Float,
        overlapPx: Float
    ) {

        if (!state.hasCreatedStartingPlatform) {

            state.platforms.add(
                Platform(
                    x = 0f,
                    baseY = ballBaseY + circleSizePx,
                    width = screenWidthPx,
                    isStartingPlatform = true
                )
            )

            state.hasCreatedStartingPlatform = true
        } else if (state.hasPassedPlatform) {
            state.platforms.removeAll { it.isStartingPlatform }
        }

        val width = circleSizePx * 1.5f
        val maxX = (screenWidthPx - width).coerceAtLeast(0f)

        if (imageHeightPx - overlapPx <= 0f || screenHeightPx <= 0f) return

        val motionOffset = state.worldOffsetY - state.jumpOffset
        val bufferScreensAbove = 2f

        val spawnTopScreenY = -bufferScreensAbove * screenHeightPx
        val spawnTopWorldY = spawnTopScreenY - motionOffset

        state.platforms.removeAll { platform ->

            val platformScreenY = platform.baseY + motionOffset

            if (platformScreenY > screenHeightPx + 300f) {
                println("REMOVE platform screenY=$platformScreenY")
                true
            } else {
                false
            }
        }
        val baseSpacing = screenHeightPx / maxPlatformsPerScreen

        val minPlatformSpacing = baseSpacing * 1.05f
        val maxPlatformSpacing = baseSpacing * 1.45f

        val topExistingPlatform = state.platforms.minOfOrNull { it.baseY }

        if (state.nextPlatformSpawnBaseY.isNaN()) {

            state.nextPlatformSpawnBaseY =
                (topExistingPlatform ?: (ballBaseY - baseSpacing))

        } else if (
            topExistingPlatform != null &&
            state.nextPlatformSpawnBaseY > topExistingPlatform
        ) {

            state.nextPlatformSpawnBaseY = topExistingPlatform
        }

        while (state.nextPlatformSpawnBaseY > spawnTopWorldY) {

            val randomX =
                if (maxX == 0f) 0f
                else Random.nextFloat() * maxX

            val spacing =
                Random.nextFloat() *
                        (maxPlatformSpacing - minPlatformSpacing) +
                        minPlatformSpacing

            state.platforms.add(
                Platform(
                    x = randomX,
                    baseY = state.nextPlatformSpawnBaseY,
                    width = width
                )
            )

            state.nextPlatformSpawnBaseY -= spacing
        }
    }

    private fun maintainTraps(
        screenWidthPx: Float,
        screenHeightPx: Float,
        circleSizePx: Float
    ) {
        val width = circleSizePx * 1.2f
        val maxX = (screenWidthPx - width).coerceAtLeast(0f)

        val motionOffset = state.worldOffsetY - state.jumpOffset
        val bufferScreensAbove = 2f

        val spawnTopScreenY = -bufferScreensAbove * screenHeightPx
        val spawnTopWorldY = spawnTopScreenY - motionOffset

        state.traps.removeAll { trap ->
            val trapScreenY = trap.baseY + motionOffset
            trapScreenY > screenHeightPx + 300f
        }

        val spacing = screenHeightPx * 2.5f

        if (state.nextTrapSpawnBaseY.isNaN()) {
            state.nextTrapSpawnBaseY = -spacing
        }

        while (state.nextTrapSpawnBaseY > spawnTopWorldY) {
            val randomX = if (maxX == 0f) 0f else Random.nextFloat() * maxX

            state.traps.add(
                Trap(
                    x = randomX,
                    baseY = state.nextTrapSpawnBaseY,
                    width = width
                )
            )

            state.nextTrapSpawnBaseY -= spacing
        }
    }

    private fun checkTrapCollision(
        ballBaseY: Float,
        circleSizePx: Float,
        platformHeightPx: Float
    ) {
        if (state.velocityY <= 0f || state.isGameOver) return

        val ballLeft = state.circleX
        val ballRight = state.circleX + circleSizePx
        val ballBottom = ballBaseY + circleSizePx

        val motionOffset = state.worldOffsetY - state.jumpOffset
        val landingTolerance = (state.velocityY + 6f).coerceIn(8f, 26f)

        val hitTrap = state.traps.any { trap ->
            val trapTop = trap.baseY + motionOffset
            val trapBottom = trapTop + platformHeightPx

            val horizontalOverlap =
                ballRight > trap.x && ballLeft < trap.x + trap.width

            val touchesFromAbove =
                ballBottom >= (trapTop - landingTolerance) &&
                        ballBottom <= trapBottom

            horizontalOverlap && touchesFromAbove
        }

        if (hitTrap) {
            state.isGameOver = true
        }
    }

    private fun checkGameOver(
        ballBaseY: Float,
        circleSizePx: Float,
        platformHeightPx: Float
    ) {
        if (state.isGameOver) return
        if (!state.hasPassedPlatform) return

        val ballBottom = ballBaseY + circleSizePx + state.jumpOffset
        val motionOffset = state.worldOffsetY - state.jumpOffset

        val lowestPlatformBottom = state.platforms
            .filter { !it.isStartingPlatform }
            .maxOfOrNull { it.baseY + motionOffset + platformHeightPx }
            ?: return

        if (ballBottom > lowestPlatformBottom + 30f) {
            state.isGameOver = true
        }
    }

    private fun updateFallingAfterDeath() {
        state.velocityY += gravity * 1.2f
        state.deathFallOffset += state.velocityY
    }

    private fun maintainCoins(
        screenWidthPx: Float,
        screenHeightPx: Float,
        circleSizePx: Float
    ) {

        val coinSize = circleSizePx * 0.6f
        val maxX = (screenWidthPx - coinSize).coerceAtLeast(0f)

        val motionOffset = state.worldOffsetY - state.jumpOffset

        val spawnTopScreenY = -2f * screenHeightPx
        val spawnTopWorldY = spawnTopScreenY - motionOffset

        state.coins.removeAll { coin ->
            coin.baseY + motionOffset > screenHeightPx + 300f
        }

        val minSpacing = screenHeightPx * 0.25f
        val maxSpacing = screenHeightPx * 0.6f

        val spacing =
            Random.nextFloat() * (maxSpacing - minSpacing) + minSpacing

        if (state.nextCoinSpawnBaseY.isNaN()) {
            state.nextCoinSpawnBaseY = -spacing
        }

        while (state.nextCoinSpawnBaseY > spawnTopWorldY) {

            state.coins.add(
                Coin(
                    x = Random.nextFloat() * maxX,
                    baseY = state.nextCoinSpawnBaseY,
                    size = coinSize
                )
            )

            state.nextCoinSpawnBaseY -= spacing
        }
    }

    private fun collectCoins(
        ballBaseY: Float,
        circleSizePx: Float
    ) {

        val ballLeft = state.circleX
        val ballRight = state.circleX + circleSizePx
        val ballTop = ballBaseY
        val ballBottom = ballBaseY + circleSizePx

        val motionOffset = state.worldOffsetY - state.jumpOffset

        val iterator = state.coins.iterator()

        while (iterator.hasNext()) {

            val coin = iterator.next()

            val coinLeft = coin.x
            val coinRight = coin.x + coin.size

            val coinTop = coin.baseY + motionOffset
            val coinBottom = coinTop + coin.size

            val collision =
                ballLeft < coinRight &&
                        ballRight > coinLeft &&
                        ballTop < coinBottom &&
                        ballBottom > coinTop

            if (collision) {
                iterator.remove()

                state.score += 10
                state.collectedCoins++
            }
        }
    }

    fun restartGame() {

        state.platforms.clear()
        state.traps.clear()
        state.coins.clear()

        state.circleX = 0f
        state.isCirclePositionInitialized = false

        state.backgroundOffsetY = 0f
        state.worldOffsetY = 0f

        state.nextPlatformSpawnBaseY = Float.NaN
        state.nextTrapSpawnBaseY = Float.NaN
        state.nextCoinSpawnBaseY = Float.NaN

        state.jumpOffset = 0f
        state.velocityY = 0f
        state.isJumping = false

        state.hasStarted = false
        state.isGameOver = false
        state.deathFallOffset = 0f

        state.hasPassedPlatform = false
        state.hasCreatedStartingPlatform = false

        state.gameTimeMillis = 0L
        state.gameStartTime = 0L

        state.score = 0
        state.collectedCoins = 0

        state.facingRight = true

        state.restartCounter++
    }
}

