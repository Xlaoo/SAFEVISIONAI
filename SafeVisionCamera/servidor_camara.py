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

import os
import time
import socket
import atexit
import threading
from datetime import datetime

import cv2
import requests
from flask import (
    Flask,
    Response,
    jsonify,
    send_from_directory,
    request as flask_request,
    request,
)

# Integración del detector EPP modular (Casco + Chaleco)
from detector_epp import (
    crear_detector_epp,
    procesar_frame_epp,
    asegurar_modelo,
)


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
SUPABASE_STORAGE_BUCKET = "fotos-alertas"
SUPABASE_STORAGE_URL = SUPABASE_URL + "/storage/v1"

# DNI del trabajador actual detectado o asignado
DNI_TRABAJADOR_ACTUAL = os.getenv("TRABAJADOR_DNI", "12345678")
SUPABASE_TIMEOUT = 10


# ==========================================================
# FOTOS Y ALERTAS
# ==========================================================

CARPETA_FOTOS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "alertas")
os.makedirs(CARPETA_FOTOS, exist_ok=True)


# ==========================================================
# VARIABLES GLOBALES Y ESTADO COMPARTIDO
# ==========================================================

frame_actual = None
bloqueo = threading.Lock()
activo = True
camara = None
detector = None
ultimo_timestamp_ms = 0

# Estado conjunto EPP
estado_epp_actual = {
    "casco": False,
    "chaleco": False,
    "epp_completo": False,
    "estado": "BUSCANDO PERSONA",
    "porcentaje_casco": 0.0,
    "porcentaje_chaleco": 0.0,
    "rostro_detectado": False,
}

# Variables de retrocompatibilidad
estado_casco = "SIN DETECTAR"
porcentaje_casco = 0.0

# Control de alerta existente
alerta_activa = False
tipo_falta_alerta_activa = None
frames_falta_actual = 0
condicion_falta_actual = None
frames_sin_casco = 0
FRAMES_CONFIRMACION_RETIRO = 8

ultimo_id_alerta = 0
ultima_alerta = None


# ==========================================================
# FUNCIONES SUPABASE: TRABAJADORES Y DNI
# ==========================================================

def obtener_headers_supabase():
    return {
        "apikey": SUPABASE_API_KEY,
        "Authorization": "Bearer " + SUPABASE_API_KEY,
        "Content-Type": "application/json",
        "Prefer": "return=representation",
    }


def buscar_trabajador_por_dni(dni):
    if not SUPABASE_API_KEY or SUPABASE_API_KEY == "REEMPLAZA_CON_TU_API_KEY":
        return None

    try:
        url = f"{SUPABASE_TRABAJADORES_URL}?dni=eq.{dni}&select=*"
        res = requests.get(
            url,
            headers=obtener_headers_supabase(),
            timeout=SUPABASE_TIMEOUT,
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
            "chaleco_colocaciones": 0,
        }
        res = requests.post(
            SUPABASE_TRABAJADORES_URL,
            headers=obtener_headers_supabase(),
            json=datos,
            timeout=SUPABASE_TIMEOUT,
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
    trabajador = buscar_trabajador_por_dni(dni)
    if trabajador is not None:
        print(f"👤 Trabajador existente encontrado: ID {trabajador.get('id')} ({trabajador.get('nombres')} {trabajador.get('apellidos')}) - DNI: {dni}")
        return trabajador, False

    print(f"⚠️ DNI {dni} no registrado. Creando nuevo trabajador en Supabase...")
    nuevo = registrar_trabajador_nuevo(dni)
    return nuevo, True


def acumular_deteccion_retiro_casco(trabajador):
    if not trabajador or not SUPABASE_API_KEY:
        return trabajador

    try:
        trabajador_id = trabajador.get("id")
        actual_retiros = int(trabajador.get("casco_retiros") or 0)
        nuevo_retiros = actual_retiros + 1

        url = f"{SUPABASE_TRABAJADORES_URL}?id=eq.{trabajador_id}"
        datos = {
            "casco_retiros": nuevo_retiros,
            "casco": False,
        }
        res = requests.patch(
            url,
            headers=obtener_headers_supabase(),
            json=datos,
            timeout=SUPABASE_TIMEOUT,
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
    if not trabajador or not SUPABASE_API_KEY:
        return trabajador

    try:
        trabajador_id = trabajador.get("id")
        actual_colocaciones = int(trabajador.get("casco_colocaciones") or 0)
        nuevo_colocaciones = actual_colocaciones + 1

        url = f"{SUPABASE_TRABAJADORES_URL}?id=eq.{trabajador_id}"
        datos = {
            "casco_colocaciones": nuevo_colocaciones,
            "casco": True,
        }
        res = requests.patch(
            url,
            headers=obtener_headers_supabase(),
            json=datos,
            timeout=SUPABASE_TIMEOUT,
        )
        if res.ok:
            trabajador["casco_colocaciones"] = nuevo_colocaciones
            trabajador["casco"] = True
            print(f"🟢 Colocación registrada: trabajador #{trabajador_id} - casco_colocaciones = {nuevo_colocaciones}")
        else:
            print("⚠️ Error registrando colocación de casco:", res.status_code, res.text)
    except Exception as e:
        print("⚠️ Error registrando colocación de casco:", e)

    return trabajador


def acumular_deteccion_retiro_chaleco(trabajador):
    if not trabajador or not SUPABASE_API_KEY:
        return trabajador

    try:
        trabajador_id = trabajador.get("id")
        actual_retiros = int(trabajador.get("chaleco_retiros") or 0)
        nuevo_retiros = actual_retiros + 1

        url = f"{SUPABASE_TRABAJADORES_URL}?id=eq.{trabajador_id}"
        datos = {
            "chaleco_retiros": nuevo_retiros,
            "chaleco": False,
        }
        res = requests.patch(
            url,
            headers=obtener_headers_supabase(),
            json=datos,
            timeout=SUPABASE_TIMEOUT,
        )
        if res.ok:
            trabajador["chaleco_retiros"] = nuevo_retiros
            trabajador["chaleco"] = False
            print(f"📊 Detección acumulada: trabajador #{trabajador_id} - chaleco_retiros = {nuevo_retiros}")
        else:
            print(f"⚠️ Error actualizando contadores de trabajador #{trabajador_id}:", res.status_code, res.text)
    except Exception as e:
        print("⚠️ Error acumulando detección de retiro de chaleco:", e)

    return trabajador


def acumular_deteccion_colocacion_chaleco(trabajador):
    if not trabajador or not SUPABASE_API_KEY:
        return trabajador

    try:
        trabajador_id = trabajador.get("id")
        actual_colocaciones = int(trabajador.get("chaleco_colocaciones") or 0)
        nuevo_colocaciones = actual_colocaciones + 1

        url = f"{SUPABASE_TRABAJADORES_URL}?id=eq.{trabajador_id}"
        datos = {
            "chaleco_colocaciones": nuevo_colocaciones,
            "chaleco": True,
        }
        res = requests.patch(
            url,
            headers=obtener_headers_supabase(),
            json=datos,
            timeout=SUPABASE_TIMEOUT,
        )
        if res.ok:
            trabajador["chaleco_colocaciones"] = nuevo_colocaciones
            trabajador["chaleco"] = True
            print(f"🟢 Colocación registrada: trabajador #{trabajador_id} - chaleco_colocaciones = {nuevo_colocaciones}")
        else:
            print("⚠️ Error registrando colocación de chaleco:", res.status_code, res.text)
    except Exception as e:
        print("⚠️ Error registrando colocación de chaleco:", e)

    return trabajador


def asegurar_bucket_storage():
    """
    Verifica que el bucket fotos-alertas exista en Supabase Storage
    y que sea público. Si no existe, lo crea automáticamente.
    """
    if not SUPABASE_API_KEY or SUPABASE_API_KEY == "REEMPLAZA_CON_TU_API_KEY":
        return False
    try:
        url_bucket = f"{SUPABASE_STORAGE_URL}/bucket/{SUPABASE_STORAGE_BUCKET}"
        headers = {
            "apikey": SUPABASE_API_KEY,
            "Authorization": f"Bearer {SUPABASE_API_KEY}",
            "Content-Type": "application/json",
        }
        res = requests.get(url_bucket, headers=headers, timeout=SUPABASE_TIMEOUT)
        if res.status_code == 200:
            return True
        elif res.status_code == 404:
            payload = {
                "id": SUPABASE_STORAGE_BUCKET,
                "name": SUPABASE_STORAGE_BUCKET,
                "public": True,
            }
            res_c = requests.post(
                f"{SUPABASE_STORAGE_URL}/bucket",
                headers=headers,
                json=payload,
                timeout=SUPABASE_TIMEOUT,
            )
            if res_c.ok:
                print(f"☁️ Bucket de almacenamiento '{SUPABASE_STORAGE_BUCKET}' creado con acceso público.")
                return True
            else:
                print(f"⚠️ Error creando bucket en Supabase Storage: {res_c.status_code} {res_c.text}")
                return False
        return False
    except Exception as e:
        print("⚠️ Excepción al verificar bucket en Supabase Storage:", e)
        return False


def subir_foto_supabase_storage(ruta_archivo, nombre_archivo):
    """
    Sube un archivo de imagen al bucket fotos-alertas de Supabase Storage
    y retorna su URL pública y permanente.
    """
    if not SUPABASE_API_KEY or SUPABASE_API_KEY == "REEMPLAZA_CON_TU_API_KEY":
        return None

    if not os.path.exists(ruta_archivo):
        return None

    try:
        url_upload = f"{SUPABASE_STORAGE_URL}/object/{SUPABASE_STORAGE_BUCKET}/{nombre_archivo}"
        headers = {
            "apikey": SUPABASE_API_KEY,
            "Authorization": f"Bearer {SUPABASE_API_KEY}",
            "Content-Type": "image/jpeg",
            "x-upsert": "true",
        }
        with open(ruta_archivo, "rb") as f:
            datos_bytes = f.read()

        res = requests.post(url_upload, headers=headers, data=datos_bytes, timeout=SUPABASE_TIMEOUT)
        if res.ok or res.status_code in (200, 201):
            url_publica = f"{SUPABASE_STORAGE_URL}/object/public/{SUPABASE_STORAGE_BUCKET}/{nombre_archivo}"
            print(f"☁️ FOTO SUBIDA A SUPABASE STORAGE: {nombre_archivo}")
            return url_publica
        else:
            print(f"⚠️ Error subiendo foto {nombre_archivo} a Supabase Storage: {res.status_code} {res.text}")
            return None
    except Exception as e:
        print(f"⚠️ Excepción subiendo foto {nombre_archivo} a Supabase Storage:", e)
        return None


def guardar_alerta_supabase(trabajador_id, fecha, problema, casco_bool, chaleco_bool, foto_normal_url, foto_zoom_url):
    if not SUPABASE_API_KEY or SUPABASE_API_KEY == "REEMPLAZA_CON_TU_API_KEY":
        return None

    headers = obtener_headers_supabase()
    datos = {
        "trabajador_id": trabajador_id,
        "problema": problema,
        "casco": casco_bool,
        "chaleco": chaleco_bool,
        "estado": "PENDIENTE",
        "imagen": foto_normal_url,
        "imagen_normal": foto_normal_url,
        "imagen_zoom": foto_zoom_url,
    }

    try:
        respuesta = requests.post(
            SUPABASE_ALERTAS_URL,
            headers=headers,
            json=datos,
            timeout=SUPABASE_TIMEOUT,
        )
        if respuesta.ok:
            try:
                resultado = respuesta.json()
            except Exception:
                resultado = []

            if isinstance(resultado, list) and len(resultado) > 0:
                id_supabase = resultado[0].get("id")
                print("✅ ALERTA GUARDADA EN SUPABASE - ID:", id_supabase)
                return id_supabase
            return None
        print("❌ ERROR GUARDANDO ALERTA EN SUPABASE:", respuesta.status_code, respuesta.text)
        return None
    except requests.RequestException as e:
        print("❌ ERROR DE CONEXIÓN CON SUPABASE:", e)
        return None


def crear_fotos_alerta(frame_con_cuadro, zona_zoom, tipo_falta="CASCO_RETIRADO", casco_bool=False, chaleco_bool=True, problema_texto="Trabajador sin casco"):
    global ultimo_id_alerta

    if zona_zoom is None:
        return None

    ahora = datetime.now()
    x1, y1, x2, y2 = zona_zoom
    h, w = frame_con_cuadro.shape[:2]

    x1 = max(0, min(x1, w - 1))
    y1 = max(0, min(y1, h - 1))
    x2 = min(w, x2)
    y2 = min(h, y2)

    if x2 <= x1 or y2 <= y1:
        return None

    ultimo_id_alerta += 1
    id_alerta = ultimo_id_alerta
    fecha_archivo = ahora.strftime("%Y%m%d_%H%M%S_%f")

    nombre_normal = f"alerta_{id_alerta}_normal_{fecha_archivo}.jpg"
    nombre_zoom = f"alerta_{id_alerta}_zoom_{fecha_archivo}.jpg"

    ruta_normal = os.path.join(CARPETA_FOTOS, nombre_normal)
    ruta_zoom = os.path.join(CARPETA_FOTOS, nombre_zoom)

    guardado_normal = cv2.imwrite(ruta_normal, frame_con_cuadro)
    if not guardado_normal:
        return None

    zoom = frame_con_cuadro[y1:y2, x1:x2]
    if zoom.size == 0:
        try:
            os.remove(ruta_normal)
        except Exception:
            pass
        return None

    zoom = cv2.resize(zoom, None, fx=2.5, fy=2.5, interpolation=cv2.INTER_CUBIC)
    guardado_zoom = cv2.imwrite(ruta_zoom, zoom)
    if not guardado_zoom:
        try:
            os.remove(ruta_normal)
        except Exception:
            pass
        return None

    print(f"🚨 ALERTA EPP GENERADA ({tipo_falta}) - Fotos: {nombre_normal} / {nombre_zoom}")

    ip = obtener_ip()
    foto_normal_local = f"http://{ip}:{PUERTO}/fotos/{nombre_normal}"
    foto_zoom_local = f"http://{ip}:{PUERTO}/fotos/{nombre_zoom}"

    # Subir fotografías permanentemente a Supabase Storage
    url_storage_normal = subir_foto_supabase_storage(ruta_normal, nombre_normal)
    url_storage_zoom = subir_foto_supabase_storage(ruta_zoom, nombre_zoom)

    # La URL permanente de Supabase Storage es la definitiva; respaldo local si no hubo conexión
    foto_normal_url = url_storage_normal if url_storage_normal else foto_normal_local
    foto_zoom_url = url_storage_zoom if url_storage_zoom else foto_zoom_local

    trabajador, _ = obtener_o_crear_trabajador(DNI_TRABAJADOR_ACTUAL)
    trabajador_id = trabajador.get("id") if trabajador else 1

    if trabajador:
        if not casco_bool:
            acumular_deteccion_retiro_casco(trabajador)
        if not chaleco_bool:
            acumular_deteccion_retiro_chaleco(trabajador)

    id_supabase = guardar_alerta_supabase(
        trabajador_id,
        ahora.isoformat(),
        problema_texto,
        casco_bool,
        chaleco_bool,
        foto_normal_url,
        foto_zoom_url,
    )

    return {
        "id": id_supabase if id_supabase is not None else id_alerta,
        "id_local": id_alerta,
        "trabajador_id": trabajador_id,
        "tipo": tipo_falta,
        "problema": problema_texto,
        "casco": casco_bool,
        "chaleco": chaleco_bool,
        "estado": "PENDIENTE",
        "fecha": ahora.isoformat(),
        "foto_normal": nombre_normal,
        "foto_zoom": nombre_zoom,
        "foto_normal_url": foto_normal_url,
        "foto_zoom_url": foto_zoom_url,
        "guardada_supabase": id_supabase is not None,
    }


# ==========================================================
# ABRIR CÁMARA ÚNICA
# ==========================================================

def abrir_camara():
    print()
    print("🎥 Intentando abrir cámara única...")

    # Probar índice 0 (estándar probado en detector_epp.py) y luego 1
    for indice in [0, 1]:
        try:
            nueva_camara = cv2.VideoCapture(indice, cv2.CAP_DSHOW)
            if not nueva_camara.isOpened():
                nueva_camara = cv2.VideoCapture(indice)

            if nueva_camara.isOpened():
                nueva_camara.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
                nueva_camara.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
                print(f"✅ Cámara abierta correctamente en índice {indice}.")
                return nueva_camara
            else:
                nueva_camara.release()
        except Exception as e:
            print(f"⚠️ Error probando cámara índice {indice}:", e)

    print("❌ No se pudo abrir ninguna cámara.")
    return None


# ==========================================================
# PROCESAMIENTO DE CÁMARA (HILO DE DETECCIÓN EPP)
# ==========================================================

def iniciar_camara():
    global frame_actual
    global activo
    global estado_epp_actual
    global estado_casco
    global porcentaje_casco
    global alerta_activa
    global tipo_falta_alerta_activa
    global frames_falta_actual
    global condicion_falta_actual
    global frames_sin_casco
    global ultima_alerta
    global camara
    global detector
    global ultimo_timestamp_ms

    nombre_ventana = "SafeVisionAI - Camara (Casco + Chaleco)"
    cv2.namedWindow(nombre_ventana, cv2.WINDOW_NORMAL)
    cv2.resizeWindow(nombre_ventana, 800, 600)

    # 1. Asegurar e inicializar detector MediaPipe DENTRO del hilo de cámara
    asegurar_modelo()
    try:
        detector = crear_detector_epp()
    except Exception as e:
        print("❌ Error fatal al inicializar MediaPipe FaceDetector:", e)
        activo = False
        return

    # 2. Inicializar cámara
    camara = abrir_camara()

    t0 = time.time()
    reintentos_recuperacion = 0
    consecutivos_exito = 0
    ultimo_error_impreso = None
    tiempo_ultimo_error = 0.0

    try:
        while activo:
            if camara is None or not camara.isOpened():
                print("⚠️ Cámara no disponible. Intentando reconectar...")
                camara = abrir_camara()
                if camara is None:
                    time.sleep(2)
                    continue
                t0 = time.time()

            ok, frame = camara.read()
            if not ok or frame is None:
                print("⚠️ No se pudo leer frame de la cámara.")
                try:
                    camara.release()
                except Exception:
                    pass
                camara = None
                time.sleep(0.5)
                continue

            # Efecto espejo
            frame = cv2.flip(frame, 1)

            # Control de timestamps para MediaPipe
            timestamp_ms = int((time.time() - t0) * 1000)
            if timestamp_ms <= ultimo_timestamp_ms:
                timestamp_ms = ultimo_timestamp_ms + 1
            ultimo_timestamp_ms = timestamp_ms

            # Procesamiento coordinado de EPP (Casco + Chaleco)
            try:
                res_epp = procesar_frame_epp(
                    frame,
                    detector,
                    timestamp_ms,
                    dibujar_hud=True,
                )
                consecutivos_exito += 1
                if reintentos_recuperacion > 0 and consecutivos_exito >= 5:
                    reintentos_recuperacion = 0
            except Exception as e:
                err_str = str(e)
                consecutivos_exito = 0

                # Control específico de error por cierre de executor en MediaPipe
                if "cannot schedule new futures after shutdown" in err_str or "shutdown" in err_str.lower():
                    if reintentos_recuperacion < 3:
                        reintentos_recuperacion += 1
                        print(f"⚠️ Detector MediaPipe cerrado o inválido. Reintentando recuperación ({reintentos_recuperacion}/3)...")
                        try:
                            if detector is not None:
                                detector.close()
                        except Exception:
                            pass
                        detector = None
                        time.sleep(0.5)
                        try:
                            detector = crear_detector_epp()
                            print(f"✅ Detector MediaPipe recreado con éxito (intento {reintentos_recuperacion}/3).")
                            continue
                        except Exception as err_recrear:
                            print(f"❌ Error al recrear detector MediaPipe: {err_recrear}")
                            continue
                    else:
                        print("❌ Error fatal: MediaPipe falló tras 3 intentos consecutivos de recuperación. Deteniendo hilo de cámara de forma controlada.")
                        activo = False
                        break
                else:
                    # Para otros errores no permitimos spam continuo idéntico
                    ahora = time.time()
                    if err_str != ultimo_error_impreso or (ahora - tiempo_ultimo_error) > 5.0:
                        print("⚠️ Error en procesamiento EPP:", e)
                        ultimo_error_impreso = err_str
                        tiempo_ultimo_error = ahora
                    time.sleep(0.05)
                    continue

            # --------------------------------------------------
            # ACTUALIZACIÓN DE ESTADO COMPARTIDO (CON LOCK)
            # --------------------------------------------------
            with bloqueo:
                estado_epp_actual = {
                    "casco": res_epp["casco"],
                    "chaleco": res_epp["chaleco"],
                    "epp_completo": res_epp["epp_completo"],
                    "estado": res_epp["estado"],
                    "porcentaje_casco": res_epp["porcentaje_casco"],
                    "porcentaje_chaleco": res_epp["porcentaje_chaleco"],
                    "rostro_detectado": res_epp["rostro_detectado"],
                }
                estado_casco = "CASCO OK" if res_epp["casco"] else ("NO HAY CASCO" if res_epp["rostro_detectado"] else "SIN DETECTAR")
                porcentaje_casco = res_epp["porcentaje_casco"]
                frame_actual = res_epp["frame_anotado"].copy()

            # --------------------------------------------------
            # GESTIÓN DE ALERTAS (EPP: Casco + Chaleco)
            # --------------------------------------------------
            rostro_detectado = res_epp["rostro_detectado"]
            casco_ok = res_epp["casco"]
            chaleco_ok = res_epp["chaleco"]
            epp_completo = res_epp["epp_completo"]
            zona_casco = res_epp.get("zona_casco")
            zona_chaleco = res_epp.get("zona_chaleco")

            if not rostro_detectado:
                frames_falta_actual = 0
                condicion_falta_actual = None
                if alerta_activa:
                    print("👤 Persona salió de la cámara. Alerta rearmada.")
                    alerta_activa = False
                    tipo_falta_alerta_activa = None

            elif epp_completo:
                frames_falta_actual = 0
                condicion_falta_actual = None
                if alerta_activa:
                    print("🟢 EPP completo colocado nuevamente. Registrando colocación y rearmando sistema...")
                    trabajador_actual = buscar_trabajador_por_dni(DNI_TRABAJADOR_ACTUAL)
                    if trabajador_actual:
                        if tipo_falta_alerta_activa in ("FALTA_CASCO", "FALTA_AMBOS"):
                            acumular_deteccion_colocacion_casco(trabajador_actual)
                        if tipo_falta_alerta_activa in ("FALTA_CHALECO", "FALTA_AMBOS"):
                            acumular_deteccion_colocacion_chaleco(trabajador_actual)
                    alerta_activa = False
                    tipo_falta_alerta_activa = None

            else:
                # Rostro presente pero falta al menos un elemento de EPP
                if not casco_ok and not chaleco_ok:
                    condicion_ahora = "FALTA_AMBOS"
                    problema_texto = "Faltan casco y chaleco de seguridad"
                    tipo_falta_code = "EPP_INCOMPLETO"
                elif not casco_ok:
                    condicion_ahora = "FALTA_CASCO"
                    problema_texto = "Trabajador sin casco de seguridad"
                    tipo_falta_code = "CASCO_RETIRADO"
                else:
                    condicion_ahora = "FALTA_CHALECO"
                    problema_texto = "Trabajador sin chaleco reflectante"
                    tipo_falta_code = "CHALECO_RETIRADO"

                # Determinar zona zoom según la falta
                if condicion_ahora == "FALTA_AMBOS":
                    if zona_casco is not None and zona_chaleco is not None:
                        zona_zoom_candidata = (
                            min(zona_casco[0], zona_chaleco[0]),
                            min(zona_casco[1], zona_chaleco[1]),
                            max(zona_casco[2], zona_chaleco[2]),
                            max(zona_casco[3], zona_chaleco[3]),
                        )
                    else:
                        zona_zoom_candidata = zona_casco if zona_casco is not None else zona_chaleco
                elif condicion_ahora == "FALTA_CASCO":
                    zona_zoom_candidata = zona_casco
                else:
                    zona_zoom_candidata = zona_chaleco

                # Antiduplicación: si la misma condición ya generó alerta activa, no volver a disparar
                if alerta_activa and tipo_falta_alerta_activa == condicion_ahora:
                    frames_falta_actual = 0
                else:
                    if condicion_ahora == condicion_falta_actual and zona_zoom_candidata is not None:
                        frames_falta_actual += 1
                    else:
                        condicion_falta_actual = condicion_ahora
                        frames_falta_actual = 1 if zona_zoom_candidata is not None else 0

                    # Indicador de confirmación sobre la ventana
                    h_f = res_epp["frame_anotado"].shape[0]
                    cv2.putText(
                        res_epp["frame_anotado"],
                        f"Confirmando {condicion_ahora} {frames_falta_actual}/{FRAMES_CONFIRMACION_RETIRO}",
                        (20, h_f - 20),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        0.65,
                        (0, 0, 255),
                        2,
                    )

                    if (
                        frames_falta_actual >= FRAMES_CONFIRMACION_RETIRO
                        and rostro_detectado
                        and zona_zoom_candidata is not None
                    ):
                        print(f"🚨 Falta de EPP confirmada ({condicion_ahora}). Generando alerta...")
                        nueva_alerta = crear_fotos_alerta(
                            res_epp["frame_anotado"],
                            zona_zoom_candidata,
                            tipo_falta=tipo_falta_code,
                            casco_bool=casco_ok,
                            chaleco_bool=chaleco_ok,
                            problema_texto=problema_texto,
                        )
                        if nueva_alerta is not None:
                            with bloqueo:
                                ultima_alerta = nueva_alerta
                            alerta_activa = True
                            tipo_falta_alerta_activa = condicion_ahora
                            frames_falta_actual = 0

            # Mostrar en ventana única de 800x600
            cv2.imshow(nombre_ventana, res_epp["frame_anotado"])

            tecla = cv2.waitKey(1) & 0xFF
            if tecla == ord("q"):
                activo = False
                break
    finally:
        # Cierre controlado y seguro de cámara, detector y ventana en el MISMO hilo
        try:
            if camara is not None:
                camara.release()
        except Exception:
            pass
        try:
            if detector is not None:
                detector.close()
        except Exception:
            pass
        cv2.destroyAllWindows()
        print("Cámara y detector EPP cerrados correctamente.")


# ==========================================================
# CERRAR APLICACIÓN
# ==========================================================

@atexit.register
def cerrar():
    global activo
    global camara

    activo = False

    try:
        if camara is not None:
            camara.release()
    except Exception:
        pass

    cv2.destroyAllWindows()
    print("Cámara cerrada.")


# ==========================================================
# OBTENER IP
# ==========================================================

def obtener_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


# ==========================================================
# STREAM VIDEO MJPEG
# ==========================================================

def video_stream():
    while activo:
        with bloqueo:
            if frame_actual is None:
                time.sleep(0.01)
                continue
            frame = frame_actual.copy()

        ok, buffer = cv2.imencode(".jpg", frame)
        if ok:
            yield (
                b"--frame\r\n"
                b"Content-Type: image/jpeg\r\n\r\n"
                + buffer.tobytes()
                + b"\r\n"
            )
        time.sleep(0.03)


# ==========================================================
# ENDPOINTS FLASK
# ==========================================================

@app.route("/")
def inicio():
    return """
    <html>
    <head>
        <title>SafeVisionAI</title>
    </head>
    <body style="background:#020D1D; color:white; text-align:center; font-family:Arial;">
        <h2>SafeVisionAI - Cámara EPP (Casco + Chaleco)</h2>
        <img src="/video" width="640">
    </body>
    </html>
    """


@app.route("/video")
def video():
    return Response(
        video_stream(),
        mimetype="multipart/x-mixed-replace; boundary=frame",
    )


@app.route("/estado")
def estado():
    with bloqueo:
        datos = estado_epp_actual.copy()
        c_estado = estado_casco
        c_porcentaje = porcentaje_casco
        a_activa = alerta_activa

    # Respuesta unificada solicitada para Android con retrocompatibilidad
    return jsonify({
        "sistema": "SafeVisionAI",
        "casco": datos["casco"],
        "chaleco": datos["chaleco"],
        "epp_completo": datos["epp_completo"],
        "estado": datos["estado"],
        "porcentaje_casco": datos["porcentaje_casco"],
        "porcentaje_chaleco": datos["porcentaje_chaleco"],
        "rostro_detectado": datos["rostro_detectado"],
        "detalle_casco": {
            "estado": c_estado,
            "porcentaje": round(c_porcentaje, 2),
            "alerta_activa": a_activa,
        },
    })


@app.route("/alerta")
def alerta():
    if ultima_alerta is None:
        return jsonify({"hay_alerta": False})

    ip = obtener_ip()
    alerta_respuesta = ultima_alerta.copy()
    alerta_respuesta["hay_alerta"] = True
    alerta_respuesta["foto_normal_url"] = alerta_respuesta.get(
        "foto_normal_url",
        f"http://{ip}:{PUERTO}/fotos/{ultima_alerta['foto_normal']}",
    )
    alerta_respuesta["foto_zoom_url"] = alerta_respuesta.get(
        "foto_zoom_url",
        f"http://{ip}:{PUERTO}/fotos/{ultima_alerta['foto_zoom']}",
    )
    return jsonify(alerta_respuesta)


@app.route("/fotos/<nombre>")
def foto(nombre):
    return send_from_directory(CARPETA_FOTOS, nombre)


@app.route("/eliminar_fotos", methods=["POST"])
def endpoint_eliminar_fotos():
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

    return jsonify({
        "status": "ok",
        "eliminadas": eliminadas,
        "no_encontradas": no_encontradas,
        "rechazadas": rechazadas,
    }), 200


@app.route("/info")
def info():
    return jsonify({
        "nombre": "SafeVisionAI_CAMERA",
        "tipo": "camara",
        "estado": "activo",
        "deteccion_epp": True,
        "deteccion_casco": True,
        "deteccion_chaleco": True,
    })


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
                "trabajador": trabajador,
            })
        return jsonify({"error": "DNI no especificado"}), 400

    trabajador = buscar_trabajador_por_dni(DNI_TRABAJADOR_ACTUAL)
    return jsonify({
        "dni_actual": DNI_TRABAJADOR_ACTUAL,
        "trabajador": trabajador,
    })


# ==========================================================
# DESCUBRIMIENTO DINÁMICO EN LAN (UDP BROADCAST)
# ==========================================================

PUERTO_UDP_DESCUBRIMIENTO = 5005

def responder_descubrimiento_lan():
    """
    Escucha paquetes UDP de descubrimiento enviados por SafeVisionAI Android
    en la red LAN y responde con los datos del servidor para conexión dinámica.
    """
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(("", PUERTO_UDP_DESCUBRIMIENTO))
        print(f"📡 Servicio de descubrimiento LAN activo en puerto UDP {PUERTO_UDP_DESCUBRIMIENTO}.")
    except Exception as e:
        print(f"⚠️ No se pudo iniciar el servicio de descubrimiento UDP ({e}).")
        return

    while activo:
        try:
            data, addr = sock.recvfrom(1024)
            if not data:
                continue
            mensaje = data.decode("utf-8", errors="ignore").strip()
            if "SAFEVISION_DISCOVER" in mensaje or "SAFEVISION_DISCOVERY_REQUEST" in mensaje:
                # Responder con información del servidor y puerto HTTP 5000
                respuesta = '{"nombre":"SafeVisionAI_CAMERA","estado":"activo","puerto":5000}'
                sock.sendto(respuesta.encode("utf-8"), addr)
        except Exception:
            if not activo:
                break
            time.sleep(0.1)

    try:
        sock.close()
    except Exception:
        pass


# ==========================================================
# INICIO FLASK Y HILOS DE EJECUCIÓN
# ==========================================================

if __name__ == "__main__":
    ip = obtener_ip()
    asegurar_bucket_storage()

    # 1. Iniciar hilo de descubrimiento LAN (UDP)
    hilo_descubrimiento = threading.Thread(
        target=responder_descubrimiento_lan,
        daemon=True,
    )
    hilo_descubrimiento.start()

    # 2. Iniciar hilo de cámara y detector EPP
    hilo_camara = threading.Thread(
        target=iniciar_camara,
        daemon=True,
    )
    hilo_camara.start()

    print()
    print("==============================")
    print("     SAFEVISIONAI CAMERA")
    print("  Coordinador EPP Integrado")
    print("==============================")
    print()
    print(f"CELULAR: http://{ip}:5000")
    print(f"ESTADO EPP: http://{ip}:5000/estado")
    print(f"STREAM VIDEO: http://{ip}:5000/video")
    print(f"ALERTA: http://{ip}:5000/alerta")
    print(f"FOTOS LOCALES: http://{ip}:5000/fotos/")
    print(f"STORAGE PUBLICO: {SUPABASE_STORAGE_URL}/object/public/{SUPABASE_STORAGE_BUCKET}/")
    print(f"DESCUBRIMIENTO LAN UDP: Puerto {PUERTO_UDP_DESCUBRIMIENTO}")
    print("==============================")

    app.run(
        host="0.0.0.0",
        port=5000,
        threaded=True,
        debug=False,
        use_reloader=False,
    )