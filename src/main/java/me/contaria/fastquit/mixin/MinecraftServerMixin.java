package me.contaria.fastquit.mixin;

import me.contaria.fastquit.FastQuit;
import me.contaria.fastquit.FastQuitConfig;
import me.contaria.fastquit.TextHelper;
import me.contaria.fastquit.WorldInfo;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
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
            method = "onServerExit",
            at = @At("RETURN")
    )
    private void fastquit$finishSaving(CallbackInfo ci) {
        //noinspection ConstantConditions
        if ((Object) this instanceof IntegratedServer server) {
            WorldInfo info = FastQuit.savingWorlds.remove(server);

            if (info == null) {
                FastQuit.warn("\"" + server.getWorldData().getLevelName() + "\" was not registered in currently saving worlds!");
                return;
            }

            MutableComponent description = TextHelper.translatable("fastquit.toast." + (info.deleted ? "deleted" : "description"), server.getWorldData().getLevelName());
            if (FastQuit.CONFIG.showSavingTime != FastQuitConfig.ShowSavingTime.FALSE && !info.deleted) {
                description.append(" (" + info.getTimeSaving() + ")");
            }
            if (FastQuit.CONFIG.showToasts) {
                Minecraft.getInstance().submit(() -> Minecraft.getInstance().getToastManager().addToast(new SystemToast(SystemToast.SystemToastId.WORLD_BACKUP, TextHelper.translatable("fastquit.toast.title"), description)));
            }
            FastQuit.log(description.getString());
        }
    }

    @WrapWithCondition(
            method = "stopServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;saveAll()V"
            )
    )
    private boolean fastquit$cancelPlayerSavingIfDeleted(PlayerList playerManager) {
        if (this.isDeleted()) {
            LOGGER.info("Cancelled saving players because level was deleted");
            return false;
        }
        return true;
    }

    @Inject(
            method = "saveAllChunks",
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Ljava/util/Iterator;next()Ljava/lang/Object;"
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;saveDataTag(Lnet/minecraft/world/level/storage/WorldData;Ljava/util/UUID;)V"
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