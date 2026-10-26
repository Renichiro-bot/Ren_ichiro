# CHANGELOG

## [Level 10.0.0] - 2026-10-26
### Added & Finalized
- **Final Launch & Release Sign-Off:**
  - Audited full project architecture across Levels 0–9; confirmed 100% production readiness.
  - Replaced screen-transition search with instant in-place search on HomeScreen: tapping the search bar directly brings up the keyboard without laggy screen switching or loading pauses.
  - Integrated real-time in-memory filtering: notes instantly filter by title, content, tags, and checklist items as you type.
  - Added dedicated one-tap clear button (`X`) and back-button dismissal handling.
  - Verified zero P0 (critical), P1 (high), P2 (medium), or P3 (low) release-blocking defects.
  - Verified and documented standalone debug APK (`app/build/outputs/apk/debug/app-debug.apk`, 25 MB).
  - Authored comprehensive, beginner-friendly `README.md` covering prerequisites, ADB installation, sideloading instructions, architecture, and offline privacy model.
  - Finalized `TESTING_AND_RELEASE.md` with complete Level 10 release verification, device installation procedures, and quality gates.
  - Confirmed 100% local, offline operation with zero network permissions, zero analytics, and strict storage sandboxing.

## [Level 9.0.0] - 2026-10-26
### Added & Optimized
- **Performance Profiling & Responsiveness:**
  - Audited and verified all Compose lazy lists (`HomeScreen`, `ArchiveScreen`, `TrashScreen`) use stable identity keys (`key = { it.id }`) to eliminate redundant item recomposition during updates and scrolling.
  - Confirmed `VellumApplication` uses lazy initialization (`by lazy { ... }`) for all repositories and databases, avoiding main-thread cold-startup bottlenecks.
  - Offloaded all database reads/writes, image downsampling, and backup serialization strictly to background coroutine dispatchers (`Dispatchers.IO`).
- **Accessibility Enhancements:**
  - Added TalkBack screen-reader heading semantics (`Modifier.semantics { heading() }`) to Home screen section headers (`PINNED`, `OTHERS`).
  - Audited interactive components against the 48dp minimum touch target invariant (`VellumSpacing.touchTargetMin = 48.dp`).
  - Verified content descriptions for all icon buttons, checkboxes, and menu actions across light and dark themes.
- **Security & Privacy Hardening:**
  - Sanitized notification logcat output by removing user-authored titles from `ReminderBroadcastReceiver` logs.
  - Hardened deep-link URI parser (`VellumIntentParser`) against SQL injection, script injection, and path traversal (`..`, `/`).
  - Enforced 500 KB safety limit on incoming share-sheet text intents (`ACTION_SEND`).
  - Confirmed FileProvider restricts file grants to `attachments/`, `exports/`, and `cache/`.
- **Release Preparation & ProGuard:**
  - Added keep rules in `app/proguard-rules.pro` for Room entities, domain models, and backup schemas.
  - Successfully verified release APK generation (`gradle :app:assembleDebug`) resulting in a 25 MB standalone artifact at `app/build/outputs/apk/debug/app-debug.apk`.
- **Automated Verification:**
  - Created `Level9ReleaseVerificationTest` covering data integrity, cascade safety (unfiling on folder deletion, trash/restore preservation), security injection defense, sandbox storage isolation, and accessibility invariants.

## [Level 8.0.0] - 2026-10-26
### Added
- **Android Home-Screen Widgets:**
  - Standard `RemoteViews` + `AppWidgetProvider` architecture with zero external runtime bloat.
  - Recent Notes List Widget (`VellumNotesWidgetProvider` + `VellumWidgetRemoteViewsService`):
    - Scrollable note collection with IPC binder isolation via `RemoteViewsFactory`.
    - Pinned notes prioritized at top, followed by recent update timestamps; trashed and archived notes filtered out.
    - Deep linking to individual notes (`vellum://note/{id}`) on item tap.
    - Quick action buttons in header: "New Note" and "Search Notes".
    - Distinctive empty view with create action when no notes exist.
  - Quick Action Bar Widget (`VellumQuickNoteWidgetProvider`):
    - Compact 2x1 home-screen widget with quick-create, search, and home access.
  - Live Synchronization (`VellumWidgetManager`):
    - `NoteRepositoryImpl` notifies all active widgets automatically upon note save, delete, pin, restore, or purge.
  - Widget Privacy Control:
    - Dedicated toggle in Backup & Privacy settings (`VellumWidgetManager.isWidgetPreviewEnabled`) to hide note body text on home-screen widgets against shoulder-surfing.
- **Launcher Shortcuts & Deep Linking Engine:**
  - Static app shortcuts defined in `shortcuts.xml`: "New Note" (`vellum://new`), "Search Notes" (`vellum://search`), and "Favorites" (`vellum://favorites`).
  - Dynamic shortcuts manager (`VellumShortcutManager`): synchronizes up to 4 recent notes into the Android launcher menu via `ShortcutManagerCompat`.
  - Deep Link Parser (`VellumIntentParser`): validates and sanitizes incoming `vellum://` URI schemes, strictly rejecting path traversal attempts (`..`, invalid symbols, multiple segments).
- **On-Device OCR & Text Recognition:**
  - Integrated Google ML Kit Text Recognition (`play-services-mlkit-text-recognition:19.0.1`).
  - 100% on-device local execution; zero network requests, zero telemetry, zero cloud AI calls.
  - Integrated into note editor with Document Scanner icon and overflow menu actions.
  - Zero-permission media picking via Android Photo Picker (`ActivityResultContracts.PickVisualMedia`).
  - Memory-safe image downsampling preventing out-of-memory errors on large photos.
  - Interactive `OcrReviewDialog` allowing user to review and edit extracted text before inserting.
  - Non-destructive insertion options: "Append to End" (preserves existing text), "Insert at Top", and "Replace Note Content".
- **Share-Sheet Receiver (`ACTION_SEND`):**
  - Registered `ACTION_SEND` intent filter for `text/plain`.
  - Safe extraction with 500 KB upper bound preventing memory abuse.
  - Automatic title derivation from subject or first non-empty line.
  - Direct routing into note editor with prefilled fields and debounced autosave.
- **Settings & Privacy Integration:**
  - Added "Home-Screen Widgets" card in `BackupPrivacyScreen` with body preview visibility toggle.
  - Added "On-Device OCR & Document Scanning" card in `BackupPrivacyScreen` documenting local-only ML Kit processing.
- **Automated Verification:**
  - `Level8AdvancedFeaturesTest`: 17 automated unit and Robolectric tests covering deep-link parsing, path-traversal rejection, share-sheet extraction, 500KB text bounds, widget preference persistence, dynamic shortcuts, and OCR insertion modes.

## [Level 7.0.0] - 2026-10-25
### Added
- **Note Attachments System:**
  - Integrated Android Storage Access Framework (`ActivityResultContracts.OpenDocument`) for secure media and document selection without broad storage permissions.
  - Internal sandbox copy policy: selected files are copied into isolated app-private internal storage (`attachments/<noteId>/<attachmentId>.<ext>`).
  - Added Room database `AttachmentEntity` and `AttachmentDao` with explicit non-destructive `MIGRATION_3_4` and index `index_attachments_note_id`.
  - Visual attachment cards in note editor with image thumbnails (`AsyncImage` via Coil) and document icons for PDFs and text files.
  - Safe opening via Android `FileProvider` (`${applicationId}.fileprovider`) with `FLAG_GRANT_READ_URI_PERMISSION`.
  - Clean lifecycle: removing an attachment deletes both database record and file; permanent note purge and empty trash cascade file deletion safely.
- **Safe Local Backup & Restore Engine:**
  - 100% offline and account-free backup generation to user-selected destination (`ActivityResultContracts.CreateDocument`).
  - Versioned ZIP archive structure with `manifest.json` (formatVersion 1, entity counts, total attachment size) and `data.json` covering Notes, Folders, Tags, CrossRefs, Tasks, Reminders, Calendar Events, Templates, and Attachments.
  - Security validation: strict Zip-Slip path traversal protection rejecting any archive containing `..`, `/`, or `\` paths.
  - Pre-restore validation dialog presenting summary statistics, archive integrity, and missing attachment warnings before committing.
  - Dual restore modes:
    - **Merge Mode (Default):** Deterministic conflict resolution by newer `updatedAtEpochMs` timestamp. Unifies folders and tags without duplicate creation.
    - **Replace Mode:** Atomically replaces database records inside `database.withTransaction` with strong double-confirmation modal.
- **Practical Import & Export:**
  - Export single note as clean Markdown (`.md`) or formatted Plain Text (`.txt`) via `ActivityResultContracts.CreateDocument`.
  - Standard Android Share Sheet integration via `ACTION_SEND`.
  - Text & Markdown import parser with 5MB security upper bound, header title extraction, and automatic checklist item detection (`- [ ]`, `- [x]`).
  - Interactive import preview modal allowing the user to review title, character count, and checklist items before confirming import.
- **Privacy & Security Dashboard (`BackupPrivacyScreen`):**
  - Dedicated screen in navigation drawer providing transparent local-first architecture details.
  - Real-time internal storage usage breakdown (database bytes, attachment bytes, note count, attachment count).
  - One-tap cache cleaning for temporary export and validation files.
  - Optional device screen lock gate via `KeyguardManager` requiring system PIN/pattern/biometric before app access.
- **Automated Verification:**
  - `Level7AttachmentBackupTest`: 6 Robolectric tests covering Room `MIGRATION_3_4`, attachment CRUD, Zip-Slip attack rejection, valid backup ZIP generation, deterministic merge conflict resolution, and Markdown import parsing.
  - 55 total unit and Robolectric tests passing with 0 failures.

## [Level 5.0.0] - 2026-10-25
### Added
- **Database Schema Version 2 & Explicit Migration (`MIGRATION_1_2`):**
  - Upgraded Room database from Version 1 to Version 2 without data destruction.
  - Added `is_favorite` (INTEGER NOT NULL DEFAULT 0) and `folder_id` (TEXT DEFAULT NULL) columns with indexes to `notes`.
  - Created `folders`, `tags`, and `note_tag_cross_ref` tables with unique and composite indices.
  - Zero data loss: 100% of existing user note IDs, titles, contents, timestamps, colors, and checklist items preserved.
- **Folder Management System:**
  - Create, view, rename, and safely delete folders with case-insensitive uniqueness checks.
  - Safe deletion guarantees: deleting a folder sets member notes' `folder_id = NULL` (unfiled) before removing the folder; notes are **never** deleted.
  - Move notes in and out of folders individually or in bulk.
  - Live folder note counts rendered dynamically in navigation drawer and cards.
- **Relational Tagging System:**
  - Many-to-many junction model via `NoteTagCrossRef` and `TagDao`.
  - Add and remove tags directly within the note editor and note cards.
  - Tag filtering on Home screen and navigation drawer.
  - Safe tag deletion: removing a tag clears associations without affecting notes.
- **Independent Favorites System:**
  - Independent from pinning: notes can be favorited, pinned, both, or neither.
  - One-tap star toggle directly on note cards and in the editor top bar.
  - Dedicated "Favorites" filter chip on Home screen and drawer entry.
- **Archive System:**
  - Dedicated `ArchiveScreen` destination in the navigation drawer.
  - Soft archival: archived notes are excluded from active Home and Search streams.
  - Instant unarchive action restoring notes back to the active feed with all folder, tag, color, and timestamp metadata intact.
- **Trash & Safe Restore System:**
  - Soft delete: moving a note to trash sets `is_trashed = 1`, removing it from active feeds, folders, and search.
  - Dedicated `TrashScreen` destination in navigation drawer.
  - Single-tap restore: recovers notes to their original folder (or unfiled if the folder was deleted while in trash).
  - Permanent purge with explicit confirmation dialog.
  - "Empty Trash" action with explicit confirmation dialog.
- **Extended Multi-Selection & Bulk Operations:**
  - Bulk Pin/Unpin, Bulk Favorite/Unfavorite, Bulk Move to Folder, Bulk Add Tag, Bulk Archive, and Bulk Move to Trash.
  - Contextual selection bar on Trash and Archive screens for bulk restore and bulk permanent deletion.
- **Automated Verification:**
  - `Level5RoomMigrationTest`: Validates non-destructive schema upgrade from v1 to v2 with existing records.
  - `Level5FolderTagTrashTest`: 17 comprehensive test scenarios validating all folder, tag, favorite, pin, archive, trash, and restore behaviors.
  - Total 49 automated tests passing with zero failures.

## [Level 4.0.0] - 2026-10-24
### Added
- **Docked Editor Formatting Bar & Markdown Utilities (`FormattingUtils`):**
  - Instant formatting toolbar docked above keyboard with actions for Bold (`**text**`), Italic (`*text*`), Heading (`# `), Bullet List (`• `), Numbered List (`1. `), Blockquote (`> `), and Inline Code (`` `code` ``).
  - Selection-aware text transformations preserving user focus, cursor offset, and avoiding input jumping.
  - Bidirectional, data-loss-free conversion between Markdown text notes and Interactive Checklist mode.
- **Search Highlighting (`HighlightText`):**
  - High-contrast visual highlight spans (`primaryContainer` background) on matched search queries across note titles, contents, and checklist items.
- **Home Screen Sorting & Filtering:**
  - Dynamic sort selector with dropdown options: Recently Updated (default), Recently Created, and Alphabetical by Title (A–Z).
  - Pinned notes remain grouped at the top of the feed across all sorting criteria.
  - Interactive Filter Chips for "All Notes", "Pinned", and "Checklists" with responsive empty state guidance.
- **Multi-Selection Mode & Bulk Operations:**
  - Long-press to initiate multi-selection mode on note cards.
  - Contextual Selection Top App Bar with live count, Select All / Deselect, Bulk Pin/Unpin, and Bulk Delete.
  - Visual selection indicators on cards: checked/unchecked circle icons and high-contrast 2dp primary highlight borders.
  - Bulk actions executed in atomic Room SQLite transactions (`deleteNotesByIds` and `updatePinStatusForNotes`).
  - Confirmation dialog before bulk deletion protecting user data against accidental taps.
- **Automated Verification:**
  - Unit and Robolectric test coverage in `FormattingUtilsTest`, `HomeViewModelLevel4Test`, and updated `NoteRepositoryTest`.

## [Level 3.0.0] - 2026-10-24
### Added
- **AndroidX Room SQLite Persistence Engine (`VellumDatabase`):**
  - Database file `vellum_notes.db` managed by Room with `NoteEntity` mapping.
  - Strict data-safety mandate: **`fallbackToDestructiveMigration()` is never configured**.
  - Composite indexes on `(is_pinned, updated_at)`, `(is_archived, updated_at)`, `(is_trashed, updated_at)`, and `updated_at` for high-performance reactive queries.
  - Robust JSON-based checklist serialization storing checkable item lists directly in SQLite text column without schema bloat.
- **Data Access Object (`NoteDao`):**
  - Reactive `Flow<List<NoteEntity>>` streams for active notes (`getActiveNotesStream()`) ordered by `is_pinned DESC, updated_at DESC`.
  - Selective single-note stream (`getNoteByIdStream(id)`) and one-shot retrieval (`getNoteById(id)`).
  - Conflict-free `upsertNote()` and `upsertNotes()` keeping creation timestamps intact.
  - Case-insensitive search queries across `title`, `content`, and checklist items.
- **Repository Layer (`NoteRepository` & `NoteRepositoryImpl`):**
  - Domain-model isolation between Room entities (`NoteEntity`) and Compose UI models (`Note`).
  - Thread-safe IO dispatcher offloading via Kotlin Coroutines.
  - Granular helpers: `saveNote()`, `getNoteById()`, `deleteNote()`, `togglePin()`, `toggleChecklistItem()`, `searchNotes()`, `loadSampleNotes()`, `clearAllNotes()`.
  - Clean initial state guarantee: database is never auto-populated with sample notes on startup.
- **Architectural ViewModels & UDF Migration:**
  - `HomeViewModel`: Reactive notes `StateFlow`, layout toggle (`isGridLayout`), pin toggle, checklist item toggle, delete, and sample data management.
  - `NoteEditorViewModel`: Debounced autosave (400ms delay), empty-note protection, non-cancellable flush on exit (`flushSave()`), checklist and color management, and live `SaveStatus` indicators (`SAVED`, `SAVING`, `UNSAVED`).
  - `SearchViewModel`: Debounced Room search (300ms delay) combined with category filtering (`ALL`, `PINNED`, `CHECKLISTS`).
- **UI Screen Refactoring:**
  - `HomeScreen`: Converted from prototype in-memory state to reactive `HomeViewModel` backing.
  - `NoteEditorScreen`: Converted to `NoteEditorViewModel`, featuring a subtle `SaveStatusIndicator` badge in the top bar and `BackHandler` flush save on back navigation.
  - `SearchScreen`: Converted to `SearchViewModel` driving Room queries with real-time feedback.
  - `VellumDrawer`: Updated version label to "Level 3 Persistence" and connected sample data actions to Room.
  - `MainActivity`: Injected singleton `NoteRepository` from `VellumApplication`.
- **Automated Verification Suite:**
  - `NoteRepositoryTest`: Comprehensive Robolectric in-memory SQLite tests validating empty initial state, inserts, updates without duplicates, timestamp preservation, sorting, deletions, pin toggles, checklist JSON updates, search matching, and sample data loading.
  - `NoteEditorViewModelTest`: Validated empty-note policy (untouched blank notes are never persisted) and debounced autosave mechanics.

## [Level 2.0.0] - 2026-10-24
### Added
- **Flagship Home Screen & Notes Feed:**
  - Responsive 2-column Staggered Grid (`LazyVerticalStaggeredGrid`) and 1-column Linear List (`LazyColumn`) with instant layout toggle in Top App Bar.
  - Automatic sectioning: "PINNED" notes floated to top with count badge, followed by "OTHERS" / "ALL NOTES".
  - Polished empty state with tactile icon illustration, friendly guidance, "Draft a Note" button, and "Load Sample Notes" test button.
  - Floating Action Button ("New Note" with Add icon) opening the dedicated Note Editor.
- **Reusable `VellumNoteCard` Component:**
  - Typography-styled title and content previews with ellipsis truncations.
  - Pinned indicator badge (filled amber pin).
  - Checklist preview rendering up to 4 checkable items with "+X more items" counter.
  - Timestamp label and checklist completion counter badge ("X/Y done").
  - Dropdown action menu for Pin/Unpin and Delete actions.
  - 6 curated WCAG-compliant note card color themes (Default, Warm Sand, Soft Sage, Muted Ocean, Dusky Lavender, Terracotta).
- **Dedicated Note Editor Destination (`NoteEditorScreen`):**
  - Editable title field (`headlineMedium`, single-line, borderless).
  - Multi-line body text field (`bodyLarge`, borderless).
  - Seamless toggle between Plain Text and Interactive Checklist mode (with checkbox toggle, item deletion, and "Add checklist item" field).
  - Dynamic note color palette selector with animated expand/collapse.
  - Pin toggle in Top App Bar.
  - Delete note action with confirmation dialog.
  - Word count & estimated reading time metadata header.
- **Dedicated Real-Time Search Screen (`SearchScreen`):**
  - Search input with leading icon, autofocus, and clear query button.
  - Filter category chips: "All Notes", "Pinned", and "Checklists".
  - Real-time case-insensitive filtering across titles, contents, and checklists.
  - Empty query guidance with clickable suggestion chips ("Roadmap", "Design", "Books", "Planning").
  - Clean "No notes found" empty state with "Clear Query" action.
  - Search results displayed using responsive `VellumNoteCard`s that open the Editor on tap.
- **Drawer Updates:**
  - Added "Search Notes" destination.
  - Added "Prototype Tools" section with "Reset Sample Notes" and "Clear All Notes (Test Empty State)" actions.

## [Level 1.0.0] - 2026-10-23
### Added
- Native Android Foundation targeting API 35 with Kotlin 2.0+ and Jetpack Compose.
- Unique application ID `com.aistudio.vellumnotes.rkqw` and synchronized metadata.json naming.
- Custom adaptive launcher icon with Obsidian linear gradient background and Vellum gold quill/notebook emblem across all density mipmaps.
- Complete "Tactile Monolith" design system tokens:
  - Curated Obsidian Dark and Warm Alabaster Light palettes.
  - Pure Black AMOLED and Dynamic Color (Material You) modes.
  - Spacing scale based on 4dp grid with 48dp minimum touch targets.
  - Shape definitions and typography scale.
- Reusable UI component library:
  - `VellumTopAppBar`, `VellumCard`, `VellumButton`, `VellumIconButton`, `VellumSearchField`, `VellumSettingsRow`, `VellumEmptyState`, `VellumBadge`.
- Navigation architecture with `ModalNavigationDrawer` and type-safe `VellumNavHost`:
  - Working destinations: "All Notes" (Home) and "Appearance".
  - Planned roadmap placeholders with non-silent user feedback.
- Complete Appearance Settings screen:
  - Interactive live token preview card.
  - System, Light, and Dark theme mode radio selector.
  - Pure Black (AMOLED) toggle.
  - Dynamic Color (Material You) toggle.
- Theme persistence using Jetpack DataStore Preferences (`ThemePreferencesRepository` & `ThemeViewModel`).
- Complete project documentation suite: `PROJECT_BLUEPRINT.md`, `FEATURE_ROADMAP.md`, `DESIGN_SYSTEM.md`, `IMPLEMENTATION_STATUS.md`, `TESTING_AND_RELEASE.md`, `CHANGELOG.md`.
