# TESTING AND QUALITY ASSURANCE STRATEGY

## 1. AUTOMATED TEST SUITE
- **Unit & Robolectric Tests:** JUnit 4 + Robolectric + Kotlin Coroutines Test.
  - `Level9ReleaseVerificationTest`: Comprehensive verification of Level 9 Release Candidate invariants:
    - `folderDeletion_doesNotDeleteContainedNotes`: Verifies safe cascade rule: deleting a folder unfiles notes (`folder_id = null`) without destroying member notes.
    - `trashAndRestore_preservesFolderAssociationAndTags`: Confirms soft deletion to Trash and restoration retains original folder associations and timestamps.
    - `deepLink_sqlInjectionAttempt_isSafelyRejected`: Tests defense against malicious SQL injection payloads (`' OR 1=1 --`).
    - `deepLink_scriptTagInjectionAttempt_isSafelyRejected`: Tests defense against XSS / script injection attacks.
    - `deepLink_directoryTraversalSlash_isSafelyRejected`: Rejects subpath / directory traversal navigation attempts.
    - `shareSheet_oversizedInput_isSafelyBoundedAt500KB`: Validates memory protection clamping oversized shared text to 500 KB.
    - `attachmentStorage_storesFilesInPrivateInternalDirectory`: Confirms attachments reside strictly inside private internal app storage (`context.filesDir/attachments`).
    - `accessibility_touchTargetMeetsOrExceeds48dp`: Enforces the 48dp minimum touch target invariant (`VellumSpacing.touchTargetMin = 48.dp`).
    - `themeConfig_serializationConsistency`: Verifies theme model immutability and state copy.
    - `widgetManager_previewPreference_togglesCorrectly`: Confirms home-screen widget privacy mode persistence.
  - `Level8AdvancedFeaturesTest`: Comprehensive verification of Level 8 capabilities (17 test cases):
    - `deepLink_newNote_returnsNewNoteTarget`: Verifies `vellum://new` routes to note creation.
    - `deepLink_search_returnsSearchTarget`: Verifies `vellum://search` routes to the search screen.
    - `deepLink_favorites_returnsFavoritesTarget`: Verifies `vellum://favorites` routes to filtered favorites feed.
    - `deepLink_openNote_withValidId_returnsOpenNoteTarget`: Verifies valid note IDs route to editor.
    - `deepLink_openNote_withPathTraversal_isRejected`: Rejects directory traversal paths (`..`).
    - `deepLink_openNote_withSpecialCharacters_isRejected`: Rejects control characters and invalid symbols.
    - `deepLink_unsupportedScheme_returnsNone`: Rejects non-`vellum://` URI schemes.
    - `shareIntent_withPlainTextAndSubject_extractsCorrectly`: Extracts subject as title and text as note body.
    - `shareIntent_withPlainTextWithoutSubject_derivesTitleFromFirstLine`: Uses first line as title when subject is omitted.
    - `shareIntent_withExcessiveTextLength_isBoundedSafely`: Clamps incoming text at 500 KB limit to prevent OOM.
    - `shareIntent_withBlankText_returnsNone`: Disregards empty/whitespace share intents.
    - `widgetManager_previewToggle_persistsPreference`: Toggles and persists `widget_show_previews` boolean in preferences.
    - `widgetManager_notifyWidgetsDataChanged_executesWithoutError`: Verifies widget broadcast notifications execute cleanly.
    - `shortcutManager_updatesShortcutsSafely`: Tests dynamic launcher shortcut publishing and clearing.
    - `ocrTextApplication_appendMode_preservesExistingContentWithSpacing`: Verifies append mode preserves text.
    - `ocrTextApplication_insertTopMode_placesTextAtBeginning`: Verifies insert-at-top prepends text cleanly.
    - `ocrTextApplication_replaceMode_overwritesContent`: Verifies replace mode overwrites content.
  - `Level7AttachmentBackupTest`: Comprehensive verification of Level 7 features:
    - `migration_3_to_4_createsAttachmentsTableWithoutDataLoss`: Tests explicit SQLite Room migration `MIGRATION_3_4`, verifying that existing notes survive without alteration, the `attachments` table is created with foreign key rules, and composite index `index_attachments_note_id` is created.
    - `attachmentDao_crudOperationsWorkReliably`: Verifies insertion, reactive querying by note ID, domain model mapping (isImage, isPdf), and single attachment deletion.
    - `backupManager_rejectsMaliciousZipSlipArchive`: Validates that ZIP archives containing path traversal entries (`..`, `/`, `\`) are strictly identified and rejected with `BackupValidationResult.Error` before extracting.
    - `backupManager_createsValidBackupZipAndManifest`: Verifies generation of verified backup ZIP containing `manifest.json` (formatVersion 1, valid record counts) and `data.json`, and confirms subsequent archive validation succeeds.
    - `backupManager_mergeModeResolvesConflictsByNewerTimestamp`: Verifies deterministic merge mode: compares `updatedAtEpochMs` on matching record IDs, updating records when backup data is newer and retaining local records when local is newer.
    - `noteImportExport_parsesMarkdownChecklistsAndHeadersAccurately`: Verifies single-note markdown parser: extracts `# Title`, separates body text, auto-detects `- [ ]` and `- [x]` checklist lines with done states, and saves to repository with zero data loss.
  - `Level5RoomMigrationTest`: Validates explicit SQLite Room migration `MIGRATION_1_2` upgrading database from Version 1 to Version 2. Verifies:
    - Legacy notes with title, content, pin status, and color theme are 100% preserved.
    - Added columns `is_favorite` (default 0) and `folder_id` (default null).
    - Creation and operational integrity of `folders`, `tags`, and `note_tag_cross_ref` tables.
  - `Level5FolderTagTrashTest`: Comprehensive verification of all 17 Level 5 functional scenarios:
    1. Folder creation with valid name.
    2. Folder duplicate name rejection (case-insensitive).
    3. Folder blank/whitespace name rejection.
    4. Folder renaming.
    5. Move note into folder.
    6. Move note out of folder.
    7. Delete folder safely (notes moved to unfiled `folder_id = null`, never deleted).
    8. Empty folder state.
    9. Tag creation & assignment to note.
    10. Tag removal from note.
    11. Tag filtering.
    12. Tag deletion does not delete notes.
    13. Favorite toggle on note (independent of pin).
    14. Pinning maintains notes at top of list.
    15. Archive note and unarchive note.
    16. Move note to trash (soft delete), verify excluded from active notes.
    17. Restore note from trash & permanent delete note with empty trash.
  - `ThemeConfigTest`: Verifies default configurations, immutable copy updates, and enum string serializations.
  - `FormattingUtilsTest`: Verifies Markdown syntax wrapping, text insertion, line-start prefixes, and cursor preservation.
  - `HomeViewModelLevel4Test`: Verifies:
    - Sorting by Recently Updated, Recently Created, and Alphabetical (A–Z) with pinned notes grouped at top.
    - Category filtering across All Notes, Pinned, and Checklists.
    - Multi-selection state management: start selection, toggle selection, select all, clear selection.
    - Atomic bulk actions: bulk pin/unpin toggles and bulk deletions.
  - `NoteRepositoryTest`: Comprehensive Room SQLite database tests using `inMemoryDatabaseBuilder`. Verifies:
    - Empty initial state (clean database guarantee on startup).
    - Note insertion, retrieval, and mapping.
    - Note updates without duplicate rows.
    - Preservation of creation timestamp (`createdAtEpochMs`) across edits.
    - Reactive `Flow` stream emission upon database mutations.
    - Sorting by pinned status (`is_pinned DESC`) and updated timestamp (`updated_at DESC`).
    - Note deletion.
    - Persistent pin toggling.
    - JSON-serialized checklist item status toggling.
    - Case-insensitive search queries across title, content, and checklist text.
    - Bulk note deletion (`deleteNotes`) in a single transaction.
    - Bulk pin status update (`setPinStatusForNotes`) across arbitrary sets of note IDs.
    - Curated sample notes loading and clearing.
  - `NoteEditorViewModelTest`: Verifies ViewModel business logic:
    - Empty-note policy: untouched blank notes are never written to Room.
    - Debounced autosave (400ms delay) triggers database writes on title/content modifications.
    - `flushSave()` on non-cancellable coroutine context immediately commits edits upon back navigation.
    - Existing note loading upon editor initialization.
  - `ExampleRobolectricTest`: String resource context validation.
- **UI Tests & Semantics:** Unique `Modifier.testTag` applied to all primary elements:
  - Home: `home_menu_button`, `home_search_action_button`, `home_layout_toggle_button`, `home_search_trigger`, `home_create_note_fab`, `home_notes_staggered_grid`, `home_notes_linear_list`, `empty_state_draft_button`, `empty_state_load_samples_button`, `home_sort_button`, `filter_chip_ALL`, `filter_chip_PINNED`, `filter_chip_CHECKLISTS`.
  - Selection Bar: `selection_close_button`, `selection_select_all_button`, `selection_pin_toggle_button`, `selection_delete_button`, `confirm_bulk_delete_button`, `cancel_bulk_delete_button`.
  - Cards: `note_card_{id}`, `note_card_menu_{id}`, `note_card_select_{id}`.
  - Editor: `editor_back_button`, `editor_pin_toggle`, `editor_checklist_toggle`, `editor_color_palette_toggle`, `editor_delete_button`, `editor_title_input`, `editor_body_input`, `add_checklist_input`, `add_checklist_button`, `editor_save_status_indicator`, `confirm_delete_note_button`, `editor_formatting_toolbar`, `format_bold_button`, `format_italic_button`, `format_heading_button`, `format_bullet_button`, `format_numbered_button`, `format_quote_button`, `format_code_button`.
  - Search: `search_text_input`, `clear_search_button`, `search_back_button`, `search_results_grid`, `search_empty_query_prompt`, `search_no_results_state`.
  - Appearance: `appearance_back_button`, `theme_mode_system`, `theme_mode_light`, `theme_mode_dark`, `theme_switch_amoled`, `theme_switch_dynamic`.
  - Drawer: `drawer_item_all_notes`, `drawer_item_search`, `drawer_item_appearance`, `drawer_reset_samples_action`, `drawer_clear_all_action`.

## 2. QUALITY GATES
- Zero destructive database migrations in any release (`fallbackToDestructiveMigration` strictly forbidden).
- Clean Unidirectional Data Flow (UDF) without leaked mutable states or cursor jumps.
- Strict WCAG 2.1 AA compliance across Light and Dark themes.
- Touch target minimum: 48dp x 48dp on all interactive elements.
- Edge-to-edge system insets handling without layout cutoffs or bar overlaps.
- Responsive layout adaptation between 2-column staggered grid and 1-column linear list.

## 3. RELEASE BUILD & DISTRIBUTION GUIDE

### Build Commands
- **Assemble Debug APK:**
  ```bash
  gradle :app:assembleDebug
  ```
  Output Location: `app/build/outputs/apk/debug/app-debug.apk` (Size: ~25 MB)
- **Run Full Automated Unit & Robolectric Tests:**
  ```bash
  gradle :app:testDebugUnitTest
  ```
  Test Reports: `app/build/reports/tests/testDebugUnitTest/index.html`

### Signing Configuration & Production Security
- **Debug Signing:** Uses standard Android SDK debug keystore (`debug.keystore`) generated automatically with password `android`. Intended for testing on devices and emulators.
- **Production Release Signing:**
  - Before distributing on Google Play or public download channels, a dedicated secure release keystore must be generated using `keytool`:
    ```bash
    keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
    ```
  - Store `my-upload-key.jks` securely offline. Never commit keystores, key aliases, or passwords to Git.
  - Inject keystore credentials into environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) during CI/CD release builds.

## 4. LEVEL 10 FINAL RELEASE SIGN-OFF & DEVICE INSTALLATION

### Verified Installable Artifact
- **APK Path:** `app/build/outputs/apk/debug/app-debug.apk`
- **File Size:** ~25 MB
- **Architecture:** Universal / Multi-ABI compatible
- **Application ID:** `com.aistudio.vellumnotes.rkqw`
- **Minimum SDK:** Android 8.0 (API Level 26)
- **Target SDK:** Android 15 (API Level 35)

### Step-by-Step Installation Procedures

#### Procedure 1: Command-Line Installation via ADB (Fastest for Developers)
1. Ensure your Android device has **Developer Options** enabled:
   - Go to **Settings > About phone** and tap **Build number** 7 times.
   - Go to **Settings > System > Developer options** and enable **USB debugging**.
2. Connect your device to your development computer via USB cable.
3. Verify connection:
   ```bash
   adb devices
   ```
4. Run the install command:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
5. Launch **Vellum** directly from the launcher or run:
   ```bash
   adb shell am start -n com.aistudio.vellumnotes.rkqw/com.example.MainActivity
   ```

#### Procedure 2: Manual Sideloading (No Computer / Direct on Device)
1. Transfer `app/build/outputs/apk/debug/app-debug.apk` to your phone via USB file transfer, cloud drive, or local network.
2. Open the **Files** or **Downloads** app on your phone.
3. Tap on `app-debug.apk`.
4. If Android displays *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings** in the popup and toggle **Allow from this source**.
5. Tap **Install** and then tap **Open**.

### Beginner Manual Smoke-Testing Checklist (Post-Install)
1. **First Launch:** Open the app; verify the Home screen loads cleanly with the "Vellum" branding and sample notes option.
2. **Create a Note:** Tap the Floating Action Button (`+`); type a title and body text; verify autosave indicator updates to "Saved".
3. **Format & Checklist:** Tap Markdown formatting chips (Bold, List); add a checklist item and toggle its checkbox.
4. **Folders & Tags:** Create a folder from the navigation drawer; move a note into the folder; verify it appears under the folder filter.
5. **Soft Delete & Restore:** Move a note to Trash; verify it disappears from Home; open Trash from drawer and tap Restore; verify it returns to Home with original tags and folder intact.
6. **Attachment & OCR:** Attach an image via Photo Picker; tap Document Scanner / OCR icon to extract text; verify preview in review dialog before insertion.
7. **Home-Screen Widget:** Long-press your device home screen, select Widgets, find "Vellum", and add the Recent Notes widget. Verify recent notes appear.
8. **Backup Export:** Go to Settings > Backup & Privacy > Create Backup; save the ZIP file to your device storage.


