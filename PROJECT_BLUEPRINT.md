# PROJECT BLUEPRINT: VELLUM NOTES

## 1. PRODUCT VISION
Vellum is a modern, high-performance, local-first Android notes application combining tactile visual aesthetics with responsive everyday productivity capabilities.

## 2. ARCHITECTURAL PILLARS
- **UI:** Jetpack Compose, Material 3, Edge-to-Edge native.
- **Pattern:** Clean Architecture + Unidirectional Data Flow (UDF) with MVVM.
- **Persistence:** Local SQLite via AndroidX Room (`vellum_notes.db`) with zero destructive fallback migrations, atomic bulk operations (`deleteNotesByIds`, `updatePinStatusForNotes`), and Jetpack DataStore Preferences (`vellum_theme_prefs`) for application settings.
- **Editing & Formatting:** Markdown formatting toolbar (`FormattingUtils`) with cursor-preserving insertions, and seamless bidirectional conversion with Interactive Checklists.
- **Organization & Search:** Real-time query highlighting (`HighlightText`), dynamic multi-criteria sorting (Updated, Created, Title), category filter chips, and multi-selection mode with bulk pin/delete actions.
- **Concurrency:** Kotlin Coroutines and StateFlow with debounced autosave (400ms delay) and non-cancellable flush on navigation.
- **Target OS:** Android 8.0 (API 26) through Android 15 (API 35).
- **Package Namespace:** `com.example`
- **Application ID:** `com.aistudio.vellumnotes.rkqw`

## 3. PACKAGE STRUCTURE CONVENTION
```text
com.example/
├── core/
│   ├── data/
│   │   └── repository/      # NoteRepository interface and NoteRepositoryImpl with Room DAO
│   ├── database/
│   │   ├── dao/             # Room NoteDao with reactive Flow queries and case-insensitive search
│   │   ├── entity/          # NoteEntity with SQLite composite indexes and JSON checklist mappers
│   │   └── VellumDatabase.kt# RoomDatabase singleton without destructive fallback migrations
│   ├── designsystem/
│   │   ├── component/       # Reusable atomic UI components (Buttons, Cards, TopBar, EmptyState, Badges)
│   │   └── theme/           # Color tokens, Typography scale, Spacing, Shapes, Theme composables
│   ├── model/               # Pure domain models (Note, ChecklistItem, NoteColorTheme)
│   └── preferences/         # DataStore backed user preferences (ThemeConfig, Repository, ViewModel)
├── feature/
│   ├── editor/              # NoteEditorScreen & NoteEditorViewModel with 400ms debounced autosave
│   ├── home/                # HomeScreen & HomeViewModel, responsive staggered grid & linear list
│   ├── search/              # SearchScreen & SearchViewModel with 300ms debounced Room queries
│   └── settings/            # AppearanceScreen & ThemeViewModel with live token preview
├── navigation/              # Navigation host, type-safe routes, and navigation drawer
├── VellumApplication.kt     # Application entry class with database and repository singletons
└── MainActivity.kt          # Edge-to-edge entry point and root NavHost
```
