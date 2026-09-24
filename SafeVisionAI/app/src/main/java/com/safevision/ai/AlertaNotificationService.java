package com.safevision.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class AlertaNotificationService extends Service {

    // =========================================================
    // CONFIGURACIÓN
    // =========================================================

    private static final String CHANNEL_ID =
            "SAFEVISION_ALERTAS";

    private static final int NOTIFICATION_ID =
            1001;


    /*
     * IP DE TU COMPUTADORA
     *
     * Si cambia la IP, cambia esta dirección.
     */
    private static final String URL_ALERTA =
            "http://10.237.144.107:5000/alerta";


    /*
     * Última alerta notificada.
     */
    private int ultimaAlertaNotificada = -1;


    /*
     * Control del servicio.
     */
    private boolean ejecutando = true;

    private Thread hilo;


    // =========================================================
    // CREAR SERVICIO
    // =========================================================

    @Override
    public void onCreate() {

        super.onCreate();

        crearCanalNotificacion();

        iniciarServicioForeground();

        iniciarMonitoreo();
    }


    // =========================================================
    // FOREGROUND
    // =========================================================

    private void iniciarServicioForeground() {

        Intent intent =
                new Intent(
                        this,
                        AlertasActivity.class
                );


        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );


        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_alert
                        )
                        .setContentTitle(
                                "SafeVisionAI"
                        )
                        .setContentText(
                                "Supervisión de seguridad activa"
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_LOW
                        )
                        .setOngoing(true)
                        .setContentIntent(
                                pendingIntent
                        )
                        .build();


        startForeground(
                NOTIFICATION_ID,
                notification
        );
    }


    // =========================================================
    // CANAL DE NOTIFICACIONES
    // =========================================================

    private void crearCanalNotificacion() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel canal =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Alertas SafeVisionAI",
                            NotificationManager.IMPORTANCE_HIGH
                    );


            canal.setDescription(
                    "Notificaciones de alertas de seguridad"
            );


            canal.enableVibration(true);


            canal.setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    null
            );


            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );


            if (manager != null) {

                manager.createNotificationChannel(
                        canal
                );
            }
        }
    }


    // =========================================================
    // MONITOREAR CÁMARA
    // =========================================================

    private void iniciarMonitoreo() {

        hilo =
                new Thread(
                        () -> {

                            while (ejecutando) {

                                try {

                                    consultarAlerta();

                                    Thread.sleep(2000);

                                } catch (
                                        InterruptedException e
                                ) {

                                    Thread.currentThread()
                                            .interrupt();

                                    break;

                                } catch (Exception e) {

                                    e.printStackTrace();

                                    try {

                                        Thread.sleep(3000);

                                    } catch (
                                            InterruptedException ex
                                    ) {

                                        Thread.currentThread()
                                                .interrupt();

                                        break;
                                    }
                                }
                            }
                        }
                );


        hilo.start();
    }


    // =========================================================
    // CONSULTAR ALERTA EN PYTHON
    // =========================================================

    private void consultarAlerta() {

        HttpURLConnection conexion = null;

        try {

            URL url =
                    new URL(
                            URL_ALERTA
                    );


            conexion =
                    (HttpURLConnection)
                            url.openConnection();


            conexion.setRequestMethod(
                    "GET"
            );


            conexion.setConnectTimeout(
                    3000
            );


            conexion.setReadTimeout(
                    3000
            );


            int codigo =
                    conexion.getResponseCode();


            if (codigo != 200) {

                return;
            }


            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    conexion.getInputStream()
                            )
                    );


            StringBuilder respuesta =
                    new StringBuilder();


            String linea;


            while (
                    (linea = reader.readLine())
                            != null
            ) {

                respuesta.append(linea);
            }


            reader.close();


            JSONObject json =
                    new JSONObject(
                            respuesta.toString()
                    );


            boolean hayAlerta =
                    json.optBoolean(
                            "hay_alerta",
                            false
                    );


            if (!hayAlerta) {

                return;
            }


            int id =
                    json.optInt(
                            "id",
                            -1
                    );


            if (id == -1) {

                return;
            }


            /*
             * EVITAR REPETIR LA MISMA ALERTA
             */

            if (
                    id ==
                            ultimaAlertaNotificada
            ) {

                return;
            }


            ultimaAlertaNotificada =
                    id;


            /*
             * Ahora buscamos la alerta completa
             * directamente en Supabase.
             */

            consultarAlertaSupabase(id);


        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            if (conexion != null) {

                conexion.disconnect();
            }
        }
    }


    // =========================================================
    // OBTENER ALERTA COMPLETA DESDE SUPABASE
    // =========================================================

    private void consultarAlertaSupabase(
            int id
    ) {

        HttpURLConnection conexion = null;

        try {

            String urlSupabase =
                    SupabaseConfig.URL
                            + "rest/v1/alertas"
                            + "?id=eq."
                            + URLEncoder.encode(
                            String.valueOf(id),
                            "UTF-8"
                    )
                            + "&select=*";


            URL url =
                    new URL(
                            urlSupabase
                    );


            conexion =
                    (HttpURLConnection)
                            url.openConnection();


            conexion.setRequestMethod(
                    "GET"
            );


            /*
             * API KEY DE SUPABASE
             */

            conexion.setRequestProperty(
                    "apikey",
                    SupabaseConfig.API_KEY
            );


            /*
             * AUTORIZACIÓN
             */

            conexion.setRequestProperty(
                    "Authorization",
                    "Bearer "
                            + SupabaseConfig.API_KEY
            );


            conexion.setRequestProperty(
                    "Accept",
                    "application/json"
            );


            conexion.setConnectTimeout(
                    5000
            );


            conexion.setReadTimeout(
                    5000
            );


            int codigo =
                    conexion.getResponseCode();


            if (codigo != 200) {

                System.out.println(
                        "ERROR SUPABASE ALERTA: "
                                + codigo
                );

                return;
            }


            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    conexion.getInputStream()
                            )
                    );


            StringBuilder respuesta =
                    new StringBuilder();


            String linea;


            while (
                    (linea = reader.readLine())
                            != null
            ) {

                respuesta.append(linea);
            }


            reader.close();


            /*
             * Supabase devuelve un ARRAY:
             *
             * [
             *   {
             *      "id": 1,
             *      ...
             *   }
             * ]
             */

            org.json.JSONArray array =
                    new org.json.JSONArray(
                            respuesta.toString()
                    );


            if (array.length() == 0) {

                System.out.println(
                        "No se encontró alerta en Supabase: "
                                + id
                );

                return;
            }


            JSONObject alerta =
                    array.getJSONObject(0);


            // =================================================
            // OBTENER DATOS
            // =================================================

            int alertaId =
                    alerta.optInt(
                            "id",
                            id
                    );


            int trabajadorId =
                    alerta.optInt(
                            "trabajador_id",
                            -1
                    );


            String problema =
                    alerta.optString(
                            "problema",
                            "Se detectó una alerta de seguridad"
                    );


            String fecha =
                    alerta.optString(
                            "created_at",
                            ""
                    );


            String estado =
                    alerta.optString(
                            "estado",
                            "PENDIENTE"
                    );


            boolean casco =
                    alerta.optBoolean(
                            "casco",
                            false
                    );


            boolean chaleco =
                    alerta.optBoolean(
                            "chaleco",
                            false
                    );


            String imagen =
                    alerta.optString(
                            "imagen",
                            ""
                    );


            String imagenNormal =
                    alerta.optString(
                            "imagen_normal",
                            ""
                    );


            String imagenZoom =
                    alerta.optString(
                            "imagen_zoom",
                            ""
                    );


            String titulo =
                    problema;


            String area =
                    "Producción";


            // =================================================
            // MOSTRAR NOTIFICACIÓN COMPLETA
            // =================================================

            mostrarNotificacion(
                    alertaId,
                    trabajadorId,
                    titulo,
                    fecha,
                    area,
                    problema,
                    casco,
                    chaleco,
                    estado,
                    imagen,
                    imagenNormal,
                    imagenZoom
            );


        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            if (conexion != null) {

                conexion.disconnect();
            }
        }
    }


    // =========================================================
    // MOSTRAR NOTIFICACIÓN
    // =========================================================

    private void mostrarNotificacion(
            int id,
            int trabajadorId,
            String tituloAlerta,
            String fecha,
            String area,
            String problema,
            boolean casco,
            boolean chaleco,
            String estado,
            String imagen,
            String imagenNormal,
            String imagenZoom
    ) {


        // =====================================================
        // ABRIR DIRECTAMENTE EL DETALLE
        // =====================================================

        Intent intent =
                new Intent(
                        this,
                        DetalleAlertaActivity.class
                );


        intent.putExtra(
                "alerta_id",
                id
        );


        intent.putExtra(
                "trabajador_id",
                trabajadorId
        );


        intent.putExtra(
                "titulo",
                tituloAlerta
        );


        intent.putExtra(
                "fecha",
                fecha
        );


        intent.putExtra(
                "area",
                area
        );


        intent.putExtra(
                "problema",
                problema
        );


        intent.putExtra(
                "casco",
                casco
        );


        intent.putExtra(
                "chaleco",
                chaleco
        );


        intent.putExtra(
                "estado",
                estado
        );


        intent.putExtra(
                "imagen",
                imagen
        );


        intent.putExtra(
                "foto_normal",
                imagenNormal
        );


        intent.putExtra(
                "foto_zoom",
                imagenZoom
        );


        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );


        // =====================================================
        // TEXTO DE LA NOTIFICACIÓN
        // =====================================================

        String titulo =
                "🚨 Alerta de seguridad";


        String mensaje;


        if (
                problema != null
                        && !problema.isEmpty()
        ) {

            mensaje =
                    problema;

        } else {

            mensaje =
                    "Se detectó una nueva alerta";
        }


        // =====================================================
        // CREAR NOTIFICACIÓN
        // =====================================================

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_alert
                        )
                        .setContentTitle(
                                titulo
                        )
                        .setContentText(
                                mensaje
                        )
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(
                                                mensaje
                                                        + "\nEstado: "
                                                        + estado
                                        )
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )
                        .setAutoCancel(true)
                        .setContentIntent(
                                pendingIntent
                        )
                        .build();


        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );


        if (manager != null) {

            manager.notify(
                    id + 5000,
                    notification
            );
        }
    }


    // =========================================================
    // START COMMAND
    // =========================================================

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        return START_STICKY;
    }


    // =========================================================
    // BIND
    // =========================================================

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }


    // =========================================================
    // DESTRUIR
    // =========================================================

    @Override
    public void onDestroy() {

        ejecutando = false;


        if (hilo != null) {

            hilo.interrupt();
        }


        super.onDestroy();
    }
}