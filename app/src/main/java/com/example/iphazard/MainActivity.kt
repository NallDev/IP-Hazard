package com.example.iphazard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.iphazard.databinding.ActivityMainBinding
import com.example.iphazard.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Berikan Izin Camera", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val image = data?.extras?.get("data") as Bitmap

                Glide.with(this)
                    .load(image)
                    .into(binding.ivCamera)


                outputGenerator(image)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cameraPermission = Manifest.permission.CAMERA

        if (ContextCompat.checkSelfPermission(this, cameraPermission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(cameraPermission)
        }

        binding.ivCamera.setOnClickListener {
            openCamera()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(this, "Berikan Izin Camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(takePictureIntent)
    }

    private fun outputGenerator(bitmap: Bitmap) {
        val labels =
            application.assets.open("labels.txt").bufferedReader().use { it.readText() }.split("\n")

        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val model = Model.newInstance(this)

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resized)
        val tbuffer = tensorImage

//            var tbuffer = TensorImage.fromBitmap(resized)
        val byteBuffer = tbuffer.buffer

        Log.e("shape", tbuffer.buffer.toString())

// Creates inputs for reference.
        val inputFeature0 =
            TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

// Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        Log.e("shape", outputFeature0.floatArray.toString())
        val max = getMax(outputFeature0.floatArray)
//            Log.e("shape", max.toString())
        Log.e("shape", inputFeature0.buffer.toString())

        binding.tvPredict.text = labels[max]

        model.close()
    }

    private fun getMax(arr: FloatArray): Int {
        var ind = 0
        var min = 0.0f

        for (i in 0..8) {
            if (arr[i] > min) {
                min = arr[i]
                ind = i
            }
        }
        return ind
    }
}