use jni::objects::{JClass, JShortArray, JString};
use jni::JNIEnv;
use windows::core::HSTRING;
use windows::Win32::Media::Speech::{ISpStream, ISpVoice, SpStream, SpVoice};
use windows::Win32::System::Com::{CoCreateInstance, CoInitialize, CoUninitialize, CLSCTX_ALL};

#[unsafe(no_mangle)]
pub extern "system" fn Java_SpeechNative_speak<'local>(
    mut env: JNIEnv<'local>,
    class: JClass<'local>,
    inputString: JString<'local>
) -> JShortArray<'local> {
    let mut array = JShortArray::default();

    unsafe {
        /*let _ = CoInitialize(None);
        let voice: windows::core::Result<ISpVoice> = CoCreateInstance(&SpVoice, None, CLSCTX_ALL);
        match voice {
            Ok(voice) => {
                let _ = voice.SetRate(-2);
                //let stream: ISpStream = SHCreateMemStream;

                let text = HSTRING::from(env.get_string(&inputString).into<>());

                if let Err(err) = voice.Speak(&text, 0, None) {
                    eprintln!("Native TTS speech error: {err}");
                    return array;
                }
            }
            Err(err) => {
                eprintln!("Native TTS initialization error: {err}");
                return array;
            }
        }

        CoUninitialize();*/
    }

    array
}
