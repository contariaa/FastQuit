package me.contaria.fastquit;

import me.contaria.fastquit.mixin.MinecraftAccessor;
import me.contaria.fastquit.mixin.MinecraftServerAccessor;
import me.contaria.fastquit.mixin.LevelStorageSessionAccessor;
import com.mojang.logging.LogUtils;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.*;

public final class FastQuit implements ClientModInitializer {

    public static final ModMetadata FASTQUIT = FabricLoader.getInstance().getModContainer("fastquit").orElseThrow().getMetadata();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_PREFIX = "[" + FASTQUIT.getName() + "] ";
    public static final FastQuitConfig CONFIG = AutoConfig.register(FastQuitConfig.class, Toml4jConfigSerializer::new).getConfig();

    public static final boolean HAS_WORLDPLAYTIME = FabricLoader.getInstance().isModLoaded("worldplaytime");

    /**
     * Synchronized {@link Map} containing all currently saving {@link IntegratedServer}'s, with a {@link WorldInfo} with more information about the world.
     */
    public static final Map<IntegratedServer, WorldInfo> savingWorlds = Collections.synchronizedMap(new HashMap<>());

    /**
     * Stores {@link LevelStorageSource.LevelStorageAccess}'s used by FastQuit as to only close them if no other process is currently using them.
     */
    public static final List<LevelStorageSource.LevelStorageAccess> occupiedSessions = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onInitializeClient() {
        log("Initialized");
    }

    /**
     * Logs the given message.
     */
    public static void log(String msg) {
        LOGGER.info(LOG_PREFIX + msg);
    }

    /**
     * Logs the given warning.
     */
    public static void warn(String msg) {
        LOGGER.warn(LOG_PREFIX + msg);
    }

    /**
     * Logs the given message and error.
     */
    public static void error(String msg, Throwable throwable) {
        LOGGER.error(LOG_PREFIX + msg, throwable);
    }

    /**
     * Waits for all {@link IntegratedServer}'s to finish saving, gets called when Minecraft is closed.
     *
     * @implNote Catches everything to avoid any issues in the areas where it's called.
     */
    public static void exit() {
        try {
            log("Exiting FastQuit.");
            wait(savingWorlds.keySet());
        } catch (Throwable throwable) {
            error("Something went horribly wrong when exiting FastQuit!", throwable);
            savingWorlds.forEach((server, info) -> {
                try {
                    server.getRunningThread().join();
                } catch (Throwable throwable2) {
                    error("Failed to wait for \"" + server.getWorldData().getLevelName() + "\"", throwable2);
                }
            });
        }
    }

    /**
     * @see #wait(Collection, CallbackInfo)
     */
    public static void wait(IntegratedServer server) {
        wait(Collections.singleton(server), null);
    }

    /**
     * @see #wait(Collection, CallbackInfo)
     */
    public static void wait(IntegratedServer server, @Nullable CallbackInfo cancellable) {
        wait(Collections.singleton(server), cancellable);
    }

    /**
     * @see #wait(Collection, CallbackInfo)
     */
    public static void wait(Collection<IntegratedServer> servers) {
        wait(servers, null);
    }

    /**
     * Waits for all the {@link IntegratedServer}'s in the given {@link Collection} to finish saving and in the meantime renders a {@link WaitingScreen}.
     * If a {@link CallbackInfo} is given, the waiting can be cancelled by the user.
     *
     * @throws IllegalStateException if called on one of the given {@link IntegratedServer}'s threads, would cause a deadlock otherwise
     */
    public static void wait(Collection<IntegratedServer> servers, @Nullable CallbackInfo cancellable) {
        if (servers == null || servers.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        if (!client.isSameThread()) {
            if (servers.stream().anyMatch(server -> Thread.currentThread() == server.getRunningThread())) {
                throw new IllegalStateException("Tried to call FastQuit.wait(...) from one of the servers it's supposed to wait for.");
            }

            client.submit(() -> wait(servers)).join();
            return;
        }

        Screen oldScreen = client.screen;

        Component stillSaving = TextHelper.translatable("fastquit.screen.waiting", String.join("\" & \"", servers.stream().map(server -> server.getWorldData().getLevelName()).toList()));
        log(stillSaving.getString());

        servers.forEach(server -> server.getRunningThread().setPriority(Thread.NORM_PRIORITY));

        try {
            client.setScreen(new WaitingScreen(stillSaving, cancellable));

            while (servers.stream().anyMatch(server -> !server.isShutdown())) {
                if (cancellable != null && cancellable.isCancelled()) {
                    if (CONFIG.backgroundPriority != 0) {
                        servers.forEach(server -> server.getRunningThread().setPriority(CONFIG.backgroundPriority));
                    }
                    log("Cancelled waiting for currently saving worlds.");
                    break;
                }
                ((MinecraftAccessor) client).fastquit$runTick(false);
            }
        } finally {
            // compatibility with "WorldGen" mod
            if (oldScreen != null && oldScreen.getClass().getName().equals("caeruleusTait.WorldGen.gui.screens.WGConfigScreen")) {
                client.screen = oldScreen;
            } else {
                client.setScreenAndShow(oldScreen);
            }
        }
    }

    /**
     * @return optionally returns the currently saving {@link IntegratedServer} matching the given {@link Path}
     */
    public static Optional<IntegratedServer> getSavingWorld(Path path) {
        // noinspection resource
        return savingWorlds.keySet().stream().filter(server -> ((LevelStorageSessionAccessor) ((MinecraftServerAccessor) server).fastquit$getStorageSource()).fastquit$getLevelDirectory().path().equals(path)).findFirst();
    }

    /**
     * @return optionally returns the currently saving {@link IntegratedServer} matching the given {@link LevelStorageSource.LevelStorageAccess}
     */
    public static Optional<IntegratedServer> getSavingWorld(LevelStorageSource.LevelStorageAccess session) {
        // noinspection resource
        return savingWorlds.keySet().stream().filter(server -> ((MinecraftServerAccessor) server).fastquit$getStorageSource() == session).findFirst();
    }

    /**
     * @apiNote Remember to {@link LevelStorageSource.LevelStorageAccess#close() close} the session after using it!
     * @return optionally returns the {@link LevelStorageSource.LevelStorageAccess} of the currently saving {@link IntegratedServer} matching the given {@link Path}
     */
    public static Optional<LevelStorageSource.LevelStorageAccess> getSession(Path path) {
        return getSavingWorld(path).flatMap(server -> {
            LevelStorageSource.LevelStorageAccess session;
            synchronized (session = ((MinecraftServerAccessor) server).fastquit$getStorageSource()) {
                // noinspection resource
                if (((LevelStorageSessionAccessor) session).fastquit$getLock().isValid()) {
                    occupiedSessions.add(session);
                    return Optional.of(session);
                }
            }
            return Optional.empty();
        });
    }
}