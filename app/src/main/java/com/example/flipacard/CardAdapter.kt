package com.example.flipacard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class CardAdapter(
    private val cards: List<MemoryCard>,
    private val onCardClick: (Int) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.cardImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun getItemCount(): Int = cards.size

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        if (card.isFaceUp || card.isMatched) {
            holder.imageView.setImageBitmap(card.image)
        } else {
            holder.imageView.setImageResource(R.drawable.card_back) // your placeholder
        }

        holder.itemView.setOnClickListener {
            onCardClick(position)
        }

        holder.itemView.alpha = if (card.isMatched) 0.2f else 1f
    }
}
