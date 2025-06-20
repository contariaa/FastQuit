package me.contaria.fastquit;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@Config(name = "fastquit")
public class FastQuitConfig implements ConfigData {

    /**
     * Determines whether the "Saving world" screen gets rendered.
     */
    @ConfigEntry.Gui.Tooltip
    public boolean renderSavingScreen = false;

    /**
     * Determines whether a toast gets shown when a world finishes saving.
     */
    @ConfigEntry.Gui.Tooltip
    public boolean showToasts = true;

    /**
     * Determines whether the time it took to save the world gets displayed on toasts and the world list.
     */
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public ShowSavingTime showSavingTime = ShowSavingTime.TRUE;

    /**
     * Determines the Thread priority used for {@link IntegratedServer}'s saving in the background.
     * Value needs to be between 0 and 10, with Thread priority staying unchanged if the value is 0.
     */
    @ConfigEntry.Category("performance")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = Thread.MAX_PRIORITY)
    public int backgroundPriority = 2;

    /**
     * Determines whether multiple {@link IntegratedServer}'s can be running at the same time.
     * This is safe in vanilla minecraft but some mods can have issues with it.
     *
     * @apiNote Access through {@link FastQuitConfig#allowMultipleServers()} to avoid known mod conflicts!
     */
    @ConfigEntry.Category("compat")
    @ConfigEntry.Gui.Tooltip
    private boolean allowMultipleServers = true;

    /**
     * This {@link Set} holds the names of all currently active mods that conflict with {@link FastQuitConfig#allowMultipleServers}.
     * @see FastQuitConfig#allowMultipleServers()
     */
    @ConfigEntry.Gui.Excluded
    private static final Set<String> MODS_THAT_CONFLICT_WITH_MULTIPLE_SERVERS = new HashSet<>();

    static {
        // Put all conflicting Mod ID's in this set
        Set<String> incompatibleModIDs = Set.of("quilt_biome", "fastback");

        for (String modID : incompatibleModIDs) {
            FabricLoader.getInstance().getModContainer(modID).ifPresent(modContainer -> MODS_THAT_CONFLICT_WITH_MULTIPLE_SERVERS.add(modContainer.getMetadata().getName()));
        }
    }

    /**
     * @return Returns {@code false} when Quilt Biome API is loaded, returns {@link FastQuitConfig#allowMultipleServers} otherwise.
     */
    public boolean allowMultipleServers() {
        if (!MODS_THAT_CONFLICT_WITH_MULTIPLE_SERVERS.isEmpty()) {
            return false;
        }
        return this.allowMultipleServers;
    }

    /**
     * @return Returns a config screen built with Cloth Config API
     */
    public Screen createConfigScreen(Screen parent) {
        // if (true) return AutoConfig.getConfigScreen(FastQuitConfig.class, parent).get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(TextHelper.translatable("text.autoconfig.fastquit.title"))
                .setSavingRunnable(() -> AutoConfig.getConfigHolder(FastQuitConfig.class).save());

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // General Settings

        ConfigCategory generalCategory = builder.getOrCreateCategory(TextHelper.translatable("text.autoconfig.fastquit.category.default"));

        generalCategory.addEntry(entryBuilder.startBooleanToggle(TextHelper.translatable("text.autoconfig.fastquit.option.renderSavingScreen"), this.renderSavingScreen)
                .setTooltip(TextHelper.translatable("text.autoconfig.fastquit.option.renderSavingScreen.@Tooltip"))
                .setDefaultValue(false)
                .setSaveConsumer(renderSavingScreen -> this.renderSavingScreen = renderSavingScreen)
                .build()
        );

        generalCategory.addEntry(entryBuilder.startBooleanToggle(TextHelper.translatable("text.autoconfig.fastquit.option.showToasts"), this.showToasts)
                .setTooltip(TextHelper.translatable("text.autoconfig.fastquit.option.showToasts.@Tooltip"))
                .setDefaultValue(true)
                .setSaveConsumer(showToasts -> this.showToasts = showToasts)
                .build()
        );

        generalCategory.addEntry(entryBuilder.startEnumSelector(TextHelper.translatable("text.autoconfig.fastquit.option.showSavingTime"), FastQuitConfig.ShowSavingTime.class, this.showSavingTime)
                .setTooltip(TextHelper.translatable("text.autoconfig.fastquit.option.showSavingTime.@Tooltip"))
                .setDefaultValue(ShowSavingTime.TRUE)
                .setSaveConsumer(showSavingTime -> this.showSavingTime = showSavingTime)
                .build()
        );

        // Performance Settings

        ConfigCategory performanceCategory = builder.getOrCreateCategory(TextHelper.translatable("text.autoconfig.fastquit.category.performance"));

        performanceCategory.addEntry(entryBuilder.startIntSlider(TextHelper.translatable("text.autoconfig.fastquit.option.backgroundPriority"), this.backgroundPriority, 0, Thread.MAX_PRIORITY)
                .setTooltip(TextHelper.translatable("text.autoconfig.fastquit.option.backgroundPriority.@Tooltip"))
                .setTextGetter(backgroundPriority -> switch (backgroundPriority) {
                    case 0 -> TextHelper.OFF;
                    case 1, 2, 5, 10 -> TextHelper.translatable("fastquit.config.performance.backgroundPriority." + backgroundPriority);
                    default -> TextHelper.literal(backgroundPriority.toString());
                })
                .setDefaultValue(2)
                .setSaveConsumer(backgroundPriority -> this.backgroundPriority = backgroundPriority)
                .build()
        );

        // Mod Compatibility Settings

        ConfigCategory modCompatCategory = builder.getOrCreateCategory(TextHelper.translatable("text.autoconfig.fastquit.category.compat"));
/*
        modCompatCategory.addEntry(entryBuilder.startBooleanToggle(TextHelper.translatable("fastquit.config.compat.allowMultipleServers"), this.allowMultipleServers)
                .setTooltip(TextHelper.translatable("fastquit.config.compat.allowMultipleServers.description"))
                .setDefaultValue(true)
                .setSaveConsumer(allowMultipleServers -> this.allowMultipleServers = allowMultipleServers)
                .setRequirement(() -> !MODS_THAT_CONFLICT_WITH_MULTIPLE_SERVERS.isEmpty())
                .build()
        );
 */

        if (MODS_THAT_CONFLICT_WITH_MULTIPLE_SERVERS.isEmpty()) {
            modCompatCategory.addEntry(entryBuilder.startBooleanToggle(TextHelper.translatable("text.autoconfig.fastquit.option.allowMultipleServers"), this.allowMultipleServers)
                    .setTooltip(TextHelper.translatable("text.autoconfig.fastquit.option.allowMultipleServers.@Tooltip"))
                    .setDefaultValue(true)
                    .setSaveConsumer(allowMultipleServers -> this.allowMultipleServers = allowMultipleServers)
                    .build()
            );
        } else {
            modCompatCategory.addEntry(entryBuilder.startEnumSelector(TextHelper.translatable("text.autoconfig.fastquit.option.allowMultipleServers"), ModCompat.class, ModCompat.DISABLED)
                    .setTooltip(TextHelper.translatable("text.autoconfig.fastquit.option.allowMultipleServers.@Tooltip").append("\n\n").append(TextHelper.translatable("fastquit.config.compat.allowMultipleServers.disabledForCompat", String.join(", ", MODS_THAT_CONFLICT_WITH_MULTIPLE_SERVERS))))
                    .setEnumNameProvider(disabled -> TextHelper.translatable("addServer.resourcePack.disabled").styled(style -> style.withColor(Formatting.RED)))
                    .build()
            );
        }

        return builder.build();
    }

    public enum ShowSavingTime implements SelectionListEntry.Translatable {
        FALSE,
        TOAST_ONLY,
        TRUE;

        @Override
        public @NotNull String getKey() {
            if (this == ShowSavingTime.TOAST_ONLY) {
                return "fastquit.config.general.showSavingTime.toastsOnly";
            }
            return "text.cloth-config.boolean.value." + (this == ShowSavingTime.TRUE);
        }
    }
    
    private enum ModCompat {
        DISABLED
    }
}
