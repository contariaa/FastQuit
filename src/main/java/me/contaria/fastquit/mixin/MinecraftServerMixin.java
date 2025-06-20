package me.contaria.fastquit.mixin;

import me.contaria.fastquit.FastQuit;
import me.contaria.fastquit.FastQuitConfig;
import me.contaria.fastquit.TextHelper;
import me.contaria.fastquit.WorldInfo;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.MutableText;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(
            method = "exit",
            at = @At("RETURN")
    )
    private void fastquit$finishSaving(CallbackInfo ci) {
        //noinspection ConstantConditions
        if ((Object) this instanceof IntegratedServer server) {
            WorldInfo info = FastQuit.savingWorlds.remove(server);

            if (info == null) {
                FastQuit.warn("\"" + server.getSaveProperties().getLevelName() + "\" was not registered in currently saving worlds!");
                return;
            }

            MutableText description = TextHelper.translatable("fastquit.toast." + (info.deleted ? "deleted" : "description"), server.getSaveProperties().getLevelName());
            if (FastQuit.CONFIG.showSavingTime != FastQuitConfig.ShowSavingTime.FALSE && !info.deleted) {
                description.append(" (" + info.getTimeSaving() + ")");
            }
            if (FastQuit.CONFIG.showToasts) {
                MinecraftClient.getInstance().submit(() -> MinecraftClient.getInstance().getToastManager().add(new SystemToast(SystemToast.Type.WORLD_BACKUP, TextHelper.translatable("fastquit.toast.title"), description)));
            }
            FastQuit.log(description.getString());
        }
    }

    @WrapWithCondition(
            method = "shutdown",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;saveAllPlayerData()V"
            )
    )
    private boolean fastquit$cancelPlayerSavingIfDeleted(PlayerManager playerManager) {
        if (this.isDeleted()) {
            LOGGER.info("Cancelled saving players because level was deleted");
            return false;
        }
        return true;
    }

    @Inject(
            method = "save",
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Ljava/util/Iterator;next()Ljava/lang/Object;"
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/storage/LevelStorage$Session;backupLevelDataFile(Lnet/minecraft/registry/DynamicRegistryManager;Lnet/minecraft/world/SaveProperties;Lnet/minecraft/nbt/NbtCompound;)V"
                    )
            },
            cancellable = true
    )
    private void fastquit$cancelSavingIfDeleted(CallbackInfoReturnable<Boolean> cir) {
        if (this.isDeleted()) {
            LOGGER.info("Cancelled saving worlds because level was deleted");
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean isDeleted() {
        WorldInfo info = FastQuit.savingWorlds.get(this);
        return info != null && info.deleted;
    }
}