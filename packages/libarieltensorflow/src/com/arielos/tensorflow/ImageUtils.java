/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.arielos.tensorflow;

import android.graphics.*;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.Image.Plane;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/** Utility class for manipulating images. */
public class ImageUtils {

    public static Bitmap imageToBitmap(Image image) {
        int format = image.getFormat();
        int width = image.getWidth();
        int height = image.getHeight();

        try {
            switch (format) {
                case ImageFormat.YUV_420_888:
                    return yuv420ToBitmap(image);

                case ImageFormat.JPEG:
                    return jpegToBitmap(image);

                case ImageFormat.RGB_565:
                    return rgb565ToBitmap(image);

                case ImageFormat.YUV_422_888:
                case ImageFormat.YUV_444_888:
                case ImageFormat.YV12:
                case ImageFormat.NV21:
                case ImageFormat.NV16:
                case ImageFormat.YUY2:
                case ImageFormat.FLEX_RGB_888:
                case ImageFormat.FLEX_RGBA_8888:
                case ImageFormat.HEIC:
                    // Additional YUV-related formats
                    return yuvToBitmap(image);

                case ImageFormat.DEPTH16:
                case ImageFormat.DEPTH_POINT_CLOUD:
                //case ImageFormat.RAW_DEPTH: // requires hidden api
                case ImageFormat.DEPTH_JPEG:
                    // Depth and specialized formats
                    Log.e("ImageUtils", "Depth formats are not directly supported for Bitmap conversion.");
                    break;

                case ImageFormat.RAW_SENSOR:
                case ImageFormat.RAW_PRIVATE:
                case ImageFormat.RAW10:
                case ImageFormat.RAW12:
                    Log.e("ImageUtils", "RAW formats are not directly supported for Bitmap conversion.");
                    break;

                case ImageFormat.PRIVATE:
                    Log.e("ImageUtils", "PRIVATE format is not accessible for conversion.");
                    break;

                case ImageFormat.UNKNOWN:
                default:
                    Log.e("ImageUtils", "Unsupported or unknown format: " + format);
                    break;
            }
        } catch (Exception e) {
            Log.e("ImageUtils", "Error converting image: " + e.getMessage());
        }
        return null;
    }

    private static Bitmap yuv420ToBitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Extract planes
        Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer(); // Y
        ByteBuffer uBuffer = planes[1].getBuffer(); // U
        ByteBuffer vBuffer = planes[2].getBuffer(); // V

        // Calculate the strides
        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        // Prepare the NV21 byte array
        byte[] nv21 = new byte[width * height * 3 / 2];
        int position = 0;

        // Copy Y data into the NV21 array
        for (int row = 0; row < height; row++) {
            yBuffer.position(row * yRowStride);
            yBuffer.get(nv21, position, width);
            position += width;
        }

        // Interleave U and V data into NV21 array
        int uvHeight = height / 2;
        for (int row = 0; row < uvHeight; row++) {
            uBuffer.position(row * uvRowStride);
            vBuffer.position(row * uvRowStride);
            for (int col = 0; col < width / 2; col++) {
                nv21[position++] = vBuffer.get(); // V
                nv21[position++] = uBuffer.get(); // U
            }
        }

        // Convert NV21 byte array to Bitmap
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] jpegBytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    private static Bitmap jpegToBitmap(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] jpegBytes = new byte[buffer.remaining()];
        buffer.get(jpegBytes);
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }

    private static Bitmap rgb565ToBitmap(Image image) {
        Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        buffer.rewind();

        int width = image.getWidth();
        int height = image.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    private static Bitmap yuvToBitmap(Image image) {
        Log.e("ImageUtils", "YUV format not fully supported: " + image.getFormat());
        return null;
    }

    // CODE FROM TFLITE EXAMPLE APP IN ANDROID STUDIO

    static final int kMaxChannelValue = 262143;

    // todo this should go to separate thread
    public static Bitmap convertImageToBitmap(Image image) {
        byte[][] yuvBytes = new byte[3][];
        int yRowStride;
        int previewWidth = image.getWidth();
        int previewHeight = image.getHeight();
        int[] rgbBytes = new int[previewWidth * previewHeight];

        final Plane[] planes = image.getPlanes();
        fillBytes(planes, yuvBytes);

        yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        convertYUV420ToARGB8888(
                  yuvBytes[0],
                  yuvBytes[1],
                  yuvBytes[2],
                  previewWidth,
                  previewHeight,
                  yRowStride,
                  uvRowStride,
                  uvPixelStride,
                  rgbBytes);
        Bitmap result = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        result.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
        return result;
    }

    private static void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
          final ByteBuffer buffer = planes[i].getBuffer();
          if (yuvBytes[i] == null) {
            yuvBytes[i] = new byte[buffer.capacity()];
          }
          buffer.get(yuvBytes[i]);
        }
      }

    public static void convertYUV420ToARGB8888(
        byte[] yData,
        byte[] uData,
        byte[] vData,
        int width,
        int height,
        int yRowStride,
        int uvRowStride,
        int uvPixelStride,
        int[] out) {
        int yp = 0;
        for (int j = 0; j < height; j++) {
            int pY = yRowStride * j;
            int pUV = uvRowStride * (j >> 1);

            for (int i = 0; i < width; i++) {
                int uv_offset = pUV + (i >> 1) * uvPixelStride;

                out[yp++] = YUV2RGB(0xff & yData[pY + i], 0xff & uData[uv_offset], 0xff & vData[uv_offset]);
            }
        }
    }

    private static int YUV2RGB(int y, int u, int v) {
        // Adjust and check YUV values
        y = (y - 16) < 0 ? 0 : (y - 16);
        u -= 128;
        v -= 128;

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);
        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v - 400 * u);
        int b = (y1192 + 2066 * u);

        // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
        r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
        g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
        b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

        return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
      }

      public static void convertYUV420SPToARGB8888(byte[] input, int width, int height, int[] output) {
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
          int uvp = frameSize + (j >> 1) * width;
          int u = 0;
          int v = 0;

          for (int i = 0; i < width; i++, yp++) {
            int y = 0xff & input[yp];
            if ((i & 1) == 0) {
              v = 0xff & input[uvp++];
              u = 0xff & input[uvp++];
            }

            output[yp] = YUV2RGB(y, u, v);
          }
        }
      }
}
