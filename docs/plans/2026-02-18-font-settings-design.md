# App-Wide Font Settings Design

## Overview

Add user-configurable font family and size settings that apply to all content text throughout the Pyanpyan Android app (checklist items, headers, labels - everything except navigation icons).

## User Requirements

- **Font Family:** Allow users to select any installed font on their device
- **Font Size:** Scalable from 70% to 150% of default size
- **Scope:** All content text (items, headers, labels, all MaterialTheme typography styles)
- **Persistence:** Settings persist across app restarts
- **UX:** Changes apply immediately with live preview

## Architecture Overview

### Components

1. **AppSettings Model** - Add `fontFamilyName` and `fontSizeScale` fields
2. **Typography Builder** - Function to create custom Typography from settings
3. **Theme Integration** - Update PyanpyanTheme to observe settings and apply typography
4. **Settings UI** - Add Typography section with font field, size slider, and preview
5. **SettingsViewModel** - Add methods to update font settings

### Data Flow

```
User changes font in Settings UI
  ↓
SettingsViewModel.updateFontFamily() / updateFontSize()
  ↓
DataStoreSettingsRepository persists to JSON
  ↓
PyanpyanTheme observes settings flow
  ↓
buildCustomTypography() creates scaled Typography
  ↓
MaterialTheme applies to all Text components
  ↓
All screens recompose with new fonts
```

## Design Details

### 1. Data Model Changes

**File:** `common/src/commonMain/kotlin/com/pyanpyan/domain/model/AppSettings.kt`

```kotlin
@Serializable
data class AppSettings(
    val swipeSound: SwipeSound = SwipeSound.SOFT_CLICK,
    val completionSound: CompletionSound = CompletionSound.NOTIFICATION,
    val enableHapticFeedback: Boolean = true,
    val fontFamilyName: String? = null,      // null = system default
    val fontSizeScale: Float = 1.0f          // 0.7 to 1.5
)
```

**Defaults:**
- `fontFamilyName: null` → system default font
- `fontSizeScale: 1.0f` → 100% scale (current behavior)

**Storage:** Existing `DataStoreSettingsRepository` handles JSON serialization automatically.

### 2. Typography Builder

**File:** `androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Typography.kt` (new file)

```kotlin
package com.pyanpyan.android.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

fun buildCustomTypography(
    fontFamilyName: String?,
    fontSizeScale: Float
): Typography {
    // Get font family, fall back to default on error
    val fontFamily = fontFamilyName?.let {
        try {
            FontFamily(Typeface.create(it, Typeface.NORMAL))
        } catch (e: Exception) {
            null  // Invalid font name, use default
        }
    }

    // Get default typography and scale all styles
    val default = Typography()
    return Typography(
        displayLarge = default.displayLarge.scale(fontFamily, fontSizeScale),
        displayMedium = default.displayMedium.scale(fontFamily, fontSizeScale),
        displaySmall = default.displaySmall.scale(fontFamily, fontSizeScale),
        headlineLarge = default.headlineLarge.scale(fontFamily, fontSizeScale),
        headlineMedium = default.headlineMedium.scale(fontFamily, fontSizeScale),
        headlineSmall = default.headlineSmall.scale(fontFamily, fontSizeScale),
        titleLarge = default.titleLarge.scale(fontFamily, fontSizeScale),
        titleMedium = default.titleMedium.scale(fontFamily, fontSizeScale),
        titleSmall = default.titleSmall.scale(fontFamily, fontSizeScale),
        bodyLarge = default.bodyLarge.scale(fontFamily, fontSizeScale),
        bodyMedium = default.bodyMedium.scale(fontFamily, fontSizeScale),
        bodySmall = default.bodySmall.scale(fontFamily, fontSizeScale),
        labelLarge = default.labelLarge.scale(fontFamily, fontSizeScale),
        labelMedium = default.labelMedium.scale(fontFamily, fontSizeScale),
        labelSmall = default.labelSmall.scale(fontFamily, fontSizeScale)
    )
}

private fun TextStyle.scale(fontFamily: FontFamily?, scale: Float): TextStyle {
    return copy(
        fontFamily = fontFamily ?: this.fontFamily,
        fontSize = fontSize * scale
    )
}
```

**Behavior:**
- If `fontFamilyName` is null → uses Material3 default font
- If `fontFamilyName` is invalid → catches exception, falls back to default (no crash)
- All Typography styles scaled proportionally
- Preserves other TextStyle properties (weight, lineHeight ratios, etc.)

### 3. Theme Integration

**File:** `androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Theme.kt`

```kotlin
@Composable
fun PyanpyanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    settingsRepository: SettingsRepository,  // NEW parameter
    content: @Composable () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

    val typography = remember(settings.fontFamilyName, settings.fontSizeScale) {
        buildCustomTypography(settings.fontFamilyName, settings.fontSizeScale)
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = typography,  // Apply custom typography
        content = content
    )
}
```

**Changes Required:**
- Add `settingsRepository` parameter to `PyanpyanTheme`
- Update `MainActivity` to pass `settingsRepository` when calling `PyanpyanTheme`
- All existing screens automatically inherit custom typography (no changes needed)

**Performance:**
- Typography rebuilds only when `fontFamilyName` or `fontSizeScale` change
- `remember()` with keys prevents unnecessary recomputation
- Font family parsing happens once per settings change

### 4. Settings UI

**File:** `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt`

Add new Card section after the existing "Sounds" card:

```
┌─ Typography Card ──────────────────────┐
│ Typography                             │
│                                        │
│ Font Family:                           │
│ [                    ]                 │
│ Leave empty for system default         │
│                                        │
│ Font Size: 100%                        │
│ [────●──────────] (70% to 150%)       │
│                                        │
│ Preview:                               │
│ "Sample checklist item text"           │
│ (renders with selected font/size)      │
└────────────────────────────────────────┘
```

**Font Family Field:**
- `OutlinedTextField` with label "Font Family"
- Placeholder: "System Default"
- Helper text: "Enter installed font name (e.g. 'serif', 'monospace', 'roboto')"
- On value change → `viewModel.updateFontFamily(value)`
- Empty string treated as null (system default)

**Font Size Slider:**
- `Slider` with valueRange = 0.7f..1.5f, steps = 16 (0.05 increments)
- Label text: "Font Size: ${(value * 100).toInt()}%"
- On value change → `viewModel.updateFontSize(value)`
- Displays percentage for user clarity

**Preview Section:**
- `Text` component with sample content
- Style: `MaterialTheme.typography.bodyLarge` (matches checklist items)
- Shows live preview as user changes settings
- Sample text: "The quick brown fox jumps over the lazy dog"

### 5. ViewModel Updates

**File:** `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsViewModel.kt`

Add two new methods:

```kotlin
fun updateFontFamily(fontName: String?) {
    viewModelScope.launch {
        val sanitizedName = fontName?.takeIf { it.isNotBlank() }
        val updated = settings.value.copy(fontFamilyName = sanitizedName)
        repository.updateSettings(updated)
            .onFailure { error ->
                Log.e("SettingsViewModel", "Failed to update font family: $error")
            }
    }
}

fun updateFontSize(scale: Float) {
    viewModelScope.launch {
        val updated = settings.value.copy(fontSizeScale = scale)
        repository.updateSettings(updated)
            .onFailure { error ->
                Log.e("SettingsViewModel", "Failed to update font size: $error")
            }
    }
}
```

## Error Handling & Edge Cases

### Invalid Font Names
- **Scenario:** User enters "MyFancyFont" but it doesn't exist on device
- **Handling:** `buildCustomTypography()` catches exception in try-catch, returns null fontFamily
- **Result:** Gracefully falls back to system default font
- **User Experience:** No error message shown (silent fallback is acceptable)

### Font Size Extremes
- **Minimum (70%):** Text becomes noticeably smaller but still readable
- **Maximum (150%):** Text grows large, may cause minor layout overflow in tight spaces
- **Trade-off:** Accessibility needs outweigh perfect layout at extremes
- **Mitigation:** Slider constrained to 70-150%, no manual input to prevent invalid values

### Performance
- Typography object rebuilds only when font settings change (not every recomposition)
- `remember()` with dependency keys optimizes recomposition
- Font family parsing happens once per settings update
- No noticeable lag expected (Typography is lightweight)

### Backward Compatibility
- Existing users get default values (`null` font, `1.0` scale)
- DataStore JSON deserialization handles missing fields with defaults
- No migration code needed
- App behavior unchanged for users who never open Settings

### Layout Impact
- Material3 components handle scaled text gracefully
- Most layouts use relative sizing (weight, fillMaxWidth) rather than fixed dimensions
- Cards and containers expand to fit scaled text
- Potential issue: ItemSlider labels at 150% may overlap slightly (acceptable)

## Testing Strategy

### Manual Testing
1. **Font Family:**
   - Try common fonts: "serif", "monospace", "cursive", "sans-serif"
   - Try invalid font name: "NonexistentFont123"
   - Try empty string (should show default)
   - Verify live preview updates

2. **Font Size:**
   - Test minimum (70%) on all screens
   - Test maximum (150%) on all screens
   - Check ChecklistScreen items, Library list, CreateEdit form
   - Verify no crashes or major layout breaks

3. **Persistence:**
   - Change settings, close app, reopen
   - Verify settings persist correctly

4. **Settings UI:**
   - Verify slider moves smoothly
   - Check percentage label updates correctly
   - Confirm preview text reflects changes immediately

### Areas to Check
- ChecklistScreen: item titles, checklist name, slider labels
- ChecklistLibraryScreen: checklist names, section headers, timestamps
- CreateEditScreen: input fields, labels, section headers
- SettingsScreen: section titles, labels, preview text

## Implementation Complexity

**Estimated effort:** Medium

**Files to modify:** 5 files
- AppSettings.kt (add 2 fields)
- SettingsViewModel.kt (add 2 methods)
- SettingsScreen.kt (add Typography card UI)
- Theme.kt (update PyanpyanTheme signature, add typography builder call)
- MainActivity.kt (pass settingsRepository to theme)

**Files to create:** 1 file
- Typography.kt (buildCustomTypography function)

**Total lines of code:** ~150 new lines

**Risk level:** Low
- Changes isolated to settings and theme
- Graceful fallbacks prevent crashes
- Existing screens require no modifications
