# HOLYDON NETWORK RADAR

Gaming-inspired Android network diagnostics for Android 6.0+.

## Initial build
- Java 17 + Android Gradle Plugin
- SDK 35, minimum SDK 23
- Live active-network type and connection status
- Cellular signal readings when Android permissions and device APIs allow them
- Manual connectivity latency check
- Local-first foundation; no account or paid API required

## Build
Open this repository in Android Studio, or run the **Build HOLYDON NETWORK RADAR APK** GitHub Actions workflow. The workflow uploads a debug APK artifact.

## Permissions
Phone-state and location permissions are requested only for cellular diagnostics. Android may restrict or omit cell information depending on device, SIM, Android version and permission state. A speed test measures the currently active data connection; it cannot test an inactive SIM's internet connection.
