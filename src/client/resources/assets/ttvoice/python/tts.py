#autoupdate: enable
import io
import os
import random
import sys
import threading
import uuid
from time import sleep
import pyttsx3
import sounddevice as sd
import soundfile as sf

# -- note for users --
# This script powers the Python backend for the mod.
# You're free to modify it in case you want to extend my mod's functionality (ex: handling TTS using your own custom library)
#
# This script has a mandatory header section on the first few lines; The mod uses this to read any additional config/metadata you want to provide
# If you want to modify the script, make sure to set "autoupdate: disable" (without quotes) as the mod will replace the file every time you load into a world otherwise
# Note that if the #autoupdate line is not present at all, the mod will replace the file and act as if autoupdate is enabled
#
# Make sure to read any patch notes before updating to a newer mod version, as the API for the commands or for the entire Python script might change.
# -- end of note --

device = 18
speaking = False

shutdown_events: dict[str, threading.Event] = {}
def speak(text: str, thread_id: str):
    rand = random.randrange(0, 10000)
    audio_file = f"audio_{rand}.wav"

    # Generating text to speech
    print(f"PY: Received text '{text}'")
    emit_speech_begin()
    engine = pyttsx3.init()
    engine.setProperty("voice", "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Speech\\Voices\\Tokens\\TTS_MS_EN-GB_HAZEL_11.0")
    engine.setProperty("rate", 190)
    engine.save_to_file(f"<pitch middle=\"-500\"> {text} </pitch>", audio_file)
    print("PY: Saved file.. Now running")
    engine.runAndWait()
    while not os.path.exists(audio_file) or os.path.getsize(audio_file) == 0:
        sleep(0.03)
    print("PY: Ran")

    # Playing back the audio
    current_frame = 0
    event = threading.Event()
    def callback(out, frames, time, status):
        nonlocal current_frame, thread_id
        if shutdown_events[thread_id].is_set():
            shutdown_events[thread_id].clear()
            emit_speech_end()
            raise sd.CallbackAbort
        chunk_size = min(len(data) - current_frame, frames)
        out[:chunk_size] = data[current_frame:current_frame + chunk_size]
        if chunk_size < frames:
            out[chunk_size:] = 0
            raise sd.CallbackStop()
        current_frame += chunk_size

    print("PY: Starting to play audio..")
    data, fs = sf.read(audio_file, always_2d=True, closefd=True)
    print("Finished dataying")
    stream = sd.OutputStream(
        samplerate=fs, device=device, callback=callback, finished_callback=event.set, channels=data.shape[1])
    print("PY: Stream created")
    with stream:
        event.wait()
        emit_speech_end()
    print("PY: Finished playing audio")

    # Cleanup
    if os.path.exists(audio_file):
        os.remove(audio_file)

def emit_speech_begin():
    print("speak: begin", flush=True)

def emit_speech_end():
    print("speak: end", flush=True)

# Required, because Python sucks balls
sys.stdin = io.TextIOWrapper(
    sys.stdin.buffer,
    encoding=sys.stdin.encoding,
    newline='\n',
    line_buffering=True
)

# Command parsing
try:
    while True:
        line = input()
        if ':' not in line: continue
        params: list[str] = line.strip().split(':', 1)
        args: str = params[1].strip()
        match params[0].strip():
            case "speak":
                thread_id = str(uuid.uuid4())
                shutdown_events[thread_id] = threading.Event()
                thread = threading.Thread(target=speak, args=(args, thread_id))
                thread.start()
            case "shutup":
                for ev in shutdown_events.values():
                    ev.set()
                shutdown_events.clear()
                emit_speech_end()
except KeyboardInterrupt:
    for i, ev in shutdown_events:
        ev.set()
    emit_speech_end()
    quit(0)
