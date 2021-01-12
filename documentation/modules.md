# Cyclone Architecture

## Overview
The core of Cyclone is the `cloud` module.
This is where all data is stored and synchronized.
When a song should be played back, PlaylistPlayer (or an extension) creates a PlayTask and uploads it to the cloud.
Any PlaybackEngine watching the cloud data can then create a job and upload a corresponding PlayTaskStatus to the cloud, describing the current progress.

For the PlaylistPlayer (main app), the device that plays back the audio also issues the tasks so playback can continue even after the device hosting the files disconnects.
Extensions may use a centralized host to upload all tasks.

## Modules


### [Player.FX](player_fx.md)

JavaFX UI to display and control player (PlayerWindow) and settings (AppSettings).

Main method in Launcher.

Language: Java, Kotlin

Dependencies:
- aquafx-0.1
- Player Model


### Player.Model
PlaylistPlayers creates tasks for playback and synchronizes its state using the cloud.

The PlaybackEngine is responsible for creating Jobs for existing tasks and playbing back the audio.

Language: Kotlin

Dependencies:
- Cloud
- Audio


### Cloud
Peer-to-peer networking library.
Uses multicast to discover devices and TCP for peer-to-peer communication.

Language: Kotlin

Dependencies: None


### Audio
Abstraction layer to play back audio files and streams.

Language: Java (simplified Kotlin version in branch `transparent-audio`)

Dependencies:
- MP3 SPI (`mp3spi1.9.5`) for MP3 decoding [http://www.javazoom.net/mp3spi/mp3spi.html](http://www.javazoom.net/mp3spi/mp3spi.html)
- Vorbis SPI (`vorbisspi1.0.3.jar`) for OGG decoding [http://www.javazoom.net/vorbisspi/vorbisspi.html](http://www.javazoom.net/vorbisspi/vorbisspi.html)
- `tritonus_share.jar` required by Vorbis SPI and MP3 SPI
- `jorbis-0.0.15.jar` required by Vorbis SPI
- `jogg-0.0.7.jar` required by Vorbis SPI
- `jl1.0.1.jar` (required by MP3 SPI)


### App Instance
Small toolkit to enforce that only a single instance of the program can be started.
Passes the command line arguments to the existing instance via Socket.

Language: Java

Dependencies: None


### Media Command
Wrapper for listening to media buttons on keyboard including volume buttons.
Allows definition of key combinations such as VolumeUp+VolumeDown.

Language: Java

Dependencies:
- jintellitype-1.3.9
- JIntellitype.dll
- JIntellitype64.dll


### System Control
OS-specific functions
- Standby control (and detection, still in audio)
- Turn off screens

Language: Kotlin

Dependencies
- Turn Off Monitor.exe