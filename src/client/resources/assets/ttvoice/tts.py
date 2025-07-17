import os
import random
import sys
import threading
import pyttsx3

rand = random.randrange(0, 10000)
audio_file = f"audio_{rand}.mp3"

# Running TTS
engine = pyttsx3.init()
text = sys.argv[1]
device = int(sys.argv[2])
speedy = False #speedy = int(sys.argv[3])

engine.setProperty("voice", "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Speech\\Voices\\Tokens\\TTS_MS_EN-GB_HAZEL_11.0")
engine.setProperty("rate", 500 if speedy else 190)
engine.save_to_file(f"<pitch middle=\"-500\">{text}</pitch>", audio_file)
#engine.save_to_file(text, audio_file)
engine.runAndWait()

# Playing back
import sounddevice as sd
import soundfile as sf

event = threading.Event()
current_frame = 0
def callback(out, frames, time, status):
    global current_frame
    chunk_size = min(len(data) - current_frame, frames)
    out[:chunk_size] = data[current_frame:current_frame + chunk_size]
    if chunk_size < frames:
        out[chunk_size:] = 0
        raise sd.CallbackStop()
    current_frame += chunk_size

data, fs = sf.read(audio_file, always_2d=True)
stream = sd.OutputStream(
    samplerate=fs, device=device, callback=callback, finished_callback=event.set, channels=data.shape[1])
with stream:
    event.wait()

# Cleanup
if os.path.exists(audio_file):
    os.remove(audio_file)
