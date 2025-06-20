package me.contaria.fastquit.mixin;

import me.contaria.fastquit.FastQuit;
import net.minecraft.client.realms.util.UploadCompressor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.nio.file.Path;

@Mixin(UploadCompressor.class)
public abstract class UploadCompressorMixin {

    @Shadow @Final private Path directory;

    @Inject(
            method = "run",
            at = @At("HEAD")
    )
    private void fastquit$waitForSaveOnRealmsUpload(CallbackInfoReturnable<File> cir) {
        FastQuit.getSavingWorld(directory).ifPresent(FastQuit::wait);
    }
}