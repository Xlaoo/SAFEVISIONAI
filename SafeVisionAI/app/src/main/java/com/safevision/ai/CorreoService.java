package com.safevision.ai;

import android.util.Log;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class CorreoService {

        public static void enviarCodigo(
                        String destino,
                        String codigo) {

                new Thread(() -> {

                        try {

                                final String usuario = "b858e5001@smtp-brevo.com";

                                final String clave = "";

                                final String correoRemitente = "rosalespapuicorafa@gmail.com";

                                Properties props = new Properties();

                                props.put(
                                                "mail.smtp.auth",
                                                "true");

                                props.put(
                                                "mail.smtp.starttls.enable",
                                                "true");

                                props.put(
                                                "mail.smtp.host",
                                                "smtp-relay.brevo.com");

                                props.put(
                                                "mail.smtp.port",
                                                "587");

                                props.put(
                                                "mail.smtp.connectiontimeout",
                                                "3000");

                                props.put(
                                                "mail.smtp.timeout",
                                                "5000");

                                Session session = Session.getInstance(
                                                props,
                                                new Authenticator() {

                                                        @Override
                                                        protected PasswordAuthentication getPasswordAuthentication() {

                                                                return new PasswordAuthentication(
                                                                                usuario,
                                                                                clave);

                                                        }
                                                });

                                long inicio = System.currentTimeMillis();

                                Message mensaje = new MimeMessage(session);

                                mensaje.setFrom(
                                                new InternetAddress(correoRemitente));

                                mensaje.setRecipients(
                                                Message.RecipientType.TO,
                                                InternetAddress.parse(destino));

                                mensaje.setSubject(
                                                "Código SafeVisionAI");

                                mensaje.setText(
                                                "Hola,\n\n"
                                                                + "Tu código de verificación SafeVisionAI es:\n\n"
                                                                + codigo
                                                                + "\n\n"
                                                                + "Este código tiene una duración de 1 minuto y 30 segundos."
                                                                + "\n\n"
                                                                + "Si no solicitaste este código, ignora este mensaje.");

                                Transport.send(mensaje);

                                long fin = System.currentTimeMillis();

                                Log.d(
                                                "CORREO",
                                                "Tiempo envío: "
                                                                +
                                                                (fin - inicio)
                                                                +
                                                                " ms");

                        } catch (Exception e) {

                                Log.e(
                                                "CORREO_ERROR",
                                                e.toString());

                        }

                }).start();

        }

}