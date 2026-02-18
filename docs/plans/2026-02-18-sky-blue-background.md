# Sky Blue Background Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Apply sky blue color to TopAppBar and app background in ChecklistLibraryScreen to create cohesive branded look.

**Architecture:** Simple color parameter changes in ChecklistLibraryScreen - update TopAppBar containerColor and add Scaffold containerColor parameter to use primary (sky blue) color.

**Tech Stack:** Kotlin, Jetpack Compose, Material3 color scheme

---

## Task 1: Apply Sky Blue to TopAppBar and Background

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt`

**Step 1: Update TopAppBar containerColor**

Find the TopAppBar colors parameter (around line 87-89):

Before:
```kotlin
colors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface
)
```

After:
```kotlin
colors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primary
)
```

This changes the TopAppBar from white to sky blue.

**Step 2: Add Scaffold containerColor**

Find the Scaffold declaration (around line 66):

Before:
```kotlin
Scaffold(
    topBar = {
```

After:
```kotlin
Scaffold(
    containerColor = MaterialTheme.colorScheme.primary,
    topBar = {
```

This applies sky blue to the area around the checklist cards.

**Step 3: Build the project**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Install and verify on devices**

Run:
```bash
./gradlew :androidApp:installDebug && ~/Library/Android/sdk/platform-tools/adb shell am start -n com.pyanpyan.android/com.pyanpyan.android.MainActivity
```

Expected visual result:
- TopAppBar displays in sky blue (#87CEEB)
- Logo banner blends seamlessly with sky blue TopAppBar
- Sky blue background/frame around white checklist cards
- Settings icon clearly visible on sky blue background
- Checklist cards remain white for contrast
- FAB button (already sky blue) matches the theme

**Step 5: Verify on all connected devices**

If multiple devices connected, install on each:
```bash
# For Pixel 7a
~/Library/Android/sdk/platform-tools/adb -s adb-39151JEHN10754-RipO7w._adb-tls-connect._tcp install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && ~/Library/Android/sdk/platform-tools/adb -s adb-39151JEHN10754-RipO7w._adb-tls-connect._tcp shell am start -n com.pyanpyan.android/com.pyanpyan.android.MainActivity

# For emulator
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && ~/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell am start -n com.pyanpyan.android/com.pyanpyan.android.MainActivity
```

**Step 6: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt
git commit -m "feat: apply sky blue background to library screen

- TopAppBar containerColor changed to primary (sky blue)
- Scaffold containerColor set to primary (sky blue)
- Creates cohesive branded frame matching logo banner
- Checklist cards remain white for contrast and readability

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Completion Checklist

- [ ] TopAppBar containerColor changed from surface to primary
- [ ] Scaffold containerColor parameter added with primary value
- [ ] Project builds successfully
- [ ] App displays sky blue TopAppBar on all devices
- [ ] Sky blue background visible around checklist cards
- [ ] Logo banner blends seamlessly with TopAppBar
- [ ] Settings icon clearly visible
- [ ] Checklist cards remain white and readable
- [ ] All changes committed with proper message

---

## Notes

**Design Document:** See `docs/plans/2026-02-18-sky-blue-background-design.md` for full design rationale.

**Key Implementation Details:**
- Only two parameter changes required
- Uses existing theme color (primary = #87CEEB)
- No new imports needed
- Purely visual change, no functional impact

**Visual Verification:**
- Check that logo banner and TopAppBar form cohesive visual unit
- Verify sky blue frame creates clear hierarchy (frame → cards)
- Ensure sufficient contrast for Settings icon
- Confirm checklist cards stand out against sky blue background

**Color Reference:**
- Sky blue (primary): `#87CEEB`
- Text on sky blue (onPrimary): `#1A4D5C` (dark blue, WCAG AA compliant)
- Checklist cards: White (unchanged)

**Other Screens:**
This change only affects ChecklistLibraryScreen. Other screens remain unchanged:
- ChecklistScreen uses checklist-specific colors
- SettingsScreen retains white TopAppBar
- CreateEditScreen retains existing styling
