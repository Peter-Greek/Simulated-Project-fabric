# Fabric 1.20.1 / Homestead port

This branch is the working branch for porting the Simulated Project to Fabric on Minecraft 1.20.1 for the Homestead modpack runtime.

## Locked runtime

- Minecraft 1.20.1
- Java 17
- Fabric Loader 0.18.4
- Fabric API 0.92.8+1.20.1
- Create Fabric 6.0.8.1+build.1744-mc1.20.1 (Modrinth version `HAqwA6X1`)

Do not update these versions independently without checking the Homestead pack first. The goal is compatibility with the pack, not compatibility with every possible 1.20.1 Fabric environment.

## Port order

1. Establish a clean Fabric-only build and metadata.
2. Port loader-neutral Simulated registries and basic blocks/items.
3. Replace NeoForge events, networking, config, and access transformers with Fabric APIs/access wideners or mixins.
4. Backport Minecraft 1.21.1 APIs used by Simulated to 1.20.1 equivalents.
5. Port/backport the Sable integration required for physics sublevels.
6. Restore rendering/Veil integration and client code.
7. Validate assembly, movement, saving/loading, networking, and multiplayer inside the full Homestead pack.
8. Port Aeronautics and Offroad after Simulated is stable.

## Current milestone

The Fabric module is intentionally a loadable skeleton. It packages the existing common resources while keeping Java isolated from the NeoForge-backed `simulated/common` Gradle module. Java systems will be moved into the Fabric compilation set incrementally so compile failures remain attributable to one subsystem at a time.

## Reference implementation

Loader-side Fabric work in `bobqianic/create-aeronautics-fabric` is useful as an MIT-licensed reference for event, registry, networking, config, and client hooks. Code brought over from that project should retain appropriate attribution and still be reviewed for 1.20.1 API differences.
