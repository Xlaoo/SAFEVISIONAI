"""
Detector de EPP (chaleco de seguridad) en tiempo real
======================================================
Usa MediaPipe (Face Detector, API de Tasks) para localizar
la cabeza y OpenCV para analizar la zona del torso buscando
colores compatibles con un chaleco de seguridad.

Compatible con Python 3.11 usando MediaPipe >= 0.10.9.
"""

import os
import sys
import time
import urllib.request

import cv2
import numpy as np
import mediapipe as mp
from mediapipe.tasks.python import vision
from mediapipe.tasks.python.core import base_options as mp_base_options


# ----------------------------------------------------------------------
# 1) CONFIGURACION
# ----------------------------------------------------------------------

MODEL_PATH = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "blaze_face_short_range.tflite"
)

MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/face_detector/"
    "blaze_face_short_range/float16/1/blaze_face_short_range.tflite"
)


# Zona del cuerpo donde buscaremos el chaleco
VEST_ZONE_SIDE_RATIO = 0.75
VEST_ZONE_TOP_RATIO = 0.15
VEST_ZONE_BOTTOM_RATIO = 2.80

# Porcentaje minimo de color para considerar que hay chaleco
MIN_VEST_COLOR_RATIO = 0.12


# ----------------------------------------------------------------------
# 2) COLORES DEL CHALECO
# ----------------------------------------------------------------------

VEST_COLOR_RANGES = [
    # Naranja del chaleco (ampliado en saturacion y brillo para tolerar sombras
    # y franjas reflectivas sin confundir con piel humana cuya S suele ser < 70)
    ((4, 75, 65), (25, 255, 255)),
]


# ----------------------------------------------------------------------
# 3) ASEGURAR MODELO MEDIAPIPE
# ----------------------------------------------------------------------

def asegurar_modelo():

    if not os.path.exists(MODEL_PATH):

        print("Descargando modelo de deteccion de rostros de MediaPipe...")

        try:

            urllib.request.urlretrieve(
                MODEL_URL,
                MODEL_PATH
            )

            print("Modelo descargado en:")
            print(MODEL_PATH)

        except Exception:

            print("No se pudo descargar el modelo automaticamente.")
            print("Descargalo manualmente desde:")
            print(MODEL_URL)

            print(
                "y colocalo en esta carpeta con el nombre:",
                os.path.basename(MODEL_PATH)
            )

            sys.exit(1)


# ----------------------------------------------------------------------
# 4) DETECTAR CHALECO POR COLOR CON ESTABILIZACIÓN TEMPORAL
# ----------------------------------------------------------------------

_ultimo_ratio_chaleco = 0.0
_chaleco_previo = False


def hay_chaleco(frame_bgr, x1, y1, x2, y2):
    global _ultimo_ratio_chaleco, _chaleco_previo

    h, w = frame_bgr.shape[:2]

    x1 = max(0, x1)
    y1 = max(0, y1)

    x2 = min(w, x2)
    y2 = min(h, y2)

    if x2 <= x1 or y2 <= y1:
        _ultimo_ratio_chaleco = 0.0
        _chaleco_previo = False
        return False, 0.0

    region = frame_bgr[y1:y2, x1:x2]

    if region.size == 0:
        _ultimo_ratio_chaleco = 0.0
        _chaleco_previo = False
        return False, 0.0

    hsv = cv2.cvtColor(
        region,
        cv2.COLOR_BGR2HSV
    )

    mask_total = np.zeros(
        hsv.shape[:2],
        dtype=np.uint8
    )

    for lower, upper in VEST_COLOR_RANGES:
        lower = np.array(
            lower,
            dtype=np.uint8
        )
        upper = np.array(
            upper,
            dtype=np.uint8
        )
        mask = cv2.inRange(
            hsv,
            lower,
            upper
        )
        mask_total = cv2.bitwise_or(
            mask_total,
            mask
        )

    ratio_instantaneo = (
        float(np.count_nonzero(mask_total))
        / mask_total.size
    )

    # Si el color naranja es prácticamente nulo (< 6%), no hay chaleco de inmediato
    if ratio_instantaneo < 0.06:
        _ultimo_ratio_chaleco = ratio_instantaneo
        _chaleco_previo = False
        return (False, ratio_instantaneo)

    # Suavizado para amortiguar fluctuaciones de micro-movimientos
    if _ultimo_ratio_chaleco > 0:
        ratio_suavizado = 0.35 * _ultimo_ratio_chaleco + 0.65 * ratio_instantaneo
    else:
        ratio_suavizado = ratio_instantaneo

    _ultimo_ratio_chaleco = ratio_suavizado

    # Histéresis: si ya estaba detectado, se tolera hasta 0.09 para evitar parpadeos
    umbral = 0.09 if _chaleco_previo else MIN_VEST_COLOR_RATIO
    chaleco_ok = ratio_suavizado >= umbral
    _chaleco_previo = chaleco_ok

    return (
        chaleco_ok,
        ratio_suavizado
    )


# ----------------------------------------------------------------------
# 5) CALCULAR ZONA DEL CHALECO
# ----------------------------------------------------------------------

def calcular_zona_chaleco(
    bbox,
    frame_w,
    frame_h
):
    x, y, w, h = bbox

    # El rostro solamente sirve como referencia para localizar el torso
    centro_x = x + (w // 2)

    # El torso tiene una anchura anatómica de ~2.4 veces el rostro
    # (evita capturar paredes y fondo lateral que diluyen el ratio)
    zona_ancho = int(w * 2.4)

    x1 = centro_x - (zona_ancho // 2)
    x2 = centro_x + (zona_ancho // 2)

    # Empezar justo debajo del mentón/cuello
    y1 = y + int(h * 1.05)

    # El torso ocupa aproximadamente 2.6 veces la altura del rostro
    zona_alto = int(h * 2.6)
    y2 = y1 + zona_alto

    # Limitar a los bordes de la cámara
    x1 = max(0, x1)
    y1 = max(0, y1)
    x2 = min(frame_w, x2)
    y2 = min(frame_h, y2)

    return x1, y1, x2, y2
# ----------------------------------------------------------------------
# 6) PROGRAMA PRINCIPAL
# ----------------------------------------------------------------------

def main():

    asegurar_modelo()

    base_options = mp_base_options.BaseOptions(
        model_asset_path=MODEL_PATH
    )

    options = vision.FaceDetectorOptions(
        base_options=base_options,
        running_mode=vision.RunningMode.VIDEO,
        min_detection_confidence=0.5,
    )

    detector = vision.FaceDetector.create_from_options(
        options
    )

    cap = cv2.VideoCapture(0)

    if not cap.isOpened():

        print(
            "No se pudo abrir la camara. "
            "Verifica el indice (0, 1, ...) "
            "o los permisos."
        )

        detector.close()
        return

    print("Detector de chaleco iniciado.")
    print("Presiona 'q' para salir.")

    t0 = time.time()

    ultimo_timestamp_ms = 0

    while True:

        ok, frame = cap.read()

        if not ok:
            break

        # Espejo
        frame = cv2.flip(
            frame,
            1
        )

        h, w = frame.shape[:2]

        # ----------------------------------------------------------
        # MediaPipe
        # ----------------------------------------------------------

        rgb = cv2.cvtColor(
            frame,
            cv2.COLOR_BGR2RGB
        )

        mp_image = mp.Image(
            image_format=mp.ImageFormat.SRGB,
            data=rgb
        )

        timestamp_ms = int(
            (time.time() - t0) * 1000
        )

        if timestamp_ms <= ultimo_timestamp_ms:

            timestamp_ms = (
                ultimo_timestamp_ms + 1
            )

        ultimo_timestamp_ms = timestamp_ms

        result = detector.detect_for_video(
            mp_image,
            timestamp_ms
        )

        # ----------------------------------------------------------
        # Si no encuentra rostro
        # ----------------------------------------------------------

        if not result.detections:

            cv2.putText(
                frame,
                "Buscando rostro...",
                (20, 40),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.9,
                (255, 255, 255),
                2
            )

        # ----------------------------------------------------------
        # Procesar rostro
        # ----------------------------------------------------------

        for det in result.detections:

            bb = det.bounding_box

            x = bb.origin_x
            y = bb.origin_y
            bw = bb.width
            bh = bb.height

            # Zona donde esperamos encontrar el chaleco
            zx1, zy1, zx2, zy2 = (
                calcular_zona_chaleco(
                    (x, y, bw, bh),
                    w,
                    h
                )
            )

            chaleco_ok, ratio = hay_chaleco(
                frame,
                zx1,
                zy1,
                zx2,
                zy2
            )

            # ------------------------------------------------------
            # Resultado visual
            # ------------------------------------------------------

            if chaleco_ok:

                color = (0, 200, 0)

                texto = (
                    f"CHALECO OK "
                    f"({ratio * 100:.0f}%)"
                )

            else:

                color = (0, 0, 255)

                texto = "NO HAY CHALECO"

            # Zona del chaleco
            cv2.rectangle(
                frame,
                (zx1, zy1),
                (zx2, zy2),
                color,
                2
            )

            # Rostro
            cv2.rectangle(
                frame,
                (x, y),
                (x + bw, y + bh),
                color,
                1
            )

            # Texto
            cv2.putText(
                frame,
                texto,
                (
                    zx1,
                    max(0, zy1 - 10)
                ),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.8,
                color,
                2
            )

        # ----------------------------------------------------------
        # Mostrar
        # ----------------------------------------------------------

        nombre_ventana = "Deteccion de chaleco (MediaPipe + OpenCV)"

        cv2.namedWindow(
            nombre_ventana,
            cv2.WINDOW_NORMAL
        )

        cv2.resizeWindow(
            nombre_ventana,
            800,
            600
        )

        cv2.imshow(
            nombre_ventana,
            frame
        )

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()

    cv2.destroyAllWindows()

    detector.close()


# ----------------------------------------------------------------------
# EJECUTAR
# ----------------------------------------------------------------------

if __name__ == "__main__":
    main()