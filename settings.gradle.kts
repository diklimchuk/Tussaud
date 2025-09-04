@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("../Build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs.create("libs").from(files("../Build-logic/libs.versions.toml"))
}

rootProject.name = "Tussaud"

include(":tussaud-android")
include(":tussaud-core")

include(":sample-coroutines-loader")
project(":sample-coroutines-loader").projectDir = file("samples/coroutines-loader")
include(":sample-kotlin-calculator")
project(":sample-kotlin-calculator").projectDir = file("samples/kotlin-calculator")

