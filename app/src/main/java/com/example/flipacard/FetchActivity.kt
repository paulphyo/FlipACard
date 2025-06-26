package com.example.flipacard

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.flipacard.databinding.ActivityFetchBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class FetchActivity : AppCompatActivity() {

    private var _binding: ActivityFetchBinding? = null
    private val binding get() = _binding!!

    private val bitmapList = mutableListOf<Bitmap>()
    private lateinit var adapter: ImageAdapter

    private var downloadJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val IMAGE_COUNT = 20
        private const val TAG = "FetchActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _binding = ActivityFetchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = ImageAdapter(List(IMAGE_COUNT) { null }).apply {
            onSelectionChanged = { count ->
                binding.startGameButton.isEnabled = (count == 6)
            }
        }


        binding.imageRecyclerView.apply {
            layoutManager = GridLayoutManager(this@FetchActivity, 4)
            adapter = this@FetchActivity.adapter
        }

        binding.fetchButton.setOnClickListener {
            val url = binding.urlEditText.text.toString().trim()
            if (url.isBlank()) {
                showToast("Please enter a URL")
                return@setOnClickListener
            }

            extractImageUrlsFromHtml(url) { urls ->
                startImageFetchFromUrls(urls)
            }
        }

        binding.startGameButton.setOnClickListener {
            val selectedImages = adapter.getSelectedImages()
            if (selectedImages.size == 6) {
                // Save Bitmaps temporarily
                val files = selectedImages.mapIndexed { index, bmp ->
                    val file = File(cacheDir, "selected_$index.jpg")
                    file.outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 100, out) }
                    file.absolutePath
                }

                val intent = Intent(this, PlayActivity::class.java).apply {
                    putStringArrayListExtra("image_paths", ArrayList(files))
                }
                startActivity(intent)
            }
        }
    }

    private fun startImageFetchFromUrls(urls: List<String>) {

        // Cancel previous job if running
        downloadJob?.cancel()

        bitmapList.clear()
        adapter.resetPlaceholders(IMAGE_COUNT)

        binding.progressContainer.visibility = View.VISIBLE
        binding.progressBar.progress = 0
        binding.progressBar.max = urls.size
        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.progressText.text = "Starting download..."

        binding.startGameButton.visibility = View.GONE

        downloadJob = lifecycleScope.launch {
            for ((i, imageUrl) in urls.withIndex()) {
                if (!isActive) break

                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        val tempFile = File.createTempFile("image_$i", ".jpg", cacheDir)
                        downloadToFileWithUserAgent(imageUrl, tempFile)
                        BitmapFactory.decodeFile(tempFile.absolutePath)
                    }

                    if (bitmap != null) {
                        withContext(Dispatchers.Main) {
                            bitmapList.add(bitmap)
                            adapter.updateImageAt(i, bitmap)

                            binding.progressBar.progress = i + 1
                            binding.progressText.text = "Downloaded ${i + 1} of ${urls.size}"
                            Log.d(TAG, "Bitmap added: index=$i, size=${bitmapList.size}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download $imageUrl", e)
                }
            }

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.progressText.visibility = View.GONE
                binding.progressContainer.visibility = View.GONE

                binding.startGameButton.visibility = View.VISIBLE
            }
        }
    }

    private fun downloadToFileWithUserAgent(url: String, file: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla")
        connection.inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun extractImageUrlsFromHtml(
        pageUrl: String,
        onDone: (List<String>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val imageUrls = mutableListOf<String>()
            try {
                val doc = Jsoup.connect(pageUrl)
                    .userAgent("Mozilla")
                    .get()

                val elements = doc.select("div.photo-grid-item img")
                for (element in elements) {
                    val src = element.attr("src")
                    if (src.isNotBlank() && src.startsWith("http")) {
                        imageUrls.add(src)
                        if (imageUrls.size >= IMAGE_COUNT) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse HTML from $pageUrl", e)
            }

            withContext(Dispatchers.Main) {
                onDone(imageUrls)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
