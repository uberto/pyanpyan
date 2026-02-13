// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    kotlin("multiplatform") version "2.1.0" apply false
    kotlin("plugin.serialization") version "2.1.0" apply false
    kotlin("plugin.compose") version "2.1.0" apply false
    kotlin("android") version "2.1.0" apply false
    alias(libs.plugins.android.application) apply false
    id("com.android.library") version "8.3.1" apply false
}