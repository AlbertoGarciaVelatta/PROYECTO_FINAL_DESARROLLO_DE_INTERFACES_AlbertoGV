package com.example.proyecto

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/**
 * PANTALLA: ScannerScreen
 * Implementa la vista de cámara en vivo para el escaneo de productos.
 * Utiliza AndroidView para integrar la PreviewView clásica dentro de Compose.
 */
@Composable
fun ScannerScreen(onCodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Obtenemos el proveedor de la cámara (se usa 'remember' para no pedirlo en cada recreación)
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // AndroidView permite usar componentes de la "View" antigua (como PreviewView) en Compose
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Configuración de la Previsualización (lo que ve el usuario)
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // 2. Configuración del Análisis de Imagen (lo que procesa la IA)
                // RA8.f (Uso de recursos): STRATEGY_KEEP_ONLY_LATEST asegura que
                // si el analizador va lento, se descarten los frames viejos para no saturar la RAM.
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, BarcodeAnalyzer { code ->
                            // Ejecuta el callback cuando detecta un código válido
                            onCodeDetected(code)
                        })
                    }

                try {
                    // RA8.e (Seguridad y Estabilidad): Desvinculamos cualquier uso previo
                    // para evitar que la cámara se quede bloqueada.
                    cameraProvider.unbindAll()

                    // Vinculamos la cámara al LifecycleOwner (la pantalla actual).
                    // RA8.f: Esto hace que la cámara se apague automáticamente si el
                    // usuario minimiza la app o recibe una llamada, ahorrando batería.
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA, // Usamos la cámara trasera por defecto
                        preview,
                        analyzer
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)
            previewView // Retornamos la vista de cámara configurada
        },
        modifier = Modifier.fillMaxSize()
    )
}