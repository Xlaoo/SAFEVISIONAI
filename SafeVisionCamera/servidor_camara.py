import sys
try:
    stdout_reconfigure = getattr(sys.stdout, "reconfigure", None)
    stderr_reconfigure = getattr(sys.stderr, "reconfigure", None)

    if callable(stdout_reconfigure):
        stdout_reconfigure(encoding="utf-8", errors="replace")

    if callable(stderr_reconfigure):
        stderr_reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

from flask import request
from flask import Flask, Response, jsonify, send_from_directory, request as flask_request
import cv2
import socket
import atexit
import threading
import time
import os
import urllib.request
import requests
from datetime import datetime

import numpy as np
import mediapipe as mp
from mediapipe.tasks.python import vision
from mediapipe.tasks.python.core import base_options as mp_base_options


# ==========================================================
# FLASK
# ==========================================================

app = Flask(__name__)

PUERTO = 5000


# ==========================================================
# SUPABASE
# ==========================================================

SUPABASE_URL = "https://bypkhulaxfzudvkavwtc.supabase.co"

SUPABASE_API_KEY = os.getenv("SUPABASE_API_KEY", "")

SUPABASE_ALERTAS_URL = SUPABASE_URL + "/rest/v1/alertas"
SUPABASE_TRABAJADORES_URL = SUPABASE_URL + "/rest/v1/trabajadores"

# DNI del trabajador actual detectado o asignado
DNI_TRABAJADOR_ACTUAL = os.getenv("TRABAJADOR_DNI", "12345678")

SUPABASE_TIMEOUT = 10


# ==========================================================
# MODELO MEDIAPIPE
# ==========================================================

MODEL_PATH = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "blaze_face_short_range.tflite"
)

MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/"
    "face_detector/blaze_face_short_range/float16/1/"
    "blaze_face_short_range.tflite"
)


# ==========================================================
# CONFIGURACIÓN CASCO
# ==========================================================

HELMET_ZONE_TOP_RATIO = 0.85
HELMET_ZONE_SIDE_RATIO = 0.15

MIN_HELMET_COLOR_RATIO = 0.28

HELMET_COLOR_RANGES = [

    # Amarillo
    ((20, 80, 90), (35, 255, 255)),

    # Blanco
    ((0, 0, 190), (179, 50, 255)),

    # Naranja
    ((5, 140, 110), (18, 255, 255)),

    # Rojo
    ((0, 140, 90), (8, 255, 255)),

    # Rojo alto
    ((170, 140, 90), (179, 255, 255)),

    # Azul
    ((95, 80, 60), (130, 255, 255))
]


# ==========================================================
# FOTOS
# ==========================================================

CARPETA_FOTOS = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "alertas"
)

os.makedirs(
    CARPETA_FOTOS,
    exist_ok=True
)


# ==========================================================
# VARIABLES GLOBALES
# ==========================================================

frame_actual = None

bloqueo = threading.Lock()

activo = True

camara = None

ultimo_timestamp_ms = 0
# ==========================================================
# ESTADO
# ==========================================================

estado_casco = "SIN DETECTAR"

porcentaje_casco = 0.0


# ==========================================================
# CONTROL ALERTA
# ==========================================================

alerta_activa = False

frames_sin_casco = 0

FRAMES_CONFIRMACION_RETIRO = 8


# ==========================================================
# IDs
# ==========================================================

ultimo_id_alerta = 0


# ==========================================================
# ÚLTIMA ALERTA
# ==========================================================

ultima_alerta = None


# ==========================================================
# ASEGURAR MODELO
# ==========================================================

def asegurar_modelo():

    if os.path.exists(MODEL_PATH):
        return

    print()
    print("Descargando modelo MediaPipe...")
    print()

    try:

        urllib.request.urlretrieve(
            MODEL_URL,
            MODEL_PATH
        )

        print(
            "Modelo descargado correctamente."
        )

    except Exception as e:

        print(
            "ERROR descargando modelo:",
            e
        )

        raise


# ==========================================================
# DETECTAR CASCO
# ==========================================================

def hay_casco(
    frame_bgr,
    x1,
    y1,
    x2,
    y2
):

    h, w = frame_bgr.shape[:2]

    x1 = max(0, x1)
    y1 = max(0, y1)

    x2 = min(w, x2)
    y2 = min(h, y2)

    if x2 <= x1 or y2 <= y1:

        return False, 0.0

    region = frame_bgr[
        y1:y2,
        x1:x2
    ]

    if region.size == 0:

        return False, 0.0

    hsv = cv2.cvtColor(
        region,
        cv2.COLOR_BGR2HSV
    )

    mask_total = np.zeros(
        hsv.shape[:2],
        dtype=np.uint8
    )

    for lower, upper in HELMET_COLOR_RANGES:

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

    ratio = (
        float(np.count_nonzero(mask_total))
        / mask_total.size
    )

    return (
        ratio >= MIN_HELMET_COLOR_RATIO,
        ratio
    )


# ==========================================================
# CALCULAR ZONA DEL CASCO
# ==========================================================

def calcular_zona_casco(
    bbox,
    frame_w,
    frame_h
):

    x, y, w, h = bbox

    extra_top = int(
        h * HELMET_ZONE_TOP_RATIO
    )

    extra_side = int(
        w * HELMET_ZONE_SIDE_RATIO
    )

    x1 = x - extra_side
    x2 = x + w + extra_side

    y1 = y - extra_top
    y2 = y + int(h * 0.25)

    x1 = max(
        0,
        x1
    )

    y1 = max(
        0,
        y1
    )

    x2 = min(
        frame_w,
        x2
    )

    y2 = min(
        frame_h,
        y2
    )

    return (
        x1,
        y1,
        x2,
        y2
    )


# ==========================================================
# FUNCIONES SUPABASE: TRABAJADORES Y DNI
# ==========================================================

def obtener_headers_supabase():
    return {
        "apikey": SUPABASE_API_KEY,
        "Authorization": "Bearer " + SUPABASE_API_KEY,
        "Content-Type": "application/json",
        "Prefer": "return=representation"
    }


def buscar_trabajador_por_dni(dni):
    """
    Busca si el DNI ya existe en public.trabajadores.
    Retorna el diccionario del trabajador o None.
    """
    if not SUPABASE_API_KEY or SUPABASE_API_KEY == "REEMPLAZA_CON_TU_API_KEY":
        return None

    try:
        url = f"{SUPABASE_TRABAJADORES_URL}?dni=eq.{dni}&select=*"
        res = requests.get(
            url,
            headers=obtener_headers_supabase(),
            timeout=SUPABASE_TIMEOUT
        )
        if res.ok:
            datos = res.json()
            if isinstance(datos, list) and len(datos) > 0:
                return datos[0]
        return None
    except Exception as e:
        print(f"⚠️ Error buscando trabajador por DNI {dni}:", e)
        return None


def registrar_trabajador_nuevo(dni):
    """
    Registra un nuevo trabajador en public.trabajadores cuando el DNI no existe.
    """
    if not SUPABASE_API_KEY or SUPABASE_API_KEY == "REEMPLAZA_CON_TU_API_KEY":
        return None

    try:
        datos = {
            "dni": str(dni),
            "nombres": "Trabajador",
            "apellidos": f"DNI {dni}",
            "area": "Producción",
            "casco": False,
            "chaleco": True,
            "casco_retiros": 0,
            "casco_colocaciones": 0,
            "chaleco_retiros": 0,
            "chaleco_colocaciones": 0
        }
        res = requests.post(
            SUPABASE_TRABAJADORES_URL,
            headers=obtener_headers_supabase(),
            json=datos,
            timeout=SUPABASE_TIMEOUT
        )
        if res.ok:
            resultado = res.json()
            if isinstance(resultado, list) and len(resultado) > 0:
                print(f"✅ Nuevo trabajador registrado. ID: {resultado[0].get('id')} - DNI: {dni}")
                return resultado[0]
        print("❌ Error registrando trabajador nuevo:", res.status_code, res.text)
        return None
    except Exception as e:
        print("❌ Error de conexión al registrar trabajador:", e)
        return None


def obtener_o_crear_trabajador(dni):
    """
    Regla del flujo:
    1. Buscar DNI en public.trabajadores.
    2. Si existe -> utilizar trabajador existente (NO DUPLICAR).
    3. Si NO existe -> registrar trabajador y obtener su trabajador_id.
    """
    trabajador = buscar_trabajador_por_dni(dni)
    if trabajador is not None:
        print(f"👤 Trabajador existente encontrado: ID {trabajador.get('id')} ({trabajador.get('nombres')} {trabajador.get('apellidos')}) - DNI: {dni}")
        return trabajador, False

    print(f"⚠️ DNI {dni} no registrado. Creando nuevo trabajador en Supabase...")
    nuevo = registrar_trabajador_nuevo(dni)
    return nuevo, True


def acumular_deteccion_retiro_casco(trabajador):
    """
    Acumula la detección de retiro de casco sobre el trabajador existente:
    Incrementa casco_retiros y actualiza casco = False.
    """
    if not trabajador or not SUPABASE_API_KEY:
        return trabajador

    try:
        trabajador_id = trabajador.get("id")
        actual_retiros = int(trabajador.get("casco_retiros") or 0)
        nuevo_retiros = actual_retiros + 1

        url = f"{SUPABASE_TRABAJADORES_URL}?id=eq.{trabajador_id}"
        datos = {
            "casco_retiros": nuevo_retiros,
            "casco": False
        }
        res = requests.patch(
            url,
            headers=obtener_headers_supabase(),
            json=datos,
            timeout=SUPABASE_TIMEOUT
        )
        if res.ok:
            trabajador["casco_retiros"] = nuevo_retiros
            trabajador["casco"] = False
            print(f"📊 Detección acumulada: trabajador #{trabajador_id} - casco_retiros = {nuevo_retiros}")
        else:
            print(f"⚠️ Error actualizando contadores de trabajador #{trabajador_id}:", res.status_code, res.text)
    except Exception as e:
        print("⚠️ Error acumulando detección de retiro:", e)

    return trabajador


def acumular_deteccion_colocacion_casco(trabajador):
    """
    Acumula la detección de colocación de casco:
    Incrementa casco_colocaciones y actualiza casco = True.
    """
    if not trabajador or not SUPABASE_API_KEY:
        return trabajador

    try:
        trabajador_id = trabajador.get("id")
        actual_colocaciones = int(trabajador.get("casco_colocaciones") or 0)
        nuevo_colocaciones = actual_colocaciones + 1

        url = f"{SUPABASE_TRABAJADORES_URL}?id=eq.{trabajador_id}"
        datos = {
            "casco_colocaciones": nuevo_colocaciones,
            "casco": True
        }
        res = requests.patch(
            url,
            headers=obtener_headers_supabase(),
            json=datos,
            timeout=SUPABASE_TIMEOUT
        )
        if res.ok:
            trabajador["casco_colocaciones"] = nuevo_colocaciones
            trabajador["casco"] = True
            print(f"🟢 Colocación registrada: trabajador #{trabajador_id} - casco_colocaciones = {nuevo_colocaciones}")
    except Exception as e:
        print("⚠️ Error registrando colocación de casco:", e)

    return trabajador


# ==========================================================
# GUARDAR ALERTA EN SUPABASE
# ==========================================================

def guardar_alerta_supabase(
    trabajador_id,
    fecha,
    foto_normal_url,
    foto_zoom_url
):

    if (
        not SUPABASE_API_KEY
        or SUPABASE_API_KEY == "REEMPLAZA_CON_TU_API_KEY"
    ):

        print()
        print("⚠️ SUPABASE_API_KEY no está configurada.")
        print("⚠️ La alerta NO se guardará.")
        print()

        return None

    headers = obtener_headers_supabase()

    datos = {

        "trabajador_id":
            trabajador_id,

        "problema":
            "Trabajador sin casco",

        "casco":
            False,

        "chaleco":
            True,

        "estado":
            "PENDIENTE",

        "imagen":
            foto_normal_url,

        "imagen_normal":
            foto_normal_url,

        "imagen_zoom":
            foto_zoom_url
    }

    try:

        respuesta = requests.post(

            SUPABASE_ALERTAS_URL,

            headers=headers,

            json=datos,

            timeout=SUPABASE_TIMEOUT
        )

        if respuesta.ok:

            try:

                resultado = respuesta.json()

            except Exception:

                resultado = []

            if (
                isinstance(resultado, list)
                and len(resultado) > 0
            ):

                registro = resultado[0]

                id_supabase = registro.get("id")

                print()
                print(
                    "✅ ALERTA GUARDADA EN SUPABASE"
                )

                print(
                    "🆔 ID:",
                    id_supabase
                )

                print(
                    "👷 Trabajador ID:",
                    trabajador_id
                )

                print(
                    "📸 Foto normal:",
                    foto_normal_url
                )

                print(
                    "🔍 Foto zoom:",
                    foto_zoom_url
                )

                print()

                return id_supabase

            print(
                "✅ Alerta guardada en Supabase."
            )

            return None

        print()
        print(
            "❌ ERROR GUARDANDO ALERTA EN SUPABASE"
        )

        print(
            "HTTP:",
            respuesta.status_code
        )

        print(
            "Respuesta:",
            respuesta.text
        )

        print()

        return None

    except requests.RequestException as e:

        print()
        print(
            "❌ ERROR DE CONEXIÓN CON SUPABASE"
        )

        print(e)

        print()

        return None


# ==========================================================
# CREAR LAS DOS FOTOS
# ==========================================================

def crear_fotos_alerta(
    frame_con_cuadro,
    zona_zoom
):

    global ultimo_id_alerta

    # ------------------------------------------------------
    # SEGURIDAD
    # ------------------------------------------------------

    if zona_zoom is None:

        print()
        print(
            "❌ NO existe cuadro válido."
        )

        print(
            "❌ No se tomarán fotografías."
        )

        return None

    ahora = datetime.now()

    # ------------------------------------------------------
    # COORDENADAS
    # ------------------------------------------------------

    x1, y1, x2, y2 = zona_zoom

    h, w = frame_con_cuadro.shape[:2]

    x1 = max(
        0,
        min(x1, w - 1)
    )

    y1 = max(
        0,
        min(y1, h - 1)
    )

    x2 = min(
        w,
        x2
    )

    y2 = min(
        h,
        y2
    )

    if x2 <= x1 or y2 <= y1:

        print(
            "❌ Cuadro inválido."
        )

        return None

    # ------------------------------------------------------
    # ID
    # ------------------------------------------------------

    ultimo_id_alerta += 1

    id_alerta = ultimo_id_alerta

    fecha_archivo = ahora.strftime(
        "%Y%m%d_%H%M%S_%f"
    )

    # ------------------------------------------------------
    # NOMBRES
    # ------------------------------------------------------

    nombre_normal = (
        f"alerta_{id_alerta}_normal_"
        f"{fecha_archivo}.jpg"
    )

    nombre_zoom = (
        f"alerta_{id_alerta}_zoom_"
        f"{fecha_archivo}.jpg"
    )

    ruta_normal = os.path.join(
        CARPETA_FOTOS,
        nombre_normal
    )

    ruta_zoom = os.path.join(
        CARPETA_FOTOS,
        nombre_zoom
    )

    # ------------------------------------------------------
    # FOTO 1
    # COMPLETA + CUADROS
    # ------------------------------------------------------

    guardado_normal = cv2.imwrite(
        ruta_normal,
        frame_con_cuadro
    )

    if not guardado_normal:

        print(
            "❌ No se pudo guardar la foto normal."
        )

        return None

    # ------------------------------------------------------
    # FOTO 2
    # ZOOM DEL CUADRO
    # ------------------------------------------------------

    zoom = frame_con_cuadro[
        y1:y2,
        x1:x2
    ]

    if zoom.size == 0:

        print(
            "❌ La zona del zoom está vacía."
        )

        try:
            os.remove(ruta_normal)
        except:
            pass

        return None

    zoom = cv2.resize(
        zoom,
        None,
        fx=2.5,
        fy=2.5,
        interpolation=cv2.INTER_CUBIC
    )

    guardado_zoom = cv2.imwrite(
        ruta_zoom,
        zoom
    )

    if not guardado_zoom:

        print(
            "❌ No se pudo guardar la foto zoom."
        )

        try:
            os.remove(ruta_normal)
        except:
            pass

        return None

    # ------------------------------------------------------
    # INFORMACIÓN
    # ------------------------------------------------------

    print()
    print("==============================")
    print("🚨 ALERTA CASCO RETIRADO")
    print("==============================")

    print(
        "📸 Foto 1:",
        nombre_normal
    )

    print(
        "🔍 Foto 2:",
        nombre_zoom
    )

    print(
        "📦 Zona detectada:",
        zona_zoom
    )

    print("==============================")
    print()

    # ------------------------------------------------------
    # URL FOTOS
    # ------------------------------------------------------

    ip = obtener_ip()

    foto_normal_url = (
        f"http://{ip}:{PUERTO}/fotos/"
        f"{nombre_normal}"
    )

    foto_zoom_url = (
        f"http://{ip}:{PUERTO}/fotos/"
        f"{nombre_zoom}"
    )

    # ------------------------------------------------------
    # GESTIÓN DEL TRABAJADOR Y DETECCIÓN (DNI)
    # ------------------------------------------------------

    trabajador, fue_creado = obtener_o_crear_trabajador(DNI_TRABAJADOR_ACTUAL)
    trabajador_id = trabajador.get("id") if trabajador else 1

    if trabajador:
        acumular_deteccion_retiro_casco(trabajador)

    # ------------------------------------------------------
    # SUPABASE
    # ------------------------------------------------------

    id_supabase = guardar_alerta_supabase(
        trabajador_id,
        ahora.isoformat(),
        foto_normal_url,
        foto_zoom_url
    )

    return {

        "id":
            id_supabase
            if id_supabase is not None
            else id_alerta,

        "id_local":
            id_alerta,

        "trabajador_id":
            trabajador_id,

        "tipo":
            "CASCO_RETIRADO",

        "estado":
            "PENDIENTE",

        "fecha":
            ahora.isoformat(),

        "foto_normal":
            nombre_normal,

        "foto_zoom":
            nombre_zoom,

        "foto_normal_url":
            foto_normal_url,

        "foto_zoom_url":
            foto_zoom_url,

        "guardada_supabase":
            id_supabase is not None
    }


# ==========================================================
# ABRIR / REABRIR CÁMARA
# ==========================================================

def abrir_camara():

    print()
    print("🎥 Intentando abrir cámara...")

    try:

        nueva_camara = cv2.VideoCapture(
            1,
            cv2.CAP_DSHOW
        )

        nueva_camara.set(
            cv2.CAP_PROP_FRAME_WIDTH,
            640
        )

        nueva_camara.set(
            cv2.CAP_PROP_FRAME_HEIGHT,
            360
        )

        nueva_camara.set(
            cv2.CAP_PROP_FPS,
            30
        )

        if nueva_camara.isOpened():

            print(
                "✅ Cámara abierta correctamente."
            )

            return nueva_camara

        print(
            "❌ No se pudo abrir la cámara."
        )

        try:
            nueva_camara.release()
        except:
            pass

        return None

    except Exception as e:

        print(
            "❌ Error abriendo cámara:",
            e
        )

        return None


# ==========================================================
# PROCESAMIENTO DE CÁMARA
# ==========================================================

def iniciar_camara():

    global frame_actual
    global activo

    global estado_casco
    global porcentaje_casco

    global alerta_activa
    global frames_sin_casco

    global ultima_alerta

    global camara
    global ultimo_timestamp_ms

    # ------------------------------------------------------
    # VENTANA
    # ------------------------------------------------------

    cv2.namedWindow(
        "SafeVisionAI - Camara",
        cv2.WINDOW_NORMAL
    )

    cv2.resizeWindow(
        "SafeVisionAI - Camara",
        640,
        360
    )

    tiempo_inicio = time.time()

    # ------------------------------------------------------
    # BUCLE
    # ------------------------------------------------------

    while activo:

        # ==================================================
        # VERIFICAR CÁMARA
        # ==================================================

        if camara is None or not camara.isOpened():

            print(
                "⚠️ Cámara no disponible."
            )

            camara = abrir_camara()

            if camara is None:

                time.sleep(2)

                continue

            tiempo_inicio = time.time()

        # ==================================================
        # LEER FRAME
        # ==================================================

        ok, frame = camara.read()

        # ==================================================
        # FRAME FALLIDO
        # ==================================================

        if not ok or frame is None:

            print(
                "⚠️ No se pudo leer frame."
            )

            try:

                camara.release()

            except:
                pass

            camara = None

            time.sleep(0.5)

            continue

        # --------------------------------------------------
        # ESPEJO
        # --------------------------------------------------

        frame = cv2.flip(
            frame,
            1
        )

        # --------------------------------------------------
        # FRAME PARA MOSTRAR
        # --------------------------------------------------

        frame_normal = frame.copy()

        h, w = frame.shape[:2]

        # ==================================================
        # MEDIAPIPE
        # ==================================================

        rgb = cv2.cvtColor(
            frame,
            cv2.COLOR_BGR2RGB
        )

        mp_image = mp.Image(
            image_format=mp.ImageFormat.SRGB,
            data=rgb
        )

        # ==================================================
        # TIMESTAMP MEDIAPIPE
        # ==================================================

        timestamp_ms = int(time.time() * 1000)

        if timestamp_ms <= ultimo_timestamp_ms:
            timestamp_ms = ultimo_timestamp_ms + 1

        ultimo_timestamp_ms = timestamp_ms

        # ==================================================
        # DETECCIÓN MEDIAPIPE
        # ==================================================

        try:

            resultado = detector.detect_for_video(
                mp_image,
                timestamp_ms
            )

        except Exception as e:

            print(
                "⚠️ Error MediaPipe:",
                e
            )

            time.sleep(0.05)

            continue

        # ==================================================
        # VARIABLES DEL FRAME
        # ==================================================

        rostro_detectado = False

        casco_detectado = False

        zona_detectada = None

        ratio_detectado = 0.0

        # ==================================================
        # DETECTAR ROSTRO
        # ==================================================

        for det in resultado.detections:

            bb = det.bounding_box

            x = bb.origin_x
            y = bb.origin_y

            bw = bb.width
            bh = bb.height

            if bw <= 0 or bh <= 0:

                continue

            rostro_detectado = True

            # ----------------------------------------------
            # ZONA CASCO
            # ----------------------------------------------

            zona_detectada = calcular_zona_casco(
                (x, y, bw, bh),
                w,
                h
            )

            zx1, zy1, zx2, zy2 = zona_detectada

            # ----------------------------------------------
            # DETECTAR CASCO
            # ----------------------------------------------

            casco_ok, ratio = hay_casco(
                frame,
                zx1,
                zy1,
                zx2,
                zy2
            )

            ratio_detectado = ratio

            # ----------------------------------------------
            # COLOR
            # ----------------------------------------------

            if casco_ok:

                casco_detectado = True

                color = (
                    0,
                    200,
                    0
                )

                texto = (
                    f"CASCO OK "
                    f"({ratio * 100:.0f}%)"
                )

            else:

                casco_detectado = False

                color = (
                    0,
                    0,
                    255
                )

                texto = "NO HAY CASCO"

            # ----------------------------------------------
            # CUADRO CASCO
            # ----------------------------------------------

            cv2.rectangle(
                frame_normal,
                (zx1, zy1),
                (zx2, zy2),
                color,
                3
            )

            # ----------------------------------------------
            # CUADRO ROSTRO
            # ----------------------------------------------

            cv2.rectangle(
                frame_normal,
                (x, y),
                (x + bw, y + bh),
                color,
                2
            )

            # ----------------------------------------------
            # TEXTO
            # ----------------------------------------------

            cv2.putText(
                frame_normal,
                texto,
                (
                    zx1,
                    max(
                        30,
                        zy1 - 10
                    )
                ),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.7,
                color,
                2
            )

            # Solo primer rostro
            break

        # ==================================================
        # NO HAY ROSTRO
        # ==================================================

        if not rostro_detectado:

            estado_casco = "SIN DETECTAR"

            porcentaje_casco = 0.0

            frames_sin_casco = 0

            zona_detectada = None

            # ----------------------------------------------
            # PERSONA SALIÓ
            # ----------------------------------------------

            if alerta_activa:

                print()
                print(
                    "👤 Persona salió de la cámara."
                )

                print(
                    "🔓 Sistema rearmado."
                )

                print(
                    "Cuando vuelva sin casco "
                    "se generará una nueva alerta."
                )

                print()

                alerta_activa = False

            cv2.putText(
                frame_normal,
                "Buscando rostro...",
                (
                    20,
                    35
                ),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.75,
                (
                    255,
                    255,
                    255
                ),
                2
            )

        # ==================================================
        # ROSTRO + CASCO
        # ==================================================

        elif casco_detectado:

            estado_casco = "CASCO OK"

            porcentaje_casco = (
                ratio_detectado * 100
            )

            frames_sin_casco = 0

            # ----------------------------------------------
            # REARMAR
            # ----------------------------------------------

            if alerta_activa:

                print()
                print(
                    "🟢 Casco colocado nuevamente."
                )

                print(
                    "🟢 Sistema preparado "
                    "para nueva retirada."
                )

                print()

                trabajador_actual = buscar_trabajador_por_dni(DNI_TRABAJADOR_ACTUAL)
                if trabajador_actual:
                    acumular_deteccion_colocacion_casco(trabajador_actual)

                alerta_activa = False

        # ==================================================
        # ROSTRO + NO HAY CASCO
        # ==================================================

        else:

            estado_casco = "NO HAY CASCO"

            porcentaje_casco = 0.0

            # ----------------------------------------------
            # SOLO CONTAR CON CUADRO
            # ----------------------------------------------

            if zona_detectada is not None:

                frames_sin_casco += 1

            else:

                frames_sin_casco = 0

            # ----------------------------------------------
            # CONTADOR
            # ----------------------------------------------

            cv2.putText(
                frame_normal,
                (
                    f"Confirmando retirada "
                    f"{frames_sin_casco}/"
                    f"{FRAMES_CONFIRMACION_RETIRO}"
                ),
                (
                    20,
                    h - 20
                ),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.65,
                (
                    0,
                    0,
                    255
                ),
                2
            )

            # ----------------------------------------------
            # CONFIRMAR ALERTA
            # ----------------------------------------------

            if (
                frames_sin_casco
                >= FRAMES_CONFIRMACION_RETIRO

                and not alerta_activa

                and rostro_detectado

                and zona_detectada is not None
            ):

                print()
                print(
                    "🚨 Retirada de casco confirmada."
                )

                # ------------------------------------------
                # CREAR LAS 2 FOTOS
                # ------------------------------------------

                nueva_alerta = crear_fotos_alerta(
                    frame_normal,
                    zona_detectada
                )

                # ------------------------------------------
                # BLOQUEAR SOLO SI SE CREARON
                # ------------------------------------------

                if nueva_alerta is not None:

                    ultima_alerta = nueva_alerta

                    alerta_activa = True

                    print(
                        "🔒 Alerta bloqueada."
                    )

                    print(
                        "La alerta NO se repetirá "
                        "mientras la persona "
                        "permanezca sin casco."
                    )

                    print(
                        "Debe ponerse el casco "
                        "o salir de cámara."
                    )

                    print()

        # ==================================================
        # GUARDAR FRAME
        # ==================================================

        with bloqueo:

            frame_actual = frame_normal.copy()

        # ==================================================
        # MOSTRAR
        # ==================================================

        cv2.imshow(
            "SafeVisionAI - Camara",
            frame_normal
        )

        # ==================================================
        # SALIR
        # ==================================================

        tecla = cv2.waitKey(1) & 0xFF

        if tecla == ord("q"):

            activo = False

            break

    # ======================================================
    # CERRAR
    # ======================================================

    try:

        if camara is not None:

            camara.release()

    except:
        pass

    cv2.destroyAllWindows()


# ==========================================================
# MODELO
# ==========================================================

asegurar_modelo()

base_options = mp_base_options.BaseOptions(
    model_asset_path=MODEL_PATH
)

options = vision.FaceDetectorOptions(
    base_options=base_options,
    running_mode=vision.RunningMode.VIDEO,
    min_detection_confidence=0.5
)

detector = vision.FaceDetector.create_from_options(
    options
)


# ==========================================================
# CÁMARA INICIAL
# ==========================================================

camara = abrir_camara()


# ==========================================================
# HILO DE CÁMARA
# ==========================================================

threading.Thread(
    target=iniciar_camara,
    daemon=True
).start()


# ==========================================================
# CERRAR
# ==========================================================

@atexit.register
def cerrar():

    global activo
    global camara

    activo = False

    try:

        if camara is not None:

            camara.release()

    except:
        pass

    try:

        detector.close()

    except:
        pass

    cv2.destroyAllWindows()

    print(
        "Cámara cerrada"
    )


# ==========================================================
# OBTENER IP
# ==========================================================

def obtener_ip():

    try:

        s = socket.socket(
            socket.AF_INET,
            socket.SOCK_DGRAM
        )

        s.connect(
            (
                "8.8.8.8",
                80
            )
        )

        ip = s.getsockname()[0]

        s.close()

        return ip

    except:

        return "127.0.0.1"


# ==========================================================
# STREAM VIDEO
# ==========================================================

def video_stream():

    while activo:

        with bloqueo:

            if frame_actual is None:

                time.sleep(0.01)

                continue

            frame = frame_actual.copy()

        ok, buffer = cv2.imencode(
            ".jpg",
            frame
        )

        if ok:

            yield (
                b"--frame\r\n"
                b"Content-Type: image/jpeg\r\n\r\n"
                + buffer.tobytes()
                + b"\r\n"
            )

        time.sleep(0.03)


# ==========================================================
# PAGINA PRINCIPAL
# ==========================================================

@app.route("/")
def inicio():

    return """
    <html>

    <head>

        <title>SafeVisionAI</title>

    </head>

    <body
        style="
        background:#020D1D;
        color:white;
        text-align:center;
        font-family:Arial;
        "
    >

        <h2>
            SafeVisionAI - Cámara
        </h2>

        <img
            src="/video"
            width="640"
        >

    </body>

    </html>
    """


# ==========================================================
# VIDEO
# ==========================================================

@app.route("/video")
def video():

    return Response(
        video_stream(),
        mimetype=
        "multipart/x-mixed-replace; boundary=frame"
    )


# ==========================================================
# ESTADO
# ==========================================================

@app.route("/estado")
def estado():

    return jsonify({

        "sistema":
            "SafeVisionAI",

        "casco": {

            "estado":
                estado_casco,

            "porcentaje":
                round(
                    porcentaje_casco,
                    2
                ),

            "alerta_activa":
                alerta_activa
        }

    })


# ==========================================================
# ÚLTIMA ALERTA
# ==========================================================

@app.route("/alerta")
def alerta():

    if ultima_alerta is None:

        return jsonify({

            "hay_alerta":
                False

        })

    ip = obtener_ip()

    alerta_respuesta = (
        ultima_alerta.copy()
    )

    alerta_respuesta[
        "hay_alerta"
    ] = True

    alerta_respuesta[
        "foto_normal_url"
    ] = alerta_respuesta.get(
        "foto_normal_url",
        f"http://{ip}:{PUERTO}/fotos/"
        f"{ultima_alerta['foto_normal']}"
    )

    alerta_respuesta[
        "foto_zoom_url"
    ] = alerta_respuesta.get(
        "foto_zoom_url",
        f"http://{ip}:{PUERTO}/fotos/"
        f"{ultima_alerta['foto_zoom']}"
    )

    return jsonify(
        alerta_respuesta
    )


# ==========================================================
# SERVIR FOTOS
# ==========================================================

@app.route("/fotos/<nombre>")
def foto(nombre):

    return send_from_directory(
        CARPETA_FOTOS,
        nombre
    )


# ==========================================================
# ELIMINAR FOTOS DE UNA ALERTA
# ==========================================================

@app.route("/eliminar_fotos", methods=["POST"])
def endpoint_eliminar_fotos():
    """
    Elimina físicamente las imágenes asociadas a una alerta.
    Valida estrictamente que los archivos pertenezcan a CARPETA_FOTOS
    y no permite rutas arbitrarias fuera de esa carpeta.
    """
    datos = flask_request.get_json(silent=True) or flask_request.form or {}

    archivos_recibidos = []
    if isinstance(datos.get("fotos"), list):
        archivos_recibidos.extend(datos.get("fotos"))
    elif datos.get("fotos"):
        archivos_recibidos.append(str(datos.get("fotos")))

    for campo in ["foto_normal", "foto_zoom", "imagen", "imagen_normal", "imagen_zoom", "foto", "nombre"]:
        valor = datos.get(campo)
        if valor and isinstance(valor, str):
            archivos_recibidos.append(valor)

    carpeta_real = os.path.abspath(CARPETA_FOTOS)
    eliminadas = []
    no_encontradas = []
    rechazadas = []

    for item in archivos_recibidos:
        if not item or not isinstance(item, str):
            continue

        item_limpio = item.strip()
        if not item_limpio or item_limpio.lower() in ("null", "empty"):
            continue

        if "://" in item_limpio:
            item_limpio = item_limpio.split("/")[-1].split("?")[0]

        nombre_archivo = os.path.basename(item_limpio)
        if not nombre_archivo:
            continue

        extension_valida = any(nombre_archivo.lower().endswith(ext) for ext in [".jpg", ".jpeg", ".png"])
        es_alerta = nombre_archivo.startswith("alerta_")

        ruta_archivo = os.path.abspath(os.path.join(carpeta_real, nombre_archivo))

        esta_en_carpeta = (
            os.path.commonpath([carpeta_real, ruta_archivo]) == carpeta_real
            and ruta_archivo.startswith(carpeta_real + os.sep)
        )

        if not (extension_valida and es_alerta and esta_en_carpeta):
            print(f"⚠️ Archivo rechazado por seguridad/formato: {nombre_archivo}")
            rechazadas.append(nombre_archivo)
            continue

        if os.path.isfile(ruta_archivo):
            try:
                os.remove(ruta_archivo)
                eliminadas.append(nombre_archivo)
                print(f"🗑️ Imagen eliminada de SafeVisionCamera/alertas/: {nombre_archivo}")
            except Exception as e:
                print(f"⚠️ Error al eliminar {nombre_archivo}: {e}")
        else:
            no_encontradas.append(nombre_archivo)
            print(f"ℹ️ Imagen no encontrada físicamente (ya no existía): {nombre_archivo}")

    return jsonify({
        "status": "ok",
        "eliminadas": eliminadas,
        "no_encontradas": no_encontradas,
        "rechazadas": rechazadas
    }), 200


# ==========================================================
# INFO
# ==========================================================

@app.route("/info")
def info():

    return jsonify({

        "nombre":
            "SafeVisionAI_CAMERA",

        "tipo":
            "camara",

        "estado":
            "activo",

        "deteccion_casco":
            True

    })


# ==========================================================
# GESTIÓN DNI TRABAJADOR
# ==========================================================

@app.route("/trabajador", methods=["GET", "POST"])
def endpoint_trabajador():
    global DNI_TRABAJADOR_ACTUAL
    if request.method == "POST":
        data = request.get_json(silent=True) or request.form
        nuevo_dni = data.get("dni")
        if nuevo_dni:
            DNI_TRABAJADOR_ACTUAL = str(nuevo_dni).strip()
            trabajador, _ = obtener_o_crear_trabajador(DNI_TRABAJADOR_ACTUAL)
            return jsonify({
                "mensaje": "DNI actualizado correctamente",
                "dni": DNI_TRABAJADOR_ACTUAL,
                "trabajador": trabajador
            })
        return jsonify({"error": "DNI no especificado"}), 400

    trabajador = buscar_trabajador_por_dni(DNI_TRABAJADOR_ACTUAL)
    return jsonify({
        "dni_actual": DNI_TRABAJADOR_ACTUAL,
        "trabajador": trabajador
    })


# ==========================================================
# INICIO FLASK
# ==========================================================

if __name__ == "__main__":

    ip = obtener_ip()

    print()
    print("==============================")
    print("     SAFEVISIONAI CAMERA")
    print("==============================")
    print()

    print(
        "CELULAR:"
    )

    print(
        f"http://{ip}:5000"
    )

    print()

    print(
        "ESTADO CASCO:"
    )

    print(
        f"http://{ip}:5000/estado"
    )

    print()

    print(
        "ALERTA:"
    )

    print(
        f"http://{ip}:5000/alerta"
    )

    print()

    print(
        "FOTOS:"
    )

    print(
        f"http://{ip}:5000/fotos/"
    )

    print()

    print("==============================")

    app.run(
        host="0.0.0.0",
        port=5000,
        threaded=True
    )