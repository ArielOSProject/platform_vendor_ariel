package com.arielos.tensorflow

/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context
import android.graphics.Bitmap
import android.media.Image;
import android.os.SystemClock
import android.util.Log;
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.File;

class TFImageClassifier(
    private val setupData: ImageClassifierSetup = TFImageClassifier.ImageClassifierSetup(
                tensorModel = EfficientNetLite4Model,
                threshold = 0.6f
            ),
    val imageClassifierListener: ClassifierListener? = null
) {

    private val TAG = "TFImageClassifier";

    private var imageClassifier: ImageClassifier? = null

    data class ImageClassifierSetup(
        val tensorModel: TensorModel,
        var threshold: Float = 0.6f, // if classification score is higher than 0.6, report it back
        var numThreads: Int = 4,
        var maxResults: Int = 3,
        val currentDelegate: Device = Device.CPU
    )

    init {
        setupImageClassifier(setupData)
    }

    private fun setupImageClassifier(setupData: ImageClassifierSetup) {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(setupData.threshold)
            .setMaxResults(setupData.maxResults)
            .setLabelAllowList(listOf("nsfw", "neutral"))

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(setupData.numThreads)

        when (setupData.currentDelegate) {
            Device.CPU -> {
                // Default
            }
            Device.GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    imageClassifierListener?.onError("GPU is not supported on this device, " +
                            "fallback to CPU")
                }
            }
            Device.NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            val modelFile = File(setupData.tensorModel.modelPath)
            imageClassifier =
                ImageClassifier.createFromFileAndOptions(modelFile, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            imageClassifierListener?.onError(
                "Image classifier failed to initialize. See error logs for details"
            )
            Log.d(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    fun classify(input: ClassifyInput) {
        if (imageClassifier == null) {
            setupImageClassifier(setupData)
        }
        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()
        val results = if (input.type == ClassifyType.IMAGE) {
            // image type contains only one image
            classifyImage(input.images[0])
        } else {
            input.images.flatMap {bmp ->
                classifyImage(bmp.copy(Bitmap.Config.ARGB_8888, true)) as List<Classifications>
            }
        }

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        Log.d(TAG, "Classification inference: $inferenceTime")
        results?.forEach { classifications ->
            classifications.categories.forEach {
                if (it.label == "nsfw") {
                    Log.d(TAG, "NSFW with score: ${it.score}")
                } else if (it.label == "neutral") {
                    Log.d(TAG, "NEUTRAL with score: ${it.score}")
                }
            }
        }
        imageClassifierListener?.onResults(
            input,
            results,
            inferenceTime
        )
    }

    fun classifyFrame(image: Image) {
        val bitmap = ImageUtils.imageToBitmap(image);
        val results = classify(TFImageClassifier.ClassifyInput(
                        TFImageClassifier.ClassifyType.IMAGE,
                        listOf(bitmap),
                        ""
                    ));
    }

    private fun classifyImage(image: Bitmap): List<Classifications>? {
        val cropSize = image.width.coerceAtMost(image.height)

        // Create preprocessor for the image.
        val imageProcessor =
            ImageProcessor.Builder()
                .add(ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(ResizeOp(setupData.tensorModel.inputImageHeight,
                    setupData.tensorModel.inputImageWidth, ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 1f))
                .build()

        // Preprocess the image and convert it into a TensorImage for classification.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val imageProcessingOptions = ImageProcessingOptions.builder()
            .build()

        return imageClassifier?.classify(tensorImage, imageProcessingOptions)
    }

    fun close() {
        imageClassifier?.close()
        imageClassifier = null
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(
            input: ClassifyInput,
            results: List<Classifications>?,
            inferenceTime: Long
        )
    }

    data class ClassifyInput(
        val type: ClassifyType,
        val images: List<Bitmap>,
        val filePath: String
    )

    data class TensorModel(
        val modelPath: String,
        val inputImageWidth: Int,
        val inputImageHeight: Int
    )

    enum class ClassifyType {
        IMAGE,
        VIDEO
    }

    companion object {
        val EfficientNetLite2Model = TensorModel(
            "ariel_nsfw_efficientnet_lite2_full_integer.tflite",
            260,
            260
        )
        val EfficientNetLite4Model = TensorModel(
            "/system_ext/etc/models/ariel_nsfw_efficientnet_lite4_full_integer.tflite",
            300,
            300
        )
        const val INCEPTION_V3_MODEL = "ariel_nsfw_inception_v3_full_integer.tflite"

        /**
         * Minimal number of frames from a video that are classified as NSFW for the whole
         * video to be declared NSFW
         */
        const val MIN_NSFW_FRAMES_PER_VIDEO = 3
    }

    enum class Device {
        CPU,
        GPU,
        NNAPI
    }

}
