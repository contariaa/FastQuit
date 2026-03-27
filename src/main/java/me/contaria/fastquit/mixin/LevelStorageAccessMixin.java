package me.contaria.fastquit.mixin;

import me.contaria.fastquit.FastQuit;
import me.contaria.fastquit.plugin.Synchronized;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.serialization.Dynamic;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.DirectoryLock;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.WorldData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public abstract class LevelStorageAccessMixin {

    @Shadow
    @Final
    private String levelId;

    @Synchronized
    @Shadow
    public abstract PlayerDataStorage createPlayerStorage();

    @Synchronized
    @Shadow
    public abstract @Nullable LevelSummary fixAndGetSummaryFromTag(Dynamic<?> dynamic);

    @Synchronized
    @Shadow
    public abstract @Nullable Dynamic<?> getUnfixedDataTag(boolean old);

    @Synchronized
    @Shadow
    public abstract void saveDataTag(WorldData saveProperties, @Nullable UUID nbt);

    @Synchronized
    @Shadow
    protected abstract void saveLevelData(CompoundTag compound) throws IOException;

    @Synchronized
    @Shadow
    public abstract long makeWorldBackup() throws IOException;

    @Synchronized
    @Shadow
    public abstract void deleteLevel() throws IOException;

    @Synchronized
    @Shadow
    protected abstract void modifyLevelDataWithoutDatafix(Consumer<CompoundTag> nbtProcessor) throws IOException;

    @Synchronized
    @Shadow
    public abstract boolean restoreLevelDataFromOld();

    @Synchronized
    @Shadow
    public abstract void close() throws IOException;

    // this now acts as a fallback in case the method gets called from somewhere else than EditWorldScreen
    @Inject(
            method = "makeWorldBackup",
            at = @At("HEAD")
    )
    private void fastquit$waitForSaveOnBackup(CallbackInfoReturnable<Long> cir) {
        this.getSavingWorld().ifPresent(FastQuit::wait);
    }

    @Inject(
            method = "renameLevel(Ljava/lang/String;)V",
            at = @At("TAIL")
    )
    private void fastquit$editSavingWorldName(String name, CallbackInfo ci) {
        this.getSavingWorld().ifPresent(server -> ((LevelSettingsAccessor) (Object) ((PrimaryLevelDataAccessor) server.getWorldData()).fastquit$getSettings()).fastquit$setLevelName(name));
    }

    @Inject(
            method = "deleteLevel",
            at = @At("TAIL")
    )
    private void fastquit$deleteSavingWorld(CallbackInfo ci) {
        this.getSavingWorld().map(FastQuit.savingWorlds::get).ifPresent(info -> info.deleted = true);
    }

    @WrapWithCondition(
            method = "close",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/DirectoryLock;close()V"
            )
    )
    private boolean fastquit$checkSessionClose(DirectoryLock lock) {
        return !FastQuit.occupiedSessions.remove((LevelStorageSource.LevelStorageAccess) (Object) this);
    }

    @Inject(
            method = "checkLock",
            at = @At("HEAD")
    )
    private void fastquit$warnIfUnSynchronizedSessionAccess(CallbackInfo ci) {
        if (!Thread.holdsLock(this)) {
            this.getSavingWorld().ifPresent(server -> {
                FastQuit.warn("Un-synchronized access to \"" + this.levelId + "\" session!");
                if (!server.isSameThread()) {
                    FastQuit.wait(server);
                }
            });
        }
    }
    
    @Unique
    private Optional<IntegratedServer> getSavingWorld() {
        return FastQuit.getSavingWorld((LevelStorageSource.LevelStorageAccess) (Object) this);
    }
}