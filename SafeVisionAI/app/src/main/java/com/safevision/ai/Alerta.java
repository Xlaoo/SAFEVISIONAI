package com.safevision.ai;

public class Alerta {

    private int id;
    private int trabajadorId;

    private String titulo;
    private String fecha;
    private String area;

    private String problema;

    private boolean casco;
    private boolean chaleco;

    private String estado;

    private String imagen;
    private String imagenNormal;
    private String imagenZoom;


    public Alerta(
            int id,
            int trabajadorId,
            String titulo,
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

        this.id = id;
        this.trabajadorId = trabajadorId;

        this.titulo = titulo;
        this.fecha = fecha;
        this.area = area;

        this.problema = problema;

        this.casco = casco;
        this.chaleco = chaleco;

        this.estado = estado;

        this.imagen = imagen;
        this.imagenNormal = imagenNormal;
        this.imagenZoom = imagenZoom;
    }


    public int getId() {
        return id;
    }


    public int getTrabajadorId() {
        return trabajadorId;
    }


    public String getTitulo() {
        return titulo;
    }


    public String getFecha() {
        return fecha;
    }


    public String getArea() {
        return area;
    }


    public String getProblema() {
        return problema;
    }


    public boolean isCasco() {
        return casco;
    }


    public boolean isChaleco() {
        return chaleco;
    }


    public String getEstado() {
        return estado;
    }


    public String getImagen() {
        return imagen;
    }


    public String getImagenNormal() {
        return imagenNormal;
    }


    public String getImagenZoom() {
        return imagenZoom;
    }
}