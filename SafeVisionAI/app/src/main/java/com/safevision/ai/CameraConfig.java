package com.safevision.ai;

/**
 * Configuración centralizada de red para la cámara de SafeVisionAI.
 * Permite modificar la IP del servidor de cámara (PC) en un solo lugar.
 */
public class CameraConfig {

    // =========================================================
    // IP MODIFICABLE DEL SERVIDOR DE CÁMARA (PC)
    // Cambiar aquí según la red local (ej. 192.168.18.13 o 10.x.x.x)
    // =========================================================
    public static String IP_SERVIDOR = "192.168.18.13";
    public static int PUERTO = 5000;

    public static String getUrlBase() {
        return "http://" + IP_SERVIDOR + ":" + PUERTO + "/";
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
}
