package me.contaria.fastquit.mixin;

import me.contaria.fastquit.FastQuit;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.OptimizeWorldScreen;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptimizeWorldScreen.class)
public abstract class OptimizeWorldScreenMixin {

    // this now acts as a fallback in case the method gets called from somewhere else than EditWorldScreen
    @Inject(
            method = "create",
            at = @At("HEAD")
    )
    private static void fastquit$waitForSaveOnOptimizeWorld(MinecraftClient client, BooleanConsumer callback, DataFixer dataFixer, LevelStorage.Session session, boolean eraseCache, CallbackInfoReturnable<OptimizeWorldScreen> cir) {
        FastQuit.getSavingWorld(session).ifPresent(FastQuit::wait);
    }
}