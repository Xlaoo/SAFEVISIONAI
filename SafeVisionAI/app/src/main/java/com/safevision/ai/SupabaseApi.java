package com.safevision.ai;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;


public interface SupabaseApi {


    @POST("auth/v1/signup")
    Call<Map<String,Object>> registrarUsuario(
            @Body Map<String,Object> datos
    );


    @POST("auth/v1/token?grant_type=password")
    Call<Map<String,Object>> iniciarSesion(
            @Body Map<String,String> datos
    );

    @POST("functions/v1/cambiar-password")
    Call<Map<String,Object>> cambiarPassword(
            @Body Map<String,String> datos
    );

    @GET("rest/v1/perfiles")
    Call<List<Map<String,Object>>> verificarDni(

            @Query("select") String select,

            @Query("dni") String dni
    );
    @GET("rest/v1/perfiles")
    Call<List<Map<String,Object>>> verificarDatosRegistro(

            @Query("select") String select,

            @Query("or") String filtro
    );
    @GET("rest/v1/perfiles")
    Call<List<Map<String,Object>>> buscarUsuarioCorreo(

            @Query("select") String select,

            @Query("correo") String correo
    );
    // =====================================================
// OBTENER PERFIL DEL SUPERVISOR LOGUEADO
// =====================================================

    @GET("rest/v1/perfiles")
    Call<List<Map<String,Object>>> obtenerPerfilUsuario(

            @Header("Authorization")
            String authorization,

            @Query("select")
            String select,

            @Query("id")
            String id
    );


// =====================================================
// OBTENER TODOS LOS TRABAJADORES
// =====================================================

    @GET("rest/v1/trabajadores")
    Call<List<Map<String,Object>>> obtenerTrabajadores(

            @Header("Authorization")
            String authorization,

            @Query("select")
            String select,

            @Query("order")
            String order
    );

}