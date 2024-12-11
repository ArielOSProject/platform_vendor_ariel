package com.arielos.tensorflow;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;

//import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TFImageClassifier {

    private static final String TAG = "TFImageClassifier";

    private final TensorModel efficientNet4 = new TensorModel(
            "/system_ext/etc/models/ariel_nsfw_efficientnet_lite4_full_integer.tflite",
            300,
            300
    );

    private ImageClassifier imageClassifier;
    private final ImageClassifierSetup setupData = new ImageClassifierSetup(
            efficientNet4,
            0.6f,
            4,
            3,
            Device.CPU
    );

    private final ClassifierListener imageClassifierListener = null;

//    public TFImageClassifier(ImageClassifierSetup setupData, ClassifierListener listener) {
//        this.setupData = setupData;
//        this.imageClassifierListener = listener;
//        setupImageClassifier(setupData);
//    }

    public TFImageClassifier() {
        //this.imageClassifierListener = null;
        setupImageClassifier(setupData);
    }

    public static class ImageClassifierSetup {
        public final TensorModel tensorModel;
        public float threshold;
        public int numThreads;
        public int maxResults;
        public final Device currentDelegate;

        public ImageClassifierSetup(TensorModel tensorModel, float threshold, int numThreads, int maxResults, Device currentDelegate) {
            this.tensorModel = tensorModel;
            this.threshold = threshold;
            this.numThreads = numThreads;
            this.maxResults = maxResults;
            this.currentDelegate = currentDelegate;
        }
    }

    private void setupImageClassifier(ImageClassifierSetup setupData) {
        ImageClassifier.ImageClassifierOptions.Builder optionsBuilder =
                ImageClassifier.ImageClassifierOptions.builder()
                        .setScoreThreshold(setupData.threshold)
                        .setMaxResults(setupData.maxResults)
                        .setLabelAllowList(List.of("nsfw", "neutral"));

        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder().setNumThreads(setupData.numThreads);

        switch (setupData.currentDelegate) {
            case CPU:
                break; // Default
            case GPU:
                // if (new CompatibilityList().isDelegateSupportedOnThisDevice()) {
                //     baseOptionsBuilder.useGpu();
                // } else {
                //     if (imageClassifierListener != null) {
                //         imageClassifierListener.onError("GPU is not supported on this device, fallback to CPU");
                //     }
                // }
                break;
            case NNAPI:
                baseOptionsBuilder.useNnapi();
                break;
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build());

        try {
            File modelFile = new File(setupData.tensorModel.modelPath);
            imageClassifier = ImageClassifier.createFromFileAndOptions(modelFile, optionsBuilder.build());
        } catch (IOException e) {
            if (imageClassifierListener != null) {
                imageClassifierListener.onError("Image classifier failed to initialize. See error logs for details");
            }
            Log.d(TAG, "TFLite failed to load model with error: " + e.getMessage());
        }
    }

    public void classify(ClassifyInput input) {
        if (imageClassifier == null) {
            setupImageClassifier(setupData);
        }

        long inferenceTime = SystemClock.uptimeMillis();
        List<Classifications> results;

        if (input.type == ClassifyType.IMAGE) {
            results = classifyImage(input.images.get(0));
        } else {
            results = new ArrayList<>();
            for (Bitmap bmp : input.images) {
                results.addAll(classifyImage(Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight())));
            }
        }

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;
        Log.d(TAG, "Classification inference: " + inferenceTime);

        if (results != null) {
            for (Classifications classifications : results) {
                classifications.getCategories().forEach(category -> {
                    if ("nsfw".equals(category.getLabel())) {
                        Log.d(TAG, "NSFW with score: " + category.getScore());
                    } else if ("neutral".equals(category.getLabel())) {
                        Log.d(TAG, "NEUTRAL with score: " + category.getScore());
                    }
                });
            }
        }

        if (imageClassifierListener != null) {
            imageClassifierListener.onResults(input, results, inferenceTime);
        }
    }

    public void classifyFrame(Image image) {
        Bitmap bitmap = ImageUtils.convertImageToBitmap(image);
        if (bitmap == null) {
            Log.e(TAG, "Image to bitmap produced null, abort classification");
        } else {
            ClassifyInput input = new ClassifyInput(ClassifyType.IMAGE, List.of(bitmap), "");
            classify(input);
        }
        // if (imageClassifier == null) {
        //     setupImageClassifier(setupData);
        // }

        // long inferenceTime = SystemClock.uptimeMillis();
        // List<Classifications> results;
        // results = classifyImage(image);
        // inferenceTime = SystemClock.uptimeMillis() - inferenceTime;
        // Log.d(TAG, "Classification inference: " + inferenceTime);

        // if (results != null) {
        //     for (Classifications classifications : results) {
        //         classifications.getCategories().forEach(category -> {
        //             if ("nsfw".equals(category.getLabel())) {
        //                 Log.d(TAG, "NSFW with score: " + category.getScore());
        //             } else if ("neutral".equals(category.getLabel())) {
        //                 Log.d(TAG, "NEUTRAL with score: " + category.getScore());
        //             }
        //         });
        //     }
        // }

        // if (imageClassifierListener != null) {
        //     imageClassifierListener.onResults(input, results, inferenceTime);
        // }
    }

    public void classifyBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Image to bitmap produced null, abort classification");
        } else {
            ClassifyInput input = new ClassifyInput(ClassifyType.IMAGE, List.of(bitmap), "");
            classify(input);
        }
    }

    private List<Classifications> classifyImage(Image image) {
        int cropSize = Math.min(image.getWidth(), image.getHeight());

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(new ResizeOp(setupData.tensorModel.inputImageHeight, setupData.tensorModel.inputImageWidth, ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0f, 1f))
                .build();

        TensorImage loadedTensorImage = new TensorImage();
        loadedTensorImage.load(image);
        TensorImage tensorImage = imageProcessor.process(loadedTensorImage);

        ImageProcessingOptions imageProcessingOptions = ImageProcessingOptions.builder().build();

        return imageClassifier != null ? imageClassifier.classify(tensorImage, imageProcessingOptions) : null;
    }

    private List<Classifications> classifyImage(Bitmap image) {
        int cropSize = Math.min(image.getWidth(), image.getHeight());

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(new ResizeOp(setupData.tensorModel.inputImageHeight, setupData.tensorModel.inputImageWidth, ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0f, 1f))
                .build();

        TensorImage tensorImage = imageProcessor.process(TensorImage.fromBitmap(image));

        ImageProcessingOptions imageProcessingOptions = ImageProcessingOptions.builder().build();

        return imageClassifier != null ? imageClassifier.classify(tensorImage, imageProcessingOptions) : null;
    }

    public void close() {
        if (imageClassifier != null) {
            imageClassifier.close();
            imageClassifier = null;
        }
    }

    public interface ClassifierListener {
        void onError(String error);

        void onResults(ClassifyInput input, List<Classifications> results, long inferenceTime);
    }

    public static class ClassifyInput {
        public final ClassifyType type;
        public final List<Bitmap> images;
        public final String filePath;

        public ClassifyInput(ClassifyType type, List<Bitmap> images, String filePath) {
            this.type = type;
            this.images = images;
            this.filePath = filePath;
        }
    }

    public static class TensorModel {
        public final String modelPath;
        public final int inputImageWidth;
        public final int inputImageHeight;

        public TensorModel(String modelPath, int inputImageWidth, int inputImageHeight) {
            this.modelPath = modelPath;
            this.inputImageWidth = inputImageWidth;
            this.inputImageHeight = inputImageHeight;
        }
    }

    public enum ClassifyType {
        IMAGE,
        VIDEO
    }

    public enum Device {
        CPU,
        GPU,
        NNAPI
    }
}
