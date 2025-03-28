package net.taunahi.ezskyblockscripts.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import net.taunahi.ezskyblockscripts.config.TaunahiConfig;
import net.taunahi.ezskyblockscripts.failsafe.FailsafeManager;
import net.taunahi.ezskyblockscripts.feature.impl.BanInfoWS;
import net.taunahi.ezskyblockscripts.feature.impl.LeaveTimer;
import net.taunahi.ezskyblockscripts.feature.impl.PestsDestroyer;
import net.taunahi.ezskyblockscripts.feature.impl.Scheduler;
import net.taunahi.ezskyblockscripts.handler.MacroHandler;
import net.taunahi.ezskyblockscripts.remote.DiscordBotHandler;
import net.taunahi.ezskyblockscripts.util.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cc.polyfrost.oneconfig.libs.universal.UGraphics.getCharWidth;

public class StatusHUD extends TextHud {

    private final boolean jdaDependencyPresent = Loader.isModLoaded("taunahijdadependency");

    public StatusHUD() {
        super(true, Minecraft.getMinecraft().displayWidth - 100, Minecraft.getMinecraft().displayHeight - 100, 1, true, true, 4f, 5, 5, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        List<String> tempLines = new ArrayList<>();
        tempLines.add(getStatusString());

        if (PestsDestroyer.getInstance().getTotalPests() > 0) {
            tempLines.add(EnumChatFormatting.UNDERLINE + "Pests in Garden:" + EnumChatFormatting.RESET + " " + EnumChatFormatting.RED + PestsDestroyer.getInstance().getTotalPests());
        }

        if (BanInfoWS.getInstance().isRunning() && TaunahiConfig.banwaveCheckerEnabled && BanInfoWS.getInstance().isConnected()) {
            tempLines.add("Ban stats from the last " + BanInfoWS.getInstance().getMinutes() + " minutes");
            tempLines.add("Staff bans: " + BanInfoWS.getInstance().getStaffBans());
            tempLines.add("Detected by Taunahi: " + BanInfoWS.getInstance().getBansByMod());
        } else if (!BanInfoWS.getInstance().isConnected() && TaunahiConfig.banwaveCheckerEnabled) {
            tempLines.add("Connecting to the analytics server...");
        }
        if (LeaveTimer.getInstance().isRunning())
            tempLines.add("Leaving in " + LogUtils.formatTime(Math.max(LeaveTimer.leaveClock.getRemainingTime(), 0)));

        if (TaunahiConfig.enableRemoteControl && jdaDependencyPresent) {
            if (!Objects.equals(DiscordBotHandler.getInstance().getConnectingState(), "")) {
                tempLines.add("");
                tempLines.add(DiscordBotHandler.getInstance().getConnectingState());
            }
        }

        float maxWidth = getWidth(tempLines);

        for (String line : tempLines) {
            lines.add(centerText(line, maxWidth));
        }
    }

    private String centerText(String text, float maxWidth) {
        float lineWidth = getLineWidth(EnumChatFormatting.getTextWithoutFormattingCodes(text).trim(), scale);
        float charWidth = getCharWidth(' ');
        int spaces = (int) ((maxWidth - lineWidth) / charWidth);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < spaces / 2; i++) {
            builder.append(" ");
        }
        return builder + text;
    }

    protected float getWidth(List<String> lines) {
        if (lines == null) return 0;
        float width = 0;
        for (String line : lines) {
            width = Math.max(width, getLineWidth(EnumChatFormatting.getTextWithoutFormattingCodes(line).trim(), scale));
        }
        return width;
    }

    public String getStatusString() {
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
            return "Emergency: §l§5" + LogUtils.capitalize(FailsafeManager.getInstance().triggeredFailsafe.get().getType().name()) + "§r";
        } else if (FailsafeManager.getInstance().getRestartMacroAfterFailsafeDelay().isScheduled()) {
            return "§l§6Restarting after failsafe in " + LogUtils.formatTime(FailsafeManager.getInstance().getRestartMacroAfterFailsafeDelay().getRemainingTime()) + "§r";
        } else if (!MacroHandler.getInstance().isMacroToggled()) {
            return (EnumChatFormatting.AQUA + "Idling");
        } else if (Scheduler.getInstance().isRunning()) {
            return Scheduler.getInstance().getStatusString();
        } else {
            return "Macroing";
        }
    }
}
