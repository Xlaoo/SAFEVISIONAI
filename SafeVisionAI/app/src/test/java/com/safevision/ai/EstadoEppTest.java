package com.safevision.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class EstadoEppTest {

    @Test
    public void testCaso1_EppCompleto() {
        String json = "{\"sistema\":\"SafeVisionAI\",\"casco\":true,\"chaleco\":true,\"epp_completo\":true,\"estado\":\"EPP COMPLETO\",\"porcentaje_casco\":35.0,\"porcentaje_chaleco\":28.0,\"rostro_detectado\":true}";
        EstadoEpp e = EstadoEpp.fromJson(json);

        assertTrue(e.isCasco());
        assertTrue(e.isChaleco());
        assertTrue(e.isEppCompleto());
        assertEquals("EPP COMPLETO", e.getEstado());
        assertEquals(35.0, e.getPorcentajeCasco(), 0.01);
        assertEquals(28.0, e.getPorcentajeChaleco(), 0.01);
        assertTrue(e.isRostroDetectado());
    }

    @Test
    public void testCaso2_FaltaCasco() {
        String json = "{\"sistema\":\"SafeVisionAI\",\"casco\":false,\"chaleco\":true,\"epp_completo\":false,\"estado\":\"FALTA CASCO\",\"porcentaje_casco\":0.0,\"porcentaje_chaleco\":28.0,\"rostro_detectado\":true}";
        EstadoEpp e = EstadoEpp.fromJson(json);

        assertFalse(e.isCasco());
        assertTrue(e.isChaleco());
        assertFalse(e.isEppCompleto());
        assertEquals("FALTA CASCO", e.getEstado());
    }

    @Test
    public void testCaso3_FaltaChaleco() {
        String json = "{\"sistema\":\"SafeVisionAI\",\"casco\":true,\"chaleco\":false,\"epp_completo\":false,\"estado\":\"FALTA CHALECO\",\"porcentaje_casco\":35.0,\"porcentaje_chaleco\":0.0,\"rostro_detectado\":true}";
        EstadoEpp e = EstadoEpp.fromJson(json);

        assertTrue(e.isCasco());
        assertFalse(e.isChaleco());
        assertFalse(e.isEppCompleto());
        assertEquals("FALTA CHALECO", e.getEstado());
    }

    @Test
    public void testCaso4_FaltanAmbos() {
        String json = "{\"sistema\":\"SafeVisionAI\",\"casco\":false,\"chaleco\":false,\"epp_completo\":false,\"estado\":\"FALTAN CASCO Y CHALECO\",\"porcentaje_casco\":0.0,\"porcentaje_chaleco\":0.0,\"rostro_detectado\":true}";
        EstadoEpp e = EstadoEpp.fromJson(json);

        assertFalse(e.isCasco());
        assertFalse(e.isChaleco());
        assertFalse(e.isEppCompleto());
        assertEquals("FALTAN CASCO Y CHALECO", e.getEstado());
    }

    @Test
    public void testCaso5_BuscandoPersona() {
        String json = "{\"sistema\":\"SafeVisionAI\",\"casco\":false,\"chaleco\":false,\"epp_completo\":false,\"estado\":\"BUSCANDO PERSONA\",\"porcentaje_casco\":0.0,\"porcentaje_chaleco\":0.0,\"rostro_detectado\":false}";
        EstadoEpp e = EstadoEpp.fromJson(json);

        assertFalse(e.isCasco());
        assertFalse(e.isChaleco());
        assertFalse(e.isEppCompleto());
        assertEquals("BUSCANDO PERSONA", e.getEstado());
        assertFalse(e.isRostroDetectado());
    }
}
