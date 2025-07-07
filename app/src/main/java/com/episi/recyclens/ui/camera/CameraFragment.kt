package com.episi.recyclens.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.episi.recyclens.domain.camera.*
import com.episi.recyclens.databinding.FragmentCameraBinding
import com.episi.recyclens.domain.camera.BoundingBox
import com.episi.recyclens.domain.camera.Constants
import com.episi.recyclens.domain.camera.Detector
import com.episi.recyclens.domain.camera.DetectorListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment(), DetectorListener {

    private var imageAnalysis: ImageAnalysis? = null

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels()

    private lateinit var detector: Detector
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var analyzer: CameraAnalyzer
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val isFrontCamera = false // o true si querés la frontal

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()

        detector = Detector(requireContext(), Constants.MODEL_PATH, Constants.LABELS_PATH, this)
        detector.setup()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun setupObservers() {
        viewModel.inferenceTime.observe(viewLifecycleOwner) {
            binding.inferenceTime.text = it
        }

        viewModel.boundingBoxes.observe(viewLifecycleOwner) { boxes ->
            binding.overlay.setResults(boxes)
        }
    }

    private fun startCamera() {
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, CameraAnalyzer(isFrontCamera, detector))
            }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindUseCases() {
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()

        analyzer = CameraAnalyzer(isFrontCamera, detector)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, analyzer)
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(
                if (isFrontCamera) CameraSelector.LENS_FACING_FRONT
                else CameraSelector.LENS_FACING_BACK
            ).build()

        cameraProvider.unbindAll()

        try {
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.CAMERA] == true) {
            startCamera()
        }
    }

    override fun onEmptyDetect() {
        viewModel.clearResults()
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        requireActivity().runOnUiThread {
            viewModel.updateResults(boundingBoxes, inferenceTime)
        }
    }

    override fun onDestroyView() {
        imageAnalysis?.clearAnalyzer()
        cameraExecutor.shutdownNow()
        try {
            cameraExecutor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        detector.clear()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}