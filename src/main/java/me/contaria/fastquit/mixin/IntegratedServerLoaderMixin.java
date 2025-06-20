package me.contaria.fastquit.mixin;

import me.contaria.fastquit.FastQuit;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServerLoader.class)
public abstract class IntegratedServerLoaderMixin {

    @Shadow
    @Final
    private LevelStorage storage;

    @Inject(
            method = "start(Ljava/lang/String;Ljava/lang/Runnable;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void fastquit$waitForSaveOnWorldLoad_cancellable(String levelName, Runnable onCancel, CallbackInfo ci) {
        FastQuit.getSavingWorld(this.storage.getSavesDirectory().resolve(levelName)).ifPresent(server -> FastQuit.wait(server, ci));
        if (ci.isCancelled()) {
            onCancel.run();
        }
    }
}