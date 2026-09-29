package com.safevision.ai;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Modelo de datos Java para el estado de EPP (Casco + Chaleco)
 * recibido desde el servidor de cámara (GET /estado).
 * Utiliza Gson para serialización y deserialización confiable.
 */
public class EstadoEpp {

    @SerializedName("sistema")
    private String sistema = "SafeVisionAI";

    @SerializedName("casco")
    private boolean casco = false;

    @SerializedName("chaleco")
    private boolean chaleco = false;

    @SerializedName("epp_completo")
    private boolean eppCompleto = false;

    @SerializedName("estado")
    private String estado = "BUSCANDO PERSONA";

    @SerializedName("porcentaje_casco")
    private double porcentajeCasco = 0.0;

    @SerializedName("porcentaje_chaleco")
    private double porcentajeChaleco = 0.0;

    @SerializedName("rostro_detectado")
    private boolean rostroDetectado = false;

    public static EstadoEpp fromJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new EstadoEpp();
        }

        try {
            EstadoEpp resultado = new Gson().fromJson(jsonString, EstadoEpp.class);
            return resultado != null ? resultado : new EstadoEpp();
        } catch (Exception ex) {
            EstadoEpp error = new EstadoEpp();
            error.estado = "ERROR DE PARSEO";
            return error;
        }
    }

    public String getSistema() {
        return sistema != null ? sistema : "SafeVisionAI";
    }

    public boolean isCasco() {
        return casco;
    }

    public boolean isChaleco() {
        return chaleco;
    }

    public boolean isEppCompleto() {
        return eppCompleto;
    }

    public String getEstado() {
        return estado != null ? estado : "BUSCANDO PERSONA";
    }

    public double getPorcentajeCasco() {
        return porcentajeCasco;
    }

    public double getPorcentajeChaleco() {
        return porcentajeChaleco;
    }

    public boolean isRostroDetectado() {
        return rostroDetectado;
    }

    @Override
    public String toString() {
        return "EstadoEpp{" +
                "casco=" + casco +
                ", chaleco=" + chaleco +
                ", eppCompleto=" + eppCompleto +
                ", estado='" + estado + '\'' +
                ", porcentajeCasco=" + porcentajeCasco +
                ", porcentajeChaleco=" + porcentajeChaleco +
                ", rostroDetectado=" + rostroDetectado +
                '}';
    }
}
