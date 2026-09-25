package com.safevision.ai;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.DELETE;

public interface SupabaseApi {

    // =========================================================
    // REGISTRO
    // =========================================================

    @POST("auth/v1/signup")
    Call<Map<String, Object>> registrarUsuario(
            @Body Map<String, Object> datos
    );


    // =========================================================
    // LOGIN
    // =========================================================

    @POST("auth/v1/token?grant_type=password")
    Call<Map<String, Object>> iniciarSesion(
            @Body Map<String, String> datos
    );


    // =========================================================
    // CAMBIAR PASSWORD
    // =========================================================

    @POST("functions/v1/cambiar-password")
    Call<Map<String, Object>> cambiarPassword(
            @Body Map<String, String> datos
    );


    // =========================================================
    // VERIFICAR DNI EN PERFILES
    // =========================================================

    @GET("rest/v1/perfiles")
    Call<List<Map<String, Object>>> verificarDni(
            @Query("select") String select,
            @Query("dni") String dni
    );


    // =========================================================
    // VERIFICAR DATOS DE REGISTRO
    // =========================================================

    @GET("rest/v1/perfiles")
    Call<List<Map<String, Object>>> verificarDatosRegistro(
            @Query("select") String select,
            @Query("or") String filtro
    );


    // =========================================================
    // BUSCAR USUARIO POR CORREO
    // =========================================================

    @GET("rest/v1/perfiles")
    Call<List<Map<String, Object>>> buscarUsuarioCorreo(
            @Query("select") String select,
            @Query("correo") String correo
    );


    // =========================================================
    // OBTENER PERFIL DEL SUPERVISOR
    // =========================================================

    @GET("rest/v1/perfiles")
    Call<List<Map<String, Object>>> obtenerPerfilUsuario(
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("id") String id
    );


    // =========================================================
    // OBTENER TODOS LOS TRABAJADORES
    // =========================================================

    @GET("rest/v1/trabajadores")
    Call<List<Map<String, Object>>> obtenerTrabajadores(
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("order") String order
    );


    // =========================================================
    // OBTENER ALERTAS PENDIENTES
    // =========================================================

    @GET("rest/v1/alertas")
    Call<List<Map<String, Object>>> obtenerAlertas(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("estado") String estado,
            @Query("order") String order
    );


    // =========================================================
    // BUSCAR TRABAJADOR POR DNI
    // =========================================================

    @GET("rest/v1/trabajadores")
    Call<List<Map<String, Object>>> buscarTrabajadorPorDni(
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("dni") String dni
    );


    // =========================================================
    // BUSCAR TRABAJADOR POR ID
    // =========================================================

    @GET("rest/v1/trabajadores")
    Call<List<Map<String, Object>>> buscarTrabajadorPorId(
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("id") String id
    );


    // =========================================================
    // REGISTRAR TRABAJADOR
    // =========================================================

    @Headers("Prefer: return=representation")
    @POST("rest/v1/trabajadores")
    Call<List<Map<String, Object>>> registrarTrabajador(
            @Header("Authorization") String authorization,
            @Body Map<String, Object> datos
    );


    // =========================================================
    // CONSULTAR ACCIÓN POR ALERTA
    // =========================================================

    @GET("rest/v1/acciones_alerta")
    Call<List<Map<String, Object>>> verificarAccionExistente(
            @Header("Authorization") String authorization,
            @Query("select") String select,
            @Query("alerta_id") String alertaId
    );


    // =========================================================
    // REGISTRAR ACCIÓN
    // =========================================================

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/acciones_alerta")
    Call<Void> registrarAccion(
            @Header("Authorization") String authorization,
            @Body Map<String, Object> datos
    );


    // =========================================================
    // ACTUALIZAR ALERTA
    // =========================================================

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/alertas")
    Call<Void> actualizarAlerta(
            @Header("Authorization") String authorization,
            @Query("id") String id,
            @Body Map<String, Object> datos
    );


    // =========================================================
    // ACTUALIZAR TRABAJADOR
    // =========================================================

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/trabajadores")
    Call<Void> actualizarContadoresTrabajador(
            @Header("Authorization") String authorization,
            @Query("id") String id,
            @Body Map<String, Object> datos
    );
    // =========================================================
    // ELIMINAR ACCIONES DE UNA ALERTA
    // =========================================================

    @Headers("Prefer: return=minimal")
    @DELETE("rest/v1/acciones_alerta")
    Call<Void> eliminarAccionesAlerta(
            @Header("Authorization") String authorization,
            @Query("alerta_id") String alertaId
    );

    // =========================================================
    // ELIMINAR ALERTA
    // =========================================================

    @Headers("Prefer: return=minimal")
    @DELETE("rest/v1/alertas")
    Call<Void> eliminarAlerta(
            @Header("Authorization") String authorization,
            @Query("id") String id
    );

}