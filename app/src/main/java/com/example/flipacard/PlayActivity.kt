package com.example.flipacard

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.flipacard.databinding.ActivityPlayBinding
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
    var elapsedSeconds = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val paths = intent.getStringArrayListExtra("image_paths") ?: emptyList()

        val originalBitmaps = paths.map { BitmapFactory.decodeFile(it) }
        val allBitmaps = (originalBitmaps + originalBitmaps).shuffled()

        cardList.clear()
        cardList.addAll(allBitmaps.mapIndexed { idx, bmp ->
            MemoryCard(id = idx, image = bmp)
        })

        cardAdapter = CardAdapter(cardList) { index -> onCardClicked(index) }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@PlayActivity, 3)
            adapter = cardAdapter
        }

        startTimer()
    }

    private fun onCardClicked(index: Int) {
        val clicked = cardList[index]
        if (clicked.isFaceUp || clicked.isMatched) return

        clicked.isFaceUp = true
        cardAdapter.notifyItemChanged(index)

        if (flippedIndex == null) {
            flippedIndex = index
        } else {
            val prev = flippedIndex!!
            val prevCard = cardList[prev]
            moveCount++
            binding.moveCounter.text = "Moves: $moveCount"

            if (prevCard.image.sameAs(clicked.image)) {
                prevCard.isMatched = true
                clicked.isMatched = true
                matchedPairs++
                binding.statusText.text = "✅ Match!"

                if (matchedPairs == 6) {
                    lifecycleScope.launch {
                        delay(600)
                        goToWinScreen()
                    }
                }
            } else {
                binding.statusText.text = "❌ Not a match"
                lifecycleScope.launch {
                    delay(600)
                    prevCard.isFaceUp = false
                    clicked.isFaceUp = false
                    cardAdapter.notifyItemChanged(prev)
                    cardAdapter.notifyItemChanged(index)
                    binding.statusText.text = "Find all the pairs!"
                }
            }
            flippedIndex = null
        }
    }

    private fun startTimer() {
        lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                elapsedSeconds++
                binding.gameTimer.text = "Time: ${elapsedSeconds}s"
            }
        }
    }

    private fun goToWinScreen() {
        val intent = Intent(this, WinActivity::class.java)
        intent.putExtra("moves", moveCount)
        intent.putExtra("time", elapsedSeconds)
        startActivity(intent)
        finish()
    }

}
