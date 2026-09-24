"""
Detector de EPP (casco de seguridad) en tiempo real
====================================================
Usa MediaPipe (Face Detector, API de Tasks) para localizar la cabeza
y OpenCV para analizar si la zona superior a la cara tiene un color
compatible con un casco de seguridad.

Compatible con Python 3.11 (y 3.9-3.12) usando mediapipe >= 0.10.9,
que ya usa la API nueva de "Tasks" (mp.tasks.vision), no la antigua
mp.solutions.face_detection que aparece en tutoriales viejos.
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

MODEL_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "blaze_face_short_range.tflite")
MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/face_detector/"
    "blaze_face_short_range/float16/1/blaze_face_short_range.tflite"
)

HELMET_ZONE_TOP_RATIO = 0.85
HELMET_ZONE_SIDE_RATIO = 0.15
MIN_HELMET_COLOR_RATIO = 0.28

HELMET_COLOR_RANGES = [
    ((20, 80, 90), (35, 255, 255)),       # amarillo
    ((0, 0, 190), (179, 50, 255)),        # blanco
    ((5, 140, 110), (18, 255, 255)),      # naranja (S alto para no confundir con piel)
    ((0, 140, 90), (8, 255, 255)),        # rojo, rango bajo (S alto por lo mismo)
    ((170, 140, 90), (179, 255, 255)),    # rojo, rango alto
    ((95, 80, 60), (130, 255, 255)),      # azul
]
# Nota: si tu casco no es de estos colores, o si detectas falsos positivos
# con la piel/fondo, usa calibrar_color.py para leer el HSV real de tu
# casco y reemplaza el rango correspondiente aqui.


def asegurar_modelo():
    if not os.path.exists(MODEL_PATH):
        print("Descargando modelo de deteccion de rostros de MediaPipe...")
        try:
            urllib.request.urlretrieve(MODEL_URL, MODEL_PATH)
            print("Modelo descargado en:", MODEL_PATH)
        except Exception:
            print("No se pudo descargar el modelo automaticamente.")
            print("Descargalo manualmente desde:\n  " + MODEL_URL)
            print("y colocalo en esta carpeta con el nombre:", os.path.basename(MODEL_PATH))
            sys.exit(1)


def hay_casco(frame_bgr, x1, y1, x2, y2):
    h, w = frame_bgr.shape[:2]
    x1, y1 = max(0, x1), max(0, y1)
    x2, y2 = min(w, x2), min(h, y2)
    if x2 <= x1 or y2 <= y1:
        return False, 0.0

    region = frame_bgr[y1:y2, x1:x2]
    if region.size == 0:
        return False, 0.0

    hsv = cv2.cvtColor(region, cv2.COLOR_BGR2HSV)
    mask_total = np.zeros(hsv.shape[:2], dtype=np.uint8)
    for lower, upper in HELMET_COLOR_RANGES:
        lower = np.array(lower, dtype=np.uint8)
        upper = np.array(upper, dtype=np.uint8)
        mask = cv2.inRange(hsv, lower, upper)
        mask_total = cv2.bitwise_or(mask_total, mask)

    ratio = float(np.count_nonzero(mask_total)) / mask_total.size
    return ratio >= MIN_HELMET_COLOR_RATIO, ratio


def calcular_zona_casco(bbox, frame_w, frame_h):
    x, y, w, h = bbox
    extra_top = int(h * HELMET_ZONE_TOP_RATIO)
    extra_side = int(w * HELMET_ZONE_SIDE_RATIO)

    x1 = x - extra_side
    x2 = x + w + extra_side
    y1 = y - extra_top
    y2 = y + int(h * 0.25)

    x1 = max(0, x1)
    y1 = max(0, y1)
    x2 = min(frame_w, x2)
    y2 = min(frame_h, y2)
    return x1, y1, x2, y2


def main():
    asegurar_modelo()

    base_options = mp_base_options.BaseOptions(model_asset_path=MODEL_PATH)
    options = vision.FaceDetectorOptions(
        base_options=base_options,
        running_mode=vision.RunningMode.VIDEO,
        min_detection_confidence=0.5,
    )
    detector = vision.FaceDetector.create_from_options(options)

    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("No se pudo abrir la camara. Verifica el indice (0, 1, ...) o los permisos.")
        return

    print("Presiona 'q' para salir.")
    t0 = time.time()

    while True:
        ok, frame = cap.read()
        if not ok:
            break

        frame = cv2.flip(frame, 1)
        h, w = frame.shape[:2]

        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)

        timestamp_ms = int((time.time() - t0) * 1000)
        result = detector.detect_for_video(mp_image, timestamp_ms)

        if not result.detections:
            cv2.putText(frame, "Buscando rostro...", (20, 40),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.9, (255, 255, 255), 2)

        for det in result.detections:
            bb = det.bounding_box
            x, y, bw, bh = bb.origin_x, bb.origin_y, bb.width, bb.height

            zx1, zy1, zx2, zy2 = calcular_zona_casco((x, y, bw, bh), w, h)
            casco_ok, ratio = hay_casco(frame, zx1, zy1, zx2, zy2)

            color = (0, 200, 0) if casco_ok else (0, 0, 255)
            texto = f"CASCO OK ({ratio*100:.0f}%)" if casco_ok else "NO HAY CASCO"

            cv2.rectangle(frame, (zx1, zy1), (zx2, zy2), color, 2)
            cv2.rectangle(frame, (x, y), (x + bw, y + bh), color, 1)
            cv2.putText(frame, texto, (zx1, max(0, zy1 - 10)),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.8, color, 2)

        cv2.imshow("Deteccion de casco (MediaPipe + OpenCV)", frame)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()
    detector.close()


if __name__ == "__main__":
    main()
