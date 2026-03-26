package me.contaria.fastquit.mixin;

import me.contaria.fastquit.FastQuit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.realmsclient.client.worldupload.RealmsUploadWorldPacker;
import java.io.File;
import java.nio.file.Path;

@Mixin(RealmsUploadWorldPacker.class)
public abstract class RealmsUploadWorldPackerMixin {

    @Shadow @Final private Path directoryToPack;

    @Inject(
            method = "tarGzipArchive",
            at = @At("HEAD")
    )
    private void fastquit$waitForSaveOnRealmsUpload(CallbackInfoReturnable<File> cir) {
        FastQuit.getSavingWorld(directoryToPack).ifPresent(FastQuit::wait);
    }
}