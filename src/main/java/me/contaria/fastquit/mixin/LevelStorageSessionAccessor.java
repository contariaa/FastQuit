package me.contaria.fastquit.mixin;

import net.minecraft.util.DirectoryLock;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public interface LevelStorageSessionAccessor {
    @Accessor("lock")
    DirectoryLock fastquit$getLock();

    @Accessor("levelDirectory")
    LevelStorageSource.LevelDirectory fastquit$getLevelDirectory();
}