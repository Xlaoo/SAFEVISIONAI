package com.safevision.ai;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Configuración centralizada y dinámica de red para la cámara de SafeVisionAI.
 * La IP se descubre dinámicamente en la LAN mediante UDP broadcast (CameraScanner)
 * y se almacena en memoria y SharedPreferences, eliminando cualquier IP fija.
 */
public class CameraConfig {

    private static final String PREFS_NAME = "SafeVisionCameraPrefs";
    private static final String KEY_IP = "servidor_ip";
    private static final String KEY_PUERTO = "servidor_puerto";

    // IP y puerto dinámicos en memoria
    public static volatile String IP_SERVIDOR = "";
    public static volatile int PUERTO = 5000;

    /**
     * Inicializa o carga la última IP guardada si existe.
     */
    public static synchronized void cargarSiExiste(Context context) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String ipGuardada = prefs.getString(KEY_IP, "");
            int puertoGuardado = prefs.getInt(KEY_PUERTO, 5000);
            if (ipGuardada != null && !ipGuardada.trim().isEmpty()) {
                IP_SERVIDOR = ipGuardada.trim();
                PUERTO = puertoGuardado;
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Guarda la IP y puerto del servidor descubierto dinámicamente.
     */
    public static synchronized void setServidor(Context context, String ip, int puerto) {
        if (ip == null || ip.trim().isEmpty()) return;
        IP_SERVIDOR = ip.trim();
        PUERTO = puerto > 0 ? puerto : 5000;

        if (context != null) {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                        .putString(KEY_IP, IP_SERVIDOR)
                        .putInt(KEY_PUERTO, PUERTO)
                        .apply();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Sobrecarga sin contexto para actualización en memoria.
     */
    public static synchronized void setServidor(String ip, int puerto) {
        setServidor(null, ip, puerto);
    }

    public static synchronized boolean estaConfigurado() {
        return IP_SERVIDOR != null && !IP_SERVIDOR.trim().isEmpty();
    }

    public static synchronized String getUrlBase() {
        String host = (IP_SERVIDOR != null && !IP_SERVIDOR.trim().isEmpty()) ? IP_SERVIDOR.trim() : "127.0.0.1";
        return "http://" + host + ":" + PUERTO + "/";
    }

    public static String getUrlVideo() {
        return getUrlBase() + "video";
    }

    public static String getUrlEstado() {
        return getUrlBase() + "estado";
    }

    public static String getUrlAlerta() {
        return getUrlBase() + "alerta";
    }

    public static String getUrlInfo() {
        return getUrlBase() + "info";
    }

    public static String getUrlTrabajador() {
        return getUrlBase() + "trabajador";
    }
}
