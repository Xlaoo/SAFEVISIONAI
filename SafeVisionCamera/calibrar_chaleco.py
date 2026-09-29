import cv2
import os
import numpy as np

CARPETA_FOTOS = "fotos_chaleco"

# Rangos iniciales para buscar colores típicos de chaleco
RANGOS_HSV = {
    "amarillo": (
        np.array([15, 80, 80]),
        np.array([40, 255, 255])
    ),
    "verde": (
        np.array([35, 60, 60]),
        np.array([90, 255, 255])
    ),
    "naranja": (
        np.array([5, 100, 100]),
        np.array([20, 255, 255])
    )
}


def analizar_foto(ruta):
    imagen = cv2.imread(ruta)

    if imagen is None:
        print(f"No se pudo abrir: {ruta}")
        return

    hsv = cv2.cvtColor(imagen, cv2.COLOR_BGR2HSV)

    print(f"\nAnalizando: {os.path.basename(ruta)}")

    for nombre, (bajo, alto) in RANGOS_HSV.items():

        mascara = cv2.inRange(hsv, bajo, alto)

        cantidad = cv2.countNonZero(mascara)
        total = imagen.shape[0] * imagen.shape[1]

        porcentaje = (cantidad / total) * 100

        print(f"  {nombre}: {porcentaje:.2f}% de la imagen")


def main():

    if not os.path.exists(CARPETA_FOTOS):
        print(f"No existe la carpeta: {CARPETA_FOTOS}")
        return

    fotos = [
        archivo for archivo in os.listdir(CARPETA_FOTOS)
        if archivo.lower().endswith((".jpg", ".jpeg", ".png"))
    ]

    if not fotos:
        print("No se encontraron fotografías.")
        return

    print(f"Se encontraron {len(fotos)} fotografías.")

    for foto in fotos:
        ruta = os.path.join(CARPETA_FOTOS, foto)
        analizar_foto(ruta)

    print("\n====================================")
    print("CALIBRACIÓN TERMINADA")
    print("====================================")


if __name__ == "__main__":
    main()