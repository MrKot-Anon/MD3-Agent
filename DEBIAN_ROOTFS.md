# Bundled Debian userland

The release APK contains the arm64 filesystem layer from the official `debian:13-slim` Docker image (Debian 13.8). The image manifest digest is `sha256:65d15c0ac2dd6639a130a05ffaf36b9d60697ef8d07125d79aecaaa9871bc97b`; the bundled layer's SHA-256 is `48fb00926fe468d83ca04ee6775f36350d17e0f328dfcb03aadb5de0c52ae3f6`.

The archive is unpacked into app private storage on the first Debian command. Its Debian package copyright notices are in `/usr/share/doc/*/copyright` inside the archive. Package installation uses the Debian repositories configured in the image. Debian commands may use the network without the Android app's model endpoint restriction.

A real chroot requires `su` root access. `/workspace` maps to the app workspace, `/app-cache` to its cache, and `/sdcard` to shared storage when the all files permission is granted. The chroot preserves Debian symlinks. The Android app's own HTTP client remains restricted to the local model endpoint.

Commands inside the chroot run with root privileges. A chroot is a filesystem view, not a security boundary for root; the agent may reach other device files if it runs commands that do so.
