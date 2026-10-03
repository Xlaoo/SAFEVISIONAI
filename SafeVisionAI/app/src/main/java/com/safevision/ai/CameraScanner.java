package com.safevision.ai;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;

/**
 * Escáner dinámico de servidor SafeVisionAI en la red LAN.
 * Utiliza UDP broadcast en el puerto 5005 para descubrir el PC sin IP hardcodeada
 * y valida la identidad del servidor mediante GET /info antes de conectarse.
 */
public class CameraScanner {

    private static final String TAG = "CameraScanner";
    private static final int PUERTO_UDP_DESCUBRIMIENTO = 5005;
    private static final int PUERTO_HTTP_DEFAULT = 5000;
    private static final String MENSAJE_DISCOVERY = "SAFEVISION_DISCOVERY_REQUEST";
    private static final int TIMEOUT_UDP_MS = 2500;
    private static final int TIMEOUT_HTTP_MS = 2000;

    public interface Callback {
        void encontrada(String url);
        void error();
    }

    public static void buscarCamara(
            Context context,
            Callback callback
    ) {
        new Thread(() -> {
            // Cargar configuración previa si existe
            if (context != null) {
                CameraConfig.cargarSiExiste(context);
            }

            // 1. Si ya tenemos un servidor guardado en memoria o prefs, verificar rápidamente si sigue activo
            if (CameraConfig.estaConfigurado()) {
                String ipActual = CameraConfig.IP_SERVIDOR;
                int puertoActual = CameraConfig.PUERTO;
                if (verificarServidor(ipActual, puertoActual)) {
                    Log.i(TAG, "Servidor SafeVisionAI previamente guardado responde correctamente: " + ipActual);
                    if (context != null) {
                        CameraConfig.setServidor(context, ipActual, puertoActual);
                    }
                    if (callback != null) {
                        callback.encontrada(CameraConfig.getUrlBase());
                    }
                    return;
                }
            }

            // 2. Descubrimiento real en la red LAN mediante UDP Broadcast
            String ipDescubierta = descubrirServidorPorUdp();

            if (ipDescubierta != null && !ipDescubierta.isEmpty()) {
                // Verificar que el servidor responde a GET /info y es SafeVisionAI_CAMERA
                if (verificarServidor(ipDescubierta, PUERTO_HTTP_DEFAULT)) {
                    Log.i(TAG, "Servidor SafeVisionAI descubierto y verificado en: " + ipDescubierta);
                    if (context != null) {
                        CameraConfig.setServidor(context, ipDescubierta, PUERTO_HTTP_DEFAULT);
                    } else {
                        CameraConfig.setServidor(ipDescubierta, PUERTO_HTTP_DEFAULT);
                    }
                    if (callback != null) {
                        callback.encontrada(CameraConfig.getUrlBase());
                    }
                    return;
                }
            }

            Log.w(TAG, "No se encontró ningún servidor SafeVisionAI activo en la LAN.");
            if (callback != null) {
                callback.error();
            }
        }).start();
    }

    /**
     * Envía petición UDP broadcast a la subred y escucha la respuesta de servidor_camara.py.
     */
    private static String descubrirServidorPorUdp() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(TIMEOUT_UDP_MS);

            byte[] sendData = MENSAJE_DISCOVERY.getBytes("UTF-8");

            // Enviar a 255.255.255.255
            try {
                DatagramPacket sendPacket = new DatagramPacket(
                        sendData,
                        sendData.length,
                        InetAddress.getByName("255.255.255.255"),
                        PUERTO_UDP_DESCUBRIMIENTO
                );
                socket.send(sendPacket);
            } catch (Exception e) {
                Log.w(TAG, "Error enviando a 255.255.255.255: " + e.getMessage());
            }

            // Enviar también a las direcciones de broadcast específicas de cada interfaz de red activa
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces != null && interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue;
                    }
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast != null) {
                            DatagramPacket broadcastPacket = new DatagramPacket(
                                    sendData,
                                    sendData.length,
                                    broadcast,
                                    PUERTO_UDP_DESCUBRIMIENTO
                            );
                            socket.send(broadcastPacket);
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            // Esperar respuesta de servidor_camara.py
            byte[] recvBuf = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(receivePacket);

            String ipServidor = receivePacket.getAddress().getHostAddress();
            String respuesta = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
            Log.i(TAG, "Respuesta UDP recibida desde " + ipServidor + ": " + respuesta);

            if (respuesta.contains("SafeVisionAI_CAMERA") || respuesta.contains("puerto")) {
                return ipServidor;
            }
        } catch (Exception e) {
            Log.d(TAG, "Finalizó espera de descubrimiento UDP: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
        return null;
    }

    /**
     * Comprueba mediante HTTP GET /info que el host responda y sea efectivamente SafeVisionAI activo.
     */
    public static boolean verificarServidor(String ip, int puerto) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://" + ip.trim() + ":" + puerto + "/info");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_HTTP_MS);
            conn.setReadTimeout(TIMEOUT_HTTP_MS);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                String nombre = json.optString("nombre", "");
                String estado = json.optString("estado", "");

                return "SafeVisionAI_CAMERA".equalsIgnoreCase(nombre) && "activo".equalsIgnoreCase(estado);
            }
        } catch (Exception e) {
            Log.d(TAG, "Fallo al verificar http://" + ip + ":" + puerto + "/info -> " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return false;
    }
}