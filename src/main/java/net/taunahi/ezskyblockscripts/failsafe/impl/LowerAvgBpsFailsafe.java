package net.taunahi.ezskyblockscripts.failsafe.impl;

import net.taunahi.ezskyblockscripts.config.TaunahiConfig;
import net.taunahi.ezskyblockscripts.config.page.FailsafeNotificationsPage;
import net.taunahi.ezskyblockscripts.failsafe.Failsafe;
import net.taunahi.ezskyblockscripts.failsafe.FailsafeManager;
import net.taunahi.ezskyblockscripts.feature.impl.ProfitCalculator;
import net.taunahi.ezskyblockscripts.handler.GameStateHandler;
import net.taunahi.ezskyblockscripts.handler.MacroHandler;
import net.taunahi.ezskyblockscripts.handler.RotationHandler;
import net.taunahi.ezskyblockscripts.util.KeyBindUtils;
import net.taunahi.ezskyblockscripts.util.helper.CircularFifoQueue;
import net.taunahi.ezskyblockscripts.util.helper.Rotation;
import net.taunahi.ezskyblockscripts.util.helper.RotationConfiguration;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class LowerAvgBpsFailsafe extends Failsafe {
    private static LowerAvgBpsFailsafe instance;

    public static LowerAvgBpsFailsafe getInstance() {
        if (instance == null) {
            instance = new LowerAvgBpsFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 9;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.LOWER_AVERAGE_BPS;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnLowerAverageBPS;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnLowerAverageBPS;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnLowerAverageBPS;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnLowerAverageBPS;
    }

    @Override
    public void onTickDetection(TickEvent.ClientTickEvent event) {
        if (MacroHandler.getInstance().isCurrentMacroPaused()) {
            bpsQueue.clear();
            lastTimeCheckedBPS = System.currentTimeMillis();
            return;
        }
        if (!TaunahiConfig.averageBPSDropCheck) return;

        if (System.currentTimeMillis() - lastTimeCheckedBPS < 1_000) return;
        lastTimeCheckedBPS = System.currentTimeMillis();
        float bps = ProfitCalculator.getInstance().getBPSFloat();
        bpsQueue.add(bps);
        if (!bpsQueue.isAtFullCapacity()) return;
        float averageBPS = getAverageBPS();
        if (averageBPS > bps) {
            float percentage = (averageBPS - bps) / averageBPS * 100;
            if (percentage > TaunahiConfig.averageBPSDrop) {
                FailsafeManager.getInstance().possibleDetection(this);
            }
        }
    }

    @Override
    public void duringFailsafeTrigger() {
        switch (lowerBPSState) {
            case NONE:
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                lowerBPSState = LowerBPSState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                KeyBindUtils.stopMovement();
                MacroHandler.getInstance().pauseMacro();
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                lowerBPSState = LowerBPSState.WARP_BACK;
                break;
            case WARP_BACK:
                if (GameStateHandler.getInstance().inGarden()) {
                    MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true, true));
                    FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                    lowerBPSState = LowerBPSState.END;
                } else {
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.HUB) {
                        MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true, false));
                        FailsafeManager.getInstance().scheduleRandomDelay(2500, 2000);
                    } else if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LIMBO) {
                        mc.thePlayer.sendChatMessage("/l");
                        FailsafeManager.getInstance().scheduleRandomDelay(2500, 2000);
                    } else {
                        mc.thePlayer.sendChatMessage("/skyblock");
                        FailsafeManager.getInstance().scheduleRandomDelay(5500, 4000);
                    }
                }
                break;
            case END:
                float randomTime = TaunahiConfig.getRandomRotationTime();
                this.rotation.easeTo(
                        new RotationConfiguration(
                                new Rotation(
                                        (float) (mc.thePlayer.rotationYaw + Math.random() * 60 - 30),
                                        (float) (30 + Math.random() * 20 - 10))
                                , (long) randomTime, null));
                FailsafeManager.getInstance().stopFailsafes();
                if (TaunahiConfig.enableRestartAfterFailSafe) {
                    MacroHandler.getInstance().pauseMacro();
                } else {
                    MacroHandler.getInstance().disableMacro();
                }
                FailsafeManager.getInstance().restartMacroAfterDelay();
                break;
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
    }

    @Override
    public void resetStates() {
        lowerBPSState = LowerBPSState.NONE;
        bpsQueue.clear();
    }

    public float getAverageBPS() {
        float averageBPS = 0;
        for (float bpsValue : bpsQueue) {
            averageBPS += bpsValue;
        }
        averageBPS /= bpsQueue.size();
        return averageBPS;
    }

    enum LowerBPSState {
        NONE,
        WAIT_BEFORE_START,
        WARP_BACK,
        END
    }

    private LowerBPSState lowerBPSState = LowerBPSState.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    private final CircularFifoQueue<Float> bpsQueue = new CircularFifoQueue<>(20);
    private long lastTimeCheckedBPS = System.currentTimeMillis();
}
