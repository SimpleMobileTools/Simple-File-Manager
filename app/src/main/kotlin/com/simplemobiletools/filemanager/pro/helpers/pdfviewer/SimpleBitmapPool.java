/*
 * Copyright (C) 2016 Olmo Gallegos Hernández.
 * Copyright (C) 2016 Franc Krueger.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.simplemobiletools.filemanager.pro.helpers.pdfviewer;

import android.graphics.Bitmap;
import android.graphics.Color;

public class SimpleBitmapPool implements BitmapContainer {

    Bitmap[] bitmaps;

    private final int poolSize;

    private final int width;

    private final int height;

    private final Bitmap.Config config;

    private static final int DEFAULT_OFFSCREENSIZE = 1;

    public SimpleBitmapPool(PdfRendererParams params) {
        this.poolSize = getPoolSize(DEFAULT_OFFSCREENSIZE);
        this.width = params.getWidth();
        this.height = params.getHeight();
        this.config = params.getConfig();
        this.bitmaps = new Bitmap[poolSize];
    }

    private int getPoolSize(int offScreenSize) {
        return (offScreenSize) * 2 + 1;
    }

    public Bitmap getBitmap(int position) {
        int index = getIndexFromPosition(position);
        if (bitmaps[index] == null) {
            createBitmapAtIndex(index);
        }

        bitmaps[index].eraseColor(Color.TRANSPARENT);

        return bitmaps[index];
    }

    @Override
    public Bitmap get(int position) {
        return getBitmap(position);
    }

    protected void createBitmapAtIndex(int index) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        bitmaps[index] = bitmap;
    }

    protected int getIndexFromPosition(int position) {
        return position % poolSize;
    }
}
