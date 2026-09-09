package com.safevision.ai;

import android.app.AlertDialog;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;
public class RegistroActivity extends AppCompatActivity {

    private EditText txtNombres;
    private EditText txtApellidos;
    private EditText txtDni;
    private EditText txtTelefono;
    private EditText txtCorreo;
    private EditText txtPassword;
    private EditText txtConfirmarPassword;

    private Button btnRegistrar;
    private TextView txtVolverLogin;

    private SupabaseApi supabaseApi;
    private String codigoGenerado;
    private long tiempoCodigoGenerado;
    private String nombres;
    private String apellidos;
    private String dni;
    private String telefono;
    private String correo;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        txtNombres = findViewById(R.id.txtNombres);
        txtApellidos = findViewById(R.id.txtApellidos);
        txtDni = findViewById(R.id.txtDni);
        txtTelefono = findViewById(R.id.txtTelefono);
        txtCorreo = findViewById(R.id.txtCorreo);
        txtPassword = findViewById(R.id.txtPassword);
        txtConfirmarPassword = findViewById(R.id.txtConfirmarPassword);

        btnRegistrar = findViewById(R.id.btnRegistrar);
        txtVolverLogin = findViewById(R.id.txtVolverLogin);

        supabaseApi = SupabaseClient
                .getClient()
                .create(SupabaseApi.class);

        btnRegistrar.setOnClickListener(v -> registrarUsuario());

        txtVolverLogin.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {

        nombres = txtNombres.getText().toString().trim();
        apellidos = txtApellidos.getText().toString().trim();
        dni = txtDni.getText().toString().trim();
        telefono = txtTelefono.getText().toString().trim();
        correo = txtCorreo.getText().toString().trim();
        password = txtPassword.getText().toString();
        String confirmar = txtConfirmarPassword.getText().toString();

        if (nombres.isEmpty() || apellidos.isEmpty() || dni.isEmpty()
                || telefono.isEmpty() || correo.isEmpty()
                || password.isEmpty() || confirmar.isEmpty()) {

            Toast.makeText(this,
                    "Complete todos los campos",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        if (dni.length() != 8) {
            txtDni.setError("El DNI debe tener 8 dígitos");
            txtDni.requestFocus();
            return;
        }

        if (telefono.length() != 9) {
            txtTelefono.setError("El teléfono debe tener 9 dígitos");
            txtTelefono.requestFocus();
            return;
        }
        if (!telefono.matches("9[0-9]{8}")) {

            txtTelefono.setError(
                    "El teléfono debe comenzar con 9 y tener 9 dígitos"
            );

            txtTelefono.requestFocus();

            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            txtCorreo.setError("Correo electrónico no válido");
            txtCorreo.requestFocus();
            return;
        }

        if (password.length() < 8) {
            txtPassword.setError("Mínimo 8 caracteres");
            txtPassword.requestFocus();
            return;
        }

        if (!password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9].*")) {

            txtPassword.setError(
                    "Incluya mayúscula, minúscula, número y símbolo"
            );

            txtPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmar)) {
            txtConfirmarPassword.setError(
                    "Las contraseñas no coinciden"
            );

            txtConfirmarPassword.requestFocus();
            return;
        }

        btnRegistrar.setEnabled(false);
        btnRegistrar.setText("VALIDANDO...");

        verificarDatosDuplicados();
    }

    private void mostrarModalOtp() {

        LinearLayout contenedor = new LinearLayout(this);
        contenedor.setOrientation(LinearLayout.VERTICAL);
        contenedor.setPadding(50, 20, 50, 10);

        TextView txtMensaje = new TextView(this);
        txtMensaje.setText(
                "Se envió un código de 6 caracteres a:\n" + correo
        );
        txtMensaje.setGravity(Gravity.CENTER);
        txtMensaje.setTextSize(15);

        EditText txtCodigo = new EditText(this);
        txtCodigo.setHint("_ _ _ _ _ _");
        txtCodigo.setGravity(Gravity.CENTER);
        txtCodigo.setTextSize(24);
        txtCodigo.setInputType(
                InputType.TYPE_CLASS_TEXT
        );

        TextView txtTiempo = new TextView(this);
        Button btnReenviarCodigo = new Button(this);

        btnReenviarCodigo.setText(
                "Enviar nuevamente código"
        );

        btnReenviarCodigo.setEnabled(false);
        txtTiempo.setGravity(Gravity.CENTER);
        txtTiempo.setPadding(0, 15, 0, 0);

        contenedor.addView(txtMensaje);
        contenedor.addView(txtCodigo);
        contenedor.addView(txtTiempo);
        contenedor.addView(btnReenviarCodigo);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Verificar correo")
                .setView(contenedor)
                .setCancelable(false)
                .setNegativeButton("Cancelar",
                        (d, which) -> d.dismiss())
                .setPositiveButton("VERIFICAR", null)
                .create();

        dialog.setOnShowListener(v -> {

            Button btnVerificar =
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnVerificar.setOnClickListener(view -> {

                String codigo =
                        txtCodigo.getText()
                                .toString()
                                .trim()
                                .toUpperCase();

                if(!codigo.matches("[A-Z0-9]{6}")){

                    txtCodigo.setError(
                            "Código inválido"
                    );

                    return;
                }

                verificarOtp(codigo, dialog);
            });
        });

        dialog.show();
        iniciarTemporizador(
                txtTiempo,
                btnReenviarCodigo
        );
        btnReenviarCodigo.setOnClickListener(v -> {


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


            btnReenviarCodigo.setEnabled(false);


            iniciarTemporizador(
                    txtTiempo,
                    btnReenviarCodigo
            );

        });
    }
    private void iniciarTemporizador(
            TextView txtTiempo,
            Button btnReenviarCodigo
    ){

        new CountDownTimer(90000, 1000){

            @Override
            public void onTick(long millis){

                long segundosTotales =
                        millis / 1000;

                long minutos =
                        segundosTotales / 60;

                long segundos =
                        segundosTotales % 60;

                txtTiempo.setText(
                        "El código vence en "
                                + String.format(
                                "%02d:%02d",
                                minutos,
                                segundos
                        )
                );
            }


            @Override
            public void onFinish(){

                txtTiempo.setText(
                        "Código vencido"
                );

                btnReenviarCodigo.setEnabled(true);
            }

        }.start();

    }
    private void verificarOtp(
            String codigo,
            AlertDialog dialog) {

        long tiempoActual = System.currentTimeMillis();

        long tiempoPasado =
                tiempoActual - tiempoCodigoGenerado;


        if(tiempoPasado > 90000){

            Toast.makeText(
                    this,
                    "El código de verificación expiró",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }
        if(codigo.equals(codigoGenerado)){


            dialog.dismiss();


            Toast.makeText(
                    RegistroActivity.this,
                    "Correo verificado correctamente",
                    Toast.LENGTH_LONG
            ).show();

            btnRegistrar.setEnabled(false);
            btnRegistrar.setText("CREANDO CUENTA...");
            crearUsuarioSupabase();


        }else{


            Toast.makeText(
                    RegistroActivity.this,
                    "Código incorrecto",
                    Toast.LENGTH_LONG
            ).show();

        }

    }
    private void crearUsuarioSupabase(){


        Map<String,Object> metadata =
                new HashMap<>();


        metadata.put(
                "nombres",
                nombres
        );


        metadata.put(
                "apellidos",
                apellidos
        );


        metadata.put(
                "dni",
                dni
        );


        metadata.put(
                "telefono",
                telefono
        );


        metadata.put(
                "rol",
                "supervisor"
        );


        metadata.put(
                "activo",
                true
        );



        Map<String,Object> datos =
                new HashMap<>();


        datos.put(
                "email",
                correo
        );


        datos.put(
                "password",
                password
        );


        datos.put(
                "data",
                metadata
        );



        supabaseApi.registrarUsuario(datos)
                .enqueue(new Callback<Map<String,Object>>() {


                    @Override
                    public void onResponse(
                            Call<Map<String,Object>> call,
                            Response<Map<String,Object>> response){


                        if(response.isSuccessful()){


                            Toast.makeText(
                                    RegistroActivity.this,
                                    "Usuario registrado correctamente",
                                    Toast.LENGTH_LONG
                            ).show();


                            finish();


                        }else{


                            Toast.makeText(
                                    RegistroActivity.this,
                                    "Error registro: "
                                            + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();

                        }


                    }



                    @Override
                    public void onFailure(
                            Call<Map<String,Object>> call,
                            Throwable t){


                        Toast.makeText(
                                RegistroActivity.this,
                                t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }


                });


    }
    private void verificarDatosDuplicados() {

        String filtro =
                "(dni.eq." + dni
                        + ",telefono.eq." + telefono
                        + ",correo.eq." + correo + ")";

        supabaseApi.verificarDatosRegistro(
                        "dni,telefono,correo",
                        filtro
                )
                .enqueue(new Callback<List<Map<String, Object>>>() {

                    @Override
                    public void onResponse(
                            Call<List<Map<String, Object>>> call,
                            Response<List<Map<String, Object>>> response
                    ) {

                        if (!response.isSuccessful()) {

                            btnRegistrar.setEnabled(true);
                            btnRegistrar.setText("REGISTRARSE");

                            Toast.makeText(
                                    RegistroActivity.this,
                                    "No se pudieron validar los datos",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }


                        List<Map<String, Object>> usuarios =
                                response.body();


                        if (usuarios != null && !usuarios.isEmpty()) {

                            for (Map<String, Object> usuario : usuarios) {

                                Object dniExistente =
                                        usuario.get("dni");

                                Object telefonoExistente =
                                        usuario.get("telefono");

                                Object correoExistente =
                                        usuario.get("correo");


                                if (dniExistente != null
                                        && dni.equals(
                                        dniExistente.toString()
                                )) {

                                    txtDni.setError(
                                            "Este DNI ya está registrado"
                                    );

                                    txtDni.requestFocus();

                                    btnRegistrar.setEnabled(true);
                                    btnRegistrar.setText("REGISTRARSE");

                                    return;
                                }


                                if (telefonoExistente != null
                                        && telefono.equals(
                                        telefonoExistente.toString()
                                )) {

                                    txtTelefono.setError(
                                            "Este teléfono ya está registrado"
                                    );

                                    txtTelefono.requestFocus();

                                    btnRegistrar.setEnabled(true);
                                    btnRegistrar.setText("REGISTRARSE");

                                    return;
                                }


                                if (correoExistente != null
                                        && correo.equalsIgnoreCase(
                                        correoExistente.toString()
                                )) {

                                    txtCorreo.setError(
                                            "Este correo ya está registrado"
                                    );

                                    txtCorreo.requestFocus();

                                    btnRegistrar.setEnabled(true);
                                    btnRegistrar.setText("REGISTRARSE");

                                    return;
                                }

                            }

                        }


                        // ==========================================
                        // DATOS NO REPETIDOS → ENVIAR CÓDIGO
                        // ==========================================

                        codigoGenerado =
                                CodigoVerificacion.generarCodigo();

                        tiempoCodigoGenerado =
                                System.currentTimeMillis();


                        CorreoService.enviarCodigo(
                                correo,
                                codigoGenerado
                        );


                        btnRegistrar.setText(
                                "REGISTRANDO..."
                        );


                        mostrarModalOtp();

                    }


                    @Override
                    public void onFailure(
                            Call<List<Map<String, Object>>> call,
                            Throwable t
                    ) {

                        btnRegistrar.setEnabled(true);

                        btnRegistrar.setText(
                                "REGISTRARSE"
                        );


                        Toast.makeText(
                                RegistroActivity.this,
                                "Error al validar los datos",
                                Toast.LENGTH_LONG
                        ).show();

                    }

                });

    }
}