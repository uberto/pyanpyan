# ItemSlider UI Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Redesign ItemSlider with sky blue circle (black border), animated background colors (gray/green), and fading text labels.

**Architecture:** Update app primary color to sky blue, modify ItemSlider to use fixed circle color with thick border, add background color animation based on position, add text labels with opacity animation.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Compose Animation

---

## Task 1: Update Primary Color to Sky Blue

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Theme.kt`

**Step 1: Update primary color in LightColorScheme**

Open `androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Theme.kt` and update the color scheme:

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF87CEEB),      // Sky blue (changed from 0xFFB4D5A8)
    secondary = Color(0xFFFFC5A8),     // Soft orange (unchanged)
    tertiary = Color(0xFFA8D5E3),      // Soft blue (unchanged)
    background = Color(0xFFFFFBF5),    // Warm white (unchanged)
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF1A4D5C),    // Dark blue for text on sky blue (changed from 0xFF2D4A26)
    onSecondary = Color(0xFF4A2D1F),
    onTertiary = Color(0xFF1F3A4A),
    onBackground = Color(0xFF3A3A3A),
    onSurface = Color(0xFF3A3A3A)
)
```

**Step 2: Build the project**

Run: `./gradlew :androidApp:assembleDebug`
Expected: SUCCESS

**Step 3: Visual check on device**

Run: `./gradlew :androidApp:installDebug`

Visual verification:
- Open app → go to any checklist
- Verify icons next to items are now sky blue (not green)
- Sliders will still have old colors (we'll fix in next task)

**Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Theme.kt
git commit -m "feat: change primary color from soft green to sky blue

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Redesign Slider Circle with Fixed Sky Blue and Black Border

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt`

**Step 1: Remove dynamic circle color logic**

Open `androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt`.

Find the Box that renders the thumb/circle (around line 153-168) and replace the entire Box:

```kotlin
Box(
    modifier = Modifier
        .offset(x = thumbX)
        .align(Alignment.TopStart)
        .padding(vertical = 4.dp)
        .size(32.dp)
        .clip(CircleShape)
        .border(2.dp, Color.Black, CircleShape)  // Thick black border
        .background(MaterialTheme.colorScheme.primary)  // Sky blue fill
)
```

This removes the color logic that changed based on `offsetX` and replaces it with:
- Fixed primary color (sky blue) background
- 2dp black border

**Step 2: Build the project**

Run: `./gradlew :androidApp:assembleDebug`
Expected: SUCCESS

**Step 3: Visual check on device**

Run: `./gradlew :androidApp:installDebug`

Visual verification:
- Open checklist, drag slider left/right
- Circle should stay sky blue with black border at all positions
- Background and text still have old behavior (we'll fix next)

**Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt
git commit -m "feat: make slider circle fixed sky blue with thick black border

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Add Animated Background Color

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt`

**Step 1: Add color constants at top of file**

After the imports in `ItemSlider.kt`, add:

```kotlin
private val SkippedGray = Color(0xFFD3D3D3)
private val DoneGreen = Color(0xFF4CAF50)
```

**Step 2: Calculate background color based on position**

In the `ItemSlider` function body, after the `threshold` calculation (around line 70), add:

```kotlin
// Background color based on position
val backgroundColor = when {
    offsetX.value < -threshold -> SkippedGray
    offsetX.value > threshold -> DoneGreen
    else -> MaterialTheme.colorScheme.surfaceVariant
}
```

**Step 3: Apply backgroundColor to the track Box**

Find the main Box that renders the track (around line 72), and update the `.background()` modifier:

Before:
```kotlin
.background(MaterialTheme.colorScheme.surfaceVariant)
```

After:
```kotlin
.background(backgroundColor)
```

**Step 4: Build and test**

Run: `./gradlew :androidApp:assembleDebug && ./gradlew :androidApp:installDebug`

Visual verification:
- Drag slider left → background turns light gray
- Drag slider right → background turns green
- Return to center → background returns to neutral gray
- Color transitions should be smooth, not abrupt

**Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt
git commit -m "feat: add animated background color to slider based on position

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Replace Moving Text Labels with Fixed Positioned Fading Text

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt`

**Step 1: Calculate text opacities based on position**

In the `ItemSlider` function body, after the `backgroundColor` calculation, add:

```kotlin
// Text opacity based on position
val skippedOpacity = if (offsetX.value < 0) {
    (kotlin.math.abs(offsetX.value) / maxOffset).coerceIn(0f, 1f)
} else {
    0f
}

val doneOpacity = if (offsetX.value > 0) {
    (offsetX.value / maxOffset).coerceIn(0f, 1f)
} else {
    0f
}
```

**Step 2: Remove old text label code**

Find and delete the entire `when` block that shows "Skipped"/"Done" text based on offsetX position (around lines 171-192). This is the code that positions text on opposite side from ball.

**Step 3: Add new fixed-position text labels**

Inside the `BoxWithConstraints` after the thumb Box, add both text labels:

```kotlin
// "Skipped" text on left side
Text(
    text = "Skipped",
    color = Color.DarkGray.copy(alpha = skippedOpacity),
    style = MaterialTheme.typography.labelSmall,
    modifier = Modifier
        .align(Alignment.CenterStart)
        .padding(start = 16.dp)
)

// "Done" text on right side
Text(
    text = "Done",
    color = Color.White.copy(alpha = doneOpacity),
    style = MaterialTheme.typography.labelSmall,
    modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 16.dp)
)
```

**Step 4: Build and test**

Run: `./gradlew :androidApp:assembleDebug && ./gradlew :androidApp:installDebug`

Visual verification:
- At center: both texts invisible
- Drag left slowly: "Skipped" fades in, stays on left
- Drag right slowly: "Done" fades in, stays on right
- Text doesn't move with circle, only fades in/out
- "Skipped" is dark gray (visible on gray background)
- "Done" is white (visible on green background)

**Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt
git commit -m "feat: add fixed-position fading text labels to slider

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Adjust Text Colors for Better Contrast

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt`

**Step 1: Improve text color logic based on background**

The previous implementation uses fixed colors, but we can improve contrast. Update the text labels:

For "Skipped" text, change color calculation:
```kotlin
// "Skipped" text on left side
Text(
    text = "Skipped",
    color = if (offsetX.value < -threshold) {
        Color.DarkGray.copy(alpha = skippedOpacity)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = skippedOpacity)
    },
    style = MaterialTheme.typography.labelSmall,
    modifier = Modifier
        .align(Alignment.CenterStart)
        .padding(start = 16.dp)
)
```

For "Done" text, keep white color since green background needs light text:
```kotlin
// "Done" text on right side
Text(
    text = "Done",
    color = Color.White.copy(alpha = doneOpacity),
    style = MaterialTheme.typography.labelSmall,
    modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 16.dp)
)
```

**Step 2: Build and test**

Run: `./gradlew :androidApp:assembleDebug && ./gradlew :androidApp:installDebug`

Visual verification:
- "Skipped" text readable on gray background
- "Done" text readable on green background
- Both texts fade smoothly

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt
git commit -m "feat: improve text color contrast based on background

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 6: Final Visual Testing and Polish

**Step 1: Comprehensive visual testing**

Launch app and test slider behavior:

1. **Color verification:**
   - Circle: Sky blue (#87CEEB) with 2dp black border
   - Left zone background: Light gray (#D3D3D3)
   - Right zone background: Green (#4CAF50)
   - Center background: Warm gray (surfaceVariant)

2. **Text verification:**
   - "Skipped" appears on left, fades in as you drag left
   - "Done" appears on right, fades in as you drag right
   - Both disappear at center position
   - Text colors have good contrast

3. **Animation smoothness:**
   - Background color transitions smoothly
   - Text opacity fades smoothly
   - Circle position follows finger smoothly
   - Snap animation works (300ms)

4. **Different states:**
   - Test pending item (center position)
   - Test skipped item (left position)
   - Test done item (right position)
   - Verify reset behavior works

5. **Haptic feedback:**
   - Verify haptic still triggers on commit (if enabled in settings)

**Step 2: Test on different screen sizes**

If possible, test on:
- Small phone screen
- Large phone screen
- Tablet (if available)

Verify:
- Text doesn't overlap with circle at any position
- Track width scales appropriately
- All elements visible and readable

**Step 3: Check icon colors across app**

Navigate through app and verify sky blue primary color looks good:
- Library screen: checklist items
- Checklist screen: item icons
- CreateEdit screen: any icons or accents

**Step 4: Document any issues**

If any visual issues found, note them but don't fix yet (this is verification only).

---

## Completion Checklist

- [ ] Primary color changed to sky blue (#87CEEB)
- [ ] onPrimary color updated to dark blue (#1A4D5C)
- [ ] Slider circle fixed to sky blue with 2dp black border
- [ ] Background color animates between gray, neutral, and green
- [ ] "Skipped" and "Done" text labels added
- [ ] Text opacity fades based on slider position
- [ ] Text colors provide good contrast
- [ ] Smooth animations verified
- [ ] All slider states tested (pending, skipped, done)
- [ ] Haptic feedback still works
- [ ] All changes committed with proper messages

---

## Visual Reference

**Final slider appearance:**

```
Center (Pending):
┌────────────────────────────────────────┐
│  ░░░░░░░░░░░░  ⚫️ (sky blue) ░░░░░░░░│
│  (neutral gray background)              │
└────────────────────────────────────────┘

Left Zone (Skipping):
┌────────────────────────────────────────┐
│  ⚫️ (sky blue)  "Skipped"  ░░░░░░░░░░│
│  (light gray background #D3D3D3)        │
└────────────────────────────────────────┘

Right Zone (Done):
┌────────────────────────────────────────┐
│  ░░░░░░░░░░  "Done"  ⚫️ (sky blue)   │
│  (green background #4CAF50)             │
└────────────────────────────────────────┘
```

Circle: ⚫️ = Sky blue fill (#87CEEB) with 2dp black border

---

## Notes

**Design Document:** See `docs/plans/2026-02-18-slider-ui-redesign-design.md` for full design rationale.

**Key Changes:**
- Circle no longer changes color (was gray/green/onSurface, now always sky blue)
- Background now changes color (was always surfaceVariant, now gray/neutral/green)
- Text labels now fixed position with opacity animation (was moving with ball)
- Primary app color changed from green to sky blue

**Future Enhancements:**
- Respect Android "Reduce motion" accessibility setting for animations
- Add customizable color themes in settings
- Consider adding more visual feedback states
