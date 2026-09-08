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


    @PUT("auth/v1/user")
    Call<Map<String,Object>> actualizarPassword(
            @Header("Authorization") String token,
            @Body Map<String,String> datos
    );


    @GET("rest/v1/perfiles")
    Call<List<Map<String,Object>>> verificarDni(

            @Query("dni") String dni,

            @Header("apikey") String apiKey,

            @Header("Authorization") String auth
    );

}