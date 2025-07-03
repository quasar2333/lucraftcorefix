package com.ultimatekevin.lucraftcorefix.mixin;

import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.superpowers.abilities.supplier.AbilityContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbilityContainer.class, remap = false)
public interface AbilityContainerMixin {

    /**
     * 这个 @Accessor 注解会自动在运行时生成一个公共的 getter 方法
     * 让我们能够安全地获取到 protected 的 'abilities' 字段。
     */
    @Accessor
    Ability.AbilityMap getAbilities();

    /**
     * 这个 @Accessor 注解会自动生成一个公共的 setter 方法
     * 让我们能够安全地修改 protected 的 'abilities' 字段。
     */
    @Accessor
    void setAbilities(Ability.AbilityMap abilities);

}