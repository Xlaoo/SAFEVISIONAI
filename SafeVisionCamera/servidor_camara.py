from flask import Flask, Response
import cv2
import socket
import atexit
import threading
import time


app = Flask(__name__)

PUERTO = 5000


# =========================
# CAMARA
# =========================

camara = cv2.VideoCapture(1)

camara.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
camara.set(cv2.CAP_PROP_FRAME_HEIGHT, 360)


if camara.isOpened():
    print("Cámara iniciada correctamente")
else:
    print("ERROR cámara")


frame_actual = None
bloqueo = threading.Lock()
activo = True



# =========================
# CAPTURA CAMARA
# =========================

def iniciar_camara():

    global frame_actual, activo


    cv2.namedWindow(
        "SafeVisionAI - Camara",
        cv2.WINDOW_NORMAL
    )

    cv2.resizeWindow(
        "SafeVisionAI - Camara",
        640,
        360
    )


    while activo:


        ok, frame = camara.read()


        if ok:


            with bloqueo:
                frame_actual = frame.copy()


            cv2.imshow(
                "SafeVisionAI - Camara",
                frame
            )


            # X o tecla Q
            if cv2.waitKey(1) & 0xFF == ord("q"):

                activo = False

                break



    camara.release()
    cv2.destroyAllWindows()



threading.Thread(
    target=iniciar_camara,
    daemon=True
).start()



# =========================
# CERRAR
# =========================

@atexit.register
def cerrar():

    global activo

    activo = False

    camara.release()

    cv2.destroyAllWindows()

    print("Cámara cerrada")



# =========================
# IP
# =========================

def obtener_ip():

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

    except:

        return "127.0.0.1"



# =========================
# STREAM
# =========================

def video_stream():

    while activo:


        with bloqueo:

            if frame_actual is None:
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




# =========================
# PAGINA
# =========================

@app.route("/")
def inicio():

    return f"""

<html>

<body style="background:black;color:white;text-align:center">

<h2>SafeVisionAI Cámara</h2>

<img src="/video" width="640">

</body>

</html>

"""



@app.route("/video")
def video():

    return Response(
        video_stream(),
        mimetype=
        "multipart/x-mixed-replace; boundary=frame"
    )



@app.route("/info")
def info():

    return {

        "nombre":"SafeVisionAI_CAMERA",
        "tipo":"camara",
        "estado":"activo"

    }



# =========================
# INICIO
# =========================

if __name__ == "__main__":


    ip = obtener_ip()


    print("======================")
    print(" SAFEVISIONAI CAMERA")
    print("======================")
    print()
    print("CELULAR:")
    print(
        f"http://{ip}:5000"
    )
    print()
    print("======================")


    app.run(
        host="0.0.0.0",
        port=5000,
        threaded=True
    )