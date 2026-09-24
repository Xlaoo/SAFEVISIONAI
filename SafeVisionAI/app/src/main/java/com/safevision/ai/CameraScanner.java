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

            // IP ACTUAL DE LA LAPTOP
            String url =
                    "http://10.237.144.107:5000/";

            try {

                callback.encontrada(url);

            } catch (Exception e) {

                callback.error();

            }

        }).start();
    }
}