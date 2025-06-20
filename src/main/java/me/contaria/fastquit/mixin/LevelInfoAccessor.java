package me.contaria.fastquit.mixin;

import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelInfo.class)
public interface LevelInfoAccessor {
    @Mutable
    @Accessor("name")
    void fastquit$setName(String name);
}