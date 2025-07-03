package com.ultimatekevin.lucraftcorefix.mixin;

import com.google.gson.JsonParser;
import com.ultimatekevin.lucraftcorefix.LucraftCoreFix;
import com.ultimatekevin.lucraftcorefix.util.CompressionUtil;
import io.netty.buffer.ByteBuf;
import lucraft.mods.lucraftcore.superpowers.network.MessageSyncJsonSuitSet;
import lucraft.mods.lucraftcore.superpowers.suitsets.JsonSuitSet;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;

@Mixin(value = MessageSyncJsonSuitSet.class, remap = false)
public abstract class MessageSyncJsonSuitSetMixin {

    @Shadow public JsonSuitSet suitset;

    @Inject(method = "toBytes", at = @At("HEAD"), cancellable = true)
    private void toBytesCompressed(ByteBuf buf, CallbackInfo ci) {
        try {
            // 写入套装注册名
            ByteBufUtils.writeUTF8String(buf, this.suitset.loc.toString());

            // 压缩 JSON 字符串并写入
            byte[] compressedJson = CompressionUtil.compress(this.suitset.jsonOriginal.toString().getBytes(StandardCharsets.UTF_8));
            buf.writeInt(compressedJson.length);
            buf.writeBytes(compressedJson);

            // 取消原方法的执行
            ci.cancel();
        } catch (Exception e) {
            LucraftCoreFix.LOGGER.error("Failed to compress suit set JSON packet for: " + this.suitset.getRegistryName(), e);
        }
    }

    @Inject(method = "fromBytes", at = @At("HEAD"), cancellable = true)
    private void fromBytesCompressed(ByteBuf buf, CallbackInfo ci) {
        try {
            // 读取套装（注意：原版实现有潜在问题，这里我们直接读取注册名，后续在 handler 中处理查找）
            String suitSetLoc = ByteBufUtils.readUTF8String(buf);

            // 读取并解压 JSON
            int length = buf.readInt();
            byte[] compressedJson = new byte[length];
            buf.readBytes(compressedJson);
            String jsonString = new String(CompressionUtil.decompress(compressedJson), StandardCharsets.UTF_8);

            // 由于无法直接设置 suitset 对象，我们在这里先将 JSON 存起来，原版的 handler 会处理它
            // 我们需要确保原版的 handler 能找到 suitset 对象，因此这里只注入 toBytes
            // fromBytes 的注入逻辑比较复杂，因为它依赖于一个静态列表，而我们的 Mixin 无法直接修改它。
            // 但幸运的是，崩溃只发生在 toBytes 阶段。
            // 为了完整性，我们还是模拟一下，但请注意，如果 LucraftCore 加载顺序变化，这里可能需要调整。
            for (JsonSuitSet jss : lucraft.mods.lucraftcore.superpowers.suitsets.AddonPackSuitSetReader.SUIT_SETS) {
                if (jss.loc.toString().equals(suitSetLoc)) {
                    this.suitset = jss;
                    break;
                }
            }
            if (this.suitset != null) {
                this.suitset.jsonOriginal = new JsonParser().parse(jsonString).getAsJsonObject();
            }

            // 取消原方法的执行
            ci.cancel();
        } catch (Exception e) {
            LucraftCoreFix.LOGGER.error("Failed to decompress suit set JSON packet.", e);
        }
    }
}