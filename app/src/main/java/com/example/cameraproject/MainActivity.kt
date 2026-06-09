package com.example.cameraproject

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView

    private lateinit var photoButton: Button
    private lateinit var videoButton: Button
    private lateinit var flashlightButton: Button
    private lateinit var switchButton: Button
    private lateinit var filterButton: Button

    private lateinit var filterOverlay: View

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var camera: Camera? = null

    private var cameraSelector =
        CameraSelector.DEFAULT_BACK_CAMERA

    private var flashlightOn = false

    private var filterMode = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)

        photoButton = findViewById(R.id.photoButton)
        videoButton = findViewById(R.id.videoButton)
        flashlightButton = findViewById(R.id.torchButton)
        switchButton = findViewById(R.id.switchButton)

        filterButton = findViewById(R.id.filterButton)
        filterOverlay = findViewById(R.id.filterOverlay)

        if (hasPermissions()) {

            startCamera()

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ),
                100
            )
        }

        photoButton.setOnClickListener {

            takePhoto()
        }

        videoButton.setOnClickListener {

            if (recording == null) {

                startRecording()

            } else {

                stopRecording()
            }
        }

        flashlightButton.setOnClickListener {

            val currentCamera = camera

            if (currentCamera == null) {

                Toast.makeText(
                    this,
                    "Camera not ready",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (!currentCamera.cameraInfo.hasFlashUnit()) {

                Toast.makeText(
                    this,
                    "No flashlight available",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            flashlightOn = !flashlightOn

            currentCamera.cameraControl.enableTorch(
                flashlightOn
            )

            if (flashlightOn) {

                flashlightButton.text =
                    "🔦 Flashlight ON"

            } else {

                flashlightButton.text =
                    "🔦 Flashlight OFF"
            }
        }

        switchButton.setOnClickListener {

            cameraSelector =
                if (
                    cameraSelector ==
                    CameraSelector.DEFAULT_BACK_CAMERA
                ) {

                    CameraSelector.DEFAULT_FRONT_CAMERA

                } else {

                    CameraSelector.DEFAULT_BACK_CAMERA
                }

            flashlightOn = false

            startCamera()
        }

        filterButton.setOnClickListener {

            filterMode++

            if (filterMode > 3) {

                filterMode = 0
            }

            when (filterMode) {

                0 -> {

                    filterOverlay.setBackgroundColor(
                        Color.TRANSPARENT
                    )

                    filterButton.text =
                        "🎨 Filter: Normal"
                }

                1 -> {

                    filterOverlay.setBackgroundColor(
                        Color.parseColor("#33FF8800")
                    )

                    filterButton.text =
                        "🎨 Filter: Warm"
                }

                2 -> {

                    filterOverlay.setBackgroundColor(
                        Color.parseColor("#3300AAFF")
                    )

                    filterButton.text =
                        "🎨 Filter: Cool"
                }

                3 -> {

                    filterOverlay.setBackgroundColor(
                        Color.parseColor("#66000000")
                    )

                    filterButton.text =
                        "🎨 Filter: Dark"
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == 100 &&
            grantResults.isNotEmpty()
        ) {

            startCamera()
        }
    }

    private fun hasPermissions(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            val preview =
                Preview.Builder().build()

            preview.setSurfaceProvider(
                previewView.surfaceProvider
            )

            imageCapture =
                ImageCapture.Builder().build()

            val recorder =
                Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(Quality.HD)
                    )
                    .build()

            videoCapture =
                VideoCapture.withOutput(recorder)

            cameraProvider.unbindAll()

            camera =
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {

        val imageCapture = imageCapture ?: return

        val name =
            "Photo_${System.currentTimeMillis()}"

        val contentValues =
            ContentValues().apply {

                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    name
                )

                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/jpeg"
                )
            }

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),

            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    output: ImageCapture.OutputFileResults
                ) {

                    Toast.makeText(
                        this@MainActivity,
                        "Photo Saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {

                    Toast.makeText(
                        this@MainActivity,
                        "Photo Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun startRecording() {

        val videoCapture =
            videoCapture ?: return

        val name =
            "Video_${System.currentTimeMillis()}"

        val contentValues =
            ContentValues().apply {

                put(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    name
                )

                put(
                    MediaStore.Video.Media.MIME_TYPE,
                    "video/mp4"
                )
            }

        val outputOptions =
            MediaStoreOutputOptions.Builder(
                contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
                .setContentValues(contentValues)
                .build()

        val pendingRecording:
                PendingRecording =

            videoCapture.output.prepareRecording(
                this,
                outputOptions
            )

        recording = pendingRecording.start(
            ContextCompat.getMainExecutor(this)
        ) { event ->

            when (event) {

                is VideoRecordEvent.Start -> {

                    videoButton.text =
                        "⏹ Stop Recording"

                    Toast.makeText(
                        this,
                        "Recording Started",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is VideoRecordEvent.Finalize -> {

                    videoButton.text =
                        "🎥 Start Video"

                    recording = null

                    Toast.makeText(
                        this,
                        "Video Saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun stopRecording() {

        recording?.stop()

        recording = null

        videoButton.text =
            "🎥 Start Video"
    }
}