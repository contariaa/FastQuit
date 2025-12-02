package me.contaria.fastquit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.fastquit.FastQuit;
import me.contaria.fastquit.WorldInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    private boolean integratedServerRunning;

    @Redirect(
            method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/integrated/IntegratedServer;isStopping()Z"
            )
    )
    private boolean fastquit(IntegratedServer server) {
        FastQuit.savingWorlds.put(server, new WorldInfo());

        if (FastQuit.CONFIG.backgroundPriority != 0) {
            server.getThread().setPriority(FastQuit.CONFIG.backgroundPriority);
        }

        FastQuit.log("Disconnected \"" + server.getSaveProperties().getLevelName() + "\" from the client.");
        return true;
    }

    @WrapOperation(
            method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;setScreenAndRender(Lnet/minecraft/client/gui/screen/Screen;)V"
            )
    )
    private void fastquit$doNotOpenSaveScreen(MinecraftClient client, Screen screen, Operation<Void> original) {
        if (FastQuit.CONFIG.renderSavingScreen && this.integratedServerRunning) {
            original.call(client, screen);
        } else {
            client.setScreen(screen);
        }
    }

    @Inject(
            method = "stop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;disconnectWithProgressScreen()V",
                    shift = At.Shift.AFTER
            )
    )
    private void fastquit$waitForSaveOnShutdown(CallbackInfo ci) {
        FastQuit.exit();
    }

    @Inject(
            method = "printCrashReport(Lnet/minecraft/client/MinecraftClient;Ljava/io/File;Lnet/minecraft/util/crash/CrashReport;)V",
            at = @At("HEAD")
    )
    private static void fastquit$waitForSaveOnCrash(CallbackInfo ci) {
        FastQuit.exit();
    }
}