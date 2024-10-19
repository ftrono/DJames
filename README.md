# 🎧 DJames
Repo for my *DJames* Android App & vocal assistant. 🤖 *NOTE: the app is currently intended to be used only by a closed group of authorized testers (close friends).*

DJames is a vocal Spotify remote & smart driving assistant for Android. It makes use of *Google Dialogflow* for Speech-to-Text and basic NLP, *Spotify's Web API* and custom NLP extraction and matching algorithms. The UI is based on Jetpack Compose.

Soon to be integrated:
* LLM conversations for information retrieval, using LangChain & Groq (currently under POC tests).

<img src="./app/src/main/res/drawable-nodpi/app_icon_round.png" alt="DJames" width="100"/>


## Software requirements
DJames is supported by **Android >= 10**.


## Versions history

**Version 2.1 (2024-10-19)** - Brand-new UI migrated & refined, based on Jetpack Compose.

**Version 2.01 (2024-09-17)** - Added clock to overlay, usability fixes.

**Version 2.0 (2024-08-10)** - New NLP structure & engine, new modular code structure, first repo cleaning & streamlining. New Spotify Login/Logout management. UI refresh.

**Version 1.16 (2024-08-04)** - Init preparation for v2: new "MyDJames" screen.

**Version 1.15 (2024-08-03)** - FuzzySearch & matching improvements. Minor usability improvements. Refactor repo.

**Version 1.14 (2024-07-22)** - Add support for: album search & play, artist play ("This is <artist name>"), playlist play outside vocabulary (all English only). Add TTS read (English only). Minor usability improvements.

**Version 1.13 (2024-07-14)** - Add support for directly playing liked songs & playlists from vocabulary (english only).

**Version 1.12 (2024-07-06)** - Minor usability improvements.

**Version 1.11 (2024-05-18)** - New Guide fragment. Minor usability improvements.

**Version 1.10 (2024-05-02)** - Add support to voice queries in Italian.

**Version 1.09 (2024-04-23)** - Simplify player info management, minor fixes.

**Version 1.08 (2024-04-16)** - Add messaging support (SMS).

**Version 1.07 (2024-04-08)** - Add Guide tab.

**Version 1.06 (2024-04-07)** - Add early stop for voice recording, usability improvements.

**Version 1.05 (2024-04-02)** - Add support for custom context (playlists & liked songs).

**Version 1.04 (2024-03-16)** - New vocabulary UI and management, new vocabulary data structure.

**Version 1.03 (2024-03-10)** - Add phone call support.

**Version 1.02 (2024-03-05)** - Finalized new navigation UI with vocabulary and config bugfixes.

**Version 1.01 (2024-02-26)** - New NLP extraction algorithm, improved matching algorithm.

**Version 1.0 (2024-02-17)** - First stable version, fully usable vocal track query & logs history.
