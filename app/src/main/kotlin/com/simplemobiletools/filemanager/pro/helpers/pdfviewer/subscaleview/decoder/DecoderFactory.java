package com.simplemobiletools.filemanager.pro.helpers.pdfviewer.subscaleview.decoder;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;


public interface DecoderFactory<T> {


    @NonNull
    T make() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException;

}
