# Create: Simulated — Fabric 1.20.1 (Homestead) port

Working branch: **`fabric-homestead-1.20.1`**. `main` is the upstream
NeoForge/Fabric 1.21.1 codebase and is reference only — do not commit to it.

The completion plan, version map, and open defect list live in
[FABRIC_PORT_PLAN.md](FABRIC_PORT_PLAN.md). Read it before starting a version,
and update its checklists and notes log as you go.

---

## Deployment target

The port is built against, and tested in, one specific modpack instance:

```
C:\Users\xerxe\curseforge\minecraft\Instances\Homestead - A Cozy Survival Experience\mods
```

That instance runs Minecraft 1.20.1 with Fabric Loader 0.18.4, which matches the
versions pinned in `gradle.properties`. If you ever change a pinned version,
check it against the instance's `minecraftinstance.json` first — compatibility
with this pack is the goal, not compatibility with every 1.20.1 Fabric setup.

Useful paths inside the instance:

| Path | What it is |
|---|---|
| `mods/` | deployment target |
| `logs/latest.log` | mod load list, our init line, in-game chat output |
| `crash-reports/` | crash dumps if it fails to start |

---

## Finishing a version: build, deploy, launch, then wait

**This is required whenever you finish a version, or any change you want tested
in game.** Do not report a version as done off the back of a green compile.

### 1. Build

```bash
./gradlew :simulated:fabric:build
```

### 2. Remove the old jar before copying the new one

The mod id is `simulated`. Two jars declaring it will make Fabric refuse to
start, so the old one must go — this is a replace, not an add. Version numbers
differ between builds, so delete by pattern, not by exact filename.

### 3. Copy only the mod jar

`build/libs/` also contains a `-sources.jar`. Deploy the plain jar only.

```bash
MODS="/c/Users/xerxe/curseforge/minecraft/Instances/Homestead - A Cozy Survival Experience/mods"

./gradlew :simulated:fabric:build || exit 1
rm -f "$MODS"/simulated-fabric-*.jar
JAR=$(ls simulated/fabric/build/libs/simulated-fabric-*.jar | grep -v -- '-sources' | head -1)
cp "$JAR" "$MODS/"
ls -la "$MODS"/simulated-fabric-*.jar
```

Confirm exactly one `simulated-fabric-*.jar` is present, at the new version,
before moving on.

### 4. Launch the game

The account and the pack profile both live in CurseForge's bundled copy of the
Minecraft launcher, at `C:\Users\xerxe\curseforge\minecraft\Install`. Point that
launcher at that directory and it comes up signed in with the Homestead profile.

```powershell
Start-Process -FilePath "C:\Users\xerxe\curseforge\minecraft\Install\minecraft.exe" `
  -ArgumentList '--workDir','C:\Users\xerxe\curseforge\minecraft\Install'
```

**Get the argument form exactly right.** It is `--workDir`, capital D, with the
path as a separate space-separated argument:

- `--workDir "<path>"` — correct
- `--workdir=<path>` — wrong, silently ignored
- do not pass `--launcherui`

Get it wrong and the exe ignores it, creates a fresh empty
`...\Install\.minecraft`, and opens a signed-out launcher with no Homestead
profile. The "Sign in with Microsoft" screen is that symptom — close it and
delete the stray `.minecraft` folder, which is junk.

`launcher_log.txt` contains the line `--launcherui --workdir="<APPDIR>\Install"`.
That is the launcher's own internal CEF child-process form. **Do not copy it as
the invocation** — it is what caused this exact mistake on 2026-09-06.

Launching CurseForge instead is also fine, just a longer path to the same place —
pressing Play there shells out through `cmd.exe` to precisely the command above:

```powershell
Start-Process -FilePath "C:\Program Files (x86)\Overwolf\OverwolfLauncher.exe" `
  -ArgumentList '-launchapp','cchhcaiapeikjbdbpfplgmpobbcdkdaphclbmkbj'
```

CurseForge is an Overwolf app; the `curseforge://` URI does nothing when the app
is closed. Background `Overwolf` processes do not mean the window is open —
check that a process has `MainWindowTitle` of `CurseForge`.

Either route, the launcher has no supported flag to auto-press Play, so it opens
the launcher and **the user presses Play**. Say that plainly rather than claiming
you started the game.

Verify rather than assume:

```powershell
# launcher window up
Get-Process minecraft -ErrorAction SilentlyContinue | Select-Object Id,MainWindowTitle
# game actually running
Get-CimInstance Win32_Process -Filter "Name='javaw.exe'" | Select-Object ProcessId,CommandLine
```

A real game process is `javaw.exe` with `--gameDir` set to the Homestead
instance, `--version fabric-loader-0.18.4-1.20.1`, `-Xmx4096m`, and main class
`net.fabricmc.loader.impl.launch.knot.KnotClient`. Its command line also carries
the account access token — never paste that into a file, a log, or chat.
`Launcher ended with 0` in `launcher_log.txt` means the launcher was closed, not
that the game ran.

### 5. Wait for test results — do not skip this

After launching, **stop and wait for the user to report what happened in game.**
A version is not done until they confirm it. While waiting you may read
`logs/latest.log` to check objective facts, and you should report what you find:

- our line: `Starting Create Simulated Fabric port for Homestead 1.20.1; Create loaded=true`
- the loaded version in the mod list, e.g. `- simulated 1.3.2-fabric-homestead.13`
- missing model, missing texture, or missing lang key warnings
- exceptions or mixin failures naming `simulated`
- in-game chat output from our blocks, which is logged as `[CHAT]`

The log tells you whether the build loaded. It does not tell you whether the
feature is correct — that is what the user's testing is for. Do not treat a
clean log as a pass, and do not mark plan checkboxes as done based on one.

If the game crashes, read the newest file in `crash-reports/`, fix, and run this
whole cycle again.

---

## Conventions

- Keep CI green. `Fabric 1.20.1 build` runs on every push to the branch; a red
  branch tip is a stop-the-line event.
- The dedicated server is a supported target, not just singleplayer. Anything
  touching networking, persistence, or world state needs to work on both.
- Record deviations from upstream behaviour, and anything you could not port, in
  FABRIC_PORT_PLAN.md rather than dropping it silently.
- Commit and push only when asked.
