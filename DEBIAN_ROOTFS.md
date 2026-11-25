# Bundled Debian userland

The arm64 filesystem archive comes from the official `debian:13-slim` Docker image (Debian 13.8). Its image manifest digest is `sha256:65d15c0ac2dd6639a130a05ffaf36b9d60697ef8d07125d79aecaaa9871bc97b`; the bundled layer's SHA-256 is `48fb00926fe468d83ca04ee6775f36350d17e0f328dfcb03aadb5de0c52ae3f6`. Debian package copyright notices are inside the archive at `/usr/share/doc/*/copyright`.

The APK also bundles PRoot, its loader, and `pr-cli` from [oonid/pr](https://github.com/oonid/pr), commit `2b12909a74373b2ba1621b1de253f0225f8a137d`. PRoot has GPL-2.0-or-later code, with talloc LGPL-3.0-or-later; the combined PRoot binary and `pr-cli` are distributed under GPL-3.0-or-later. SHA-256: `libproot.so` `bd7788935d8d29688ec251925af61d11ac72b83313f91c022fcbcf544509c2f1`; `libproot-loader.so` `e427c4fac91d18a197e6624e5907b408b1868f86de9022f06e8f39448b991fe1`; `libpr-cli.so` `64532ef2d00f3d6b6fa139656d0ca567848001d35b5c6f6c6f802a9145d55619`.

The archive is extracted into app private storage on the first Debian command, preserving symlinks. PRoot runs commands as a simulated root inside Debian while the Android process keeps the app's real UID. `apt` can install packages inside this filesystem. Commands may use the network without the Android app's model endpoint restriction. The host filesystem access is limited by Android permissions granted to the app; `/workspace` maps to its workspace, `/app` to its private files, `/app-cache` to its cache, and `/sdcard` to shared storage when Android allows it.

This is an arm64 environment. It does not need device root or a separate Termux installation.
