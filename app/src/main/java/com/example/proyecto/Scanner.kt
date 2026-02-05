package com.example.proyecto

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * CLASE: BarcodeAnalyzer
 * Implementa ImageAnalysis.Analyzer de CameraX para procesar el flujo de video.
 * RA7.d: Uso de herramientas externas (Google ML Kit) para añadir funcionalidad avanzada.
 */
class BarcodeAnalyzer(private val onCodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    // Inicializa el cliente de escaneo de Google ML Kit
    private val scanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        // Extraemos la imagen real del buffer de la cámara
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Preparamos la imagen para ML Kit incluyendo la rotación correcta del dispositivo
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // Iniciamos el procesamiento asíncrono de la imagen
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Si detecta códigos, iteramos sobre ellos
                    for (barcode in barcodes) {
                        barcode.rawValue?.let {
                            // Ejecutamos el callback para enviar el código detectado al ViewModel
                            onCodeScanned(it)
                        }
                    }
                }
                .addOnCompleteListener {
                    // RA8.f (Uso de recursos): Es CRÍTICO cerrar el imageProxy.
                    // Si no se cierra, la cámara se bloquea y deja de analizar nuevos fotogramas.
                    imageProxy.close()
                }
        }
    }
}