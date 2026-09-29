"""
Detector de EPP Coordinador (Casco + Chaleco) en tiempo real
============================================================
Coordina la detección simultánea de Casco y Chaleco de seguridad
utilizando una sola cámara y una sola instancia de MediaPipe FaceDetector.

Reutiliza la lógica probada de:
- detector_casco.py  (cálculo de zona superior y validación por color HSV)
- detector_chaleco.py (cálculo de zona del torso y validación por color HSV)

Estados EPP:
- CASO 1: Casco detectado + Chaleco detectado     -> EPP COMPLETO
- CASO 2: Casco no detectado + Chaleco detectado  -> FALTA CASCO
- CASO 3: Casco detectado + Chaleco no detectado  -> FALTA CHALECO
- CASO 4: Casco no detectado + Chaleco no detect. -> FALTAN CASCO Y CHALECO
- Condición base: Sin rostro                     -> BUSCANDO PERSONA
"""

import os
import sys
import time
import cv2
import numpy as np
import mediapipe as mp
from mediapipe.tasks.python import vision
from mediapipe.tasks.python.core import base_options as mp_base_options

# Reutilización directa de los módulos existentes
from detector_casco import (
    MODEL_PATH,
    asegurar_modelo,
    calcular_zona_casco,
    hay_casco,
)
from detector_chaleco import (
    calcular_zona_chaleco,
    hay_chaleco,
)


def crear_detector_epp(model_path=MODEL_PATH, min_confidence=0.5):
    """
    Crea e inicializa una única instancia de MediaPipe FaceDetector
    para el análisis de EPP en modo VIDEO.
    """
    asegurar_modelo()
    base_options = mp_base_options.BaseOptions(model_asset_path=model_path)
    options = vision.FaceDetectorOptions(
        base_options=base_options,
        running_mode=vision.RunningMode.VIDEO,
        min_detection_confidence=min_confidence,
    )
    return vision.FaceDetector.create_from_options(options)


def procesar_frame_epp(frame, detector, timestamp_ms, dibujar_hud=True):
    """
    Procesa un frame BGR evaluando simultáneamente Casco y Chaleco.
    
    Retorna:
        resultado (dict): {
            "casco": bool,
            "chaleco": bool,
            "epp_completo": bool,
            "estado": str,
            "porcentaje_casco": float,
            "porcentaje_chaleco": float,
            "rostro_detectado": bool,
            "zona_casco": tuple or None,
            "zona_chaleco": tuple or None,
            "zona_rostro": tuple or None,
            "frame_anotado": np.ndarray
        }
    """
    frame_anotado = frame.copy()
    h, w = frame_anotado.shape[:2]

    # Conversión a formato MediaPipe
    rgb = cv2.cvtColor(frame_anotado, cv2.COLOR_BGR2RGB)
    mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)

    # Inferencia con MediaPipe
    result = detector.detect_for_video(mp_image, timestamp_ms)

    if not result.detections:
        # ----------------------------------------------------
        # SIN DETECCIÓN DE ROSTRO
        # ----------------------------------------------------
        if dibujar_hud:
            cv2.rectangle(frame_anotado, (0, 0), (w, 55), (30, 30, 30), -1)
            cv2.putText(
                frame_anotado,
                "BUSCANDO PERSONA...",
                (20, 36),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.9,
                (220, 220, 220),
                2,
            )

        return {
            "casco": False,
            "chaleco": False,
            "epp_completo": False,
            "estado": "BUSCANDO PERSONA",
            "porcentaje_casco": 0.0,
            "porcentaje_chaleco": 0.0,
            "rostro_detectado": False,
            "zona_casco": None,
            "zona_chaleco": None,
            "zona_rostro": None,
            "frame_anotado": frame_anotado,
        }

    # Procesar el rostro detectado (primer rostro principal)
    det = result.detections[0]
    bb = det.bounding_box
    x, y, bw, bh = bb.origin_x, bb.origin_y, bb.width, bb.height

    # 1. EVALUAR CASCO (detector_casco.py)
    cx1, cy1, cx2, cy2 = calcular_zona_casco((x, y, bw, bh), w, h)
    casco_ok, ratio_casco = hay_casco(frame, cx1, cy1, cx2, cy2)

    # 2. EVALUAR CHALECO (detector_chaleco.py)
    vx1, vy1, vx2, vy2 = calcular_zona_chaleco((x, y, bw, bh), w, h)
    chaleco_ok, ratio_chaleco = hay_chaleco(frame, vx1, vy1, vx2, vy2)

    # 3. DETERMINAR ESTADO EPP CONJUNTO (4 CASOS)
    if casco_ok and chaleco_ok:
        estado_epp = "EPP COMPLETO"
        color_epp = (0, 200, 0)       # Verde
        epp_completo = True
    elif not casco_ok and chaleco_ok:
        estado_epp = "FALTA CASCO"
        color_epp = (0, 140, 255)     # Naranja / Alerta
        epp_completo = False
    elif casco_ok and not chaleco_ok:
        estado_epp = "FALTA CHALECO"
        color_epp = (0, 140, 255)     # Naranja / Alerta
        epp_completo = False
    else:
        estado_epp = "FALTAN CASCO Y CHALECO"
        color_epp = (0, 0, 255)       # Rojo crítico
        epp_completo = False

    # 4. RECTÁNGULOS Y OVERLAYS VISUALES
    color_casco = (0, 200, 0) if casco_ok else (0, 0, 255)
    color_chaleco = (0, 200, 0) if chaleco_ok else (0, 0, 255)

    # Zona del Casco
    cv2.rectangle(frame_anotado, (cx1, cy1), (cx2, cy2), color_casco, 2)
    texto_casco_caja = f"CASCO: {'OK' if casco_ok else 'NO'} ({ratio_casco * 100:.0f}%)"
    cv2.putText(
        frame_anotado,
        texto_casco_caja,
        (cx1, max(20, cy1 - 8)),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.6,
        color_casco,
        2,
    )

    # Zona del Rostro (Referencia)
    cv2.rectangle(frame_anotado, (x, y), (x + bw, y + bh), (200, 200, 200), 1)

    # Zona del Chaleco
    cv2.rectangle(frame_anotado, (vx1, vy1), (vx2, vy2), color_chaleco, 2)
    texto_chaleco_caja = f"CHALECO: {'OK' if chaleco_ok else 'NO'} ({ratio_chaleco * 100:.0f}%)"
    cv2.putText(
        frame_anotado,
        texto_chaleco_caja,
        (vx1, max(20, vy1 - 8)),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.6,
        color_chaleco,
        2,
    )

    # 5. HUD SUPERIOR CONSOLIDADO
    if dibujar_hud:
        cv2.rectangle(frame_anotado, (0, 0), (w, 65), (20, 20, 20), -1)

        txt_casco = f"CASCO: {'OK' if casco_ok else 'NO'}"
        txt_chaleco = f"CHALECO: {'OK' if chaleco_ok else 'NO'}"
        cv2.putText(frame_anotado, txt_casco, (15, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.65, color_casco, 2)
        cv2.putText(frame_anotado, txt_chaleco, (160, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.65, color_chaleco, 2)

        txt_estado = f"ESTADO: {estado_epp}"
        cv2.putText(frame_anotado, txt_estado, (15, 53), cv2.FONT_HERSHEY_SIMPLEX, 0.75, color_epp, 2)

    return {
        "casco": casco_ok,
        "chaleco": chaleco_ok,
        "epp_completo": epp_completo,
        "estado": estado_epp,
        "porcentaje_casco": round(ratio_casco * 100, 2),
        "porcentaje_chaleco": round(ratio_chaleco * 100, 2),
        "rostro_detectado": True,
        "zona_casco": (cx1, cy1, cx2, cy2),
        "zona_chaleco": (vx1, vy1, vx2, vy2),
        "zona_rostro": (x, y, bw, bh),
        "frame_anotado": frame_anotado,
    }


def main():
    # 1. Asegurar e inicializar detector MediaPipe
    detector = crear_detector_epp()

    # 2. Inicializar UNA SOLA cámara (índice 0)
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("❌ No se pudo abrir la cámara. Verifica el índice (0, 1, ...) o permisos.")
        detector.close()
        return

    # 3. Configurar UNA SOLA ventana (800x600)
    nombre_ventana = "SafeVisionAI - Detector EPP (Casco + Chaleco)"
    cv2.namedWindow(nombre_ventana, cv2.WINDOW_NORMAL)
    cv2.resizeWindow(nombre_ventana, 800, 600)

    print("==================================================")
    print(" SafeVisionAI - Detector EPP Coordinador Iniciado")
    print(" Casco + Chaleco simultáneos con una sola cámara")
    print(" Presiona 'q' para salir de la aplicación")
    print("==================================================")

    t0 = time.time()
    ultimo_timestamp_ms = 0

    try:
        while True:
            ok, frame = cap.read()
            if not ok or frame is None:
                print("⚠️ No se pudo leer el frame de la cámara.")
                break

            # Efecto espejo para mayor comodidad visual
            frame = cv2.flip(frame, 1)

            # Control de timestamps estrictamente crecientes para MediaPipe
            timestamp_ms = int((time.time() - t0) * 1000)
            if timestamp_ms <= ultimo_timestamp_ms:
                timestamp_ms = ultimo_timestamp_ms + 1
            ultimo_timestamp_ms = timestamp_ms

            # Procesamiento unificado de Casco + Chaleco
            res = procesar_frame_epp(frame, detector, timestamp_ms, dibujar_hud=True)

            # Mostrar en la ventana única
            cv2.imshow(nombre_ventana, res["frame_anotado"])

            # Salir con la tecla 'q'
            if cv2.waitKey(1) & 0xFF == ord("q"):
                break
    finally:
        cap.release()
        cv2.destroyAllWindows()
        detector.close()
        print("Detector EPP cerrado correctamente.")


if __name__ == "__main__":
    main()
