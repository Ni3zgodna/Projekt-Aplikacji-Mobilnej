package com.example.projekt_10

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.projekt_10.game.*
import kotlin.math.roundToInt
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun GameScreen() {

    val state = remember { GameState() }
    val engine = remember { GameEngine(state) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        val screenWidthPx = maxWidth.value * density.density
        val screenHeightPx = maxHeight.value * density.density
        val circleSizePx = 80.dp.value * density.density

        val imageWidthPx = screenWidthPx
        val imageHeightPx = imageWidthPx * (709f / 500f)

        val overlapPx = 1.dp.value

        val platformHeightPx = 12.dp.value * density.density
        val ballBaseY = (screenHeightPx - circleSizePx) - 300f

        LaunchedEffect(imageHeightPx, overlapPx, screenWidthPx, screenHeightPx, circleSizePx, ballBaseY) {
            engine.startGameLoop(
                imageHeightPx = imageHeightPx,
                overlapPx = overlapPx,

                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,

                circleSizePx = circleSizePx,
                ballBaseY = ballBaseY,
                platformHeightPx = platformHeightPx
            )
        }

        LaunchedEffect(
            screenWidthPx,
            circleSizePx,
            state.restartCounter
        ) {
            if (screenWidthPx > circleSizePx) {
                state.circleX =
                    (screenWidthPx - circleSizePx) / 2f

                state.isCirclePositionInitialized = true
            }
        }

        val worldMotionOffset = state.worldOffsetY - state.jumpOffset
        val tileStep = imageHeightPx - overlapPx

        val wrappedOffset = if (tileStep > 0f) {
            worldMotionOffset % tileStep
        } else {
            worldMotionOffset
        }

        val offsets = calculateBackgroundOffsets(
            screenHeightPx,
            imageHeightPx,
            wrappedOffset,
            overlapPx
        )

        val totalSeconds = (state.gameTimeMillis / 100).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        val formattedTime = "%02d:%02d".format(minutes, seconds)

        Box(modifier = Modifier.fillMaxSize()) {

            offsets.forEach { y ->
                Image(
                    painter = painterResource(id = R.drawable.plik),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, y.roundToInt()) }
                )
            }

            state.platforms
                .filter { !it.isStartingPlatform }
                .forEach { platform ->

                    val renderedPlatformY =
                        platform.baseY + worldMotionOffset

                    if (renderedPlatformY in -platformHeightPx..(screenHeightPx + platformHeightPx)) {

                        Image(
                            painter = painterResource(R.drawable.platform),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        platform.x.roundToInt(),
                                        renderedPlatformY.roundToInt()
                                    )
                                }
                                .width(with(density) { platform.width.toDp() })
                                .height(with(density) { platformHeightPx.toDp() })
                        )
                    }
                }

            state.traps.forEach { trap ->
                val renderedTrapY = trap.baseY + worldMotionOffset

                if (renderedTrapY in -platformHeightPx..(screenHeightPx + platformHeightPx)) {
                    Image(
                        painter = painterResource(id = R.drawable.trap),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    trap.x.roundToInt(),
                                    renderedTrapY.roundToInt()
                                )
                            }
                            .width(with(density) { trap.width.toDp() })
                            .height(with(density) { platformHeightPx.toDp() })
                    )
                }
            }

            state.coins.forEach { coin ->

                val renderedCoinY =
                    coin.baseY + worldMotionOffset

                Image(
                    painter = painterResource(R.drawable.coin),
                    contentDescription = null,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                coin.x.roundToInt(),
                                renderedCoinY.roundToInt()
                            )
                        }
                        .size(with(density) { coin.size.toDp() })
                )
            }

            val playerPainter = painterResource(
                id = if (state.isJumping)
                    R.drawable.player_jump
                else
                    R.drawable.player
            )

            val playerY = if (state.isGameOver) {
                ballBaseY + state.deathFallOffset
            } else {
                ballBaseY
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            state.circleX.roundToInt(),
                            playerY.roundToInt()
                        )
                    }
                    .size(80.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                if (!state.hasStarted) {
                                    state.hasStarted = true
                                }
                            }
                        ) { change, dragAmount ->

                            change.consume()

                            if (dragAmount.x > 2f) {
                                state.facingRight = true
                            }

                            if (dragAmount.x < -2f) {
                                state.facingRight = false
                            }

                            state.circleX = updatePlayerPosition(
                                state.circleX,
                                dragAmount.x,
                                0f,
                                screenWidthPx - circleSizePx
                            )
                        }
                    }
            ) {

                Image(
                    painter = playerPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = if (state.facingRight) 1f else -1f
                        }
                )
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {

                Text(
                    text = formattedTime,
                    color = Color.Black,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Punkty: ${state.score}",
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        state.platforms
            .filter { it.isStartingPlatform }
            .forEach { platform ->

                val renderedPlatformY =
                    platform.baseY + worldMotionOffset

                Image(
                    painter = painterResource(R.drawable.start_platform),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                platform.x.roundToInt(),
                                renderedPlatformY.roundToInt()
                            )
                        }
                        .fillMaxWidth()
                        .aspectRatio(2f)
                )
            }

        if (state.isGameOver) {

            Image(
                painter = painterResource(R.drawable.gameover),
                contentDescription = "Game Over",
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.7f)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            engine.restartGame()
                        }
                    }
            )
        }
    }

}