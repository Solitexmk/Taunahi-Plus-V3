package net.taunahi.ezskyblockscripts.failsafe.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import net.taunahi.ezskyblockscripts.config.TaunahiConfig;
import net.taunahi.ezskyblockscripts.config.page.FailsafeNotificationsPage;
import net.taunahi.ezskyblockscripts.event.ReceivePacketEvent;
import net.taunahi.ezskyblockscripts.failsafe.Failsafe;
import net.taunahi.ezskyblockscripts.failsafe.FailsafeManager;
import net.taunahi.ezskyblockscripts.feature.impl.BanInfoWS;
import net.taunahi.ezskyblockscripts.handler.GameStateHandler;
import net.taunahi.ezskyblockscripts.handler.MacroHandler;
import net.taunahi.ezskyblockscripts.util.LogUtils;
import net.taunahi.ezskyblockscripts.util.helper.AudioManager;
import net.minecraft.util.ChatComponentText;

import java.util.concurrent.TimeUnit;

public class BanwaveFailsafe extends Failsafe {
    private static BanwaveFailsafe instance;
    public static BanwaveFailsafe getInstance() {
        if (instance == null) {
            instance = new BanwaveFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 6;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.BANWAVE;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnBanwaveFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnBanwaveFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnBanwaveFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnBanwaveFailsafe;
    }

    @Override
    public void duringFailsafeTrigger() {
        if (TaunahiConfig.banwaveAction) {
            // pause
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Paused the macro because of banwave!", false);
                MacroHandler.getInstance().pauseMacro();
            } else {
                if (!BanInfoWS.getInstance().isBanwave()) {
                    endOfFailsafeTrigger();
                }
            }
        } else {
            // leave
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Leaving because of banwave!", false);
                MacroHandler.getInstance().pauseMacro();
                Multithreading.schedule(() -> {
                    try {
                        mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Will reconnect after end of banwave!"));
                        AudioManager.getInstance().resetSound();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 500, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
        LogUtils.sendFailsafeMessage("[Failsafe] Resuming the macro because banwave is over!", false);
        FailsafeManager.getInstance().stopFailsafes();
        MacroHandler.getInstance().resumeMacro();
    }

    @Override
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (!BanInfoWS.getInstance().isBanwave()) return;
        if (!TaunahiConfig.banwaveCheckerEnabled) return;
        if (!TaunahiConfig.enableLeavePauseOnBanwave) return;
        if (TaunahiConfig.banwaveDontLeaveDuringJacobsContest && GameStateHandler.getInstance().inJacobContest())
            return;
        FailsafeManager.getInstance().possibleDetection(this);
    }
}
