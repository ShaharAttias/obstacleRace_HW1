package com.example.obstaclerace_hw1

import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GameLogic
    private lateinit var gameLayout: RelativeLayout
    private var laneWidth: Int = 0
    private var screenWidth: Int = 0
    private var lastCollisionTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val playerView = findViewById<ImageView>(R.id.player)
        val leftButton = findViewById<Button>(R.id.leftButton)
        val rightButton = findViewById<Button>(R.id.rightButton)
        val gameOverText = findViewById<TextView>(R.id.gameOverText)
        val restartButton = findViewById<Button>(R.id.restartButton)
        val heart1 = findViewById<ImageView>(R.id.main_IMG_heart0)
        val heart2 = findViewById<ImageView>(R.id.main_IMG_heart1)
        val heart3 = findViewById<ImageView>(R.id.main_IMG_heart2)

        gameLayout = findViewById(R.id.gameLayout)
        viewModel = ViewModelProvider(this)[GameLogic::class.java]

        gameLayout.post {
            val displayMetrics: DisplayMetrics = resources.displayMetrics
            screenWidth = displayMetrics.widthPixels
            laneWidth = screenWidth / 3
            updatePlayerPosition(playerView, viewModel.playerLane.value)
        }

        leftButton.setOnClickListener { viewModel.moveLeft() }
        rightButton.setOnClickListener { viewModel.moveRight() }
        restartButton.setOnClickListener {
            viewModel.restartGame()
            updatePlayerPosition(playerView, 0)
            gameOverText.visibility = View.GONE
            restartButton.visibility = View.GONE
        }

        lifecycleScope.launch {
            viewModel.playerLane.collectLatest { lane ->
                updatePlayerPosition(playerView, lane)
            }
        }

        lifecycleScope.launch {
            viewModel.lives.collectLatest { lives ->
                heart1.visibility = if (lives > 0) View.VISIBLE else View.GONE
                heart2.visibility = if (lives > 1) View.VISIBLE else View.GONE
                heart3.visibility = if (lives > 2) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.gameOver.collectLatest { isOver ->
                gameOverText.visibility = if (isOver) View.VISIBLE else View.GONE
                restartButton.visibility = if (isOver) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.obstacles.collectLatest { obstacleList ->
                // מחיקה חכמה לפי tag
                for (i in gameLayout.childCount - 1 downTo 0) {
                    val child = gameLayout.getChildAt(i)
                    if (child.tag == "obstacle") {
                        gameLayout.removeViewAt(i)
                    }
                }

                val playerX = (playerView.left + playerView.translationX).toInt()
                val playerY = playerView.top
                val playerRect = Rect(
                    playerX,
                    playerY,
                    playerX + playerView.width,
                    playerY + playerView.height
                )

                var hasCollidedInFrame = false

                for (obstacle in obstacleList) {
                    val obstacleView = ImageView(this@MainActivity)
                    obstacleView.tag = "obstacle"
                    obstacleView.setImageResource(R.drawable.meteor)
                    val size = 150
                    val params = RelativeLayout.LayoutParams(size, size)
                    val x = obstacle.lane * laneWidth + (laneWidth - size) / 2
                    val y = obstacle.y.toInt()
                    params.leftMargin = x
                    params.topMargin = y
                    obstacleView.layoutParams = params
                    gameLayout.addView(obstacleView)

                    val obstacleRect = Rect(x, y, x + size, y + size)

                    val currentTime = System.currentTimeMillis()
                    if (!hasCollidedInFrame && Rect.intersects(playerRect, obstacleRect) && currentTime - lastCollisionTime > 500) {
                        hasCollidedInFrame = true
                        lastCollisionTime = currentTime
                        Log.d("DEBUG", "התנגשות!")
                        viewModel.hitPlayer()
                        Toast.makeText(this@MainActivity, "בום!", Toast.LENGTH_SHORT).show()
                        val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
                        vibrator.vibrate(300)
                    }
                }
            }
        }
    }

    private fun updatePlayerPosition(playerView: ImageView, lane: Int) {
        val adjustedX = lane * laneWidth + (laneWidth - playerView.width) / 2f
        playerView.translationX = adjustedX
    }
}



