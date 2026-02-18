# Sky Blue Background Design

## Goal

Apply sky blue color to the TopAppBar and app background in the ChecklistLibraryScreen to create a cohesive branded look that matches the Pyanpyan logo banner.

## Context

The app currently has:
- TopAppBar with white background (surface color)
- App background around checklists in warm white (background color)
- Logo banner with Pyanpyan branding recently added
- Sky blue already defined as primary color (#87CEEB)

The logo banner looks disconnected from the rest of the UI. By applying sky blue to the TopAppBar and surrounding area, we create a unified branded appearance.

## Architecture

### Visual Design

Apply sky blue (primary color) to two UI elements in ChecklistLibraryScreen:

1. **TopAppBar containerColor** - Changes from white to sky blue, creating seamless visual flow with the logo banner
2. **Scaffold containerColor** - Changes from warm white to sky blue, creating a branded frame around the white checklist cards

### Component Structure

Both changes are simple parameter updates in the existing Scaffold/TopAppBar components in ChecklistLibraryScreen.kt.

### Color Scheme

- **Sky blue:** `Color(0xFF87CEEB)` - Already defined as `MaterialTheme.colorScheme.primary`
- **White checklist cards:** Remain unchanged for contrast and readability
- **Text on sky blue:** Uses `onPrimary` color (dark blue #1A4D5C) for accessibility

## Implementation Details

### Changes Required

**File:** `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt`

1. **TopAppBar colors (line ~87-89):**
   ```kotlin
   // Before
   colors = TopAppBarDefaults.topAppBarColors(
       containerColor = MaterialTheme.colorScheme.surface
   )

   // After
   colors = TopAppBarDefaults.topAppBarColors(
       containerColor = MaterialTheme.colorScheme.primary
   )
   ```

2. **Scaffold parameter (line ~66):**
   ```kotlin
   // Before
   Scaffold(
       topBar = { ... },
       floatingActionButton = { ... }
   ) { padding ->

   // After
   Scaffold(
       containerColor = MaterialTheme.colorScheme.primary,
       topBar = { ... },
       floatingActionButton = { ... }
   ) { padding ->
   ```

### Visual Impact

**Before:**
- White TopAppBar
- Warm white background around checklists
- Logo banner appears disconnected from UI

**After:**
- Sky blue TopAppBar blending with logo banner
- Sky blue frame around white checklist cards
- Cohesive branded appearance
- Better visual hierarchy (branded frame → content cards)

### Accessibility

- Text color automatically uses `onPrimary` (dark blue #1A4D5C)
- Sufficient contrast ratio maintained (WCAG AA compliant)
- Settings icon remains clearly visible
- Checklist cards remain white for optimal readability

### Other Screens

This change only affects ChecklistLibraryScreen (main screen). Other screens remain unchanged:
- ChecklistScreen (detail view) - uses checklist-specific colors
- SettingsScreen - retains white TopAppBar
- CreateEditScreen - retains existing styling

## User Experience

- Immediate branded appearance on app launch
- Creates visual connection between logo and app UI
- White checklist cards stand out against sky blue frame
- Professional, cohesive design language
- No functional changes - purely visual enhancement

## Technical Considerations

### Performance
- No performance impact - simple color parameter changes
- No additional rendering overhead

### Compatibility
- Works across all screen sizes and densities
- No Android version requirements
- Dark mode not affected (app currently light-mode only)

### Maintenance
- Uses existing theme colors (primary)
- No new color definitions needed
- Consistent with Material 3 design system

## Success Criteria

- TopAppBar displays in sky blue (#87CEEB)
- Area around checklist cards displays in sky blue
- Logo banner blends seamlessly with TopAppBar
- Checklist cards remain white and readable
- Settings icon clearly visible on sky blue background
- No visual glitches or layout issues

## Future Enhancements

- Apply similar sky blue framing to other screens if desired
- Add subtle gradient or texture to sky blue background (optional)
- Implement dark mode with matching color scheme
