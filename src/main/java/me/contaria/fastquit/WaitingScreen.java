package me.contaria.fastquit;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class WaitingScreen extends GenericMessageScreen {

    private final CallbackInfo callbackInfo;

    public WaitingScreen(Component text, @Nullable CallbackInfo callbackInfo) {
        super(text);
        if (callbackInfo != null && !callbackInfo.isCancellable()) {
            FastQuit.warn("Provided CallbackInfo for \"" + callbackInfo.getId() + "\" is not cancellable!");
            callbackInfo = null;
        }
        this.callbackInfo = callbackInfo;
    }

    @Override
    public void init() {
        super.init();
        if (this.callbackInfo != null) {
            this.addRenderableWidget(Button.builder(TextHelper.BACK, button -> this.onClose()).bounds(this.width - 100 - 5, this.height - 20 - 5, 100, 20).build());
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.callbackInfo != null;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (this.callbackInfo != null) {
            this.callbackInfo.cancel();
        }
    }
}