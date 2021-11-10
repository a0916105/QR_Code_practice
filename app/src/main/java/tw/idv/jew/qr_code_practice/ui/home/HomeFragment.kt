package tw.idv.jew.qr_code_practice.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import tw.idv.jew.qr_code_practice.databinding.FragmentHomeBinding
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var result: TextView
    private var scanner: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null

    private val screenAspectRatio: Int
        get() {
            // Get screen metrics used to setup camera for full screen resolution
            val metrics = DisplayMetrics().also { scanner?.display?.getRealMetrics(it) }
            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }

    companion object {
        private const val TAG = "HomeFragment"
        private const val PERMISSION_CAMERA_REQUEST = 1

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        result = binding.result
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            result.text = it
        })

        setupCamera()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupCamera() {
        scanner = binding.scanner
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(HomeViewModel::class.java)
            .processCameraProvider
            .observe(requireActivity()) { provider: ProcessCameraProvider? ->
                cameraProvider = provider
                if (isCameraPermissionGranted()) {
                    bindCameraUseCases()
                } else {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.CAMERA),
                        PERMISSION_CAMERA_REQUEST
                    )
                }
            }
    }

    private fun bindCameraUseCases() {
        bindPreviewUseCase()
        bindAnalyseUseCase()
    }

    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        previewUseCase = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(scanner!!.display.rotation)
            .build()
        previewUseCase!!.setSurfaceProvider(scanner!!.surfaceProvider)

        try {
            cameraProvider!!.bindToLifecycle(
                /* lifecycleOwner= */this,
                cameraSelector!!,
                previewUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    private fun bindAnalyseUseCase() {
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats one by one, e.g.
        // BarcodeScannerOptions.Builder()
        //     .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        //     .build();
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()

        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }

        analysisUseCase = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(scanner!!.display.rotation)
            .build()

        // Initialize our background executor
        val cameraExecutor = Executors.newSingleThreadExecutor()

        analysisUseCase?.setAnalyzer(
            cameraExecutor,
            ImageAnalysis.Analyzer { imageProxy ->
                processImageProxy(barcodeScanner, imageProxy)
            }
        )

        try {
            cameraProvider!!.bindToLifecycle(
                /* lifecycleOwner= */this,
                cameraSelector!!,
                analysisUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.forEach {
                    Log.d(TAG, it.rawValue)
                    requireActivity().runOnUiThread {
                        val str: String = it.displayValue
                        result.text = str
                        val rawStr: String = it.rawValue

                        if (str.contains("http") || str.contains("https")) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("開啟連結")
                                .setMessage("請先確認掃描的QR code來源安全，按下確認後會導向網站")
                                .setCancelable(false)
                                .setPositiveButton(
                                    "確認"
                                ) { dialog, which ->
                                    val intent = Intent()
                                    intent.action = Intent.ACTION_VIEW
                                    intent.data = Uri.parse(str)
                                    startActivity(intent)
                                }
                                .show()
                        } else if (str.substring(0, 4) == "1922") {
                            val intent = Intent()
                            intent.action = Intent.ACTION_SENDTO
                            intent.data = Uri.parse("smsto:1922")
                            intent.putExtra("sms_body", str.substring(5))
                            startActivity(intent)
                        } else if (rawStr.contains("SMSTO:", true)) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("傳送簡訊")
                                .setMessage("請先確認掃描的QR code來源安全，按下確認後會傳送簡訊")
                                .setPositiveButton(
                                    "確認"
                                ) { dialog, which ->
                                    val firstColon = rawStr.indexOf(':')
                                    val secColon = rawStr.indexOf(':', firstColon + 1)
                                    val intent = Intent()
                                    intent.action = Intent.ACTION_SENDTO
                                    intent.data =
                                        Uri.parse(rawStr.substring(0, secColon).toLowerCase())
                                    intent.putExtra("sms_body", rawStr.substring(secColon + 1))
                                    startActivity(intent)
                                }
                                .setNegativeButton("取消") { dialog, which ->
                                    dialog.dismiss()
                                }
                                .show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, it.message ?: it.toString())
            }.addOnCompleteListener {
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                imageProxy.close()
            }
    }

    /**
     *  [androidx.camera.core.ImageAnalysis], [androidx.camera.core.Preview] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_CAMERA_REQUEST) {
            if (isCameraPermissionGranted()) {
                bindCameraUseCases()
            } else {
                Log.e(TAG, "no camera permission")
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}