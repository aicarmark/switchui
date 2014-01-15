/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License
 */
package com.android.providers.contacts;

import com.android.providers.contacts.util.MemoryUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
//MOT MOD BEGIN
import android.graphics.BitmapFactory.Options;
// MOT MOD END
import android.os.SystemProperties;

/**
 * Class that converts a bitmap (or byte array representing a bitmap) into a display
 * photo and a thumbnail photo.
 */
/* package-protected */ final class PhotoProcessor {

    /** Compression for display photos. They are very big, so we can use a strong compression */
    private static final int COMPRESSION_DISPLAY_PHOTO = 75;

    /**
     * Compression for thumbnails that don't have a full size photo. Those can be blown up
     * full-screen, so we want to make sure we don't introduce JPEG artifacts here
     */
    private static final int COMPRESSION_THUMBNAIL_HIGH = 95;

    /** Compression for thumbnails that also have a display photo */
    private static final int COMPRESSION_THUMBNAIL_LOW = 90;

    private static int sMaxThumbnailDim;
    private static int sMaxDisplayPhotoDim;

    static {
        final boolean isExpensiveDevice =
                MemoryUtils.getTotalMemorySize() >= PhotoSizes.LARGE_RAM_THRESHOLD;

        sMaxThumbnailDim = SystemProperties.getInt(
                PhotoSizes.SYS_PROPERTY_THUMBNAIL_SIZE, PhotoSizes.DEFAULT_THUMBNAIL);

        sMaxDisplayPhotoDim = SystemProperties.getInt(
                PhotoSizes.SYS_PROPERTY_DISPLAY_PHOTO_SIZE,
                isExpensiveDevice
                        ? PhotoSizes.DEFAULT_DISPLAY_PHOTO_LARGE_MEMORY
                        : PhotoSizes.DEFAULT_DISPLAY_PHOTO_MEMORY_CONSTRAINED);
    }

    /**
     * The default sizes of a thumbnail/display picture. This is used in {@link #initialize()}
     */
    private interface PhotoSizes {
        /** Size of a thumbnail */
        public static final int DEFAULT_THUMBNAIL = 96;

        /**
         * Size of a display photo on memory constrained devices (those are devices with less than
         * {@link #DEFAULT_LARGE_RAM_THRESHOLD} of reported RAM
         */
        public static final int DEFAULT_DISPLAY_PHOTO_MEMORY_CONSTRAINED = 480;

        /**
         * Size of a display photo on devices with enough ram (those are devices with at least
         * {@link #DEFAULT_LARGE_RAM_THRESHOLD} of reported RAM
         */
        public static final int DEFAULT_DISPLAY_PHOTO_LARGE_MEMORY = 720;

        /**
         * If the device has less than this amount of RAM, it is considered RAM constrained for
         * photos
         */
        public static final int LARGE_RAM_THRESHOLD = 640 * 1024 * 1024;

        /** If present, overrides the size given in {@link #DEFAULT_THUMBNAIL} */
        public static final String SYS_PROPERTY_THUMBNAIL_SIZE = "contacts.thumbnail_size";

        /** If present, overrides the size determined for the display photo */
        public static final String SYS_PROPERTY_DISPLAY_PHOTO_SIZE = "contacts.display_photo_size";
    }



    //MOTO MOD BEGIN
    private int mMaxDisplayPhotoDim;
    private  int mMaxThumbnailPhotoDim;
    private boolean mForceCropToSquare;
    private Bitmap mOriginal;
    private Bitmap mDisplayPhoto;
    private Bitmap mThumbnailPhoto;
    //MOTO MOD END
    /**
     * Initializes a photo processor for the given bitmap.
     * @param original The bitmap to process.
     * @param maxDisplayPhotoDim The maximum height and width for the display photo.
     * @param maxThumbnailPhotoDim The maximum height and width for the thumbnail photo.
     * @throws IOException If bitmap decoding or scaling fails.
     */
    public PhotoProcessor(Bitmap original, int maxDisplayPhotoDim, int maxThumbnailPhotoDim)
            throws IOException {
        this(original, maxDisplayPhotoDim, maxThumbnailPhotoDim, false);
    }

    /**
     * Initializes a photo processor for the given bitmap.
     * @param originalBytes A byte array to decode into a bitmap to process.
     * @param maxDisplayPhotoDim The maximum height and width for the display photo.
     * @param maxThumbnailPhotoDim The maximum height and width for the thumbnail photo.
     * @throws IOException If bitmap decoding or scaling fails.
     */
    //MOTO MOD BEGIN
    public PhotoProcessor(byte[] originalBytes, int maxDisplayPhotoDim, int maxThumbnailPhotoDim)
            throws IOException {
        Options options = new Options();
        options.inPurgeable = true;
        Bitmap bit = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.length, options);
        init(BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.length, options),
                maxDisplayPhotoDim, maxThumbnailPhotoDim, false);
    }
    private void init(Bitmap original, int maxDisplayPhotoDim, int maxThumbnailPhotoDim,
            boolean forceCropToSquare) throws IOException{
        this.mOriginal = original;
        this.mMaxDisplayPhotoDim = maxDisplayPhotoDim;
        this.mMaxThumbnailPhotoDim = maxThumbnailPhotoDim;
        this.mForceCropToSquare = forceCropToSquare;
        process();
    }
    //MOTO MOD END
    /**
     * Initializes a photo processor for the given bitmap.
     * @param original The bitmap to process.
     * @param maxDisplayPhotoDim The maximum height and width for the display photo.
     * @param maxThumbnailPhotoDim The maximum height and width for the thumbnail photo.
     * @param forceCropToSquare Whether to force the processed images to be square.  If the source
     *     photo is not square, this will crop to the square at the center of the image's rectangle.
     *     If this is not set to true, the image will simply be downscaled to fit in the given
     *     dimensions, retaining its original aspect ratio.
     * @throws IOException If bitmap decoding or scaling fails.
     */
    public PhotoProcessor(Bitmap original, int maxDisplayPhotoDim, int maxThumbnailPhotoDim,
            boolean forceCropToSquare) throws IOException {
        mOriginal = original;
        mMaxDisplayPhotoDim = maxDisplayPhotoDim;
        mMaxThumbnailPhotoDim = maxThumbnailPhotoDim;
        mForceCropToSquare = forceCropToSquare;
        process();
    }

    /**
     * Initializes a photo processor for the given bitmap.
     * @param originalBytes A byte array to decode into a bitmap to process.
     * @param maxDisplayPhotoDim The maximum height and width for the display photo.
     * @param maxThumbnailPhotoDim The maximum height and width for the thumbnail photo.
     * @param forceCropToSquare Whether to force the processed images to be square.  If the source
     *     photo is not square, this will crop to the square at the center of the image's rectangle.
     *     If this is not set to true, the image will simply be downscaled to fit in the given
     *     dimensions, retaining its original aspect ratio.
     * @throws IOException If bitmap decoding or scaling fails.
     */
    public PhotoProcessor(byte[] originalBytes, int maxDisplayPhotoDim, int maxThumbnailPhotoDim,
            boolean forceCropToSquare) throws IOException {
        //MOTO MOD BEGIN
        Options options = new Options();
        options.inPurgeable = true;
        Bitmap bit = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.length, options);
        init(bit,
                maxDisplayPhotoDim, maxThumbnailPhotoDim, forceCropToSquare);
        //MOTO MOD END
    }

    /**
     * Processes the original image, producing a scaled-down display photo and thumbnail photo.
     * @throws IOException If bitmap decoding or scaling fails.
     */
    private void process() throws IOException {
        if (mOriginal == null) {
            throw new IOException("Invalid image file");
        }
        mDisplayPhoto = getScaledBitmap(mMaxDisplayPhotoDim);
        mThumbnailPhoto = getScaledBitmap(mMaxThumbnailPhotoDim);
    }

    /**
     * Scales down the original bitmap to fit within the given maximum width and height.
     * If the bitmap already fits in those dimensions, the original bitmap will be
     * returned unmodified unless the photo processor is set up to crop it to a square.
     * @param maxDim Maximum width and height (in pixels) for the image.
     * @return A bitmap that fits the maximum dimensions.
     */
    @SuppressWarnings({"SuspiciousNameCombination"})
    private Bitmap getScaledBitmap(int maxDim) {
        Bitmap scaledBitmap = mOriginal;
        int width = mOriginal.getWidth();
        int height = mOriginal.getHeight();
        int cropLeft = 0;
        int cropTop = 0;
        if (mForceCropToSquare && width != height) {
            // Crop the image to the square at its center.
            if (height > width) {
                cropTop = (height - width) / 2;
                height = width;
            } else {
                cropLeft = (width - height) / 2;
                width = height;
            }
        }
        float scaleFactor = ((float) maxDim) / Math.max(width, height);
        if (scaleFactor < 1.0f || cropLeft != 0 || cropTop != 0) {
            // Need to scale or crop the photo.
            Matrix matrix = new Matrix();
            if (scaleFactor < 1.0f) matrix.setScale(scaleFactor, scaleFactor);
            scaledBitmap = Bitmap.createBitmap(
                    mOriginal, cropLeft, cropTop, width, height, matrix, true);
        }
        return scaledBitmap;
    }

    /**
     * Helper method to compress the given bitmap as a JPEG and return the resulting byte array.
     */
    private byte[] getCompressedBytes(Bitmap b, int quality) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final boolean compressed = b.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        baos.flush();
        baos.close();
        byte[] result = baos.toByteArray();

        if (!compressed) {
            throw new IOException("Unable to compress image");
        }
        return result;
    }

    /**
     * Retrieves the uncompressed display photo.
     */
    public Bitmap getDisplayPhoto() {
        return mDisplayPhoto;
    }

    /**
     * Retrieves the uncompressed thumbnail photo.
     */
    public Bitmap getThumbnailPhoto() {
        return mThumbnailPhoto;
    }

    /**
     * Retrieves the compressed display photo as a byte array.
     */
    public byte[] getDisplayPhotoBytes() throws IOException {
        return getCompressedBytes(mDisplayPhoto, COMPRESSION_DISPLAY_PHOTO);
    }

    /**
     * Retrieves the compressed thumbnail photo as a byte array.
     */
    public byte[] getThumbnailPhotoBytes() throws IOException {
        // If there is a higher-resolution picture, we can assume we won't need to upscale the
        // thumbnail often, so we can compress stronger
        final boolean hasDisplayPhoto = mDisplayPhoto != null &&
                (mDisplayPhoto.getWidth() > mThumbnailPhoto.getWidth() ||
                mDisplayPhoto.getHeight() > mThumbnailPhoto.getHeight());
        return getCompressedBytes(mThumbnailPhoto,
                hasDisplayPhoto ? COMPRESSION_THUMBNAIL_LOW : COMPRESSION_THUMBNAIL_HIGH);
    }

    /**
     * Retrieves the maximum width or height (in pixels) of the display photo.
     */
    public int getMaxDisplayPhotoDim() {
        return mMaxDisplayPhotoDim;
    }

    /**
     * Retrieves the maximum width or height (in pixels) of the thumbnail.
     */
    public int getMaxThumbnailPhotoDim() {
        return mMaxThumbnailPhotoDim;
    }

    /**
     * Returns the maximum size in pixel of a thumbnail (which has a default that can be overriden
     * using a system-property)
     */
    public static int getMaxThumbnailSize() {
        return sMaxThumbnailDim;
    }

    /**
     * Returns the maximum size in pixel of a display photo (which is determined based
     * on available RAM or configured using a system-property)
     */
    public static int getMaxDisplayPhotoSize() {
        return sMaxDisplayPhotoDim;
    }
}
