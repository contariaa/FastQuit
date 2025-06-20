package me.contaria.fastquit;

import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Utility class for ease of porting to older Minecraft versions.
 */
public final class TextHelper {

    public static final Text OFF = ScreenTexts.OFF;
    public static final Text BACK = ScreenTexts.BACK;

    public static MutableText translatable(String key, Object... args) {
        return Text.translatable(key, args);
    }

    public static MutableText literal(String string) {
        return Text.literal(string);
    }
}