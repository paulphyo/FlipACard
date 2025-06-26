package com.example.flipacard

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.flipacard.databinding.ItemCardBinding

class CardAdapter(
    private val cards: List<MemoryCard>,
    private val onCardClick: (Int) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        val front = binding.cardFront
        val back = binding.cardBack
        private var isAnimating = false

        init {
            binding.root.setOnClickListener {
                if (!isAnimating) {
                    onCardClick(adapterPosition)
                }
            }
        }

        fun bind(card: MemoryCard) {
            // Don't update views if currently animating
            if (isAnimating) return

            if (card.isFaceUp || card.isMatched) {
                front.setImageBitmap(card.image)
                front.visibility = View.VISIBLE
                back.visibility = View.GONE
            } else {
                back.setBackgroundResource(R.drawable.card_back)
                front.visibility = View.GONE
                back.visibility = View.VISIBLE
            }

            binding.root.alpha = if (card.isMatched) 0.3f else 1.0f
        }

        fun animateFlip(card: MemoryCard) {
            if (isAnimating) return
            isAnimating = true

            val scale = binding.root.context.resources.displayMetrics.density
            binding.root.cameraDistance = 8000 * scale

            val flipOut = AnimatorInflater.loadAnimator(binding.root.context, R.animator.flip_out) as AnimatorSet
            val flipIn = AnimatorInflater.loadAnimator(binding.root.context, R.animator.flip_in) as AnimatorSet

            // Determine which view is currently visible based on actual visibility
            val currentlyShowingBack = back.visibility == View.VISIBLE

            if (currentlyShowingBack) {
                // Flipping from back to front (showing the image)
                flipOut.setTarget(back)
                flipIn.setTarget(front)
                // Prepare the front view with the image
                front.setImageBitmap(card.image)
            } else {
                // Flipping from front to back (hiding the image)
                flipOut.setTarget(front)
                flipIn.setTarget(back)
                // Prepare the back view
                back.setBackgroundResource(R.drawable.card_back)
            }

            flipOut.addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}

                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (currentlyShowingBack) {
                        // Show front, hide back
                        back.visibility = View.GONE
                        front.visibility = View.VISIBLE
                    } else {
                        // Show back, hide front
                        front.visibility = View.GONE
                        back.visibility = View.VISIBLE
                    }
                    flipIn.start()
                }

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    isAnimating = false
                }
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })

            flipIn.addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimating = false
                    // Update alpha for matched cards
                    binding.root.alpha = if (card.isMatched) 0.3f else 1.0f
                }

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    isAnimating = false
                }

                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })

            flipOut.start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    override fun getItemCount() = cards.size

    fun animateFlipAtPosition(recyclerView: RecyclerView, position: Int) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? CardViewHolder
        viewHolder?.animateFlip(cards[position])
    }
}