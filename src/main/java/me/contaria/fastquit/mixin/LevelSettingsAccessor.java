package me.contaria.fastquit.mixin;

import net.minecraft.world.level.LevelSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelSettings.class)
public interface LevelSettingsAccessor {
    @Mutable
    @Accessor("levelName")
    void fastquit$setLevelName(String name);
}