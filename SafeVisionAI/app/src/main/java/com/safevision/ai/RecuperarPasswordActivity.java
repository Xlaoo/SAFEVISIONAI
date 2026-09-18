package com.safevision.ai;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Map;
import android.content.SharedPreferences;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecuperarPasswordActivity extends BaseActivity {

    private EditText txtCorreoRecuperar;
    private Button btnEnviarInstrucciones;
    private TextView btnVolverRecuperar;
    private TextView txtMensajeRecuperar;
    private CountDownTimer timer;
    private SharedPreferences prefs;
    private String codigoGenerado;
    private long tiempoCodigoGenerado;
    private String correo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recuperar_password);


        txtCorreoRecuperar =
                findViewById(R.id.txtCorreoRecuperar);

        btnEnviarInstrucciones =
                findViewById(R.id.btnEnviarInstrucciones);

        btnVolverRecuperar =
                findViewById(R.id.btnVolverRecuperar);

        txtMensajeRecuperar =
                findViewById(R.id.txtMensajeRecuperar);

        prefs =
                getSharedPreferences(
                        "RECUPERACION",
                        MODE_PRIVATE
                );

        btnVolverRecuperar.setOnClickListener(v -> finish());


        btnEnviarInstrucciones.setOnClickListener(v ->
                enviarRecuperacion()
        );

    }



    private void enviarRecuperacion(){


        correo =
                txtCorreoRecuperar
                        .getText()
                        .toString()
                        .trim();



        if(correo.isEmpty()){

            txtCorreoRecuperar.setError(
                    "Ingrese correo"
            );

            return;
        }



        if(!Patterns.EMAIL_ADDRESS
                .matcher(correo)
                .matches()){

            txtCorreoRecuperar.setError(
                    "Correo inválido"
            );

            return;
        }



        codigoGenerado =
                CodigoVerificacion.generarCodigo();

        prefs.edit()
                .putString(
                        "codigo",
                        codigoGenerado
                )
                .putLong(
                        "tiempo",
                        System.currentTimeMillis()
                )
                .apply();


        tiempoCodigoGenerado =
                System.currentTimeMillis();



        Toast.makeText(
                this,
                "Enviando código...",
                Toast.LENGTH_SHORT
        ).show();


        CorreoService.enviarCodigo(
                correo,
                codigoGenerado
        );


        mostrarModalCodigo();

    }





    private void mostrarModalCodigo(){


        LinearLayout layout =
                new LinearLayout(this);


        layout.setOrientation(
                LinearLayout.VERTICAL
        );


        layout.setPadding(
                40,20,40,10
        );



        EditText txtCodigo =
                new EditText(this);


        txtCodigo.setHint(
                "Código"
        );


        txtCodigo.setGravity(
                Gravity.CENTER
        );


        txtCodigo.setTextSize(22);



        txtCodigo.setInputType(
                InputType.TYPE_CLASS_TEXT
        );



        TextView tiempo =
                new TextView(this);



        Button btnReenviar =
                new Button(this);


        btnReenviar.setText(
                "Enviar nuevamente código"
        );


        btnReenviar.setEnabled(false);



        layout.addView(txtCodigo);
        layout.addView(tiempo);
        layout.addView(btnReenviar);



        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Verificación")
                        .setView(layout)
                        .setPositiveButton(
                                "VERIFICAR",
                                null
                        )
                        .setNegativeButton(
                                "Cancelar",
                                null
                        )
                        .create();



        dialog.setOnShowListener(v -> {


            Button verificar =
                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    );



            verificar.setOnClickListener(x -> {


                long tiempoGuardado =
                        prefs.getLong(
                                "tiempo",
                                0
                        );


                long diferencia =
                        System.currentTimeMillis()
                                -
                                tiempoGuardado;



                if(diferencia > 90000){


                    Toast.makeText(
                            this,
                            "Código expirado",
                            Toast.LENGTH_LONG
                    ).show();


                    return;

                }




                String codigo =
                        txtCodigo
                                .getText()
                                .toString()
                                .trim()
                                .toUpperCase();




                String codigoGuardado =
                        prefs.getString(
                                "codigo",
                                ""
                        );


                if(codigo.equals(codigoGuardado)){

                    prefs.edit()
                            .clear()
                            .apply();


                    buscarUidPorCorreo(correo, dialog);

                }else{


                    Toast.makeText(
                            this,
                            "Código incorrecto",
                            Toast.LENGTH_SHORT
                    ).show();

                }



            });



        });



        dialog.show();
        iniciarTemporizador(
                tiempo,
                btnReenviar
        );

        btnReenviar.setOnClickListener(v -> {


            codigoGenerado =
                    CodigoVerificacion.generarCodigo();


            prefs.edit()
                    .putString(
                            "codigo",
                            codigoGenerado
                    )
                    .putLong(
                            "tiempo",
                            System.currentTimeMillis()
                    )
                    .apply();


            tiempoCodigoGenerado =
                    System.currentTimeMillis();


            CorreoService.enviarCodigo(
                    correo,
                    codigoGenerado
            );


            Toast.makeText(
                    this,
                    "Nuevo código enviado",
                    Toast.LENGTH_LONG
            ).show();



            btnReenviar.setEnabled(false);


            iniciarTemporizador(
                    tiempo,
                    btnReenviar
            );


        });

    }




    private void iniciarTemporizador(
            TextView tiempo,
            Button btnReenviar
    ){


        if(timer != null){
            timer.cancel();
        }


        timer = new CountDownTimer(
                90000,
                1000
        ){


            public void onTick(long millis){


                tiempo.setText(
                        "Tiempo restante: "
                                +
                                millis / 1000
                                +
                                " segundos"
                );


            }


            public void onFinish(){


                tiempo.setText(
                        "Código expirado"
                );


                btnReenviar.setEnabled(true);


            }


        }.start();


    }
    private void buscarUidPorCorreo(
            String correo,
            AlertDialog dialog
    ){

        SupabaseApi api =
                SupabaseClient
                        .getClient()
                        .create(SupabaseApi.class);


        api.buscarUsuarioCorreo(
                        "*",
                        "eq."+correo
                )
                .enqueue(new Callback<List<Map<String,Object>>>() {


                    @Override
                    public void onResponse(
                            Call<List<Map<String,Object>>> call,
                            Response<List<Map<String,Object>>> response
                    ){
                        Log.d("SUPABASE_CORREO",
                                "Codigo: "+response.code());


                        Log.d("SUPABASE_CORREO",
                                "Respuesta: "+response.body());

                        if(response.body()!=null
                                &&
                                !response.body().isEmpty()){


                            Map<String,Object> usuario =
                                    response.body().get(0);


                            String uid =
                                    usuario.get("id").toString();



                            Intent intent =
                                    new Intent(
                                            RecuperarPasswordActivity.this,
                                            NuevaPasswordActivity.class
                                    );


                            intent.putExtra(
                                    "uid",
                                    uid
                            );


                            intent.putExtra(
                                    "correo",
                                    correo
                            );


                            dialog.dismiss();


                            startActivity(intent);



                        }else{


                            Toast.makeText(
                                    RecuperarPasswordActivity.this,
                                    "Usuario no encontrado",
                                    Toast.LENGTH_LONG
                            ).show();

                        }


                    }


                    @Override
                    public void onFailure(
                            Call<List<Map<String,Object>>> call,
                            Throwable t
                    ){

                        Toast.makeText(
                                RecuperarPasswordActivity.this,
                                "Error buscando usuario",
                                Toast.LENGTH_LONG
                        ).show();

                    }


                });


    }

}