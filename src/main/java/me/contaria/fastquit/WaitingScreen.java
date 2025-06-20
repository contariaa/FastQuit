package me.contaria.fastquit;

import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class WaitingScreen extends MessageScreen {

    private final CallbackInfo callbackInfo;

    public WaitingScreen(Text text, @Nullable CallbackInfo callbackInfo) {
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
            this.addDrawableChild(ButtonWidget.builder(TextHelper.BACK, button -> this.close()).dimensions(this.width - 100 - 5, this.height - 20 - 5, 100, 20).build());
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.callbackInfo != null;
    }

    @Override
    public void close() {
        super.close();
        if (this.callbackInfo != null) {
            this.callbackInfo.cancel();
        }
    }
}