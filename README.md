# Motion Cues Android

Motion Cues is an Android application inspired by the iOS motion assist feature. It helps reduce motion sickness when using your device in moving vehicles by displaying animated alignment dots on the edges of the screen that respond to the vehicle's motion.

## Features

- **Motion Sickness Reduction**: Animated dots visually align your device with the actual motion you are experiencing, reducing the conflict between your eyes and inner ear.
- **Customizable Appearance**: Change dot color, size, spacing, and opacity to match your preference.
- **Configurable Physics**: Adjust sensitivity and smoothness so the dots respond precisely to your liking.
- **Quick Settings Tile (QP Button)**: Conveniently toggle the overlay on and off straight from your device's Quick Settings panel.

## Installation

### Option 1: Download and Build via Android Studio

1. Download or clone this repository to your local machine.
2. Open **Android Studio**.
3. Select **File > Open** and choose the downloaded repository folder.
4. Wait for Gradle to sync the project.
5. Connect your Android device or start an emulator.
6. Click the **Run** button (green play icon) in Android Studio to build and install the app on your device.

### Option 2: Build via GitHub Actions (APK Download)

If you don't have Android Studio, you can build the APK directly using GitHub Actions:

1. **Fork** this repository to your own GitHub account.
2. Go to the **Actions** tab in your forked repository.
3. If prompted, click **"I understand my workflows, go ahead and enable them"**.
4. Select the **Android CI** workflow from the left sidebar.
5. Click **Run workflow** and select the main branch.
6. Once the build finishes successfully, click on the workflow run.
7. Scroll down to the **Artifacts** section and download the generated APK.
8. Transfer the APK to your Android device and install it (ensure you have "Install from unknown sources" enabled).

## Permissions

The app requires the following permissions to function correctly:
- **Display over other apps (System Alert Window)**: Required to display the motion dots overlay on top of any app.
- **Notifications**: Required to keep the service running in the background persistently.

## Privacy

This app operates entirely offline and does not collect, store, or transmit any personal data.

## License

This project is open-source and free to use.
