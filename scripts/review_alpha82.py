#!/usr/bin/env python3
"""Static release critics for Reader chrome, Speed narration and optional screen effects.

These guards prove source wiring, not actual Android rendering or compilation.
"""
from pathlib import Path
import re, sys

ROOT=Path(__file__).resolve().parent.parent
UI=ROOT/"app/src/main/java/io/fluxreader/app/ui"
DATA=ROOT/"app/src/main/java/io/fluxreader/app/data"
def source(name): return (UI/name).read_text()
reader=source("ReaderScreen.kt")
speed=source("SpeedReaderScreen.kt")
prefs=source("ReaderPreferences.kt")
fx=source("ExperimentalUiOverlay.kt")
fx_sheet=source("ExperimentalEffectsSheet.kt")
model=(DATA/"Models.kt").read_text()
storage=(DATA/"ReaderSettingsRepository.kt").read_text()
speech=(ROOT/"app/src/main/java/io/fluxreader/app/reader/SpeedSpeechNarrator.kt").read_text()
app=source("FluxReaderApp.kt")
header=reader.split('topBar = {',1)[1].split('bottomBar = {',1)[0]

checks={
  "Reader UX": [
     ("one overflow for all skins", 'contentDescription = "Меню чтения"' in header),
     ("back button", 'contentDescription = "Назад"' in header),
     ("no permanent fullscreen button", 'Icons.Default.Fullscreen' not in header),
     ("no permanent voice button", 'RecordVoiceOver' not in header),
     ("only two header buttons", header.count("IconButton(")==2),
     ("search under overflow", 'Text("Поиск")' in header),
     ("bookmarks under overflow", 'Text("Закладки и заметки")' in header),
     ("text settings under overflow", 'Text("Параметры текста")' in header),
     ("speech is exclusive to Speed Reader", 'Text("Озвучивание")' not in header and 'onNarrationToggle' not in reader),
     ("fullscreen toggles on page tap", 'onCenterTap = { setChromeVisiblePreservingPosition(!chromeVisible) }' in reader
      and 'BackHandler(enabled = !chromeVisible && selectedText == null)' in reader
      and 'setChromeVisiblePreservingPosition(true)' in reader),
  ],
  "Speed narration": [
     ("narration sheet route", 'SpeedSettingsPanel.VOICE -> SpeedVoiceSettingsSheet(' in speed),
     ("voice settings link", 'SpeedSettingsNavRow("Голос и озвучка"' in speed),
     ("offline voices", 'filter { !it.isNetworkConnectionRequired }' in speech),
     ("voice engine initialized on demand", 'if (settingsPanel == SpeedSettingsPanel.VOICE) speech.loadVoices()' in speed),
     ("voice id saved", 'narrationVoiceId = voice.id' in speed),
     ("voice rate and pitch", 'narrationRate = value' in speed and 'narrationPitch = value' in speed),
     ("voice and WPM still separate", 'if (voiceMode)' in speed and 'viewModel.speedToggle()' in speed),
  ],
  "Effects / persistence": [
     ("master CRT toggle", 'experimentalCrtEnabled' in fx and 'experimentalCrtEnabled' in fx_sheet),
     ("scanlines", 'experimentalScanlinesEnabled' in fx and 'experimentalScanlinesEnabled' in fx_sheet),
     ("glow", 'experimentalGlowEnabled' in fx),
     ("noise", 'experimentalNoiseEnabled' in fx),
     ("vignette", 'experimentalVignetteEnabled' in fx),
     ("intensity bounded", 'experimentalEffectIntensity.coerceIn(0f, 1f)' in fx and 'experimentalEffectIntensity = value.experimentalEffectIntensity.coerceIn(0f, 1f)' in storage),
     ("prefs saved", all('putBoolean("'+key+'V1"' in storage for key in
        ["experimentalCrtEnabled","experimentalScanlinesEnabled","experimentalGlowEnabled","experimentalNoiseEnabled","experimentalVignetteEnabled"])),
     ("default persisted intensity", 'experimentalEffectIntensity = prefs.getFloat("experimentalEffectIntensityV1", 0.65f)' in storage),
     ("active theme remains opt in", app.count('ExperimentalUiOverlay(settings.activeUiTheme, settings, immersiveReader)') == 2 and 'settings.useWastelandDesign -> WastelandDeviceFrame' in app and 'settings.useCyberpunkDesign -> CyberpunkDeviceFrame' in app),
     ("global settings controls effects", 'SettingsToggleRow(' in prefs and '"CRT-эффект"' in prefs),
     ("effect sheet from Speed", 'SpeedSettingsPanel.EFFECTS -> ExperimentalEffectsSheet(' in speed),
     ("effect sheet from Reader", 'ReaderOverlay.EFFECTS -> ExperimentalEffectsSheet(' in reader),
     ("no overlay pointer input", '.pointerInput(' not in fx),
  ],
}

fails=[]
for group, tests in checks.items():
    bad=[name for name, ok in tests if not ok]
    print(f"{group}: {len(tests)-len(bad)}/{len(tests)}")
    fails += [f"{group}: {name}" for name in bad]
if fails:
    for item in fails: print("FAIL",item)
    sys.exit(1)
print(f"PASS {sum(map(len,checks.values()))} alpha.82 static critic criteria")
