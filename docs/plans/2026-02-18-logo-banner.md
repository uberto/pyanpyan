# Logo Banner Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace "Checklists" text title with Pyanpyan logo banner image in the main library screen.

**Architecture:** Copy PNG logo to drawable resources, modify ChecklistLibraryScreen TopAppBar to use Image composable instead of Text for the title, spanning full width with Settings icon overlaid.

**Tech Stack:** Kotlin, Jetpack Compose, Android Resources, painterResource

---

## Task 1: Copy Logo to Drawable Resources

**Files:**
- Create: `androidApp/src/main/res/drawable/topbanner.png`

**Step 1: Copy the logo file**

Run:
```bash
cp topbanner.png androidApp/src/main/res/drawable/topbanner.png
```

Expected: File copied successfully

**Step 2: Verify the file exists**

Run:
```bash
ls -lh androidApp/src/main/res/drawable/topbanner.png
```

Expected: Shows file with ~1.2M size

**Step 3: Commit**

```bash
git add androidApp/src/main/res/drawable/topbanner.png
git commit -m "feat: add Pyanpyan logo banner to drawable resources

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Update ChecklistLibraryScreen with Logo

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt`

**Step 1: Add required imports**

Add these imports at the top of the file (after existing imports):

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pyanpyan.android.R
```

**Step 2: Replace TopAppBar title**

Find the TopAppBar component (around line 64):

Before:
```kotlin
TopAppBar(
    title = { Text("Checklists") },
    actions = {
```

After:
```kotlin
TopAppBar(
    title = {
        Image(
            painter = painterResource(R.drawable.topbanner),
            contentDescription = "Pyanpyan",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )
    },
    actions = {
```

**Step 3: Build the project**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Install and verify on emulator**

Run:
```bash
./gradlew :androidApp:installDebug && ~/Library/Android/sdk/platform-tools/adb shell am start -n com.pyanpyan.android/com.pyanpyan.android.MainActivity
```

Expected:
- App installs and launches
- Library screen shows Pyanpyan logo banner instead of "Checklists" text
- Logo spans full width of TopAppBar
- Settings icon visible on the right

**Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt
git commit -m "feat: replace Checklists text with Pyanpyan logo banner

- Use Image composable in TopAppBar title
- Logo spans full width with ContentScale.Fit
- Settings icon remains overlaid on top-right
- Height set to 48dp for optimal appearance

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Completion Checklist

- [ ] topbanner.png copied to drawable resources
- [ ] Imports added to ChecklistLibraryScreen.kt
- [ ] TopAppBar title replaced with Image composable
- [ ] Project builds successfully
- [ ] App displays logo correctly on emulator
- [ ] Logo maintains aspect ratio
- [ ] Settings icon still accessible
- [ ] All changes committed with proper messages

---

## Notes

**Design Document:** See `docs/plans/2026-02-18-logo-banner-design.md` for full design rationale.

**Key Implementation Details:**
- Logo height: 48dp (fits within TopAppBar constraints)
- ContentScale.Fit maintains aspect ratio
- fillMaxWidth() ensures logo spans full title area
- Settings icon remains as TopAppBar action (overlaid)

**Visual Verification:**
- Logo should be clearly visible and not distorted
- White/transparent background should blend with app theme
- Settings icon should not obscure important parts of logo
- Logo should scale appropriately on different screen sizes

**Accessibility:**
- contentDescription = "Pyanpyan" for screen readers
- No functional changes to navigation

**Future Enhancements:**
- Create multi-density versions (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) for optimized file sizes
- Convert to vector drawable for perfect scaling and smaller size
- Add subtle fade-in animation on screen launch
