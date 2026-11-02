# Alfred Nmap

Developed by Tambet Sarapu.

Alfred Nmap is an Android private-network device availability checker. It displays the phone's local IPv4 address and router gateway, discovers responding hosts on the local private subnet, and lets you copy discovered IP addresses by tapping a device row.

## Build and test

Requirements: JDK 17+ and Android SDK Platform 35.

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug
```

The debug APK is available as [`alfred-nmap.apk`](./alfred-nmap.apk). Android may ask you to allow installation from the app used to open the file.

## Discovery behavior

Use **Scan**, **Stop**, and **Clear** to control local device discovery. The app scans at most the local private `/24` block, or the actual subnet when it is smaller. Device names are shown only when the local network provides them. This is a host availability check, not a complete router client list; some devices do not respond to reachability checks. No root access is required. The app does not execute shell commands, run Nmap, or scan ports/services.
