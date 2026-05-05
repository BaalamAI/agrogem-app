package com.agrogem.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.posix.memcpy

// =============================================================================
// ImagePickerLauncher iOS — usa UIImagePickerController para cámara y galería.
// Guarda la imagen capturada/seleccionada como JPEG en NSTemporaryDirectory()
// y devuelve un ImageResult con uri "file://..." que Gemma puede leer luego.
// =============================================================================

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberImagePickerLauncher(
    onResult: (ImageResult?) -> Unit,
): ImagePickerLauncher {
    // Estado que mantiene viva la referencia al delegate mientras el picker
    // está presentado (UIImagePickerController guarda el delegate como weak).
    val state = remember { ImagePickerState() }

    return remember(onResult) {
        object : ImagePickerLauncher {
            override fun launchCamera() {
                state.present(
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
                    onResult = onResult,
                )
            }

            override fun launchGallery() {
                state.present(
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
                    onResult = onResult,
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class ImagePickerState {
    // Strong reference al delegate. Sin esto, el GC lo libera y crashea.
    private var pickerDelegate: PickerDelegate? = null

    fun present(
        sourceType: UIImagePickerControllerSourceType,
        onResult: (ImageResult?) -> Unit,
    ) {
        if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
            onResult(null)
            return
        }

        val picker = UIImagePickerController()
        picker.sourceType = sourceType
        picker.allowsEditing = false

        val delegate = PickerDelegate(
            onComplete = { image ->
                pickerDelegate = null // ya no necesitamos retenerlo
                if (image == null) {
                    onResult(null)
                } else {
                    onResult(saveImageToTemp(image))
                }
            },
        )
        pickerDelegate = delegate
        picker.delegate = delegate

        val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        root?.presentViewController(picker, animated = true, completion = null)
    }
}

@OptIn(BetaInteropApi::class)
private class PickerDelegate(
    private val onComplete: (UIImage?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true) {
            onComplete(image)
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true) {
            onComplete(null)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun saveImageToTemp(image: UIImage): ImageResult? {
    val data = UIImageJPEGRepresentation(image, 0.85) ?: return null
    val fileName = "agrogem_${NSUUID().UUIDString()}.jpg"
    val path = NSTemporaryDirectory() + fileName
    val written = data.writeToFile(path, atomically = true)
    if (!written) return null
    return ImageResult(
        uri = "file://$path",
        timestamp = (NSDate().timeIntervalSince1970 * 1000.0).toLong(),
        bytes = data.toByteArray(),
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val len = this.length.toInt()
    if (len == 0) return ByteArray(0)
    val out = ByteArray(len)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return out
}
