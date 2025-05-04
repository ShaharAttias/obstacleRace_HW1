package com.example.obstaclerace_hw1

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GameLogic

    private lateinit var hearts: List<ImageView>
    private lateinit var gameOverText: TextView
    private lateinit var restartButton: Button
    private lateinit var playerPositions: List<ImageView>
    private lateinit var obstacleMatrix: List<List<ImageView>>

    private var previousGrid: List<List<Boolean>> = List(14) { List(3) { false } } // מצב קודם של המכשולים

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[GameLogic::class.java]

        hearts = listOf(
            findViewById(R.id.main_IMG_heart0),
            findViewById(R.id.main_IMG_heart1),
            findViewById(R.id.main_IMG_heart2)
        )

        gameOverText = findViewById(R.id.gameOverText)
        restartButton = findViewById(R.id.restartButton)
        val leftButton: Button = findViewById(R.id.leftButton)
        val rightButton: Button = findViewById(R.id.rightButton)

        playerPositions = listOf(
            findViewById(R.id.player_lane_0),
            findViewById(R.id.player_lane_1),
            findViewById(R.id.player_lane_2)
        )

        obstacleMatrix = List(14) { row ->
            List(3) { col ->
                findViewById(
                    resources.getIdentifier(
                        "obstacle_${row}_${col}",
                        "id",
                        packageName
                    )
                )
            }
        }

        leftButton.setOnClickListener { viewModel.moveLeft() }
        rightButton.setOnClickListener { viewModel.moveRight() }
        restartButton.setOnClickListener { viewModel.restartGame() }

        lifecycleScope.launch {
            viewModel.playerLane.collectLatest { lane ->
                updatePlayerPosition(lane)
            }
        }

        lifecycleScope.launch {
            viewModel.lives.collectLatest { lives ->
                updateHearts(lives)
            }
        }

        lifecycleScope.launch {
            viewModel.gameOver.collectLatest { isOver ->
                gameOverText.visibility = if (isOver) View.VISIBLE else View.GONE
                restartButton.visibility = if (isOver) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            var lastLives = viewModel.lives.value
            viewModel.obstacleGrid.collectLatest { grid ->
                updateObstaclesSmoothly(grid)
                val currentLives = viewModel.lives.value
                if (currentLives < lastLives) {
                    Toast.makeText(this@MainActivity, "בום!", Toast.LENGTH_SHORT).show()
                    val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        vibrator.vibrate(300)
                    }
                    lastLives = currentLives
                }
            }
        }
    }

    private fun updatePlayerPosition(lane: Int) {
        playerPositions.forEachIndexed { index, imageView ->
            imageView.visibility = if (index == lane) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun updateHearts(lives: Int) {
        hearts.forEachIndexed { index, imageView ->
            imageView.visibility = if (lives > index) View.VISIBLE else View.GONE
        }
    }

    private fun updateObstaclesSmoothly(grid: List<List<Boolean>>) {
        for (row in 0 until 14) {
            for (col in 0 until 3) {
                val view = obstacleMatrix[row][col]
                val current = grid[row][col]
                val previous = previousGrid.getOrNull(row - 1)?.getOrNull(col) ?: false
                val wasHere = previousGrid[row][col]

                if (current) {
                    view.visibility = View.VISIBLE
                    val shouldAnimate = (row > 0 && previous && !wasHere) ||
                            (row == 0 && current && !wasHere)

                    if (shouldAnimate) {
                        val fromAbove = (row == 0)
                        startObstacleFallAnimation(view, fromAbove)
                    }
                } else {
                    view.clearAnimation()
                    view.visibility = View.INVISIBLE
                }
            }
        }

        previousGrid = grid.map { it.toList() }
    }

    private fun startObstacleFallAnimation(view: ImageView, fromAbove: Boolean = false) {
        view.clearAnimation()

        val height = view.height.toFloat()
        val fromY = if (fromAbove) -height else 0f
        val toY = if (fromAbove) 0f else height * 1.1f

        val animation = TranslateAnimation(0f, 0f, fromY, toY).apply {
            duration = 300L
            interpolator = LinearInterpolator()
            fillAfter = true
        }

        view.startAnimation(animation)
    }
}
