package me.contaria.fastquit.mixin;

import me.contaria.fastquit.FastQuit;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(LevelStorage.class)
public abstract class LevelStorageMixin {

    @Shadow
    @Final
    private Path savesDirectory;

    @Inject(
            method = "createSession",
            at = @At("HEAD")
    )
    private void fastquit$waitForSaveOnSessionCreation(String levelName, CallbackInfoReturnable<LevelStorage.Session> cir) {
        if (!FastQuit.CONFIG.allowMultipleServers()) {
            FastQuit.wait(FastQuit.savingWorlds.keySet());
        }
        FastQuit.getSavingWorld(this.savesDirectory.resolve(levelName)).ifPresent(FastQuit::wait);
        if (!FastQuit.savingWorlds.isEmpty()) {
            FastQuit.warn(String.join(" ",
                    "FastQuit is allowing a world to load while another is currently being saved, which may cause problems with mod compatibility.",
                    "Try disabling \"Allow multiple running worlds\" in FastQuit settings if you are experiencing issues."
            ));
        }
    }

    // method_43418 - lambda in loadSummaries
    @Inject(
            method = "method_43418",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"
            ),
            cancellable = true,
            remap = false
    )
    private void fastquit$addCurrentlySavingLevelsToWorldList(LevelStorage.LevelSave levelSave, CallbackInfoReturnable<LevelSummary> cir) {
        FastQuit.getSession(levelSave.path()).ifPresent(session -> {
            try (session) {
                cir.setReturnValue(session.getLevelSummary(session.readLevelProperties()));
            } catch (Exception e) {
                FastQuit.error("Failed to load level summary from saving server!", e);
            }
        });
    }
}