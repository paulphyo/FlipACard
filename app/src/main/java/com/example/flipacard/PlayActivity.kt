package com.example.flipacard

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.flipacard.databinding.ActivityPlayBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayBinding
    private lateinit var cardAdapter: CardAdapter

    private val cardList = mutableListOf<MemoryCard>()
    private var flippedIndex: Int? = null
    private var moveCount = 0
    private var matchedPairs = 0
    private var elapsedSeconds = 0
    private var timerJob: Job? = null

    private val totalPairs = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val paths = intent.getStringArrayListExtra("image_paths") ?: emptyList()
        val originalBitmaps = paths.map { BitmapFactory.decodeFile(it) }

        setupCards(originalBitmaps)
        resetUI()
        startTimer()

        binding.btnRestart.setOnClickListener {
            setupCards(originalBitmaps)
            resetUI()
            resetTimer()
        }
    }

    private fun setupCards(originalBitmaps: List<Bitmap>) {
        val allBitmaps = (originalBitmaps + originalBitmaps).shuffled()
        cardList.clear()
        cardList.addAll(allBitmaps.mapIndexed { idx, bmp -> MemoryCard(id = idx, image = bmp) })

        if (::cardAdapter.isInitialized) {
            cardAdapter.notifyDataSetChanged()
        } else {
            cardAdapter = CardAdapter(cardList) { index -> onCardClicked(index) }
            binding.recyclerView.apply {
                layoutManager = GridLayoutManager(this@PlayActivity, 3)
                adapter = cardAdapter
            }
        }
    }

    private fun onCardClicked(index: Int) {
        val clicked = cardList[index]
        if (clicked.isFaceUp || clicked.isMatched) return

        // Update the card state first
        clicked.isFaceUp = true

        // Then animate
        cardAdapter.animateFlipAtPosition(binding.recyclerView, index)

        if (flippedIndex == null) {
            flippedIndex = index
            return
        }

        val prevIndex = flippedIndex!!
        val prevCard = cardList[prevIndex]
        moveCount++
        updateMoveCount(moveCount)

        if (prevCard.image.sameAs(clicked.image)) {
            prevCard.isMatched = true
            clicked.isMatched = true
            matchedPairs++
            updatePairsFound(matchedPairs, totalPairs)
            updateStatus("✅ Match!")

            // Wait longer for animation to complete and avoid flickering
            binding.recyclerView.postDelayed({
                // Only update alpha for matched cards, don't call notifyItemChanged
                // as it can interfere with the animation state
                cardAdapter.notifyItemChanged(prevIndex)
                cardAdapter.notifyItemChanged(index)

                if (matchedPairs == totalPairs) {
                    goToWinScreen()
                }
            }, 1000) // Increased delay
        } else {
            updateStatus("❌ Not a match")

            lifecycleScope.launch {
                delay(1500) // Give more time to see the images
                prevCard.isFaceUp = false
                clicked.isFaceUp = false

                // Animate both cards back to face down
                cardAdapter.animateFlipAtPosition(binding.recyclerView, prevIndex)
                cardAdapter.animateFlipAtPosition(binding.recyclerView, index)

                updateStatus(getString(R.string.status_find_pairs))
            }
        }

        flippedIndex = null
    }


    private fun startTimer() {
        timerJob = lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                elapsedSeconds++
                updateTimer(elapsedSeconds)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    private fun resetTimer() {
        stopTimer()
        elapsedSeconds = 0
        updateTimer(elapsedSeconds)
        startTimer()
    }

    private fun updateMoveCount(moves: Int) {
        binding.moveCounter.text = getString(R.string.moves, moves)
    }

    private fun updatePairsFound(pairs: Int, total: Int) {
        binding.pairsFound.text = getString(R.string.pairs_found, pairs, total)
    }

    private fun updateStatus(message: String) {
        binding.statusText.text = message
    }

    private fun updateTimer(seconds: Int) {
        binding.gameTimer.text = getString(R.string.time, seconds)
    }

    private fun resetUI() {
        moveCount = 0
        matchedPairs = 0
        flippedIndex = null  // Reset flipped index

        updateMoveCount(moveCount)
        updatePairsFound(matchedPairs, totalPairs)
        updateTimer(elapsedSeconds)
        updateStatus(getString(R.string.status_find_pairs))
    }

    private fun goToWinScreen() {
        stopTimer()  // Stop timer when game ends
        val intent = Intent(this, WinActivity::class.java)
        intent.putExtra("moves", moveCount)
        intent.putExtra("time", elapsedSeconds)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()  // Clean up timer when activity is destroyed
    }
}