package me.contaria.fastquit.mixin;

import me.contaria.fastquit.FastQuit;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {

    @Shadow
    @Final
    private LevelStorageSource levelSource;

    @Inject(
            method = "openWorld(Ljava/lang/String;Ljava/lang/Runnable;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void fastquit$waitForSaveOnWorldLoad_cancellable(String levelName, Runnable onCancel, CallbackInfo ci) {
        FastQuit.getSavingWorld(this.levelSource.getBaseDir().resolve(levelName)).ifPresent(server -> FastQuit.wait(server, ci));
        if (ci.isCancelled()) {
            onCancel.run();
        }
    }
}