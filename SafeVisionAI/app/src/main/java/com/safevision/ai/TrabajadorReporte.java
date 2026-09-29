package com.safevision.ai;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class TrabajadorReporte {

    private int id;
    private String dni;
    private String nombres;
    private String apellidos;
    private String area;
    private String foto;
    private int vecesReportado;
    private ZonedDateTime ultimoReporteFecha;
    private String ultimoReporteTexto;
    private List<IncidenciaItem> listaIncidencias;

    public static class IncidenciaItem {
        private String fechaTexto;
        private String accion;
        private String observacion;

        public IncidenciaItem(String fechaTexto, String accion, String observacion) {
            this.fechaTexto = fechaTexto;
            this.accion = accion;
            this.observacion = observacion;
        }

        public String getFechaTexto() {
            return fechaTexto;
        }

        public String getAccion() {
            return accion;
        }

        public String getObservacion() {
            return observacion;
        }
    }

    public TrabajadorReporte(int id, String dni, String nombres, String apellidos, String area) {
        this.id = id;
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.area = area != null && !area.trim().isEmpty() ? area : "Producción";
        this.vecesReportado = 0;
        this.ultimoReporteFecha = null;
        this.ultimoReporteTexto = "--/--/----";
        this.listaIncidencias = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getDni() {
        if (dni == null || dni.trim().isEmpty() || "12345678".equals(dni.trim())) {
            return "";
        }
        return dni.trim();
    }

    public String getNombres() {
        return nombres != null ? nombres : "";
    }

    public String getApellidos() {
        return apellidos != null ? apellidos : "";
    }

    public String getNombreCompleto() {
        String n = getNombres().trim();
        String a = getApellidos().trim();

        if (a.toLowerCase().contains("12345678") || a.equalsIgnoreCase("DNI") || a.startsWith("DNI ")) {
            a = "";
        }

        if (n.equalsIgnoreCase("Trabajador") && a.isEmpty()) {
            return "Trabajador";
        }

        if (n.isEmpty() && a.isEmpty()) {
            return "Trabajador";
        }

        if (n.isEmpty()) {
            return a;
        }

        if (a.isEmpty()) {
            return n;
        }

        return (n + " " + a).trim();
    }

    public String getArea() {
        return area;
    }

    public String getFoto() {
        return foto != null ? foto : "";
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public int getVecesReportado() {
        return vecesReportado;
    }

    public void setVecesReportado(int vecesReportado) {
        this.vecesReportado = vecesReportado;
    }

    public ZonedDateTime getUltimoReporteFecha() {
        return ultimoReporteFecha;
    }

    public void setUltimoReporteFecha(ZonedDateTime ultimoReporteFecha) {
        this.ultimoReporteFecha = ultimoReporteFecha;
    }

    public String getUltimoReporteTexto() {
        return ultimoReporteTexto;
    }

    public void setUltimoReporteTexto(String ultimoReporteTexto) {
        this.ultimoReporteTexto = ultimoReporteTexto;
    }

    public List<IncidenciaItem> getListaIncidencias() {
        return listaIncidencias;
    }

    public void agregarIncidencia(IncidenciaItem item) {
        this.listaIncidencias.add(item);
    }
}
