# 🎧 DJames

DJames is a vocal Spotify remote & smart driving assistant for Android. It makes use of a **Multi-Agent AI architecture** based on *Mistral AI* for Speech-to-Text and NLP, *ElevenLabs* for Text-To-Speech, *Spotify's Web API* and custom matching algorithms. The UI is based on Jetpack Compose.

DJames integrates a **self-made agentic orchestration library**, ***KAIGraph***, which I built for Kotlin, inspired by LangGraph.

<img src="./app/src/main/res/drawable-nodpi/app_icon_round.png" alt="DJames" width="100"/>


## Software requirements
DJames is supported by **Android >= 10**.


## Major versions history
**Version 3.*** **(alpha 8/2025, major 4/2026)** - BE based on *KAIGraph* for agents orchestration, *Mistral AI* for LLM APIs, *ElevenLabs* for TTS APIs, *Spotify* APIs, *ObjectBox* for DBs; FE based on *Jetpack Compose*.

**Version 2.*** **(major 8/2024)** - BE based on *Google Dialogflow* for Speech-to-Text and NLP, native *Android TTS* and custom NLP extraction and algorithms, *Spotify* APIs, *ObjectBox* for DBs; FE based on *Jetpack Compose*.

**Version 1.*** **(alpha 12/2023, major 2/2024)** - BE based on *Google Dialogflow* for Speech-to-Text and NLP, custom NLP extraction and algorithms, *Spotify* APIs; FE based on xmls.


## All versions history
**Version 3.0.1 (2026-05-05)** - New UI for Home Screen & Overlay button (WIP).

**Version 3.0.0 (2026-04-26)** - First V3-only version. Kept v3.0.alpha's UI.

**Version 3.0.a11 (2026-04-25)** - V3 FINALIZED! Reworked all Graph nodes. Add new ElevenLabs TTS voice for V3. Starts repackaging agentic library as KAIGraph (WIP). Last version with co-existing IntentGraph & AgentGraph.

**Version 3.0.a10 (2025-12-28)** - Add AccountsScreen, backup & restore, notification icon, remove FFmpeg. Add DualApp debug variant. SpotifyQuery: fix legacy bug (!) and code modularization. Align to new Spotify Development Mode policy. Start rework of Graph nodes for V3. Add new ElevenLabs TTS voice for V3.

**Version 3.0.a9 (v3 alpha) (2025-10-28)** - MULTI-AGENT ARCHITECTURE! Implemented new LangGraph-inspired Conversational Graph with State, Nodes & Tools. Migrated NLPDispatcher to IntentsGraph. Init LLM AgentsGraph works.

**Version 3.0.a8 (v3 alpha) (2025-10-20)** - Add noise-cleaning Band-Pass Filter as support to VAD. Reworked Home screen: improved tips & guidance. Add open links from Library item options. Tested many alternative VADs and noise filters (i.e. RTNR NoiseSuppressor, Silero VAD, Yamnet VAD, constant wind dynamic suppression, ...), then all removed.

**Version 3.0.a7 (v3 alpha) (2025-09-07)** - New AudioRecorder & Silence Detector (WebRTC-VAD). Cache multiple recording files using rec timestamp as name.

**Version 3.0.a6 (v3 alpha) (2025-08-31)** - New SpotifyPlayable data structure, make Messages DB fields nullable! Add RaiseVolume button to DJames overlay. Various fixes & debugs, stabilized entire V3 FE & DBs.

**Version 3.0.a5 (v3 alpha) (2025-08-26)** - New Library DB management! New unified LibraryItem data class, remove PlayLinks & PhoneSets, updated FE management. Add info extraction from Link Previews.

**Version 3.0.a4 (v3 alpha) (2025-08-25)** - Reworked Overlay functionality: new DJames Paw pads.

**Version 3.0.a3 (v3 alpha) (2025-08-14)** - ChatManager finalized & ActionsExecutor re-adapted! Reworked Messages refresh, booleans, conv starters & starting, text states.

**Version 3.0.a2 (v3 alpha) (2025-08-02)** - New v3 FE! Add isKeyboardOpen(). Fixed Messages leftovers deletion.

**Version 3.0.a1 (v3 alpha) (2025-07-26)** - Init reworks for V3. Replaces Logs with Messages. Replaced History with Messages screen. Re-add Threads to VoiceQueryService.

**Version 2.6.2 (2025-07-24)** - Add get parent Artist / Podcast from Track / Episode URL.

**Version 2.6.1 (2025-07-24)** - Centralized logs opening & init stores. Add permissions requests handling.

**Version 2.6.0 (2025-07-21)** - Rework VoiceQueryService: centralize AudioRequestsManager, TTS & Actions, reworked threads / jobs, remove toasts.

**Version 2.5.3 (2025-07-12)** - Centralize output messages, add Messages to HistoryLog class.

**Version 2.5.2 (2025-06-28)** - Add support for sending Whatsapp audio & text messages.

**Version 2.5.1 (2025-06-14)** - Add library import / export from/to file. Finalize renaming of all "Vocabulary" references into "Library".

**Version 2.5.0 (2025-06-09)** - Fulfillment overhaul: replaced JsonObjects() with data classes. Isolated DialogFlow & manual extractions logic. Add HistoryLog() to ObjectBox DB. Restored ids as Long for Library. Moved Guide out of raw Json.

**Version 2.4.3 (2025-05-14)** - Code streamlining: rework PlayLinks and PhoneSets access management.

**Version 2.4.2 (2025-05-05)** - Add support to Podcasts playing (WIP: latest episode only). Fixed annoying audioFocus bug after call end.

**Version 2.4.1 (2025-05-03)** - Add support to Driving directions (Places). Introduce multi-language TTS read.

**Version 2.4.0 (2025-04-14)** - Repo refactor. Add Library images & AddLink basic Dialog (WIP), with direct external share links. Minor UI refinements.

**Version 2.3.6 (2025-04-12)** - Rework Dialog Composables for easy reutilization, rework Spotify Query processes & calls.

**Version 2.3.5 (2025-04-05)** - Full Gradle & code updates for Android 15 and Kotlin v2. Add nickname and Spotify user image download (WIP).

**Version 2.3.4 (2025-03-30)** - Introduce Quick Actions: click 2 times on overlay / volume to save currently playing track, 3 times for enabling/disabling silence detection in voice queries. Rework some multithreading.

**Version 2.3.3 (2025-03-17)** - Add numbers conversion to words in voice queries (WIP). Increase frequency of silence detection.

**Version 2.3.2 (2025-03-12)** - New EditVoc Dialogs UI. Added play extra links for Artists (Radio, Mix, Custom).

**Version 2.3.1 (2025-03-03)** - New Library UI states management. Added search & play top tracks for Artists.

**Version 2.3.0 (2025-03-03)** - Migrated Library to new ObjectBox DB, add aliases & save artists URLs. UI visual refinements.

**Version 2.2.1 (2025-02-04)** - Migrated Vocabulary to new Library structure (using JSON files); renamed Vocabulary to Library.

**Version 2.2.0 (2025-01-18)** - StreetSign-UI FE completed and fully consistent, add Splash Screen, libs cleaning.

**Version 2.1.6 (2025-01-17)** - Reworked History update status.

**Version 2.1.5 (2025-01-07)** - Finalized UI alignment using StreetSign Design.

**Version 2.1.4 (2025-01-02)** - New Overlay Bubble Service UI, based on Jetpack Compose.

**Version 2.1.3 (2024-12-30)** - New Spotify Login Auth window & process.

**Version 2.1.2 (2024-12-03)** - Update Artists BE & FE, following changes in Spotify Web API.

**Version 2.1.1 (2024-11-06)** - New StreetSign Design language for UI.

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
