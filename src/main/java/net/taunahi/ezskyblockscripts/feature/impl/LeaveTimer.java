package net.taunahi.ezskyblockscripts.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import net.taunahi.ezskyblockscripts.config.TaunahiConfig;
import net.taunahi.ezskyblockscripts.failsafe.FailsafeManager;
import net.taunahi.ezskyblockscripts.feature.FeatureManager;
import net.taunahi.ezskyblockscripts.feature.IFeature;
import net.taunahi.ezskyblockscripts.handler.MacroHandler;
import net.taunahi.ezskyblockscripts.util.LogUtils;
import net.taunahi.ezskyblockscripts.util.helper.AudioManager;
import net.taunahi.ezskyblockscripts.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

/*
    Credits to Yuro for this superb class
*/
public class LeaveTimer implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static LeaveTimer instance;

    public static LeaveTimer getInstance() {
        if (instance == null) {
            instance = new LeaveTimer();
        }
        return instance;
    }

    public static final Clock leaveClock = new Clock();

    @Override
    public String getName() {
        return "Leave Timer";
    }

    @Override
    public boolean isRunning() {
        return leaveClock.isScheduled();
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return isToggled();
    }

    @Override
    public void start() {
        leaveClock.schedule(TaunahiConfig.leaveTime * 60 * 1000L);
    }

    @Override
    public void stop() {
        leaveClock.reset();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        leaveClock.reset();
    }

    @Override
    public boolean isToggled() {
        return TaunahiConfig.leaveTimer;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isRunning()) return;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;
        if (leaveClock.isScheduled() && leaveClock.passed()) {
            LogUtils.sendDebug("Leave timer has ended.");
            leaveClock.reset();
            MacroHandler.getInstance().disableMacro();
            Multithreading.schedule(() -> {
                try {
                    mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("The timer has ended"));
                    AudioManager.getInstance().resetSound();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 500, TimeUnit.MILLISECONDS);
        }
    }
}
