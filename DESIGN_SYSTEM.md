# DESIGN SYSTEM SPECIFICATION: TACTILE MONOLITH

## 1. COLOR TOKENS
- **Dark Surface Base:** `#0E1117`
- **Dark Surface Primary:** `#161B22` / Border: `#2E3440`
- **Dark Elevated:** `#212631`
- **Dark Text High-Contrast:** `#EDEDF0` / Muted: `#9098A9`
- **AMOLED Variant:** `#000000` / Surface: `#0A0C10`
- **Light Surface Base:** `#F9F9FB`
- **Light Surface Primary:** `#FFFFFF` / Border: `#E2E4E9`
- **Light Elevated:** `#F0F2F5`
- **Light Text High-Contrast:** `#11141A` / Muted: `#6B7280`
- **Signature Accent:** `#F59E0B` (Dark) / `#B45309` (Light)
- **Status Accents:** `#10B981` (Success Jade) / `#EF4444` (Crimson Warning)
- **Curated Note Tints:**
  - **Warm Sand:** `#231B13` / Border `#4A3828` (Dark) • `#FFF9EE` / Border `#EDE0C8` (Light)
  - **Soft Sage:** `#132219` / Border `#244432` (Dark) • `#F0F8F3` / Border `#D1E8D9` (Light)
  - **Muted Ocean:** `#12202E` / Border `#233E59` (Dark) • `#F0F6FD` / Border `#CCE0F5` (Light)
  - **Dusky Lavender:** `#21172A` / Border `#412E53` (Dark) • `#F8F2FC` / Border `#E5D5F2` (Light)
  - **Terracotta:** `#261616` / Border `#4F2C2C` (Dark) • `#FCF2F2` / Border `#F4D4D4` (Light)

## 2. ACCESSIBILITY RULES
- Minimum touch target: 48dp x 48dp on all interactive elements (`VellumSpacing.touchTargetMin`).
- Text contrast ratio: >= 4.5:1 against respective surface backgrounds (WCAG 2.1 AA).
- Explicit `contentDescription` on non-decorative icons and accessibility semantics on controls.
- Dynamic color support on Android 12+ (Material You) with user toggle.

## 3. COMPONENT INVENTORY
- `VellumTopAppBar`: Edge-to-edge center-aligned top app bar with navigation action support.
- `VellumCard`: Rounded container (16dp corner radius) with subtle structural borders.
- `VellumNoteCard`: Responsive note card with title, content preview, checklist rendering, pin badge, metadata, and overflow actions.
- `VellumButton` & `VellumOutlinedButton`: High-contrast, tactile action triggers.
- `VellumIconButton`: 48dp touch-target verified icon button with accessible descriptions.
- `VellumSearchField`: Embedded surface search trigger with search icon and placeholder.
- `VellumSettingsRadioRow` & `VellumSettingsSwitchRow`: Standardized settings controls.
- `VellumEmptyState`: Polished first-launch empty canvas layout with call-to-action button.
- `VellumBadge`: Pill badge for metadata, categories, and roadmap milestone tags.
