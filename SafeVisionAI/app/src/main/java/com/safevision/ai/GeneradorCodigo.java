package com.safevision.ai;

import java.util.Random;

public class GeneradorCodigo {

    private static final String CARACTERES =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String generarCodigo() {

        Random random = new Random();

        StringBuilder codigo = new StringBuilder();

        for(int i = 0; i < 6; i++){

            int posicion =
                    random.nextInt(CARACTERES.length());

            codigo.append(
                    CARACTERES.charAt(posicion)
            );
        }

        return codigo.toString();
    }
}