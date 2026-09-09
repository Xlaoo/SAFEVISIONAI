package com.safevision.ai;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static Retrofit retrofit;

    public static Retrofit getClient() {

        if (retrofit == null) {

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {

                        Request request = chain.request()
                                .newBuilder()
                                .addHeader(
                                        "apikey",
                                        SupabaseConfig.API_KEY
                                )
                                .addHeader(
                                        "Authorization",
                                        "Bearer " + SupabaseConfig.API_KEY
                                )
                                .addHeader(
                                        "Content-Type",
                                        "application/json"
                                )
                                .build();

                        return chain.proceed(request);
                    })
                    .build();


            retrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.URL)
                    .client(client)
                    .addConverterFactory(
                            GsonConverterFactory.create()
                    )
                    .build();
        }

        return retrofit;
    }
}