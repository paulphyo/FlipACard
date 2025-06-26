package com.example.flipacard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.flipacard.databinding.ActivityWinBinding

class WinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val moves = intent.getIntExtra("moves", 0)
        val time = intent.getIntExtra("time", 0)

        binding.movesText.text = "Moves: $moves"
        binding.timeText.text = "Time: ${time}s"

        binding.playAgainButton.setOnClickListener {
            val intent = Intent(this, FetchActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}