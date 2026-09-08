package com.safevision.ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NuevaPasswordActivity extends AppCompatActivity {
    private String correoUsuario;
    private EditText txtNuevaPassword;
    private EditText txtConfirmarNuevaPassword;
    private Button btnCambiarPassword;
    private TextView btnVolverNuevaPassword;
    private SupabaseApi supabaseApi;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nueva_password);

        txtNuevaPassword =
                findViewById(R.id.txtNuevaPassword);

        txtConfirmarNuevaPassword =
                findViewById(R.id.txtConfirmarNuevaPassword);

        btnCambiarPassword =
                findViewById(R.id.btnCambiarPassword);

        btnVolverNuevaPassword =
                findViewById(R.id.btnVolverNuevaPassword);

        supabaseApi = SupabaseClient
                .getClient()
                .create(SupabaseApi.class);

        correoUsuario =
                getIntent().getStringExtra("correo");
        token = getSharedPreferences(
                "SafeVisionSession",
                MODE_PRIVATE
        )
                .getString(
                        "access_token",
                        null
                );

        btnVolverNuevaPassword.setOnClickListener(v -> {
            volverLogin();
        });

        btnCambiarPassword.setOnClickListener(v -> {
            cambiarPassword();
        });
    }


    private void cambiarPassword() {

        String password =
                txtNuevaPassword
                        .getText()
                        .toString();

        String confirmar =
                txtConfirmarNuevaPassword
                        .getText()
                        .toString();

        if (password.isEmpty()) {

            txtNuevaPassword.setError(
                    "Ingrese la nueva contraseña"
            );

            txtNuevaPassword.requestFocus();

            return;
        }

        if (password.length() < 8) {

            txtNuevaPassword.setError(
                    "Mínimo 8 caracteres"
            );

            txtNuevaPassword.requestFocus();

            return;
        }

        if (!password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9].*")) {

            txtNuevaPassword.setError(
                    "Incluya mayúscula, minúscula, número y símbolo"
            );

            txtNuevaPassword.requestFocus();

            return;
        }

        if (!password.equals(confirmar)) {

            txtConfirmarNuevaPassword.setError(
                    "Las contraseñas no coinciden"
            );

            txtConfirmarNuevaPassword.requestFocus();

            return;
        }

        btnCambiarPassword.setEnabled(false);
        btnCambiarPassword.setText("ACTUALIZANDO...");

        Map<String, String> datos =
                new HashMap<>();

        datos.put("password", password);

        String token = getSharedPreferences(
                "SafeVisionSession",
                MODE_PRIVATE
        )
                .getString(
                        "access_token",
                        null
                );


        if(token == null){

            Toast.makeText(
                    this,
                    "Sesión no encontrada",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        supabaseApi.actualizarPassword(
                        "Bearer " + token,
                        datos
                )
                .enqueue(
                        new Callback<Map<String,Object>>() {

                            @Override
                            public void onResponse(
                                    Call<Map<String,Object>> call,
                                    Response<Map<String,Object>> response) {

                                btnCambiarPassword.setEnabled(true);
                                btnCambiarPassword.setText(
                                        "CAMBIAR CONTRASEÑA"
                                );

                                if (response.isSuccessful()) {

                                    Toast.makeText(
                                            NuevaPasswordActivity.this,
                                            "Contraseña actualizada correctamente",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    volverLogin();

                                } else {

                                    Toast.makeText(
                                            NuevaPasswordActivity.this,
                                            "No se pudo actualizar. Código: "
                                                    + response.code(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<Map<String,Object>> call,
                                    Throwable t) {

                                btnCambiarPassword.setEnabled(true);
                                btnCambiarPassword.setText(
                                        "CAMBIAR CONTRASEÑA"
                                );

                                Toast.makeText(
                                        NuevaPasswordActivity.this,
                                        "Error de conexión: "
                                                + t.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    private void volverLogin() {

        Intent intent =
                new Intent(
                        NuevaPasswordActivity.this,
                        MainActivity.class
                );


        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK
        );


        startActivity(intent);

        finish();
    }
}
