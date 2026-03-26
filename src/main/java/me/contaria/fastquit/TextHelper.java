package me.contaria.fastquit;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Utility class for ease of porting to older Minecraft versions.
 */
public final class TextHelper {

    public static final Component OFF = CommonComponents.OPTION_OFF;
    public static final Component BACK = CommonComponents.GUI_BACK;

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static MutableComponent literal(String string) {
        return Component.literal(string);
    }
}