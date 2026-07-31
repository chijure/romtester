# QApp ROM Tester

Simple Android app to test basic ROM/device hardware behavior:

- Speaker sound test (beep)
- Vibration test
- Touch input test area

## Compatibility target

- `minSdk 8` (Android 2.2/Froyo)
- `targetSdk 35` (latest modern Android behavior)

## Open in Android Studio

1. Open Android Studio from your installation in `/home/chijure/Documentos`.
2. Select **Open** and choose this folder: `/home/chijure/projects/qapp`.
3. Let Gradle sync and install suggested SDK packages.
4. Run on a device or emulator.

## Notes about very old Android (1.6)

Android 1.6 is API 4. Building for API 4 with modern Android Studio/Gradle is no longer practical.
This project uses API 8 as the practical minimum, which still covers very old devices.
