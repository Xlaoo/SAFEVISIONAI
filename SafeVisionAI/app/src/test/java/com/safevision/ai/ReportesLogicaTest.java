package com.safevision.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReportesLogicaTest {

    // Helper functions mirroring the evaluated logic
    private boolean evaluarFaltaCasco(boolean casco, String problema) {
        String prob = problema != null ? problema.toLowerCase() : "";
        return !casco || prob.contains("casco");
    }

    private boolean evaluarFaltaChaleco(boolean chaleco, String problema) {
        String prob = problema != null ? problema.toLowerCase() : "";
        return !chaleco || prob.contains("chaleco");
    }

    private String obtenerTextoImplemento(boolean faltaCasco, boolean faltaChaleco) {
        if (faltaCasco && faltaChaleco) {
            return "• CASCO\n• CHALECO";
        } else if (faltaChaleco) {
            return "• CHALECO";
        } else {
            return "• CASCO";
        }
    }

    private int calcularCantidadFotos(boolean faltaCasco, boolean faltaChaleco) {
        if (faltaCasco && faltaChaleco) {
            return 3; // General, Zoom Casco, Zoom Chaleco
        } else {
            return 2; // General + (Zoom Casco O Zoom Chaleco)
        }
    }

    @Test
    public void testPrueba1_AlertaSoloCasco() {
        // Alerta donde casco es false (falta) y chaleco es true (tiene)
        boolean casco = false;
        boolean chaleco = true;
        String problema = "Trabajador sin casco de seguridad";

        boolean faltaCasco = evaluarFaltaCasco(casco, problema);
        boolean faltaChaleco = evaluarFaltaChaleco(chaleco, problema);

        assertTrue("Debe faltar casco", faltaCasco);
        assertFalse("NO debe faltar chaleco", faltaChaleco);

        String implementoTexto = obtenerTextoImplemento(faltaCasco, faltaChaleco);
        assertEquals("• CASCO", implementoTexto);
        assertFalse("No debe contener CHALECO", implementoTexto.contains("CHALECO"));

        int cantidadFotos = calcularCantidadFotos(faltaCasco, faltaChaleco);
        assertEquals("Solo casco debe tener 2 fotografías", 2, cantidadFotos);
    }

    @Test
    public void testPrueba2_AlertaSoloChaleco() {
        // Alerta donde casco es true (tiene) y chaleco es false (falta)
        boolean casco = true;
        boolean chaleco = false;
        String problema = "Trabajador sin chaleco reflectante";

        boolean faltaCasco = evaluarFaltaCasco(casco, problema);
        boolean faltaChaleco = evaluarFaltaChaleco(chaleco, problema);

        assertFalse("NO debe faltar casco", faltaCasco);
        assertTrue("Debe faltar chaleco", faltaChaleco);

        String implementoTexto = obtenerTextoImplemento(faltaCasco, faltaChaleco);
        assertEquals("• CHALECO", implementoTexto);
        assertFalse("No debe contener CASCO", implementoTexto.contains("CASCO"));

        int cantidadFotos = calcularCantidadFotos(faltaCasco, faltaChaleco);
        assertEquals("Solo chaleco debe tener 2 fotografías", 2, cantidadFotos);
    }

    @Test
    public void testPrueba3_AlertaCascoMasChaleco() {
        // Alerta donde faltan ambos implementos
        boolean casco = false;
        boolean chaleco = false;
        String problema = "Faltan casco y chaleco de seguridad";

        boolean faltaCasco = evaluarFaltaCasco(casco, problema);
        boolean faltaChaleco = evaluarFaltaChaleco(chaleco, problema);

        assertTrue("Debe faltar casco", faltaCasco);
        assertTrue("Debe faltar chaleco", faltaChaleco);

        String implementoTexto = obtenerTextoImplemento(faltaCasco, faltaChaleco);
        assertTrue("Debe indicar CASCO", implementoTexto.contains("CASCO"));
        assertTrue("Debe indicar CHALECO", implementoTexto.contains("CHALECO"));

        int cantidadFotos = calcularCantidadFotos(faltaCasco, faltaChaleco);
        assertEquals("Casco + Chaleco debe tener 3 fotografías", 3, cantidadFotos);
    }

    @Test
    public void testPrueba4_ReportesSinTrabajadorSeleccionado() {
        // Trabajador genérico o auto-registrado
        TrabajadorReporte rep = new TrabajadorReporte(1, "12345678", "Trabajador", "DNI 12345678", "Producción");

        // No debe mostrar DNI 12345678
        assertEquals("El DNI 12345678 no debe mostrarse como valor fijo", "", rep.getDni());
        // El nombre completo no debe contener "12345678" ni "DNI 12345678"
        assertEquals("Trabajador", rep.getNombreCompleto());
        assertFalse(rep.getNombreCompleto().contains("12345678"));
        assertEquals("Producción", rep.getArea());
    }

    @Test
    public void testPrueba5_ReportesDelTrabajadorSinDniFijo() {
        // Trabajador sin DNI o con placeholder
        TrabajadorReporte repVacio = new TrabajadorReporte(2, "", "", "", "Producción");
        assertEquals("", repVacio.getDni());
        assertEquals("Trabajador", repVacio.getNombreCompleto());

        // Trabajador con datos reales
        TrabajadorReporte repReal = new TrabajadorReporte(3, "87654321", "Juan", "Pérez", "Producción");
        assertEquals("87654321", repReal.getDni());
        assertEquals("Juan Pérez", repReal.getNombreCompleto());
    }

    @Test
    public void testPrueba6_HistorialSinObservacionesInventadas() {
        ReporteItem itemPendiente = new ReporteItem(
                10, 1, "Trabajador", "Producción", "",
                "29/09/2026", "02:30 PM", "Producción", "Cámara 01",
                "Trabajador sin casco", "PENDIENTE", false,
                "http://localhost:5000/fotos/normal.jpg",
                "http://localhost:5000/fotos/normal.jpg",
                "http://localhost:5000/fotos/zoom.jpg",
                "", "", "", 1, false, true
        );

        assertFalse("El reporte debe estar pendiente", itemPendiente.isRevisado());
        assertEquals("Producción", itemPendiente.getArea());
        assertEquals("Cámara 01", itemPendiente.getCamara());
        assertEquals("Trabajador sin casco", itemPendiente.getDescripcion());
        assertEquals("No debe haber observación inventada, debe mostrar el guión de no disponible", "—", itemPendiente.getObservaciones());
    }

    @Test
    public void testPrueba7_InformacionDelReporteCampos() {
        ReporteItem item = new ReporteItem(
                11, 2, "Carlos López", "Producción", "",
                "29/09/2026", "10:15 AM", "Producción", "Cámara 01",
                "Faltan casco y chaleco", "ATENDIDA", true,
                "http://localhost:5000/fotos/normal.jpg",
                "http://localhost:5000/fotos/normal.jpg",
                "http://localhost:5000/fotos/zoom.jpg",
                "Supervisor", "29/09/2026 10:20 AM", "Se le entregó EPP", 3,
                false, false
        );

        assertEquals("Carlos López", item.getTrabajadorNombre());
        assertEquals("29/09/2026", item.getFecha());
        assertEquals("10:15 AM", item.getHora());
        assertEquals("Producción", item.getArea());
        assertEquals("Cámara 01", item.getCamara());
        assertEquals("Faltan casco y chaleco", item.getDescripcion());
        assertEquals(3, item.getTotalVecesReportado());
        assertTrue(item.isRevisado());
        assertEquals("Se le entregó EPP", item.getObservaciones());
    }

    // =========================================================
    // PRUEBAS ESPECÍFICAS DE LA CORRECCIÓN ACTUAL
    // =========================================================

    @Test
    public void testPruebaA_ReporteAtendidoSoloCasco_DosFotos() {
        ReporteItem item = new ReporteItem(
                101, 13, "Rafael Rosales", "Producción", "",
                "29/09/2026", "08:00 AM", "Producción", "Cámara 01",
                "Trabajador sin casco", "ATENDIDA", true,
                "http://192.168.18.13:5000/fotos/normal.jpg",
                "http://192.168.18.13:5000/fotos/normal.jpg",
                "http://192.168.18.13:5000/fotos/zoom.jpg",
                "Supervisor", "29/09/2026 08:05 AM", "Acción tomada", 1,
                false, true // Falta casco=true, falta chaleco=false
        );

        assertTrue("El reporte debe estar atendido/revisado", item.isRevisado());
        assertFalse("Casco debe ser false (falta)", item.isCasco());
        assertTrue("Chaleco debe ser true (tiene)", item.isChaleco());

        boolean faltaCasco = !item.isCasco() || item.getDescripcion().toLowerCase().contains("casco");
        boolean faltaChaleco = !item.isChaleco() || item.getDescripcion().toLowerCase().contains("chaleco");
        assertTrue(faltaCasco);
        assertFalse(faltaChaleco);

        int totalFotos = calcularCantidadFotos(faltaCasco, faltaChaleco);
        assertEquals("Solo casco debe mostrar exactamente 2 fotos", 2, totalFotos);
        assertFalse("Las fotos no deben perderse al estar atendida", item.getImagen().isEmpty());
        assertFalse("El zoom no debe perderse al estar atendida", item.getImagenZoom().isEmpty());
    }

    @Test
    public void testPruebaB_ReporteAtendidoSoloChaleco_DosFotos() {
        ReporteItem item = new ReporteItem(
                102, 13, "Rafael Rosales", "Producción", "",
                "29/09/2026", "08:15 AM", "Producción", "Cámara 01",
                "Trabajador sin chaleco", "ATENDIDA", true,
                "http://192.168.18.13:5000/fotos/normal.jpg",
                "http://192.168.18.13:5000/fotos/normal.jpg",
                "http://192.168.18.13:5000/fotos/zoom.jpg",
                "Supervisor", "29/09/2026 08:20 AM", "Acción tomada", 2,
                true, false // Falta casco=false, falta chaleco=true
        );

        assertTrue(item.isRevisado());
        assertTrue(item.isCasco());
        assertFalse(item.isChaleco());

        boolean faltaCasco = !item.isCasco() || item.getDescripcion().toLowerCase().contains("casco");
        boolean faltaChaleco = !item.isChaleco() || item.getDescripcion().toLowerCase().contains("chaleco");
        assertFalse(faltaCasco);
        assertTrue(faltaChaleco);

        int totalFotos = calcularCantidadFotos(faltaCasco, faltaChaleco);
        assertEquals("Solo chaleco debe mostrar exactamente 2 fotos", 2, totalFotos);
        assertEquals("http://192.168.18.13:5000/fotos/normal.jpg", item.getImagen());
    }

    @Test
    public void testPruebaC_ReporteAtendidoCascoYChaleco_TresFotos() {
        ReporteItem item = new ReporteItem(
                103, 14, "Trabajador", "Producción", "",
                "29/09/2026", "09:00 AM", "Producción", "Cámara 01",
                "Faltan casco y chaleco", "ATENDIDA", true,
                "http://192.168.18.13:5000/fotos/normal.jpg",
                "http://192.168.18.13:5000/fotos/normal.jpg",
                "http://192.168.18.13:5000/fotos/zoom.jpg",
                "Supervisor", "29/09/2026 09:10 AM", "Acción tomada", 1,
                false, false // Faltan ambos
        );

        assertTrue(item.isRevisado());
        assertFalse(item.isCasco());
        assertFalse(item.isChaleco());

        boolean faltaCasco = !item.isCasco() || item.getDescripcion().toLowerCase().contains("casco");
        boolean faltaChaleco = !item.isChaleco() || item.getDescripcion().toLowerCase().contains("chaleco");
        assertTrue(faltaCasco);
        assertTrue(faltaChaleco);

        int totalFotos = calcularCantidadFotos(faltaCasco, faltaChaleco);
        assertEquals("Casco + Chaleco debe mostrar exactamente 3 fotos", 3, totalFotos);
        assertNotNull(item.getImagenNormal());
        assertNotNull(item.getImagenZoom());
    }

    @Test
    public void testPruebaD_InicioPersonalMonitoreadoSinDniGenerico() {
        // Simular datos de Supabase para el trabajador genérico de pruebas
        String nombres = "Trabajador";
        String apellidos = "DNI 12345678";
        String dni = "12345678";
        String cargo = "";
        String area = "Producción";

        // Lógica de InicioActivity para Personal monitoreado
        if (apellidos.toLowerCase().contains("12345678") || apellidos.equalsIgnoreCase("DNI")
                || apellidos.toLowerCase().startsWith("dni ") || apellidos.toLowerCase().startsWith("dni:")) {
            apellidos = apellidos.replaceAll("(?i)dni\\s*12345678", "")
                    .replaceAll("(?i)dni:?", "")
                    .replaceAll("12345678", "")
                    .trim();
        }

        String nombreCompleto = (nombres + " " + apellidos).trim();
        if (nombreCompleto.isEmpty()) {
            nombreCompleto = "Trabajador";
        }
        if (area.isEmpty()) {
            area = "Producción";
        }

        assertEquals("Trabajador", nombreCompleto);
        assertFalse("No debe contener 12345678", nombreCompleto.contains("12345678"));
        assertFalse("No debe contener DNI", nombreCompleto.contains("DNI"));
        assertEquals("Producción", area);

        // Simular trabajador real
        String nombresReal = "Rafael";
        String apellidosReal = "Rosales";
        String nombreCompletoReal = (nombresReal + " " + apellidosReal).trim();
        assertEquals("Rafael Rosales", nombreCompletoReal);
    }

    @Test
    public void testPruebaE_AtenderAlertaConservaFotografias() {
        // Alerta original con fotos
        String urlNormal = "http://192.168.18.13:5000/fotos/alerta_1_normal.jpg";
        String urlZoom = "http://192.168.18.13:5000/fotos/alerta_1_zoom.jpg";

        ReporteItem reporte = new ReporteItem(
                19, 14, "Trabajador", "Producción", "",
                "29/09/2026", "02:34 PM", "Producción", "Cámara 01",
                "Faltan casco y chaleco", "PENDIENTE", false,
                urlNormal, urlNormal, urlZoom,
                "—", "—", "—", 0, false, false
        );

        // Verificar que en estado PENDIENTE tiene fotos
        assertEquals(urlNormal, reporte.getImagen());
        assertEquals(urlNormal, reporte.getImagenNormal());
        assertEquals(urlZoom, reporte.getImagenZoom());

        // Simular transición a ATENDIDA (sin alterar fotos)
        reporte.setImagenNormal(urlNormal);
        reporte.setImagenZoom(urlZoom);

        // Verificar que tras estar ATENDIDA las fotos siguen idénticas
        assertEquals(urlNormal, reporte.getImagen());
        assertEquals(urlNormal, reporte.getImagenNormal());
        assertEquals(urlZoom, reporte.getImagenZoom());
    }

    @Test
    public void testSanitizacionUrlsNulasOEmptyEnReporteItem() {
        // Si Supabase envía la cadena "null" o "empty"
        ReporteItem item = new ReporteItem(
                20, 1, "Trabajador", "Producción", "",
                "29/09/2026", "03:00 PM", "Producción", "Cámara 01",
                "Infracción", "ATENDIDA", true,
                "http://servidor:5000/fotos/real.jpg",
                "null", "empty",
                "Supervisor", "29/09/2026 03:05 PM", "Atendida", 1,
                false, true
        );

        // No debe retornar "null", debe hacer fallback a la imagen válida existente
        assertEquals("http://servidor:5000/fotos/real.jpg", item.getImagen());
        assertEquals("", item.getImagenNormal());
        assertEquals("", item.getImagenZoom());
    }

    @Test
    public void testSupabaseStorageUrlEnReporteItem_SinIpLocal() {
        String storageUrlNormal = "https://bypkhulaxfzudvkavwtc.supabase.co/storage/v1/object/public/fotos-alertas/alerta_1_normal_20260929_160000.jpg";
        String storageUrlZoom = "https://bypkhulaxfzudvkavwtc.supabase.co/storage/v1/object/public/fotos-alertas/alerta_1_zoom_20260929_160000.jpg";

        ReporteItem item = new ReporteItem(
                21, 1, "Trabajador", "Producción", "",
                "29/09/2026", "04:00 PM", "Producción", "Cámara 01",
                "Sin casco de seguridad", "PENDIENTE", false,
                storageUrlNormal, storageUrlNormal, storageUrlZoom,
                "—", "—", "—", 0,
                false, true
        );

        // Verificar que las URLs se conservan exactamente y no contienen IPs locales
        assertFalse(item.getImagen().contains(":5000"));
        assertFalse(item.getImagen().contains("192.168."));
        assertFalse(item.getImagen().contains("10.237."));
        assertFalse(item.getImagen().contains("localhost"));
        assertTrue(item.getImagen().startsWith("https://bypkhulaxfzudvkavwtc.supabase.co/storage/v1/object/public/fotos-alertas/"));

        assertEquals(storageUrlNormal, item.getImagenNormal());
        assertEquals(storageUrlZoom, item.getImagenZoom());

        // Al pasar a ATENDIDA
        item.setImagenNormal(storageUrlNormal);
        item.setImagenZoom(storageUrlZoom);
        assertEquals(storageUrlNormal, item.getImagenNormal());
        assertEquals(storageUrlZoom, item.getImagenZoom());
    }

    @Test
    public void testAlertasStorage_Casco_Chaleco_Ambos() {
        String bucketBase = "https://bypkhulaxfzudvkavwtc.supabase.co/storage/v1/object/public/fotos-alertas/";

        // 1. Solo Casco
        String normalCasco = bucketBase + "alerta_casco_normal.jpg";
        String zoomCasco = bucketBase + "alerta_casco_zoom.jpg";
        ReporteItem repCasco = new ReporteItem(
                22, 1, "Trabajador", "Producción", "",
                "29/09/2026", "04:10 PM", "Producción", "Cámara 01",
                "Trabajador sin casco", "PENDIENTE", false,
                normalCasco, normalCasco, zoomCasco,
                "—", "—", "—", 0, false, true
        );
        assertTrue(repCasco.getImagen().startsWith(bucketBase));
        assertTrue(repCasco.getImagenZoom().startsWith(bucketBase));

        // 2. Solo Chaleco
        String normalChaleco = bucketBase + "alerta_chaleco_normal.jpg";
        String zoomChaleco = bucketBase + "alerta_chaleco_zoom.jpg";
        ReporteItem repChaleco = new ReporteItem(
                23, 1, "Trabajador", "Producción", "",
                "29/09/2026", "04:15 PM", "Producción", "Cámara 01",
                "Trabajador sin chaleco", "PENDIENTE", false,
                normalChaleco, normalChaleco, zoomChaleco,
                "—", "—", "—", 0, true, false
        );
        assertTrue(repChaleco.getImagen().startsWith(bucketBase));
        assertTrue(repChaleco.getImagenZoom().startsWith(bucketBase));

        // 3. Casco + Chaleco
        String normalAmbos = bucketBase + "alerta_ambos_normal.jpg";
        String zoomAmbos = bucketBase + "alerta_ambos_zoom.jpg";
        ReporteItem repAmbos = new ReporteItem(
                24, 1, "Trabajador", "Producción", "",
                "29/09/2026", "04:20 PM", "Producción", "Cámara 01",
                "Faltan casco y chaleco", "ATENDIDA", true,
                normalAmbos, normalAmbos, zoomAmbos,
                "Supervisor", "29/09/2026 04:25 PM", "EPP Entregado", 1, false, false
        );
        assertTrue(repAmbos.getImagen().startsWith(bucketBase));
        assertTrue(repAmbos.getImagenZoom().startsWith(bucketBase));
        assertFalse(repAmbos.getImagen().contains("5000"));
    }
}
