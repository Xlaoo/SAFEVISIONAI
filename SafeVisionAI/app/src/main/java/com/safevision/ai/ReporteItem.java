package com.safevision.ai;

import java.io.Serializable;

public class ReporteItem implements Serializable {

    private int alertaId;
    private int trabajadorId;
    private String trabajadorNombre;
    private String trabajadorArea;
    private String trabajadorFoto;
    private String fecha;
    private String hora;
    private String area;
    private String camara;
    private String descripcion;
    private String estado;
    private boolean revisado;
    private String imagen;
    private String imagenNormal;
    private String imagenZoom;
    private String revisadoPor;
    private String fechaRevision;
    private String observaciones;
    private int totalVecesReportado;

    public ReporteItem(int alertaId, int trabajadorId, String trabajadorNombre, String trabajadorArea,
                       String trabajadorFoto, String fecha, String hora, String area, String camara,
                       String descripcion, String estado, boolean revisado, String imagen,
                       String imagenNormal, String imagenZoom, String revisadoPor,
                       String fechaRevision, String observaciones, int totalVecesReportado) {
        this.alertaId = alertaId;
        this.trabajadorId = trabajadorId;
        this.trabajadorNombre = trabajadorNombre;
        this.trabajadorArea = trabajadorArea;
        this.trabajadorFoto = trabajadorFoto;
        this.fecha = fecha;
        this.hora = hora;
        this.area = area;
        this.camara = camara;
        this.descripcion = descripcion;
        this.estado = estado;
        this.revisado = revisado;
        this.imagen = imagen;
        this.imagenNormal = imagenNormal;
        this.imagenZoom = imagenZoom;
        this.revisadoPor = revisadoPor;
        this.fechaRevision = fechaRevision;
        this.observaciones = observaciones;
        this.totalVecesReportado = totalVecesReportado;
    }

    public int getAlertaId() {
        return alertaId;
    }

    public int getTrabajadorId() {
        return trabajadorId;
    }

    public String getTrabajadorNombre() {
        return trabajadorNombre != null ? trabajadorNombre : "";
    }

    public String getTrabajadorArea() {
        return trabajadorArea != null ? trabajadorArea : "";
    }

    public String getTrabajadorFoto() {
        return trabajadorFoto != null ? trabajadorFoto : "";
    }

    public String getFecha() {
        return fecha != null ? fecha : "";
    }

    public String getHora() {
        return hora != null ? hora : "";
    }

    public String getArea() {
        return area != null ? area : "";
    }

    public String getCamara() {
        return camara != null && !camara.isEmpty() ? camara : "Cámara 01";
    }

    public String getDescripcion() {
        return descripcion != null ? descripcion : "";
    }

    public String getEstado() {
        return estado != null ? estado : "Pendiente";
    }

    public boolean isRevisado() {
        return revisado;
    }

    public String getImagen() {
        if (imagenNormal != null && !imagenNormal.trim().isEmpty()) {
            return imagenNormal;
        }
        if (imagen != null && !imagen.trim().isEmpty()) {
            return imagen;
        }
        return imagenZoom != null ? imagenZoom : "";
    }

    public String getImagenNormal() {
        return imagenNormal;
    }

    public String getImagenZoom() {
        return imagenZoom;
    }

    public String getRevisadoPor() {
        return revisadoPor != null && !revisadoPor.trim().isEmpty() ? revisadoPor : "—";
    }

    public String getFechaRevision() {
        return fechaRevision != null && !fechaRevision.trim().isEmpty() ? fechaRevision : "—";
    }

    public String getObservaciones() {
        return observaciones != null && !observaciones.trim().isEmpty() ? observaciones : "—";
    }

    public int getTotalVecesReportado() {
        return totalVecesReportado;
    }

    public void setTotalVecesReportado(int totalVecesReportado) {
        this.totalVecesReportado = totalVecesReportado;
    }
}
