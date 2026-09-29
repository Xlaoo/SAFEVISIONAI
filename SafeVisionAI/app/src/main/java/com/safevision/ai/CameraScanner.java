package com.safevision.ai;

import android.content.Context;

public class CameraScanner {

    public interface Callback {

        void encontrada(String url);

        void error();
    }

    public static void buscarCamara(
            Context context,
            Callback callback
    ) {

        new Thread(() -> {

            // URL del servidor de cámara configurada en CameraConfig
            String url = CameraConfig.getUrlBase();

            try {

                callback.encontrada(url);

            } catch (Exception e) {

                callback.error();

            }

        }).start();
    }
}