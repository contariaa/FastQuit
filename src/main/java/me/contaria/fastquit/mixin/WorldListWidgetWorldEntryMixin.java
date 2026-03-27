package me.contaria.fastquit.mixin;

import me.contaria.fastquit.FastQuit;
import me.contaria.fastquit.FastQuitConfig;
import me.contaria.fastquit.WorldInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListWidgetWorldEntryMixin extends WorldSelectionList.Entry {
    @Shadow
    @Final
    private Screen screen;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    @Final
    LevelSummary summary;

    @WrapOperation(
            method = {
                    "editWorld",
                    "recreateWorld"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/LevelStorageSource;validateAndCreateAccess(Ljava/lang/String;)Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;"
            ),
            require = 2
    )
    private LevelStorageSource.LevelStorageAccess fastquit$editSavingWorld(LevelStorageSource storage, String directoryName, Operation<LevelStorageSource.LevelStorageAccess> original) {
        return FastQuit.getSession(storage.getBaseDir().resolve(directoryName)).orElseGet(() -> original.call(storage, directoryName));
    }

    @WrapOperation(
            method = "doDeleteWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/LevelStorageSource;createAccess(Ljava/lang/String;)Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;"
            )
    )
    private LevelStorageSource.LevelStorageAccess fastquit$deleteSavingWorld(LevelStorageSource storage, String directoryName, Operation<LevelStorageSource.LevelStorageAccess> original) {
        return FastQuit.getSession(storage.getBaseDir().resolve(directoryName)).orElseGet(() -> original.call(storage, directoryName));
    }

    // While this should not be needed anymore, I'll leave it in just in case something goes wrong.
    @Inject(
            method = "editWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/toasts/SystemToast;onWorldAccessFailure(Lnet/minecraft/client/Minecraft;Ljava/lang/String;)V"
            )
    )
    private void fastquit$openWorldListWhenFailed(CallbackInfo ci) {
        this.minecraft.setScreen(this.screen);
    }

    @Inject(
            method = "extractContent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/StringWidget;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void fastquit$renderSavingTimeOnWorldList(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks, CallbackInfo ci) {
        if (FastQuit.CONFIG.showSavingTime != FastQuitConfig.ShowSavingTime.TRUE || FastQuit.HAS_WORLDPLAYTIME) {
            return;
        }
        FastQuit.getSavingWorld(this.minecraft.getLevelSource().getBaseDir().resolve(this.summary.getLevelId())).ifPresent(server -> {
            WorldInfo info = FastQuit.savingWorlds.get(server);
            if (info != null) {
                String time = info.getTimeSaving() + " ⌛";
                context.text(this.minecraft.font, time, this.getX() + this.getWidth() - this.minecraft.font.width(time) - 4, this.getY() + 1, -6939106, false);
            }
        });
    }
}