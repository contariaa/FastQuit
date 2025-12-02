package me.contaria.fastquit.mixin;

import me.contaria.fastquit.FastQuit;
import me.contaria.fastquit.plugin.Synchronized;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.PlayerSaveHandler;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.SessionLock;
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
import java.util.function.Consumer;

@Mixin(LevelStorage.Session.class)
public abstract class LevelStorageSessionMixin {

    @Shadow
    @Final
    private String directoryName;

    @Synchronized
    @Shadow
    public abstract PlayerSaveHandler createSaveHandler();

    @Synchronized
    @Shadow
    public abstract @Nullable LevelSummary getLevelSummary(Dynamic<?> dynamic);

    @Synchronized
    @Shadow
    protected abstract @Nullable Dynamic<?> readLevelProperties(boolean old);

    @Synchronized
    @Shadow
    public abstract void backupLevelDataFile(DynamicRegistryManager registryManager, SaveProperties saveProperties, @Nullable NbtCompound nbt);

    @Synchronized
    @Shadow
    protected abstract void save(NbtCompound compound) throws IOException;

    @Synchronized
    @Shadow
    public abstract long createBackup() throws IOException;

    @Synchronized
    @Shadow
    public abstract void deleteSessionLock() throws IOException;

    @Synchronized
    @Shadow
    protected abstract void save(Consumer<NbtCompound> nbtProcessor) throws IOException;

    @Synchronized
    @Shadow
    public abstract boolean tryRestoreBackup();

    @Synchronized
    @Shadow
    public abstract void close() throws IOException;

    // this now acts as a fallback in case the method gets called from somewhere else than EditWorldScreen
    @Inject(
            method = "createBackup",
            at = @At("HEAD")
    )
    private void fastquit$waitForSaveOnBackup(CallbackInfoReturnable<Long> cir) {
        this.getSavingWorld().ifPresent(FastQuit::wait);
    }

    @Inject(
            method = "save(Ljava/lang/String;)V",
            at = @At("TAIL")
    )
    private void fastquit$editSavingWorldName(String name, CallbackInfo ci) {
        this.getSavingWorld().ifPresent(server -> ((LevelInfoAccessor) (Object) ((LevelPropertiesAccessor) server.getSaveProperties()).fastquit$getLevelInfo()).fastquit$setName(name));
    }

    @Inject(
            method = "deleteSessionLock",
            at = @At("TAIL")
    )
    private void fastquit$deleteSavingWorld(CallbackInfo ci) {
        this.getSavingWorld().map(FastQuit.savingWorlds::get).ifPresent(info -> info.deleted = true);
    }

    @WrapWithCondition(
            method = "close",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/SessionLock;close()V"
            )
    )
    private boolean fastquit$checkSessionClose(SessionLock lock) {
        return !FastQuit.occupiedSessions.remove((LevelStorage.Session) (Object) this);
    }

    @Inject(
            method = "checkValid",
            at = @At("HEAD")
    )
    private void fastquit$warnIfUnSynchronizedSessionAccess(CallbackInfo ci) {
        if (!Thread.holdsLock(this)) {
            this.getSavingWorld().ifPresent(server -> {
                FastQuit.warn("Un-synchronized access to \"" + this.directoryName + "\" session!");
                if (!server.isOnThread()) {
                    FastQuit.wait(server);
                }
            });
        }
    }
    
    @Unique
    private Optional<IntegratedServer> getSavingWorld() {
        return FastQuit.getSavingWorld((LevelStorage.Session) (Object) this);
    }
}