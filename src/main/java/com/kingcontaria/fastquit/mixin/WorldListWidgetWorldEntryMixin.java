package com.kingcontaria.fastquit.mixin;

import com.kingcontaria.fastquit.FastQuit;
import com.kingcontaria.fastquit.FastQuitConfig;
import com.kingcontaria.fastquit.WorldInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldListWidget.WorldEntry.class)
public abstract class WorldListWidgetWorldEntryMixin {

    @Shadow
    @Final
    private SelectWorldScreen screen;
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    LevelSummary level;

    @WrapOperation(
            method = {
                    "edit",
                    "recreate"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/LevelStorage;createSession(Ljava/lang/String;)Lnet/minecraft/world/level/storage/LevelStorage$Session;"
            ),
            require = 2
    )
    private LevelStorage.Session fastquit$editSavingWorld(LevelStorage storage, String directoryName, Operation<LevelStorage.Session> original) {
        return FastQuit.getSession(storage.getSavesDirectory().resolve(directoryName)).orElseGet(() -> original.call(storage, directoryName));
    }

    @WrapOperation(
            method = "delete",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/LevelStorage;createSessionWithoutSymlinkCheck(Ljava/lang/String;)Lnet/minecraft/world/level/storage/LevelStorage$Session;"
            )
    )
    private LevelStorage.Session fastquit$deleteSavingWorld(LevelStorage storage, String directoryName, Operation<LevelStorage.Session> original) {
        return FastQuit.getSession(storage.getSavesDirectory().resolve(directoryName)).orElseGet(() -> original.call(storage, directoryName));
    }

    // While this should not be needed anymore, I'll leave it in just in case something goes wrong.
    @Inject(
            method = "edit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/toast/SystemToast;addWorldAccessFailureToast(Lnet/minecraft/client/MinecraftClient;Ljava/lang/String;)V"
            )
    )
    private void fastquit$openWorldListWhenFailed(CallbackInfo ci) {
        this.client.setScreen(this.screen);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void fastquit$renderSavingTimeOnWorldList(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        if (FastQuit.CONFIG.showSavingTime == FastQuitConfig.ShowSavingTime.TRUE) {
            FastQuit.getSavingWorld(this.client.getLevelStorage().getSavesDirectory().resolve(this.level.getName())).ifPresent(server -> {
                WorldInfo info = FastQuit.savingWorlds.get(server);
                if (info != null) {
                    String time = info.getTimeSaving() + " ⌛";
                    context.drawText(this.client.textRenderer, time, x + entryWidth - this.client.textRenderer.getWidth(time) - 4, y + 1, -6939106, false);
                }
            });
        }
    }
}