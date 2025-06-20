package me.contaria.fastquit.mixin;

import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelProperties.class)
public interface LevelPropertiesAccessor {
    @Accessor("levelInfo")
    LevelInfo fastquit$getLevelInfo();
}