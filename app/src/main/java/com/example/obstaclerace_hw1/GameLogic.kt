package com.example.obstaclerace_hw1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameLogic : ViewModel() {

    private val laneWidth = 280f // רוחב כל נתיב
    private val screenWidth = 840f // רוחב המסך
    private val moveSpeed = 0f // אין תנועה חופשית, השחקן קופץ בין נתיבים

    private val _playerLane = MutableStateFlow(0) // הנתיב שבו השחקן נמצא, התחלנו מהאמצע
    val playerLane = _playerLane.asStateFlow()

    private val _obstacles = MutableStateFlow<List<Obstacle>>(emptyList())
    val obstacles = _obstacles.asStateFlow()

    private val _lives = MutableStateFlow(3)
    val lives = _lives.asStateFlow()

    private val _gameOver = MutableStateFlow(false)
    val gameOver = _gameOver.asStateFlow()

    private val _isCrashed = MutableStateFlow(false)
    val isCrashed: Boolean get() = _isCrashed.value

    private val lastObstacleTimeOnLane = mutableMapOf(0 to 0L, 1 to 0L, 2 to 0L)

    init {
        startGameLoop()
    }

    fun moveLeft() {
        if (_playerLane.value > -1) {
            _playerLane.value = _playerLane.value - 1
        }
    }

    fun moveRight() {
        if (_playerLane.value < 1) {
            _playerLane.value = _playerLane.value + 1
        }
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (_lives.value > 0) {
                delay(50L)

                val movedObstacles = _obstacles.value.map { it.copy(y = it.y + 10f) }
                    .filter { it.y < 2000f }
                    .toMutableList()

                if ((0..100).random() < 2) {
                    val newLane = (0..2).random()
                    val currentTime = System.currentTimeMillis()
                    val lastObstacleTime = lastObstacleTimeOnLane[newLane] ?: 0L
                    if (currentTime - lastObstacleTime > 1000) {
                        movedObstacles.add(Obstacle(newLane, 0f))
                        lastObstacleTimeOnLane[newLane] = currentTime
                    }
                }

                _obstacles.value = movedObstacles

                if (_lives.value <= 0) {
                    _gameOver.value = true
                }
            }
        }
    }

    fun restartGame() {
        _playerLane.value = 1
        _obstacles.value = emptyList()
        _lives.value = 3
        _gameOver.value = false
        _isCrashed.value = false
        startGameLoop()
    }

    fun resetCrashState() {
        _isCrashed.value = false
    }

    fun hitPlayer() {
        if (_lives.value > 0) {
            _lives.value -= 1
            if (_lives.value == 0) {
                _gameOver.value = true
            }
        }
    }
}
