# Font Settings Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add user-configurable font family and size settings that apply to all content text throughout the app.

**Architecture:** Extend AppSettings with font fields, build custom MaterialTheme Typography from settings, update PyanpyanTheme to observe and apply typography, add Settings UI with live preview.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, DataStore, Coroutines

---

## Task 1: Add Font Fields to AppSettings Model

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/AppSettings.kt`

**Step 1: Add font fields to AppSettings data class**

Open `common/src/commonMain/kotlin/com/pyanpyan/domain/model/AppSettings.kt` and update the AppSettings data class:

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

**Step 2: Verify existing DataStore serialization works**

The existing `DataStoreSettingsRepository` already handles JSON serialization with `ignoreUnknownKeys = true` and `encodeDefaults = true`, so new fields will serialize/deserialize automatically with defaults.

No code changes needed for repository - verify it compiles.

**Step 3: Build the project**

Run: `./gradlew :common:assembleDebug`
Expected: SUCCESS

**Step 4: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/AppSettings.kt
git commit -m "feat: add font family and size fields to AppSettings

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Add ViewModel Methods for Font Settings

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsViewModel.kt`

**Step 1: Add updateFontFamily method**

Open `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsViewModel.kt` and add after the `updateHapticFeedback` method:

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
```

**Step 2: Add updateFontSize method**

Add after the `updateFontFamily` method:

```kotlin
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

**Step 3: Build the project**

Run: `./gradlew :androidApp:assembleDebug`
Expected: SUCCESS

**Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsViewModel.kt
git commit -m "feat: add font settings update methods to SettingsViewModel

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Create Typography Builder

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Typography.kt`

**Step 1: Create Typography.kt file**

Create new file `androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Typography.kt`:

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

**Step 2: Build the project**

Run: `./gradlew :androidApp:assembleDebug`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Typography.kt
git commit -m "feat: add custom typography builder with font family and scaling

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Update Theme to Use Custom Typography

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Theme.kt`

**Step 1: Add imports**

Add these imports at the top of `Theme.kt`:

```kotlin
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.pyanpyan.domain.model.AppSettings
import com.pyanpyan.domain.repository.SettingsRepository
```

**Step 2: Update PyanpyanTheme signature**

Replace the current `PyanpyanTheme` function:

```kotlin
@Composable
fun PyanpyanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    settingsRepository: SettingsRepository,
    content: @Composable () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

    val typography = remember(settings.fontFamilyName, settings.fontSizeScale) {
        buildCustomTypography(settings.fontFamilyName, settings.fontSizeScale)
    }

    // Currently only light theme (calm, soft palette for ADHD users)
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = typography,
        content = content
    )
}
```

**Step 3: Build the project (will fail - MainActivity not updated yet)**

Run: `./gradlew :androidApp:assembleDebug`
Expected: FAIL with compilation error about missing settingsRepository parameter

This is expected - we'll fix MainActivity in next task.

**Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Theme.kt
git commit -m "feat: update PyanpyanTheme to apply custom typography from settings

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Update MainActivity to Pass SettingsRepository

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`

**Step 1: Find PyanpyanTheme call in MainActivity**

Open `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt` and locate the `PyanpyanTheme` call (likely in `setContent` block).

**Step 2: Add settingsRepository parameter**

Update the `PyanpyanTheme` call to pass `settingsRepository`:

Before:
```kotlin
PyanpyanTheme {
    // content
}
```

After:
```kotlin
PyanpyanTheme(
    settingsRepository = settingsRepository
) {
    // content
}
```

Note: `settingsRepository` should already be instantiated in MainActivity from the previous activation sounds feature. If not, you'll need to create it:

```kotlin
private val settingsRepository by lazy {
    DataStoreSettingsRepository(applicationContext)
}
```

**Step 3: Build the project**

Run: `./gradlew :androidApp:assembleDebug`
Expected: SUCCESS

**Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt
git commit -m "feat: pass settingsRepository to PyanpyanTheme for font customization

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 6: Add Typography Settings UI

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt`

**Step 1: Add import for Slider**

Add at the top with other imports:

```kotlin
import androidx.compose.material3.Slider
import kotlin.math.roundToInt
```

**Step 2: Add Typography Card after Sounds Card**

In `SettingsScreen`, after the existing `Card` for Sounds settings, add:

```kotlin
Spacer(modifier = Modifier.padding(8.dp))

Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Typography",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.padding(8.dp))

        // Font Family Setting
        OutlinedTextField(
            value = settings.fontFamilyName ?: "",
            onValueChange = { viewModel.updateFontFamily(it) },
            label = { Text("Font Family") },
            placeholder = { Text("System Default") },
            supportingText = { Text("Enter installed font name (e.g. 'serif', 'monospace', 'roboto')") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.padding(8.dp))

        // Font Size Setting
        Text(
            text = "Font Size: ${(settings.fontSizeScale * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodyLarge
        )

        Slider(
            value = settings.fontSizeScale,
            onValueChange = { viewModel.updateFontSize(it) },
            valueRange = 0.7f..1.5f,
            steps = 15,  // 0.05 increments
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.padding(8.dp))

        // Preview
        Text(
            text = "Preview:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.padding(4.dp))

        Text(
            text = "The quick brown fox jumps over the lazy dog",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}
```

**Step 3: Build the project**

Run: `./gradlew :androidApp:assembleDebug`
Expected: SUCCESS

**Step 4: Test manually on device/emulator**

Run: `./gradlew :androidApp:installDebug`

Manual test:
1. Open app → Settings
2. Enter "serif" in Font Family field
3. Move Font Size slider
4. Verify preview text updates immediately
5. Navigate to Library and Checklist screens
6. Verify font changes apply everywhere

**Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt
git commit -m "feat: add typography settings UI with font family, size, and preview

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 7: Manual Testing & Verification

**Step 1: Test font family variations**

1. Launch app and go to Settings
2. Test these font names:
   - "serif" → should show serif font
   - "monospace" → should show monospace
   - "cursive" → should show cursive
   - "sans-serif" → should show sans-serif
   - "InvalidFontXYZ" → should fall back to default (no crash)
   - Empty string → should show default

**Step 2: Test font size scaling**

1. Set slider to 70% (minimum)
2. Check ChecklistScreen, Library, CreateEdit screens
3. Verify text is smaller but readable
4. Set slider to 150% (maximum)
5. Check same screens
6. Verify text is larger, may overflow slightly in ItemSlider (acceptable)

**Step 3: Test persistence**

1. Set font to "serif" and size to 120%
2. Close app completely
3. Reopen app
4. Verify settings persisted correctly

**Step 4: Test live preview**

1. In Settings, type font name character by character
2. Verify preview updates on each keystroke
3. Move slider
4. Verify preview and percentage label update immediately

**Step 5: Document any issues**

If any issues found, document them but don't fix yet (this is verification only).

---

## Completion Checklist

- [ ] AppSettings has fontFamilyName and fontSizeScale fields
- [ ] SettingsViewModel has updateFontFamily and updateFontSize methods
- [ ] Typography.kt created with buildCustomTypography function
- [ ] PyanpyanTheme updated to use custom typography
- [ ] MainActivity passes settingsRepository to theme
- [ ] Settings UI has Typography card with font field, slider, and preview
- [ ] Manual testing completed successfully
- [ ] All changes committed with proper messages

---

## Notes

**Design Document:** See `docs/plans/2026-02-18-font-settings-design.md` for full design rationale.

**Trade-offs:**
- No error message for invalid fonts (silent fallback is simpler and acceptable)
- Layout may overflow slightly at 150% scale (accessibility > perfect layout)
- TopAppBar text also scales (intentional - user chose global font settings)

**Future Enhancements:**
- Dropdown of common fonts instead of text field
- Font preview before applying
- Save favorite font combinations
- Per-screen font settings (not recommended - complexity vs. value)
