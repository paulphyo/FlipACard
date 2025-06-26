package com.example.flipacard

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ImageAdapter(initialImages: List<Bitmap?>) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private val imageList = initialImages.toMutableList()
    private val selectedIndices = mutableSetOf<Int>()

    var onSelectionChanged: ((Int) -> Unit)? = null

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)

        init {
            view.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION && imageList[pos] != null) {
                    toggleSelection(pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = imageList.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val bitmap = imageList[position]
        val isSelected = position in selectedIndices
        val hasSelection = selectedIndices.isNotEmpty()

        if (bitmap != null) {
            holder.imageView.setImageBitmap(bitmap)

            holder.itemView.alpha = when {
                isSelected -> 1.0f
                hasSelection -> 0.3f
                else -> 1.0f
            }

            holder.itemView.setBackgroundResource(
                if (isSelected) R.drawable.selected_border else 0
            )
        } else {
            holder.imageView.setImageBitmap(null)
            holder.imageView.setBackgroundColor(getRandomBlueOrPurple())

            holder.itemView.alpha = 1.0f
            holder.itemView.setBackgroundResource(0)
        }
    }


    private fun toggleSelection(position: Int) {
        if (selectedIndices.contains(position)) {
            selectedIndices.remove(position)
        } else {
            if (selectedIndices.size >= 6) return
            selectedIndices.add(position)
        }
        notifyItemChanged(position)
        onSelectionChanged?.invoke(selectedIndices.size)
    }

    fun resetPlaceholders(count: Int) {
        imageList.clear()
        repeat(count) { imageList.add(null) }
        selectedIndices.clear()
        notifyDataSetChanged()
    }

    fun updateImageAt(position: Int, bitmap: Bitmap) {
        if (position in imageList.indices) {
            imageList[position] = bitmap
            notifyItemChanged(position)
        }
    }

    fun clearImages() {
        imageList.clear()
        selectedIndices.clear()
        notifyDataSetChanged()
    }

    fun getImages(): List<Bitmap> = imageList.filterNotNull()

    fun getSelectedImages(): List<Bitmap> {
        return selectedIndices.mapNotNull { imageList[it] }
    }

    private fun getRandomBlueOrPurple(): Int {
        val shades = listOf(
            0xFFBBDEFB.toInt(),
            0xFF2196F3.toInt(),
            0xFF3F51B5.toInt(),
            0xFF9FA8DA.toInt(),
            0xFFE1BEE7.toInt(),
            0xFFBA68C8.toInt(),
            0xFF7E57C2.toInt(),
        )
        return shades.random()
    }
}
