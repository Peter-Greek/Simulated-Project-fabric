#!/usr/bin/env python3
"""Resolve every asset and lang key the game would actually load for this mod.

V1 ships on a clean log: no missing model, texture, sound or translation in the
modpack. Those only appear once the game is running, which makes them a slow
thing to find. This does the same resolution offline, against the same three
places the game looks — this mod's resources, Create's jar, and vanilla.

Reachability matters, so this walks the same way the game does rather than
checking every file on disk. The roots are the blockstates and item models
generated for what is actually registered, plus `sounds.json`; from there it
follows model parents and texture references. An upstream model for a block V1
does not register yet is never loaded, and so is never a warning — reporting it
would bury the real ones.

    python tools/check_resources.py

It reads what a build would ship: `src/main/generated` first, then the upstream
tree minus the two directories `processResources` excludes. Exit status is
non-zero when something is unresolved, so it can gate a build.
"""

import io
import json
import os
import re
import sys
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GENERATED = os.path.join(REPO, "simulated", "fabric", "src", "main", "generated")
UPSTREAM = os.path.join(REPO, "simulated", "common", "src", "main", "resources")
FABRIC_RES = os.path.join(REPO, "simulated", "fabric", "src", "main", "resources")
BASES = (GENERATED, UPSTREAM, FABRIC_RES)

# processResources drops these; see simulated/fabric/build.gradle.
EXCLUDED = ("assets/simulated/pinwheel/", "data/sable/")

GRADLE_HOME = os.environ.get("GRADLE_USER_HOME") or os.path.join(os.path.expanduser("~"), ".gradle")
CACHE = os.path.join(GRADLE_HOME, "caches")


def jars():
    """Create's jar and the Minecraft jar, for the assets we inherit.

    Both are only in the Gradle cache once a build has run, so this has to come
    after one; with neither present every inherited reference reads as missing,
    which is loud enough to notice rather than a silent pass.
    """
    found = []
    for root, _, names in os.walk(CACHE):
        for name in names:
            if not name.endswith(".jar") or "sources" in name:
                continue
            if name.startswith("create-fabric-") or name.startswith("minecraft-merged-1"):
                found.append(os.path.join(root, name))
    return found


class Resources:
    """Every resource path a build would ship, plus the ones we inherit."""

    def __init__(self):
        self.local = {}
        self.inherited = set()

        for base in BASES:
            if not os.path.isdir(base):
                continue
            for root, _, names in os.walk(base):
                for name in names:
                    full = os.path.join(root, name)
                    rel = os.path.relpath(full, base).replace(os.sep, "/")
                    if rel.startswith(".cache/"):
                        continue
                    if base is UPSTREAM and rel.startswith(EXCLUDED):
                        continue
                    self.local.setdefault(rel, full)

        for path in jars():
            try:
                with zipfile.ZipFile(path) as archive:
                    self.inherited.update(n for n in archive.namelist() if not n.endswith("/"))
            except zipfile.BadZipFile:
                continue

    def has(self, path):
        return path in self.local or path in self.inherited

    def read(self, path):
        if path in self.local:
            try:
                return json.load(io.open(self.local[path], encoding="utf-8"))
            except ValueError:
                return None
        return None


def as_path(reference, kind, extension):
    """A resource location as the file path the game would look for."""
    namespace, _, path = reference.partition(":")
    if not path:
        namespace, path = "minecraft", namespace
    return "assets/%s/%s/%s%s" % (namespace, kind, path, extension)


SOURCE = os.path.join(REPO, "simulated", "fabric", "src", "main", "java")

# SimLang.translate("gui.x") is simulated.gui.x; Component.translatable takes the
# whole key. Only literals are checkable — a key built by concatenation ends at
# the quote, so anything ending in a dot is skipped rather than reported.
LANG_PATTERNS = (
    (re.compile(r'SimLang\.translate\(\s*"([^"]+)"'), "simulated."),
    (re.compile(r'translatable\(\s*"(simulated\.[^"]+)"'), ""),
    (re.compile(r'translatable\(\s*"((?:block|item|itemGroup|stat|advancement|generator|commands|key|subtitles)'
                r'\.simulated[^"]*)"'), ""),
)


def lang_keys():
    """Every simulated: translation key named as a literal in the source."""
    path = os.path.join(GENERATED, "assets", "simulated", "lang", "en_us.json")
    if not os.path.exists(path):
        return [("lang", "en_us.json", "not generated")]
    known = set(json.load(io.open(path, encoding="utf-8")))

    unresolved = []
    for root, _, names in os.walk(SOURCE):
        for name in names:
            if not name.endswith(".java"):
                continue
            full = os.path.join(root, name)
            rel = os.path.relpath(full, SOURCE).replace(os.sep, "/")
            text = io.open(full, encoding="utf-8").read()
            for pattern, prefix in LANG_PATTERNS:
                for found in pattern.findall(text):
                    key = prefix + found
                    if key.endswith(".") or key in known:
                        continue
                    unresolved.append(("lang", key, rel))
    return unresolved


def main():
    resources = Resources()
    missing = []
    seen = set()
    queue = []

    def want(reference, kind, extension, source):
        path = as_path(reference, kind, extension)
        if not resources.has(path):
            missing.append((kind, reference, source))
            return None
        return path

    def visit_model(reference, source):
        path = want(reference, "models", ".json", source)
        if path is None or path in seen:
            return
        seen.add(path)
        queue.append((path, reference))

    # Roots: what is registered. Generated blockstates and item models exist
    # for exactly the blocks and items that made it into the registries.
    roots = [rel for rel in resources.local
             if rel.startswith("assets/simulated/blockstates/")
             or rel.startswith("assets/simulated/models/item/")]

    for rel in sorted(roots):
        data = resources.read(rel)
        if data is None:
            continue
        if "/blockstates/" in rel:
            for variant in (data.get("variants") or {}).values():
                for entry in (variant if isinstance(variant, list) else [variant]):
                    if isinstance(entry, dict) and "model" in entry:
                        visit_model(entry["model"], rel)
            for case in (data.get("multipart") or []):
                entry = case.get("apply")
                for one in (entry if isinstance(entry, list) else [entry]):
                    if isinstance(one, dict) and "model" in one:
                        visit_model(one["model"], rel)
        else:
            seen.add(rel)
            queue.append((rel, rel))

    while queue:
        path, source = queue.pop()
        data = resources.read(path)
        if data is None:
            # Inherited from Create or vanilla; their own references are theirs.
            continue
        if isinstance(data.get("parent"), str):
            visit_model(data["parent"], source)
        for value in (data.get("textures") or {}).values():
            if isinstance(value, str) and not value.startswith("#"):
                want(value, "textures", ".png", source)
        # OBJ models name their geometry as a path under assets/<ns>/, and the
        # .obj in turn names a .mtl beside it. Neither is a resource location,
        # so neither is checked by the walk above.
        geometry = data.get("model")
        if isinstance(geometry, str) and geometry.endswith(".obj"):
            namespace, _, inner = geometry.partition(":")
            if not inner:
                namespace, inner = "minecraft", namespace
            obj = "assets/%s/%s" % (namespace, inner)
            if not resources.has(obj):
                missing.append(("obj", geometry, source))
            else:
                for line in (io.open(resources.local[obj], encoding="utf-8", errors="replace")
                             if obj in resources.local else []):
                    if line.startswith("mtllib "):
                        mtl = obj.rsplit("/", 1)[0] + "/" + line.split(None, 1)[1].strip()
                        if not resources.has(mtl):
                            missing.append(("mtl", mtl, obj))

        for override in (data.get("overrides") or []):
            if isinstance(override, dict) and isinstance(override.get("model"), str):
                visit_model(override["model"], source)

    sounds = resources.read("assets/simulated/sounds.json")
    for event, definition in (sounds or {}).items():
        for sound in definition.get("sounds", []):
            if isinstance(sound, dict):
                if sound.get("type") == "event":
                    continue
                name = sound.get("name", "")
            else:
                name = sound
            want(name, "sounds", ".ogg", "sounds.json [" + event + "]")

    missing += lang_keys()

    print("walked %d model(s) from %d registered root(s)" % (len(seen), len(roots)))
    if not missing:
        print("every model, texture, sound and lang key the game would load resolves")
        return 0

    print("\n%d unresolved reference(s):\n" % len(missing))
    for kind, reference, source in sorted(set(missing)):
        print("  %-9s %-46s  from %s" % (kind, reference, source))
    return 1


if __name__ == "__main__":
    sys.exit(main())
