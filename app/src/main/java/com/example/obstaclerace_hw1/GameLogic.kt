package com.example.obstaclerace_hw1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameLogic : ViewModel() {

    private val _playerLane = MutableStateFlow(1)
    val playerLane = _playerLane.asStateFlow()

    private val _obstacleGrid = MutableStateFlow(createEmptyGrid())
    val obstacleGrid = _obstacleGrid.asStateFlow()

    private val _lives = MutableStateFlow(3)
    val lives = _lives.asStateFlow()

    private val _gameOver = MutableStateFlow(false)
    val gameOver = _gameOver.asStateFlow()

    init {
        startGameLoop()
    }

    fun moveLeft() {
        if (_playerLane.value > 0) _playerLane.value--
    }

    fun moveRight() {
        if (_playerLane.value < 2) _playerLane.value++
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (_lives.value > 0) {
                delay(300L)
                moveObstaclesDown()
                checkCollision()
                maybeSpawnObstacle()
            }
        }
    }

    private fun moveObstaclesDown() {
        val current = _obstacleGrid.value
        val newGrid = createEmptyGrid()
        for (row in 13 downTo 0) { // שורות 0–13 יורדות לשורות 1–14
            for (col in 0..2) {
                newGrid[row + 1][col] = current[row][col]
            }
        }
        _obstacleGrid.value = newGrid
    }

    private fun maybeSpawnObstacle() {
        val grid = _obstacleGrid.value.map { it.toMutableList() }
        if ((0..100).random() < 20) {
            val lane = (0..2).random()
            grid[0][lane] = true
        }
        _obstacleGrid.value = grid
    }

    private fun checkCollision() {
        val playerCol = _playerLane.value
        if (_obstacleGrid.value[14][playerCol]) { // בדיקה בשורה האחרונה (14)
            hitPlayer()
        }
    }

    fun hitPlayer() {
        if (_lives.value > 0) {
            _lives.value--
            if (_lives.value == 0) _gameOver.value = true
        }
    }

    fun restartGame() {
        _playerLane.value = 1
        _obstacleGrid.value = createEmptyGrid()
        _lives.value = 3
        _gameOver.value = false
        startGameLoop()
    }

    private fun createEmptyGrid(): List<MutableList<Boolean>> {
        return List(15) { MutableList(3) { false } }
    }
}
