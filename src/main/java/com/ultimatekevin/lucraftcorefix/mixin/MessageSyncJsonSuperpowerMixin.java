package com.ultimatekevin.lucraftcorefix.mixin;

import com.google.gson.JsonParser;
import com.ultimatekevin.lucraftcorefix.LucraftCoreFix;
import com.ultimatekevin.lucraftcorefix.util.CompressionUtil;
import io.netty.buffer.ByteBuf;
import lucraft.mods.lucraftcore.superpowers.JsonSuperpower;
import lucraft.mods.lucraftcore.superpowers.network.MessageSyncJsonSuperpower;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;

@Mixin(value = MessageSyncJsonSuperpower.class, remap = false)
public abstract class MessageSyncJsonSuperpowerMixin {

    @Shadow public JsonSuperpower superpower;

    @Inject(method = "toBytes", at = @At("HEAD"), cancellable = true)
    private void toBytesCompressed(ByteBuf buf, CallbackInfo ci) {
        try {
            // 写入超能力注册名
            ByteBufUtils.writeRegistryEntry(buf, this.superpower);

            // 压缩 JSON 字符串并写入
            byte[] compressedJson = CompressionUtil.compress(this.superpower.jsonOriginal.toString().getBytes(StandardCharsets.UTF_8));
            buf.writeInt(compressedJson.length);
            buf.writeBytes(compressedJson);

            // 取消原方法的执行
            ci.cancel();
        } catch (Exception e) {
            LucraftCoreFix.LOGGER.error("Failed to compress superpower JSON packet for: " + this.superpower.getRegistryName(), e);
        }
    }

    @Inject(method = "fromBytes", at = @At("HEAD"), cancellable = true)
    private void fromBytesCompressed(ByteBuf buf, CallbackInfo ci) {
        try {
            // 读取超能力
            this.superpower = (JsonSuperpower) ByteBufUtils.readRegistryEntry(buf, lucraft.mods.lucraftcore.superpowers.SuperpowerHandler.SUPERPOWER_REGISTRY);

            // 读取并解压 JSON
            int length = buf.readInt();
            byte[] compressedJson = new byte[length];
            buf.readBytes(compressedJson);
            String jsonString = new String(CompressionUtil.decompress(compressedJson), StandardCharsets.UTF_8);
            this.superpower.jsonOriginal = new JsonParser().parse(jsonString).getAsJsonObject();

            // 取消原方法的执行
            ci.cancel();
        } catch (Exception e) {
            LucraftCoreFix.LOGGER.error("Failed to decompress superpower JSON packet.", e);
        }
    }
}