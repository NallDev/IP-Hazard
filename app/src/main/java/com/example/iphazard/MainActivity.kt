package com.example.iphazard

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.iphazard.databinding.ActivityMainBinding
import com.example.iphazard.ml.Mobilenetv2final
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private var uri: Uri? = null
    private var startTime: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardView.setOnClickListener {
            pickImage()
        }

        binding.btnInput.setOnClickListener {
            pickImage()
        }
    }

    private fun outputGenerator(bitmap: Bitmap) {
        val labels =
            application.assets.open("labels.txt").bufferedReader().use { it.readText() }.split("\n")
        val presentase = arrayListOf<String>()
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val model = Mobilenetv2final.newInstance(this)

        val tbuffer = resizePic(resized)
        val byteBuffer = tbuffer.buffer

        val inputFeature0 =
            TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        for (value in outputFeature0.floatArray) {
            val percentageString = convertToPercentage(value)
            presentase.add(percentageString)
        }

        for (i in labels.indices){
            Log.e("PRESENTASE","${labels[i]} : ${presentase[i]}")
        }

        val max = getMax(outputFeature0.floatArray, labels)


        detailItem(labels[max])
        model.close()
        val endTime : Long = System.currentTimeMillis()
        if (startTime != null){
            val duration = endTime - startTime!!
            binding.clContainer2.visibility = View.VISIBLE
            binding.tvTimeSpend.text = String.format("Kecepatan waktu deteksi : $duration ms")
            startTime = null
        }
        binding.apply {
            clContainer.visibility = View.VISIBLE
            tvTimeSpend.visibility = View.VISIBLE
            tvResult.text = labels[max]
        }
    }

    private fun convertToPercentage(value: Float): String {
        val percentage = (value * 100).toInt()
        return "$percentage%"
    }

    private fun detailItem(label: String) {
        val labell = label.replace("\r", "")
        binding.apply {
            when (labell) {
                "Carcinogenic Tetragenic Mutagenic" -> {
                    ivSymbol.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.ktm))
                }

                "Dangerous For Environment" -> {
                    ivSymbol.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.dangerous))
                }

                "Harmful" -> {
                    ivSymbol.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.berbahaya))
                }

                "Infectious" -> {
                    ivSymbol.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.infeksius))
                }

                "Non hazard"-> {
                    ivSymbol.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.nonhazard))
                }
                "Pressure Gas"-> {
                    ivSymbol.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.gas))
                }
                "Toxic"-> {
                    ivSymbol.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.beracun))
                }
                else -> {
                    Toast.makeText(this@MainActivity, "Tidak terdeteksi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resizePic(image: Bitmap): TensorImage {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(1f, 255f))
            .build()
        var tImage = TensorImage(DataType.FLOAT32)
        tImage.load(image)
        tImage = imageProcessor.process(tImage)
        return tImage
    }

    private fun getMax(arr: FloatArray, list: List<String>): Int {
        var ind = 0
        var min = 0.0f

        for (i in list.indices) {
            Log.d("accuracy", min.toString())
            if (arr[i] > min) {
                min = arr[i]
                ind = i
            }
        }
        return ind
    }

    private fun pickImage() {
        ImagePicker.with(this)
            .provider(ImageProvider.BOTH)
            .crop()
            .createIntent { launcher.launch(it) }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                uri = it.data?.data
                if (uri == null) {
                    Toast.makeText(this, "Upload gagal, silahkan coba lagi", Toast.LENGTH_SHORT)
                        .show()
                }
                try {
                    startTime = System.currentTimeMillis()
                    uri?.let { uri ->
                        val bitmap = MediaStore.Images.Media.getBitmap(
                            contentResolver,
                            uri
                        )

                        Glide.with(this)
                            .load(bitmap)
                            .into(binding.ivOutput)

                        outputGenerator(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
}