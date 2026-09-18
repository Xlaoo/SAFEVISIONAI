from flask import Flask, Response
import cv2

app = Flask(__name__)

camara = cv2.VideoCapture(0)


def generar_video():

    while True:

        correcto, frame = camara.read()

        if not correcto:
            continue

        _, buffer = cv2.imencode(".jpg", frame)

        frame_bytes = buffer.tobytes()

        yield (
            b"--frame\r\n"
            b"Content-Type: image/jpeg\r\n\r\n"
            + frame_bytes
            + b"\r\n"
        )


@app.route("/")
def inicio():

    return "SafeVisionAI - Cámara activa"


@app.route("/video")
def video():

    return Response(
        generar_video(),
        mimetype="multipart/x-mixed-replace; boundary=frame"
    )


app.run(
    host="0.0.0.0",
    port=5000,
    threaded=True
)