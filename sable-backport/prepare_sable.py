#!/usr/bin/env python3
"""Prepare pinned Sable 2.0.4 for a Fabric 1.20.1 / Java 17 compile probe.

This intentionally rewrites only the build shell. Source-level 1.21 -> 1.20.1
backports are added here incrementally as CI exposes them. Keeping the transform
reproducible lets us stay pinned to the exact Sable generation Simulated 1.3.2
expects instead of maintaining a large copied third-party tree in this fork.
"""

from __future__ import annotations

import pathlib
import re
import sys


ROOT = pathlib.Path(sys.argv[1]).resolve()
LIBS = pathlib.Path(sys.argv[2]).resolve() if len(sys.argv) > 2 else ROOT / ".backport-libs"


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace(rel: str, old: str, new: str) -> None:
    path = ROOT / rel
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"expected text not found in {rel}: {old!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


# Gradle 9 + Loom 1.16 are for the current 1.21 build. Match the known-good
# toolchain already used by the Homestead Simulated port.
replace(
    "gradle/wrapper/gradle-wrapper.properties",
    "gradle-9.5.0-bin.zip",
    "gradle-8.10.2-bin.zip",
)

write(
    "build.gradle",
    """plugins {
    id 'fabric-loom' version '1.8.13' apply false
}
""",
)

# Fabric-only backport lane. The common project remains only as a source/resource
# provider; it is compiled through Loom in the Fabric project so we do not need
# NeoForge ModDev just to obtain Mojang classes.
write(
    "settings.gradle",
    """pluginManagement {
    repositories {
        maven { name = 'Fabric'; url = uri('https://maven.fabricmc.net/') }
        gradlePluginPortal()
        mavenCentral()
    }
}
rootProject.name = 'sable-fabric-1.20.1-backport'
include('common')
include('fabric')
""",
)

props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
replacements = {
    r"(?m)^java_version=.*$": "java_version=17",
    r"(?m)^minecraft_version=.*$": "minecraft_version=1.20.1",
    r"(?m)^minecraft_version_range=.*$": "minecraft_version_range=[1.20.1]",
    r"(?m)^fabric_version=.*$": "fabric_version=0.92.8+1.20.1",
    r"(?m)^fabric_loader_version=.*$": "fabric_loader_version=0.18.4",
}
for pattern, value in replacements.items():
    props, count = re.subn(pattern, value, props)
    if count != 1:
        raise RuntimeError(f"expected one property match for {pattern}, got {count}")
(ROOT / "gradle.properties").write_text(props, encoding="utf-8")

# Strip 1.21 compatibility dependencies from the shared convention for the
# first core compile. We will add back 1.20.1 equivalents only when a retained
# source package actually requires them.
write(
    "buildSrc/src/main/groovy/multiloader-common.gradle",
    """plugins {
    id 'java-library'
}

base {
    archivesName = "${mod_id}-${project.name}-${minecraft_version}"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(java_version)
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven { url = 'https://maven.fabricmc.net/' }
    maven { url = 'https://maven.ryanhcode.dev/releases' }
    maven { url = 'https://maven.blamejared.com' }
    maven { url = 'https://raw.githubusercontent.com/Fuzss/modresources/main/maven/' }
    maven { url = 'https://maven.createmod.net' }
    maven { url = 'https://maven.tterrag.com' }
    maven { url = 'https://api.modrinth.com/maven' }
}
""",
)

write(
    "buildSrc/src/main/groovy/multiloader-loader.gradle",
    """plugins {
    id 'multiloader-common'
}

configurations {
    commonJava { canBeResolved = true }
    commonResources { canBeResolved = true }
}

dependencies {
    commonJava project(path: ':common', configuration: 'commonJava')
    commonResources project(path: ':common', configuration: 'commonResources')
}

tasks.named('compileJava', JavaCompile) {
    dependsOn(configurations.commonJava)
    source(configurations.commonJava)
}

processResources {
    dependsOn(configurations.commonResources)
    from(configurations.commonResources)
}

tasks.named('sourcesJar', Jar) {
    dependsOn(configurations.commonJava)
    from(configurations.commonJava)
    dependsOn(configurations.commonResources)
    from(configurations.commonResources)
}
""",
)

write(
    "common/build.gradle",
    """plugins {
    id 'multiloader-common'
}

configurations {
    commonJava {
        canBeResolved = false
        canBeConsumed = true
    }
    commonResources {
        canBeResolved = false
        canBeConsumed = true
    }
}

artifacts {
    commonJava sourceSets.main.java.sourceDirectories.singleFile
    commonResources sourceSets.main.resources.sourceDirectories.singleFile
}
""",
)

companion_jars = sorted(
    p for p in LIBS.rglob("*.jar")
    if "sources" not in p.name.lower() and "javadoc" not in p.name.lower()
)
if not companion_jars:
    raise RuntimeError(f"No non-sources Sable Companion jar found under {LIBS}")
companion = companion_jars[0].as_posix()

# First source probe deliberately leaves Rapier nesting and Veil/client-only
# dependencies out. The goal is to expose the Java API delta in Sable's core
# against 1.20.1 before native/runtime packaging is attempted.
write(
    "fabric/build.gradle",
    f"""plugins {{
    id 'fabric-loom'
    id 'multiloader-loader'
}}

repositories {{
    mavenCentral()
    maven {{ url = 'https://maven.fabricmc.net/' }}
    maven {{ url = 'https://maven.ryanhcode.dev/releases' }}
    maven {{ url = 'https://raw.githubusercontent.com/Fuzss/modresources/main/maven/' }}
    maven {{ url = 'https://api.modrinth.com/maven' }}
}}

dependencies {{
    minecraft "com.mojang:minecraft:${{minecraft_version}}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${{fabric_loader_version}}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${{fabric_version}}"
    modImplementation files('{companion}')
}}

loom {{
    def aw = project(':common').file('src/main/resources/' + mod_id + '.accesswidener')
    if (aw.exists()) {{
        accessWidenerPath.set(aw)
    }}
}}

java {{
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}}

tasks.withType(JavaCompile).configureEach {{
    options.release = 17
}}
""",
)

# Java 21 collection convenience methods have direct Java 17 equivalents.
for path in ROOT.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    changed = text.replace(".getFirst()", ".get(0)")
    if changed != text:
        path.write_text(changed, encoding="utf-8")

print(f"Prepared Sable {ROOT} for Fabric 1.20.1 core compile using {companion}")
