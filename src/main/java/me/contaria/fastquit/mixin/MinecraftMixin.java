package me.contaria.fastquit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.fastquit.FastQuit;
import me.contaria.fastquit.WorldInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    private boolean isLocalServer;

    @Redirect(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/server/IntegratedServer;isShutdown()Z"
            )
    )
    private boolean fastquit(IntegratedServer server) {
        FastQuit.savingWorlds.put(server, new WorldInfo());

        if (FastQuit.CONFIG.backgroundPriority != 0) {
            server.getRunningThread().setPriority(FastQuit.CONFIG.backgroundPriority);
        }

        FastQuit.log("Disconnected \"" + server.getWorldData().getLevelName() + "\" from the client.");
        return true;
    }

    @WrapOperation(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;setScreenAndShow(Lnet/minecraft/client/gui/screens/Screen;)V"
            )
    )
    private void fastquit$doNotOpenSaveScreen(Minecraft client, Screen screen, Operation<Void> original) {
        if (FastQuit.CONFIG.renderSavingScreen && this.isLocalServer) {
            original.call(client, screen);
        } else {
            client.setScreen(screen);
        }
    }

    @Inject(
            method = "destroy",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;disconnectWithProgressScreen()V",
                    shift = At.Shift.AFTER
            )
    )
    private void fastquit$waitForSaveOnShutdown(CallbackInfo ci) {
        FastQuit.exit();
    }

    @Inject(
            method = "crash(Lnet/minecraft/client/Minecraft;Ljava/io/File;Lnet/minecraft/CrashReport;)V",
            at = @At("HEAD")
    )
    private static void fastquit$waitForSaveOnCrash(CallbackInfo ci) {
        FastQuit.exit();
    }
}