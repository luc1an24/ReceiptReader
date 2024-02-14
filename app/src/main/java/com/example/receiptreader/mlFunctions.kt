package com.example.receiptreader

import android.app.Activity
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.storage.StorageMetadata
import java.io.IOException
import java.time.LocalDate
import java.util.*

class MlFunctions {
    private val TAG = "ML"

    private val ORIENTATIONS = SparseIntArray()
    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    fun processTextBlock(result: FirebaseVisionText) : StorageMetadata  {
        val shopName = result.textBlocks.first().lines.first().text
        var totalBuyPrice = 0.0
        var perfectRead = true
        for (block in result.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text
                if(lineText.toLowerCase().contains("eur")){
                    val index = block.lines.indexOf(line)
                    if(block.lines.count() > index+1){
                        val totalPriceLine = block.lines[index+1]!!.text
                        val totalPrice = totalPriceLine.toDoubleOrNull()
                        if (totalPrice != null){
                            totalBuyPrice = totalPrice
                        }
                    }
                }
            }
        }

        if(totalBuyPrice == 0.0 || shopName == ""){
            perfectRead = false
        }
        val metadata = StorageMetadata.Builder()
        //.setCustomMetadata("date", datumNakupa)
        .setCustomMetadata("shopName", shopName)
        .setCustomMetadata("price", totalBuyPrice.toString())
        .setCustomMetadata("readOk", perfectRead.toString())
        .build()

        return metadata
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Throws(CameraAccessException::class)
    fun getRotationCompensation(
        activity: Activity,
        context: Context
    ): Int {
        var camId = getCameraId(context, CameraCharacteristics.LENS_FACING_BACK)
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        val cameraManager =
            context.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(camId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        val result: Int
        when (rotationCompensation) {
            0 -> result = FirebaseVisionImageMetadata.ROTATION_0
            90 -> result = FirebaseVisionImageMetadata.ROTATION_90
            180 -> result = FirebaseVisionImageMetadata.ROTATION_180
            270 -> result = FirebaseVisionImageMetadata.ROTATION_270
            else -> {
                result = FirebaseVisionImageMetadata.ROTATION_0
                Log.e(TAG, "Bad rotation value: $rotationCompensation")
            }
        }
        return result
    }

    private fun getCameraId(context: Context, facing: Int): String {
        val manager = context.getSystemService(CAMERA_SERVICE) as CameraManager

        return manager.cameraIdList.first {
            manager
                .getCameraCharacteristics(it)
                .get(CameraCharacteristics.LENS_FACING) == facing
        }
    }
}