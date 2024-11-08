<img width="200" src="https://github.com/RelativityMC/C2ME-fabric/raw/ver/1.17/src/main/resources/assets/c2me/icon.png" alt="C2ME icon" align="right">
<div align="left">
<h1>C^2M-Engine</h1>

[![Github-CI](https://github.com/RelativityMC/C2ME-fabric/workflows/C2ME%20Build%20Script/badge.svg)](https://github.com/RelativityMC/C2ME-fabric/actions?query=workflow%3ACI)
[![Build Status](https://ci.codemc.io/job/RelativityMC/job/C2ME-fabric/job/ver%252F1.18/badge/icon)](https://ci.codemc.io/job/RelativityMC/job/C2ME-fabric/job/ver%252F1.18/)
[![Discord](https://img.shields.io/discord/756715786747248641?logo=discord&logoColor=white)](https://discord.gg/Kdy8NM5HW4)
<h3>A Fabric mod designed to improve the chunk performance of Minecraft.</h3>
</div>

## So what is C2ME?
C^2M-Engine, or C2ME for short, is a Fabric mod designed to improve the performance of chunk generation, I/O, and loading. This is done by taking advantage of multiple CPU cores in parallel. For the best performance it is recommended to use C2ME with [Lithium](https://github.com/CaffeineMC/lithium-fabric) and [Starlight](https://github.com/Spottedleaf/Starlight).

## What does C2ME stand for?
Concurrent chunk management engine, it's about making the game better threaded and more scalable in regard to world gen and chunk io performance.

## Vanilla parity
C2ME does not sacrifice vanilla functionality or behavior, or alter the vanilla world generation in the name of raw speed by default.
However, due to the [non-determinism of vanilla world generation](https://bugs.mojang.com/browse/MC-55596), worlds will vary
significantly run-to-run even with the same seed. This is not a bug on our side. 

While we carefully check that we do not modify any vanilla behavior, bugs are unavoidable after all. 
So, if you do encounter an issue where C2ME deviates from the intended vanilla behavior, don't hesitate to open an issue.

## Mod and Datapack compatibility
World generation datapacks that can run on vanilla Minecraft are fully supported.  
Custom world generators implemented in mods usually runs well, but *may* cause compatibility issues due to certain
design assumption used by mod authors being broken for further speedups of world generation.  
As a world generation mod author, if you find your mod broken, don't hesitate to look for help in our discord server (linked below).
We are willing to help mod authors to embrace scalable world generation.  

### Undefined behavior sanitization
C2ME includes `CheckedThreadLocalRandom` for world random (included in [UWRAD](https://modrinth.com/mod/uwrad)) plus a few others.
These detections exist to prevent mods from screwing up Minecraft internals and causing undebuggable problems.  
The detection should almost **never** produce false positives, and should be taken seriously and reported
to corresponding mod authors instead.

## Usage notice
**Backup your worlds and practice good game modding skills.**

## Downloads
Modrinth: https://modrinth.com/mod/c2me-fabric  
CurseForge: https://www.curseforge.com/minecraft/mc-mods/c2me

## Support status for Minecraft versions
Only the latest Minecraft release and the latest Minecraft snapshot are fully supported. 
Older Minecraft releases are in long-term support and will receive critical bug fixes.
Older Minecraft snapshots are not supported. 

## Support
Our issue tracker: [link](https://github.com/RelativityMC/C2ME-fabric/issues)  
Our discord server: [link](https://discord.gg/Kdy8NM5HW4)

## Building and setting up
JDK 22+, Clang 18+ are required to build C2ME  
Run the following commands in the root directory:

```shell
git submodule update --init --recursive
./gradlew clean build
```

## License
License information can be found [here](/LICENSE).

