# MD3 Agent — user-configured model endpoint

Configure an OpenAI-compatible model endpoint in Models. Local HTTP and remote HTTPS URLs are supported, including paths with multiple segments.

No provider or model is preconfigured. Add an endpoint after installation and select a model returned by that server. The app's HTTP client permits requests only to saved provider endpoints and their API paths. Redirects are disabled. Cloud provider choices are hidden from setup.

Web search, webpage reading, weather, MCP servers, remote favicons and remote image loading are disabled. Update checks contact only this fork's GitHub releases; the user chooses whether to download and install a new signed APK. The Android app's own model requests are restricted to user-configured model endpoints. Links opened in a separate browser are outside the app's network policy.

The `run_command` agent tool runs in a bundled Debian environment through PRoot, without device root or Termux. It inherits the app's Android UID and permissions. Debian commands can connect to any address, including package repositories; this traffic is separate from the Android app's model client. The tool starts in `/workspace`; `/app` exposes app files, `/app-cache` its cache, and `/sdcard` shared storage when Android permits it. Packages can be installed with `apt`. See [DEBIAN_ROOTFS.md](DEBIAN_ROOTFS.md) for sources and checksums.

At first launch, the app requests microphone and notification permissions, then opens Android screens for All files access, permission to install APK updates, accessibility control, exact alarms where needed, unrestricted background activity and the default assistant role. Android requires the user to grant special access manually. If All files access is granted, the app selects shared storage as its file scope. These prompts run once; access can later be changed in Android Settings or the app's Device access page.

## Build

JDK 17 and Android SDK Platform 37 are required.

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

To include x86_64 native libraries for an emulator build:

```sh
./gradlew assembleDebug -PincludeX86Emulator=true
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The standard build targets arm64 phones. The release APK is built and signed locally; GitHub Actions does not build it when a release tag is created.

Install the signed APK from the [latest release](https://github.com/MrKot-Anon/MD3-Agent/releases/latest). Its signing certificate SHA-256 is `3baf5ec839e28dfe62a074db5477e8458cad01d3940093a6947d504b5bbde686`.

## Upstream and license

Forked from [huecoder/MD3-Agent](https://github.com/huecoder/MD3-Agent). The source is licensed under GPL-3.0; see [LICENSE](LICENSE). Third-party notices are in [THIRD_PARTY.md](THIRD_PARTY.md).
