# Shinobi Upscaler — recompiler l'APK

Nouvelle interface Jetpack Compose (Material 3) pour le moteur de
[RealSR-NCNN-Android](https://github.com/tumuyan/RealSR-NCNN-Android) (MIT, tumuyan). Moteur et modèles inchangés.

## Prérequis
- JDK 17 ou plus récent (testé avec JDK 21)
- SDK Android : plateforme 36 et build-tools 36.0.0

```bash
export ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools
# télécharger « commandlinetools-linux » depuis developer.android.com, le décompresser dans $ANDROID_HOME/cmdline-tools/latest
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
echo "sdk.dir=$ANDROID_HOME" > RealSR-NCNN-Android-GUI/local.properties
```

## Assets du moteur (programmes + modèles, non versionnés)
```bash
curl -L -o assets.zip https://github.com/tumuyan/RealSR-NCNN-Android/releases/download/1.11.1/assets.zip
unzip assets.zip -d tmp_assets
mkdir -p RealSR-NCNN-Android-GUI/app/src/main/assets
cp -r tmp_assets/assets/realsr RealSR-NCNN-Android-GUI/app/src/main/assets/
```

## Compiler
```bash
cd RealSR-NCNN-Android-GUI
chmod +x gradlew
./gradlew assembleDebug
# APK : app/build/outputs/apk/debug/Shinobi-Upscaler-debug-1.0.0.apk
```

Installer : `adb install -r app/build/outputs/apk/debug/Shinobi-Upscaler-debug-1.0.0.apk`

Notes : `targetSdk` reste à 28 (obligatoire pour exécuter les binaires du moteur depuis le cache de l'app).
Si le téléchargement des dépendances renvoie des erreurs 429, relancer avec `./gradlew assembleDebug --max-workers=2`.
