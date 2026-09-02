#!/usr/bin/env python3
"""Prepare pinned Sable 2.0.4 for a Fabric 1.20.1 / Java 17 core compile probe.

The backport deliberately starts with the server/headless sublevel and physics
core. 1.21-only client rendering, optional integrations and mixins are excluded
from this probe so CI reports the API delta that actually blocks Simulated's
assembly handoff instead of stopping on unrelated renderer compatibility code.
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


# Match the known-good Gradle/Loom/Java stack already used by the Homestead port.
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

# This is a headless compile probe, not the final runtime jar. Renderer/client
# code and optional compatibility are intentionally deferred until the server
# sublevel/physics core compiles on 1.20.1.
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
    modImplementation "fuzs.forgeconfigapiport:forgeconfigapiport-fabric:8.0.3"
    implementation "org.apache.maven:maven-artifact:3.8.5"
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

    // Defer 1.21 renderer, client hooks, mixins and third-party integrations.
    // Keeping them out of this probe exposes the core sublevel/physics delta.
    exclude 'dev/ryanhcode/sable/mixin/**'
    exclude 'dev/ryanhcode/sable/sublevel/render/**'
    exclude 'dev/ryanhcode/sable/debug/**'
    exclude 'dev/ryanhcode/sable/compatibility/**'
    exclude 'dev/ryanhcode/sable/SableClient.java'
    exclude 'dev/ryanhcode/sable/SableClientConfig.java'
    exclude 'dev/ryanhcode/sable/fabric/**'
}}
""",
)

# Straightforward source-level compatibility rewrites that preserve semantics.
for path in ROOT.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    changed = text.replace(".getFirst()", ".get(0)")
    changed = changed.replace(
        "net.minecraft.world.level.chunk.status.ChunkStatus",
        "net.minecraft.world.level.chunk.ChunkStatus",
    )
    changed = changed.replace(
        "net.neoforged.neoforge.common.ModConfigSpec",
        "net.minecraftforge.common.ForgeConfigSpec",
    )
    changed = changed.replace("ModConfigSpec", "ForgeConfigSpec")
    if changed != text:
        path.write_text(changed, encoding="utf-8")

print(f"Prepared Sable {ROOT} for Fabric 1.20.1 headless core compile using {companion}")
