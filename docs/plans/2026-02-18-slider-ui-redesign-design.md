# ItemSlider UI Redesign Design

## Overview

Redesign the ItemSlider component with clearer visual feedback:
- Change app primary color from soft green to sky blue
- Keep slider circle fixed sky blue with thick black border (no color changes)
- Animate slider background between gray (skipped), neutral (center), and green (done)
- Show text labels ("Skipped"/"Done") that fade in/out based on position

## User Requirements

- **Primary Color:** Change from soft green (#B4D5A8) to sky blue (#87CEEB)
- **Circle:** Fixed sky blue fill with thick black border, no color changes during interaction
- **Background:** Smooth color transitions between gray (#D3D3D3), neutral, and green (#4CAF50)
- **Text:** "Skipped" and "Done" labels that fade in as user approaches zones
- **Behavior:** Smooth animations provide clear visual feedback during drag

## Design Details

### 1. Primary Color Update

**File:** `androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Theme.kt`

**Current primary color:**
```kotlin
primary = Color(0xFFB4D5A8),      // Soft green
```

**New primary color:**
```kotlin
primary = Color(0xFF87CEEB),      // Sky blue
onPrimary = Color(0xFF1A4D5C),    // Dark blue for text on sky blue
```

**Impact:**
- Checklist icons change from green to sky blue
- Slider circle becomes sky blue
- Any UI elements using `MaterialTheme.colorScheme.primary` update to sky blue

### 2. Slider Visual Design

**Circle (Thumb):**
- **Size:** 32dp diameter (unchanged)
- **Fill:** Sky blue (#87CEEB) - matches new primary color
- **Border:** 2dp thick, black (#000000)
- **Behavior:** No color changes - always sky blue with black border

**Background Track:**
- **Shape:** RoundedCornerShape(20.dp) - pill shape (unchanged)
- **Height:** 40dp (unchanged)
- **Border:** 1dp dark gray (unchanged)
- **Color States:**
  - **Center (pending):** MaterialTheme.colorScheme.surfaceVariant (warm gray)
  - **Left zone (skipped):** Light gray (#D3D3D3)
  - **Right zone (done):** Success green (#4CAF50)

**Text Labels:**
- **Left side:** "Skipped" text centered in left half
- **Right side:** "Done" text centered in right half
- **Typography:** MaterialTheme.typography.labelSmall
- **Color:**
  - On gray background: Dark gray/black for contrast
  - On green background: White for contrast
  - On neutral background: Transparent (faded out)
- **Opacity:** Animates based on slider position (0% at center, 100% past threshold)

### 3. Animation Behavior

**During Drag:**

Background color interpolation:
```kotlin
val backgroundColor = when {
    offsetX < -threshold -> Color(0xFFD3D3D3) // Gray
    offsetX > threshold -> Color(0xFF4CAF50)  // Green
    else -> MaterialTheme.colorScheme.surfaceVariant // Neutral
}
```

Text opacity calculation:
```kotlin
val skippedOpacity = if (offsetX < 0) {
    (abs(offsetX) / maxOffset).coerceIn(0f, 1f)
} else {
    0f
}

val doneOpacity = if (offsetX > 0) {
    (offsetX / maxOffset).coerceIn(0f, 1f)
} else {
    0f
}
```

**On Release:**
- If `offsetX < -threshold`: Snap to left (-maxOffset), show gray background
- If `offsetX > threshold`: Snap to right (+maxOffset), show green background
- If between thresholds: Spring back to previous committed state or center
- Snap animation: 300ms tween (unchanged)
- Haptic feedback on commit (if enabled, unchanged)

**Color Transitions:**
- Smooth lerp between colors based on offsetX position
- No abrupt jumps - continuous gradient feel
- Circle color never changes (always sky blue)

### 4. Visual States Summary

| State | Circle | Background | Left Text | Right Text |
|-------|--------|------------|-----------|------------|
| **Center (pending)** | Sky blue + black border | SurfaceVariant (warm gray) | Transparent | Transparent |
| **Dragging left** | Sky blue + black border | Gray (#D3D3D3) | Fading in "Skipped" | Transparent |
| **Committed left** | Sky blue + black border | Gray (#D3D3D3) | Opaque "Skipped" | Transparent |
| **Dragging right** | Sky blue + black border | Green (#4CAF50) | Transparent | Fading in "Done" |
| **Committed right** | Sky blue + black border | Green (#4CAF50) | Transparent | Opaque "Done" |

### 5. Implementation Changes

**File:** `androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt`

**Changes needed:**
1. Remove color logic from circle/thumb - make it fixed sky blue with black border
2. Add background color animation based on offsetX
3. Add text labels with opacity animation
4. Update text positioning (always centered in their zones, not moving with ball)
5. Calculate text colors for contrast (dark on gray, white on green)

**File:** `androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Theme.kt`

**Changes needed:**
1. Update primary color to #87CEEB (sky blue)
2. Update onPrimary color to #1A4D5C (dark blue for contrast)

## Color Palette Reference

```kotlin
// New colors
val SkyBlue = Color(0xFF87CEEB)           // Primary color, circle fill
val SuccessGreen = Color(0xFF4CAF50)      // Done state background
val LightGray = Color(0xFFD3D3D3)         // Skipped state background
val Black = Color(0xFF000000)             // Circle border
val DarkBlue = Color(0xFF1A4D5C)          // Text on sky blue

// Existing colors (for reference)
val SurfaceVariant = MaterialTheme.colorScheme.surfaceVariant  // Center state
val DarkGray = Color.DarkGray             // Track border
```

## Design Rationale

**Why fixed circle color:**
- Reduces visual noise
- Circle position already indicates state - color change is redundant
- Thick black border makes circle highly visible on all backgrounds
- Sky blue matches new primary color for brand consistency

**Why background color changes:**
- Provides clear zone feedback (where you are)
- Doesn't compete with circle for attention
- Creates pleasant "entering zone" feeling

**Why smooth transitions:**
- Swipe gestures feel more natural with gradual changes
- No jarring color jumps
- Builds anticipation as user approaches commit threshold

**Why fading text:**
- Always-visible text would clutter center state
- Opacity animation guides user toward commit zones
- Clear labels eliminate ambiguity about left vs right actions

## Accessibility Considerations

**Color Contrast:**
- Black border on sky blue circle: High contrast, visible on all backgrounds
- Dark text on gray background: WCAG AA compliant
- White text on green background: WCAG AA compliant
- Sky blue (#87CEEB) vs warm white background: Sufficient contrast

**Visual Clarity:**
- 2dp border provides strong visual boundary
- Text labels supplement color coding
- Position + color + text = three redundant signals (good for accessibility)

**Motion:**
- Smooth animations may need reduction for users with motion sensitivity
- Consider respecting Android's "Reduce motion" accessibility setting in future iteration

## Testing Strategy

**Visual Testing:**
1. Verify sky blue circle with black border visible on all backgrounds
2. Check text fades in smoothly as slider moves
3. Confirm background color transitions are smooth (no jumps)
4. Test on light and dark device backgrounds
5. Verify colors match spec exactly (use color picker tool)

**Interaction Testing:**
1. Drag slowly left → verify gray background and "Skipped" text appear
2. Drag slowly right → verify green background and "Done" text appear
3. Drag past threshold and release → verify snap animation works
4. Drag slightly and release → verify returns to previous state
5. Check haptic feedback still works (if enabled)

**Cross-screen Testing:**
1. ChecklistScreen: Verify all item sliders look correct
2. Test with different checklist states (pending, done, skipped)
3. Verify slider works correctly when returning to previous state

## Implementation Complexity

**Estimated effort:** Small to Medium

**Files to modify:** 2 files
- Theme.kt (update primary color)
- ItemSlider.kt (redesign circle, background, text)

**Lines of code:** ~80 lines modified

**Risk level:** Low
- Visual-only changes, no logic changes
- Background color animation is straightforward lerp
- Text opacity calculation is simple math
- No breaking changes to API
