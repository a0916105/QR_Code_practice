package tw.idv.jew.qr_code_practice.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import tw.idv.jew.qr_code_practice.databinding.FragmentHomeBinding

private const val REQUEST_CAMERA: Int = 40

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    lateinit var result: TextView
    private lateinit var scanner: SurfaceView
    private lateinit var cameraSource: CameraSource
    private lateinit var barcodeDetector: BarcodeDetector

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

        //先獲取相機權限
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        } else {
            //初始化掃描器
            initScanner()
            //偵測後回傳
            resultCallback()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun resultCallback() {
        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {

            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                //獲取資料
                val barcode = detections.detectedItems
                if (barcode.size() > 0) {
                    requireActivity().runOnUiThread {
//                        result.text = barcode.valueAt(0).displayValue
                        val str: String = barcode.valueAt(0).displayValue
                        result.text = str

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
                        }
                    }
                }
            }
        })
    }

    private fun initScanner() {
        scanner = binding.scanner
        scanner.apply {
            //創建Barcode偵測
            barcodeDetector = BarcodeDetector.Builder(requireActivity())
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build()

            //獲取寬高後創建Camera
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    cameraSource = CameraSource.Builder(requireActivity(), barcodeDetector)
                        .setAutoFocusEnabled(true)  //自動對焦
                        .setRequestedPreviewSize(measuredWidth, measuredHeight)
                        .build()
                }
            })

            //camera綁定surfaceView
            holder.addCallback(object : SurfaceHolder.Callback {
                @SuppressLint("MissingPermission")
                override fun surfaceCreated(holder: SurfaceHolder) {
                    cameraSource.start(holder)
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {

                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    cameraSource.stop() //關閉的同時就會關掉相機
                }
            })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (REQUEST_CAMERA == requestCode && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //初始化掃描器
            initScanner()
            //偵測後回傳
            resultCallback()
        }
    }
}