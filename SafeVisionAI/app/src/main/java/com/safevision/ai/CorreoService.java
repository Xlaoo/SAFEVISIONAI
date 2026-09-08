package com.safevision.ai;

import android.os.AsyncTask;
import android.util.Log;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;


public class CorreoService {


    public static void enviarCodigo(
            String destino,
            String codigo
    ){

        AsyncTask.execute(() -> {

            try {

                final String usuario =
                        "b858e5001@smtp-brevo.com";

                final String clave =
                        "";
                final String correoRemitente =
                        "rosalespapuicorafa@gmail.com";

                Properties props = new Properties();

                props.put(
                        "mail.smtp.auth",
                        "true"
                );

                props.put(
                        "mail.smtp.starttls.enable",
                        "true"
                );

                props.put(
                        "mail.smtp.host",
                        "smtp-relay.brevo.com"
                );

                props.put(
                        "mail.smtp.port",
                        "587"
                );


                Session session =
                        Session.getInstance(
                                props,
                                new Authenticator() {

                                    protected PasswordAuthentication
                                    getPasswordAuthentication(){

                                        return new PasswordAuthentication(
                                                usuario,
                                                clave
                                        );

                                    }

                                });


                Message mensaje =
                        new MimeMessage(session);


                mensaje.setFrom(
                        new InternetAddress(correoRemitente)
                );


                mensaje.setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(destino)
                );


                mensaje.setSubject(
                        "Código de verificación SafeVisionAI"
                );


                mensaje.setText(
                        "Tu código de verificación es: "
                                + codigo
                                +
                                "\n\nEste código vence en 1 minuto."
                );


                Transport.send(mensaje);


            }catch(Exception e){

                Log.e("CORREO_ERROR", e.getMessage(), e);

            }

        });

    }

}