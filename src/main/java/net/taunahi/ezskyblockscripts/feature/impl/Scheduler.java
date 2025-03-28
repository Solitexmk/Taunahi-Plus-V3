package net.taunahi.ezskyblockscripts.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import net.taunahi.ezskyblockscripts.config.TaunahiConfig;
import net.taunahi.ezskyblockscripts.feature.FeatureManager;
import net.taunahi.ezskyblockscripts.feature.IFeature;
import net.taunahi.ezskyblockscripts.handler.GameStateHandler;
import net.taunahi.ezskyblockscripts.handler.MacroHandler;
import net.taunahi.ezskyblockscripts.handler.RotationHandler;
import net.taunahi.ezskyblockscripts.macro.AbstractMacro;
import net.taunahi.ezskyblockscripts.util.InventoryUtils;
import net.taunahi.ezskyblockscripts.util.KeyBindUtils;
import net.taunahi.ezskyblockscripts.util.LogUtils;
import net.taunahi.ezskyblockscripts.util.PlayerUtils;
import net.taunahi.ezskyblockscripts.util.helper.Clock;
import net.taunahi.ezskyblockscripts.util.helper.Rotation;
import net.taunahi.ezskyblockscripts.util.helper.RotationConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

public class Scheduler implements IFeature {
    private static Scheduler instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final RotationHandler rotation = RotationHandler.getInstance();
    @Getter
    private final Clock schedulerClock = new Clock();
    @Getter
    @Setter
    private SchedulerState schedulerState = SchedulerState.NONE;

    public static Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Scheduler";
    }

    @Override
    public boolean isRunning() {
        return schedulerClock.isScheduled();
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
        schedulerState = SchedulerState.FARMING;
        schedulerClock.schedule((long) (TaunahiConfig.schedulerFarmingTime * 60_000f + (Math.random() * TaunahiConfig.schedulerFarmingTimeRandomness * 60_000f)));
        if (TaunahiConfig.pauseSchedulerDuringJacobsContest && GameStateHandler.getInstance().inJacobContest()) {
            schedulerClock.pause();
        }
    }

    @Override
    public void stop() {
        schedulerState = SchedulerState.NONE;
        schedulerClock.reset();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        GameStateHandler.getInstance().setWasInJacobContest(false);
    }

    @Override
    public boolean isToggled() {
        return TaunahiConfig.enableScheduler;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    public boolean isFarming() {
        return !TaunahiConfig.enableScheduler || schedulerState == SchedulerState.FARMING;
    }

    public String getStatusString() {
        if (TaunahiConfig.enableScheduler) {
            return (schedulerState == SchedulerState.FARMING ? (EnumChatFormatting.GREEN + "Farming") : (EnumChatFormatting.DARK_AQUA + "Break")) + EnumChatFormatting.RESET + " for " +
                    EnumChatFormatting.GOLD + LogUtils.formatTime(Math.max(schedulerClock.getRemainingTime(), 0)) + EnumChatFormatting.RESET + (schedulerClock.isPaused() ? " (Paused)" : "");
        } else {
            return "Farming";
        }
    }

    public void pause() {
        LogUtils.sendDebug("[Scheduler] Pausing");
        schedulerClock.pause();
    }

    public void resume() {
        LogUtils.sendDebug("[Scheduler] Resuming");
        schedulerClock.resume();
    }

    public void farmingTime() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        schedulerState = SchedulerState.FARMING;
        if (mc.currentScreen != null) {
            PlayerUtils.closeScreen();
        }
        MacroHandler.getInstance().resumeMacro();
        schedulerClock.schedule((long) ((TaunahiConfig.schedulerFarmingTime * 60_000f) + (Math.random() * TaunahiConfig.schedulerFarmingTimeRandomness * 60_000f)));
    }

    public void breakTime() {
        schedulerState = SchedulerState.BREAK;
        MacroHandler.getInstance().pauseMacro(true);
        schedulerClock.schedule((long) ((TaunahiConfig.schedulerBreakTime * 60_000f) + (Math.random() * TaunahiConfig.schedulerBreakTimeRandomness * 60_000f)));
        KeyBindUtils.stopMovement();
        Multithreading.schedule(() -> {
            long randomTime4 = TaunahiConfig.getRandomRotationTime();
            this.rotation.easeTo(
                    new RotationConfiguration(
                            new Rotation(
                                    (float) (mc.thePlayer.rotationYaw + Math.random() * 20 - 10),
                                    (float) Math.min(90, Math.max(-90, (mc.thePlayer.rotationPitch + Math.random() * 30 - 15)))
                            ),
                            randomTime4, null
                    )
            );
            if (TaunahiConfig.openInventoryOnSchedulerBreaks) {
                Multithreading.schedule(InventoryUtils::openInventory, randomTime4 + 350, TimeUnit.MILLISECONDS);
            }
        }, (long) (300 + Math.random() * 200), TimeUnit.MILLISECONDS);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!TaunahiConfig.enableScheduler || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || !MacroHandler.getInstance().getCurrentMacro().isPresent())
            return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;

        if (TaunahiConfig.pauseSchedulerDuringJacobsContest && GameStateHandler.getInstance().inJacobContest() && !GameStateHandler.getInstance().isWasInJacobContest()) {
            LogUtils.sendDebug("[Scheduler] Jacob contest started, pausing scheduler");
            schedulerClock.pause();
            GameStateHandler.getInstance().setWasInJacobContest(true);
            return;
        } else if (TaunahiConfig.pauseSchedulerDuringJacobsContest && GameStateHandler.getInstance().isWasInJacobContest() && !GameStateHandler.getInstance().inJacobContest()) {
            LogUtils.sendDebug("[Scheduler] Jacob contest ended, resuming scheduler");
            schedulerClock.resume();
            GameStateHandler.getInstance().setWasInJacobContest(false);
            return;
        }

        if (MacroHandler.getInstance().isMacroToggled() && MacroHandler.getInstance().isCurrentMacroEnabled() && schedulerState == SchedulerState.FARMING && !schedulerClock.isPaused() && schedulerClock.passed()) {
            if (MacroHandler.getInstance().getCurrentMacro().get().getCurrentState().equals(AbstractMacro.State.SWITCHING_LANE)) {
                LogUtils.sendDebug("[Scheduler] Macro is switching row, won't pause.");
                return;
            }
            LogUtils.sendDebug("[Scheduler] Farming time has passed, stopping");
            breakTime();
        } else if (MacroHandler.getInstance().isMacroToggled() && schedulerState == SchedulerState.BREAK && !schedulerClock.isPaused() && schedulerClock.passed()) {
            LogUtils.sendDebug("[Scheduler] Break time has passed, starting");
            farmingTime();
        }
    }

    public enum SchedulerState {
        NONE,
        FARMING,
        BREAK
    }
}
