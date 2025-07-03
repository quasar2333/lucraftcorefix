# LucraftCore Fix

This mod aims to fix some issues and add utility to the LucraftCore Minecraft mod.

## Table of Contents
- [Introduction](#introduction)
- [Core Problem Addressed: Network Packet Size](#core-problem-addressed-network-packet-size)
- [Feature: Hot Reloading for Addon Packs](#feature-hot-reloading-for-addon-packs)
- [How it Works (Technical Details)](#how-it-works-technical-details)
  - [Mixins](#mixins)
  - [Compression Utility](#compression-utility)
  - [Reload Handler](#reload-handler)
- [Setup/Installation](#setupinstallation)
- [For Developers/Modpack Creators](#for-developersmodpack-creators)
- [Contributions and Credits](#contributions-and-credits)

## Introduction
**LucraftCore Fix** is a companion mod for [LucraftCore](https://www.curseforge.com/minecraft/mc-mods/lucraft-core) designed to address specific technical challenges and enhance server management.

- **Minecraft Version:** 1.12.2 (as inferred from ForgeGradle version and common LucraftCore versions)
- **Main Dependency:** LucraftCore
- **Core Functionality:**
    1.  Fixes network packet size issues related to LucraftCore's JSON-based data synchronization.
    2.  Introduces a command to hot-reload LucraftCore addon pack configurations (superpowers and suit sets) on the server.

## Core Problem Addressed: Network Packet Size
LucraftCore synchronizes complex data, such as superpower and suit set definitions, between the server and clients using JSON. In scenarios with many or very detailed addons, this JSON data can become very large, potentially exceeding Minecraft's network packet size limits. This can lead to players being disconnected or experiencing errors when joining a server or when data is synced.

This mod addresses this by:
- Using **Mixins** to intercept LucraftCore's networking code.
- Compressing the JSON data using **GZIP** before it's sent over the network.
- Decompressing the data on the receiving end.

This significantly reduces the size of the synchronization packets, making the network communication more robust. The primary network messages patched are:
- `lucraft.mods.lucraftcore.superpowers.network.MessageSyncJsonSuperpower`
- `lucraft.mods.lucraftcore.superpowers.network.MessageSyncJsonSuitSet`

## Feature: Hot Reloading for Addon Packs
This mod introduces the `/lcreload` command, which allows server administrators to reload configurations from LucraftCore addon packs without needing to restart the entire server.

**Purpose:**
- To apply changes to superpower and suit set JSON files (e.g., modifying abilities, adding new tiers, changing stats) in real-time.
- Useful for addon pack developers and server maintainers for faster iteration and testing.

**Process via `ReloadHandler`:**
1.  **Scan Files:** The command triggers a scan of all registered LucraftCore addon pack directories for `superpowers` and `suitsets` JSON files.
2.  **Update Server Data:** The mod parses these JSON files and updates the corresponding `JsonSuperpower` and `JsonSuitSet` instances in the server's memory. If a previously existing JSON file is removed, the corresponding superpower/suit set is "soft-deleted" (cleared of its abilities/attributes).
3.  **Synchronize to Clients:** The updated (and compressed) superpower and suit set data is then sent to all connected clients using the patched network messages.
4.  **Refresh Player Abilities:** For all online players, their current superpower and suit set abilities are refreshed to reflect the changes.

**Important Note on Hot Reloading:**
As per the original author's feedback, **the hot-reloading feature (`/lcreload`) is not fully functional or may have unresolved issues.** While it attempts a comprehensive reload, there might be edge cases, specific data types, or downstream effects that are not correctly handled, potentially leading to unexpected behavior or desynchronization. Use with caution and test thoroughly after use.

**Limitation:**
Changes to resources such as textures, models, or sounds defined in addon packs are **not** applied by this command. These still require a manual client-side resource pack reload (typically by pressing `F3+T`).

## How it Works (Technical Details)
This mod leverages Java bytecode manipulation (via Mixins) and data compression to achieve its goals.

### Mixins
Mixins are used to inject custom code into LucraftCore's existing classes at runtime, allowing modification of their behavior without directly altering the original LucraftCore JAR.

-   **`MessageSyncJsonSuitSetMixin.java` & `MessageSyncJsonSuperpowerMixin.java`:**
    -   These are the core of the network fix.
    -   They target the `toBytes()` method of LucraftCore's `MessageSyncJsonSuitSet` and `MessageSyncJsonSuperpower` classes. Before the original method writes the JSON data to the network buffer, these Mixins intercept the data, compress it using GZIP, and then write the compressed byte array.
    -   They also target the `fromBytes()` method. When data is received, these Mixins read the compressed byte array, decompress it, and then allow the original method (or a modified version of it) to parse the reconstructed JSON data.
    -   *Note:* The implementation of `fromBytes` in the Mixin for `MessageSyncJsonSuitSet` notes potential complexities due to how LucraftCore handles these objects, suggesting it primarily ensures the `toBytes` compression is safe and that `fromBytes` might rely on specific conditions to work correctly with the decompressed data.
    -   `MessageSyncJsonSuperpowerMixin.java` mirrors this functionality for superpower data.

-   **`AbilityContainerMixin.java`:**
    -   Targets `lucraft.mods.lucraftcore.superpowers.abilities.supplier.AbilityContainer`.
    -   Uses `@Accessor` annotations to generate public getter and setter methods for the private `abilities` field within the `AbilityContainer`.
    -   This doesn't directly change game logic but provides the `ReloadHandler` (or other parts of this mod) with the necessary access to read and modify a player's abilities, which is crucial for the hot-reloading feature to correctly refresh player powers.

-   **`UpdateCheckerMixin.java`:**
    -   Targets `lucraft.mods.lucraftcore.util.updatechecker.UpdateChecker`.
    -   Injects into the constructor of `UpdateChecker` and cancels its execution.
    -   This effectively disables LucraftCore's built-in update checking mechanism. This might be done to prevent duplicate notifications if this fix mod has its own checker, to avoid potential startup errors if the update URL is unavailable, or simply to reduce unnecessary network calls.

### Compression Utility
-   **`util/CompressionUtil.java`:**
    -   This class provides simple static methods (`compress` and `decompress`) that use Java's built-in `GZIPOutputStream` and `GZIPInputStream` to perform GZIP compression and decompression on byte arrays. This is used by the network message Mixins.

### Reload Handler
-   **`util/ReloadHandler.java`:**
    -   This class contains all the logic for the `/lcreload` command as described in the "Hot Reloading Feature" section. It handles file scanning, JSON parsing, updating LucraftCore's data structures, and triggering client synchronization.

## Setup/Installation
1.  Ensure you have Minecraft Forge installed.
2.  Install [LucraftCore](https://www.curseforge.com/minecraft/mc-mods/lucraft-core).
3.  Place the `LucraftCoreFix-x.x.x.jar` file into your Minecraft `mods` folder.

## For Developers/Modpack Creators
The `/lcreload` command can be a valuable tool during the development of LucraftCore addon packs. It allows you to see the effects of your JSON changes (for superpowers and suit sets) much more quickly than by restarting the server each time.
However, given the note about its potential instability, always back up your data and test its effects in a non-production environment first.

## Contributions and Credits
-   This mod was developed based on an initial codebase provided by **qincaizheng**. Thank you for your foundational work! You can find their GitHub profile here: [https://github.com/qincaizheng](https://github.com/qincaizheng)
-   **LucraftCore Mod:** Developed by Lucraft.
-   **Minecraft Forge & FML:** The teams behind the Forge modding platform.
-   The Mixin library by SpongePowered.

---
*This README provides an overview of the LucraftCore Fix mod's functionality and technical implementation based on analysis of its source code.*
