### Main
- [ ] Port to Neoforge _(high-priority)_
- [ ] Add a Lua speaker to allow players to integrate other TTS libraries
- [ ] Consider switching to [ktoml](https://github.com/orchestr7/ktoml), as toml4j is no longer maintained
- [ ] Add user-made functions and expressions inside the speech screen

### Audio
- [ ] Add audio effects via [jsys](https://github.com/philburk/jsyn) and an audio effect customization screen
- [ ] Add eSpeak voice parameters and a `[Voice]` tab in the settings to allow the player to change the pitch/attenuation

### Optimization
- [ ] Fix random jitters in the audio _(problem when communicating with the main thread? maybe an issue with Kotlin coroutines?)_

