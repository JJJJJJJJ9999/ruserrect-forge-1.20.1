# Resurrect - Forge 1.20.1

Death screen gets a "Resurrect" button (unlocks after 15 s). Pressing it respawns you where you died and plays
an 8 second cutscene: third-person camera, body reassembles feet -> head while gore flies inward.

Needs to be installed on BOTH client and server for multiplayer.

## Build the jar (pick one)

**A. GitHub (no installs):** create a new GitHub repo, upload the contents of this folder, open the
Actions tab -> "build" -> download the `resurrect-forge-1.20.1` artifact. Inside is the jar.

**B. Locally:** install JDK 17 and Gradle 8.8, then in this folder run `gradle build`.
The jar appears in `build/libs/resurrect-forge-1.20.1-1.0.0.jar`.

## Install in Prism Launcher
Instance -> Edit -> Mods -> Add file -> pick the jar (not the zip). Use a Forge 1.20.1 instance (Forge 47.x).
