// ===== BEGIN ReloadHandler.java (FINAL, WITH ERROR HANDLING) =====
package com.ultimatekevin.lucraftcorefix.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lucraft.mods.lucraftcore.LucraftCore;
import lucraft.mods.lucraftcore.addonpacks.AddonPackHandler;
import lucraft.mods.lucraftcore.network.LCPacketDispatcher;
import lucraft.mods.lucraftcore.superpowers.JsonSuperpower;
import lucraft.mods.lucraftcore.superpowers.Superpower;
import lucraft.mods.lucraftcore.superpowers.SuperpowerHandler;
import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.superpowers.abilities.supplier.AbilityContainer;
import lucraft.mods.lucraftcore.superpowers.network.MessageSyncJsonSuperpower;
import lucraft.mods.lucraftcore.superpowers.network.MessageSyncJsonSuitSet;
import lucraft.mods.lucraftcore.superpowers.suitsets.JsonSuitSet;
import lucraft.mods.lucraftcore.superpowers.suitsets.SuitSet;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ReloadHandler {

    public static void reloadAddons(MinecraftServer server, ICommandSender sender) {
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Starting LucraftCore addon pack reload..."));
        long startTime = System.currentTimeMillis();

        // --- 1. 扫描文件系统获取新的 JSON 定义 ---
        Map<ResourceLocation, JsonObject> newSuperpowers = new ConcurrentHashMap<>();
        Map<ResourceLocation, JsonObject> newSuitSets = new ConcurrentHashMap<>();
        AtomicInteger failedFiles = new AtomicInteger(0);

        scanAllAddons(newSuperpowers, newSuitSets, sender, failedFiles);

        if (failedFiles.get() > 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Found " + failedFiles.get() + " invalid or unreadable addon files. Check the server log for details."));
        }

        // --- 2. 更新内存中的超能力和套装实例 ---
        LucraftCore.LOGGER.info("[LucraftCoreFix] Applying reloaded data...");

        // 更新超能力
        for (Superpower sp : SuperpowerHandler.SUPERPOWER_REGISTRY) {
            if (sp instanceof JsonSuperpower) {
                JsonObject json = newSuperpowers.get(sp.getRegistryName());
                try {
                    // 如果在新文件中找不到，就传入一个空Json来“软删除”(清空其能力)
                    ((JsonSuperpower) sp).deserialize(json != null ? json : new JsonObject());
                    if (json != null) {
                        ((JsonSuperpower) sp).jsonOriginal = json;
                    }
                } catch (Exception e) {
                    String errorMsg = "Failed to reload superpower '" + sp.getRegistryName() + "'. It may have been soft-deleted. Reason: " + e.getMessage();
                    LucraftCore.LOGGER.error(errorMsg);
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + errorMsg));
                }
            }
        }

        // 更新套装 (逻辑与超能力类似)
        for (SuitSet ss : SuitSet.REGISTRY) {
            if (ss instanceof JsonSuitSet) {
                JsonObject json = newSuitSets.get(ss.getRegistryName());
                try {
                    ((JsonSuitSet) ss).deserialize(json != null ? json : new JsonObject(), ss.getRegistryName());
                    if (json != null) {
                        ((JsonSuitSet) ss).jsonOriginal = json;
                    }
                } catch (Exception e) {
                    String errorMsg = "Failed to reload suit set '" + ss.getRegistryName() + "'. It may have been soft-deleted. Reason: " + e.getMessage();
                    LucraftCore.LOGGER.error(errorMsg);
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + errorMsg));
                }
            }
        }

        // --- 3. 同步所有更新到客户端 ---
        LucraftCore.LOGGER.info("[LucraftCoreFix] Syncing reloaded data to all players...");
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            for (Superpower sp : SuperpowerHandler.SUPERPOWER_REGISTRY) {
                if (sp instanceof JsonSuperpower) {
                    LCPacketDispatcher.sendTo(new MessageSyncJsonSuperpower((JsonSuperpower) sp, ((JsonSuperpower) sp).jsonOriginal), player);
                }
            }
            for (SuitSet ss : SuitSet.REGISTRY) {
                if (ss instanceof JsonSuitSet) {
                    LCPacketDispatcher.sendTo(new MessageSyncJsonSuitSet((JsonSuitSet) ss, ((JsonSuitSet) ss).jsonOriginal), player);
                }
            }
        }

        // --- 4. 强制刷新所有在线玩家的能力容器 ---
        LucraftCore.LOGGER.info("[LucraftCoreFix] Refreshing abilities for all online players...");
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            Superpower currentSuperpower = SuperpowerHandler.getSuperpower(player);
            if (currentSuperpower != null) {
                AbilityContainer container = SuperpowerHandler.getSuperpowerCapability(player).getAbilityContainer(Ability.EnumAbilityContext.SUPERPOWER);
                container.switchProvider(currentSuperpower);
            }

            SuitSet currentSuitSet = SuitSet.getSuitSet(player);
            if (currentSuitSet != null) {
                AbilityContainer container = SuperpowerHandler.getSuperpowerCapability(player).getAbilityContainer(Ability.EnumAbilityContext.SUIT);
                container.switchProvider(currentSuitSet);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Reload complete! Took " + duration + "ms."));
        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Note: Resource changes (textures, models) require a manual resource pack reload (F3+T)."));
    }

    private static void scanAllAddons(Map<ResourceLocation, JsonObject> superpowerMap, Map<ResourceLocation, JsonObject> suitsetMap, ICommandSender sender, AtomicInteger failedCounter) {
        File addonDir = AddonPackHandler.getAddonPacksDir();
        if (addonDir.exists() && addonDir.isDirectory()) {
            scanDirectory(addonDir, superpowerMap, suitsetMap, sender, failedCounter);
        }
    }

    private static void scanDirectory(File directory, Map<ResourceLocation, JsonObject> superpowerMap, Map<ResourceLocation, JsonObject> suitsetMap, ICommandSender sender, AtomicInteger failedCounter) {
        if (directory.listFiles() == null) return;
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file, superpowerMap, suitsetMap, sender, failedCounter);
            } else {
                parseFile(file, directory, superpowerMap, suitsetMap, sender, failedCounter);
            }
        }
    }

    private static void parseFile(File file, File packDir, Map<ResourceLocation, JsonObject> superpowerMap, Map<ResourceLocation, JsonObject> suitsetMap, ICommandSender sender, AtomicInteger failedCounter) {
        String relativePath = file.getAbsolutePath().substring(packDir.getAbsolutePath().length() + 1).replace("\\", "/");
        String[] parts = relativePath.split("/");

        if (parts.length >= 3 && parts[0].equals("assets")) {
            String domain = parts[1];
            String type = parts[2];

            // 构建不包含子文件夹的路径
            String fileNamePath = "";
            for (int i = 3; i < parts.length; i++) {
                fileNamePath += (i == 3 ? "" : "/") + parts[i];
            }
            fileNamePath = FilenameUtils.removeExtension(fileNamePath);

            ResourceLocation loc = new ResourceLocation(domain, fileNamePath);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                if (type.equals("superpowers")) {
                    superpowerMap.put(loc, json);
                } else if (type.equals("suitsets")) {
                    suitsetMap.put(loc, json);
                }
            } catch (JsonSyntaxException e) {
                String errorMsg = "Syntax error in " + relativePath + ": " + e.getLocalizedMessage();
                LucraftCore.LOGGER.error("[LucraftCoreFix] " + errorMsg);
                sender.sendMessage(new TextComponentString(TextFormatting.RED + errorMsg));
                failedCounter.incrementAndGet();
            }
            catch (Exception e) {
                String errorMsg = "Failed to read file " + relativePath + ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
                LucraftCore.LOGGER.error("[LucraftCoreFix] " + errorMsg);
                sender.sendMessage(new TextComponentString(TextFormatting.RED + errorMsg));
                failedCounter.incrementAndGet();
            }
        }
    }
}
// ===== END ReloadHandler.java (FINAL, WITH ERROR HANDLING) =====