package com.safevision.ai;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static Retrofit retrofit;

    public static Retrofit getClient() {

        if (retrofit == null) {

            OkHttpClient client =
                    new OkHttpClient.Builder()

                            .addInterceptor(chain -> {

                                Request original =
                                        chain.request();

                                Request.Builder builder =
                                        original.newBuilder();

                                // API KEY SIEMPRE
                                builder.header(
                                        "apikey",
                                        SupabaseConfig.API_KEY
                                );

                                // SI EL REQUEST NO TRAE TOKEN,
                                // USAMOS LA API KEY COMO ANTES
                                if (original.header("Authorization") == null) {

                                    builder.header(
                                            "Authorization",
                                            "Bearer " + SupabaseConfig.API_KEY
                                    );
                                }

                                builder.header(
                                        "Content-Type",
                                        "application/json"
                                );

                                return chain.proceed(
                                        builder.build()
                                );

                            })
                            .build();


            retrofit =
                    new Retrofit.Builder()

                            .baseUrl(
                                    SupabaseConfig.URL
                            )

                            .client(client)

                            .addConverterFactory(
                                    GsonConverterFactory.create()
                            )

                            .build();
        }

        return retrofit;
    }
}