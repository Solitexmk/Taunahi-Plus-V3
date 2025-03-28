package net.taunahi.ezskyblockscripts;

import baritone.api.BaritoneAPI;
import cc.polyfrost.oneconfig.utils.Notifications;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.taunahi.ezskyblockscripts.command.TaunahiCommand;
import net.taunahi.ezskyblockscripts.command.RewarpCommand;
import net.taunahi.ezskyblockscripts.config.TaunahiConfig;
import net.taunahi.ezskyblockscripts.event.MillisecondEvent;
import net.taunahi.ezskyblockscripts.failsafe.FailsafeManager;
import net.taunahi.ezskyblockscripts.feature.FeatureManager;
import net.taunahi.ezskyblockscripts.feature.impl.MovRecPlayer;
import net.taunahi.ezskyblockscripts.handler.GameStateHandler;
import net.taunahi.ezskyblockscripts.handler.MacroHandler;
import net.taunahi.ezskyblockscripts.handler.RotationHandler;
import net.taunahi.ezskyblockscripts.remote.DiscordBotHandler;
import net.taunahi.ezskyblockscripts.remote.WebsocketHandler;
import net.taunahi.ezskyblockscripts.util.FailsafeUtils;
import net.taunahi.ezskyblockscripts.util.LogUtils;
import net.taunahi.ezskyblockscripts.util.ReflectionUtils;
import net.taunahi.ezskyblockscripts.util.helper.AudioManager;
import net.taunahi.ezskyblockscripts.util.helper.BaritoneEventListener;
import net.taunahi.ezskyblockscripts.util.helper.FlyPathfinder;
import net.taunahi.ezskyblockscripts.util.helper.TickTask;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(modid = "taunahiv2", useMetadata = true)
public class Taunahi {
    public static final String VERSION = "%%VERSION%%";
    public static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
    public static TaunahiConfig config;
    public static boolean sentInfoAboutShittyClient = false;
    public static boolean isDebug = false;
    private final Minecraft mc = Minecraft.getMinecraft();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        initializeFields();
        initializeListeners();
        initializeCommands();
        FeatureManager.getInstance().fillFeatures().forEach(MinecraftForge.EVENT_BUS::register);

        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.gammaSetting = 1000;
        isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
        Display.setTitle("Farm Helper 〔v" + VERSION + "〕 " + (!isDebug ? "Bing Chilling" : "wazzup dev?") + " ☛ " + Minecraft.getMinecraft().getSession().getUsername());
        FailsafeUtils.getInstance();

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(() -> MinecraftForge.EVENT_BUS.post(new MillisecondEvent()), 0, 1, TimeUnit.MILLISECONDS);
        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().registerEventListener(new BaritoneEventListener());
    }

    @SubscribeEvent
    public void onTickSendInfoAboutShittyClient(TickEvent.PlayerTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (sentInfoAboutShittyClient) return;

        if (ReflectionUtils.hasPackageInstalled("feather")) {
            Notifications.INSTANCE.send("Taunahi", "You've got Feather Client installed! Be aware, you might have a lot of bugs because of this shitty client!", 15000);
            LogUtils.sendError("You've got §6§lFeather Client §cinstalled! Be aware, you might have a lot of bugs because of this shitty client!");
        }
        if (ReflectionUtils.hasPackageInstalled("cc.woverflow.hytils.HytilsReborn")) {
            Notifications.INSTANCE.send("Taunahi", "You've got Hytils installed in your mods folder! This will cause many issues with rewarping as it sends tons of commands every minute.", 15000);
            LogUtils.sendError("You've got §6§lHytils §cinstalled in your mods folder! This will cause many issues with rewarping as it sends tons of commands every minute.");
        }
        if (Minecraft.isRunningOnMac && TaunahiConfig.autoUngrabMouse) {
            TaunahiConfig.autoUngrabMouse = false;
            Notifications.INSTANCE.send("Taunahi", "Auto Ungrab Mouse feature doesn't work properly on Mac OS. It has been disabled automatically.", 15000);
            LogUtils.sendError("Auto Ungrab Mouse feature doesn't work properly on Mac OS. It has been disabled automatically.");
        }
        if (TaunahiConfig.configVersion == 1 && TaunahiConfig.pestsKillerTicksOfNotSeeingPestWhileAttacking < 80) {
            TaunahiConfig.pestsKillerTicksOfNotSeeingPestWhileAttacking = 100;
            Notifications.INSTANCE.send("Taunahi", "Pests Killer Ticks Of Not Seeing Pest While Attacking has been set to 100 ticks because of a bug in the previous version.", 15000);
            LogUtils.sendWarning("Pests Killer Ticks Of Not Seeing Pest While Attacking has been set to 100 ticks because of a bug in the previous version.");
        }
        if (TaunahiConfig.configVersion == 1)
            TaunahiConfig.configVersion = 2;
        sentInfoAboutShittyClient = true;
    }

    private void initializeListeners() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(FailsafeManager.getInstance());
        MinecraftForge.EVENT_BUS.register(GameStateHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(MacroHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(TickTask.getInstance());
        MinecraftForge.EVENT_BUS.register(MovRecPlayer.getInstance());
        MinecraftForge.EVENT_BUS.register(WebsocketHandler.getInstance());
        if (Loader.isModLoaded("taunahijdadependency"))
            MinecraftForge.EVENT_BUS.register(DiscordBotHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(AudioManager.getInstance());
        MinecraftForge.EVENT_BUS.register(RotationHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(FlyPathfinder.getInstance());
    }

    private void initializeFields() {
        config = new TaunahiConfig();
    }

    private void initializeCommands() {
        ClientCommandHandler.instance.registerCommand(new RewarpCommand());
        ClientCommandHandler.instance.registerCommand(new TaunahiCommand());
    }
}
