from flask import Flask, Response
import cv2
import socket
import atexit


app = Flask(__name__)


# ==========================================================
# CONFIGURACIÓN
# ==========================================================

PUERTO = 5000


# ==========================================================
# CÁMARA USB / LAPTOP
# ==========================================================

camara = cv2.VideoCapture(1)


# Mejor calidad y estabilidad
camara.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
camara.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)


if not camara.isOpened():

    print("ERROR: No se pudo abrir la cámara.")

else:

    print("Cámara iniciada correctamente.")



# ==========================================================
# CERRAR CÁMARA AL FINALIZAR
# ==========================================================

@atexit.register
def cerrar_camara():

    if camara:

        camara.release()

    print("Cámara liberada.")



# ==========================================================
# OBTENER IP LOCAL
# ==========================================================

def obtener_ip_local():

    try:

        s = socket.socket(
            socket.AF_INET,
            socket.SOCK_DGRAM
        )

        s.connect(
            ("8.8.8.8",80)
        )


        ip = s.getsockname()[0]


        s.close()


        return ip


    except Exception:

        return "127.0.0.1"




# ==========================================================
# GENERAR VIDEO
# ==========================================================

def generar_video():


    while True:


        correcto, frame = camara.read()



        if not correcto:

            print("No se pudo leer cámara")

            continue



        # Convertir a JPG

        correcto, buffer = cv2.imencode(

            ".jpg",

            frame,

            [
                cv2.IMWRITE_JPEG_QUALITY,
                80
            ]

        )



        if not correcto:

            continue



        frame_bytes = buffer.tobytes()



        yield (

            b"--frame\r\n"

            b"Content-Type: image/jpeg\r\n"

            b"Content-Length: " +

            str(len(frame_bytes)).encode() +

            b"\r\n\r\n"

            +

            frame_bytes

            +

            b"\r\n"

        )




# ==========================================================
# PAGINA PRINCIPAL
# ==========================================================

@app.route("/")
def inicio():


    ip = obtener_ip_local()



    return f"""

    <!DOCTYPE html>

    <html>

    <head>

        <meta charset="UTF-8">

        <meta name="viewport"
        content="width=device-width, initial-scale=1.0">


        <title>SafeVisionAI</title>


        <style>


        body{{

            background:#111;

            color:white;

            text-align:center;

            font-family:Arial;

        }}


        img{{

            width:100%;

            max-width:900px;

        }}


        </style>


    </head>


    <body>


        <h1>
        SafeVisionAI - Cámara activa
        </h1>


        <p>
        Servidor conectado
        </p>


        <p>

        IP:
        {ip}:{PUERTO}

        </p>



        <img src="/video">


    </body>


    </html>

    """




# ==========================================================
# STREAM VIDEO
# ==========================================================

@app.route("/video")
def video():


    return Response(

        generar_video(),

        mimetype=
        "multipart/x-mixed-replace; boundary=frame"

    )




# ==========================================================
# INICIAR SERVIDOR
# ==========================================================

if __name__ == "__main__":



    ip = obtener_ip_local()



    print()

    print("================================")

    print(" SAFEVISIONAI - CAMARA ")

    print("================================")

    print()

    print("IP:")

    print(ip)


    print()

    print("ABRIR CELULAR:")

    print(
        f"http://{ip}:{PUERTO}/"
    )

    print()

    print("VIDEO:")

    print(
        f"http://{ip}:{PUERTO}/video"
    )


    print()

    print("================================")




    app.run(

        host="0.0.0.0",

        port=PUERTO,

        threaded=True

    )