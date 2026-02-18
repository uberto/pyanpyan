# Pyanpyan Logo Banner Design

## Goal

Replace the "Checklists" text title in the main library screen with the Pyanpyan logo banner image that spans the full width of the TopAppBar.

## Context

The app currently displays "Checklists" as a text title in the TopAppBar of the ChecklistLibraryScreen. We want to replace this with a branded logo banner (topbanner.png - 2152x584 pixels, 1.2MB).

## Architecture

### Component Structure

The ChecklistLibraryScreen uses a Scaffold with TopAppBar. We'll modify the TopAppBar's title parameter to display an Image composable instead of Text.

### Resource Management

- **Source file:** `topbanner.png` (2152x584 pixels, PNG format)
- **Destination:** `androidApp/src/main/res/drawable/topbanner.png`
- **Approach:** Single-density resource (Android will handle scaling automatically)

### Layout Approach

**Chosen: Approach A - Single Density Resource**

Use Image composable within the title parameter with `ContentScale.Fit` to maintain aspect ratio. This is the simplest approach and works well for the wide banner format.

**Alternatives considered:**
- Multi-density resources: More work, optimized per device but unnecessary for initial implementation
- Custom TopAppBar layout: More complex, not needed for this use case

## Implementation Details

### Visual Design

- Banner spans full width of TopAppBar title area
- Image height: 48dp (fits nicely within TopAppBar constraints)
- Maintains aspect ratio using `ContentScale.Fit`
- Settings icon remains overlaid on top-right as action button
- Background: App's surface color (from MaterialTheme)

### Code Changes

**File:** `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt`

Replace:
```kotlin
title = { Text("Checklists") }
```

With:
```kotlin
title = {
    Image(
        painter = painterResource(R.drawable.topbanner),
        contentDescription = "Pyanpyan",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    )
}
```

### Required Imports

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
```

## User Experience

- Logo is immediately visible on app launch (main screen)
- Professional branding replaces generic "Checklists" text
- Settings icon remains accessible in top-right corner
- Logo scales appropriately across different screen sizes and densities

## Technical Considerations

### File Size
- 1.2MB PNG is acceptable for modern Android devices
- Can optimize with multi-density resources in future if needed

### Accessibility
- contentDescription = "Pyanpyan" for screen readers
- No functional change to navigation or interaction

### Performance
- Image loaded once via painterResource (cached by Android)
- Minimal performance impact

## Future Enhancements

- Create multi-density versions (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) for optimized file sizes
- Convert to vector drawable if possible for smaller file size and perfect scaling
- Add subtle animation on screen launch (optional)

## Success Criteria

- Logo displays clearly on library screen
- Maintains aspect ratio across devices
- Settings icon remains accessible
- No performance degradation
- Accessible via screen readers
