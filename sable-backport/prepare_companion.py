#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: prepare_companion.py <sable-companion checkout>")

root = Path(sys.argv[1]).resolve()
versions = root / "libs.versions.toml"
text = versions.read_text(encoding="utf-8")
replacements = {
    'java-version = "21"': 'java-version = "17"',
    'minecraft = "1.21.1"': 'minecraft = "1.20.1"',
    'loom = "1.16.+"': 'loom = "1.8.13"',
    'loom = { id = "net.fabricmc.fabric-loom", version.ref = "loom" }': 'loom = { id = "fabric-loom", version.ref = "loom" }',
    'loom-remap = { id = "net.fabricmc.fabric-loom-remap", version.ref = "loom" }': 'loom-remap = { id = "fabric-loom", version.ref = "loom" }',
}
for old, new in replacements.items():
    if old not in text:
        raise SystemExit(f"expected version line not found: {old}")
    text = text.replace(old, new)
versions.write_text(text, encoding="utf-8")

# Current Companion dynamically discovers subprojects with a Kotlin DSL idiom
# that no longer type-infers cleanly after moving the build back to Gradle 8.
# The backport only needs the common and Fabric projects, so make that explicit.
settings = root / "settings.gradle.kts"
settings.write_text(
    '''pluginManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net")
        gradlePluginPortal()
    }
}

val modName = "sable-companion"
rootProject.name = modName

includeBuild("build-logic")

include(":sable-companion-common")
project(":sable-companion-common").projectDir = file("common")

include(":sable-companion-fabric")
project(":sable-companion-fabric").projectDir = file("fabric")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("./libs.versions.toml"))
        }
    }
}
''',
    encoding="utf-8",
)

# Loom 1.8 targets Gradle 8.10, whereas current Companion uses Gradle 9.4.
wrapper = root / "gradle" / "wrapper" / "gradle-wrapper.properties"
wrapper_text = wrapper.read_text(encoding="utf-8")
old_distribution = "gradle-9.4.0-bin.zip"
if old_distribution not in wrapper_text:
    raise SystemExit("unexpected Sable Companion Gradle wrapper version")
wrapper.write_text(
    wrapper_text.replace(old_distribution, "gradle-8.10.2-bin.zip"),
    encoding="utf-8",
)

# Give the experimental artifact a distinct version so it can never be
# confused with RyanHCode's official release when installed/published locally.
properties = root / "gradle.properties"
text = properties.read_text(encoding="utf-8")
if "version = 1.6.0" not in text:
    raise SystemExit("unexpected Sable Companion version")
text = text.replace("version = 1.6.0", "version = 1.6.0-fabric-1.20.1-homestead.1")
properties.write_text(text, encoding="utf-8")

print("Prepared Sable Companion for Java 17 / Minecraft 1.20.1 / Loom 1.8")
