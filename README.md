# MD3 Agent — local-network fork

This fork runs the Android assistant against one local OpenAI-compatible endpoint:

- API: `http://172.16.88.10:8080/v1`
- Default model: `minimax-m2.5`

The app's HTTP client permits only this host, port and `/v1/` path. Redirects are disabled. The model profile is fixed in the app, and the model screen no longer offers cloud providers.

Web search, webpage reading, weather, MCP servers, shell and Python agent tools, GitHub update checks, remote favicons and remote image loading are disabled. The remaining tools operate on local chats, files, reminders, memory and the phone. Links opened in a separate browser are outside the app's network policy.

At first launch, the app requests microphone and notification permissions, then opens Android screens for All files access, accessibility control, exact alarms where needed, unrestricted background activity and the default assistant role. Android requires the user to grant special access manually. If All files access is granted, the app selects shared storage as its file scope. These prompts run once; access can later be changed in Android Settings or the app's Device access page.

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

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The standard build targets arm64 phones.

## Upstream and license

Forked from [huecoder/MD3-Agent](https://github.com/huecoder/MD3-Agent). The source is licensed under GPL-3.0; see [LICENSE](LICENSE). Third-party notices are in [THIRD_PARTY.md](THIRD_PARTY.md).
