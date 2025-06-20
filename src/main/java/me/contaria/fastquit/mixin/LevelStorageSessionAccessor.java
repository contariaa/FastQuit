package me.contaria.fastquit.mixin;

import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.SessionLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelStorage.Session.class)
public interface LevelStorageSessionAccessor {
    @Accessor("lock")
    SessionLock fastquit$getLock();

    @Accessor("directory")
    LevelStorage.LevelSave fastquit$getDirectory();
}