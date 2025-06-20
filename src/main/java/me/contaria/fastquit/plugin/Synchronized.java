package me.contaria.fastquit.plugin;

import org.spongepowered.asm.mixin.Shadow;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Used to annotate {@link Shadow}'d methods to make them synchronized in {@link FastQuitMixinConfigPlugin#postApply}.
 */
@Target(ElementType.METHOD)
public @interface Synchronized {
}