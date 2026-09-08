package com.safevision.ai;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RecuperarPasswordActivity extends AppCompatActivity {

    private EditText txtCorreoRecuperar;
    private Button btnEnviarInstrucciones;
    private TextView btnVolverRecuperar;
    private TextView txtMensajeRecuperar;
    private CountDownTimer timer;

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


        tiempoCodigoGenerado =
                System.currentTimeMillis();



        CorreoService.enviarCodigo(
                correo,
                codigoGenerado
        );



        Toast.makeText(
                this,
                "Código enviado",
                Toast.LENGTH_LONG
        ).show();



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


                long diferencia =
                        System.currentTimeMillis()
                                -
                                tiempoCodigoGenerado;



                if(diferencia > 60000){


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




                if(codigo.equals(codigoGenerado)){


                    dialog.dismiss();


                    Intent intent =
                            new Intent(
                                    this,
                                    NuevaPasswordActivity.class
                            );


                    intent.putExtra(
                            "correo",
                            correo
                    );


                    startActivity(intent);



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
                60000,
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

}