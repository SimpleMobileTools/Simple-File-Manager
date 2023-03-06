package com.simplemobiletools.rvpdfviewer.pdfviewer.subscaleview;

@SuppressWarnings("EmptyMethod")
public interface OnImageEventListener {

    void onReady();

    void onImageLoaded();

    void onPreviewLoadError(Exception e);

    void onImageLoadError(Exception e);

    void onTileLoadError(Exception e);

    void onPreviewReleased();
}
