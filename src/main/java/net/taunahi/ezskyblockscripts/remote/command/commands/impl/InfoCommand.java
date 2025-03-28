package net.taunahi.ezskyblockscripts.remote.command.commands.impl;

import com.google.gson.JsonObject;
import net.taunahi.ezskyblockscripts.feature.impl.ProfitCalculator;
import net.taunahi.ezskyblockscripts.handler.MacroHandler;
import net.taunahi.ezskyblockscripts.remote.command.commands.ClientCommand;
import net.taunahi.ezskyblockscripts.remote.command.commands.Command;
import net.taunahi.ezskyblockscripts.remote.struct.RemoteMessage;
import net.taunahi.ezskyblockscripts.util.LogUtils;

@Command(label = "info")
public class InfoCommand extends ClientCommand {
    @Override
    public void execute(RemoteMessage message) {
        JsonObject data = new JsonObject();

        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("runtime", LogUtils.getRuntimeFormat());
        data.addProperty("totalProfit", ProfitCalculator.getInstance().getRealProfitString());
        data.addProperty("profitPerHour", ProfitCalculator.getInstance().getProfitPerHourString());
        data.addProperty("cropType", String.valueOf(MacroHandler.getInstance().getCrop() == null ? "None" : MacroHandler.getInstance().getCrop()));
        data.addProperty("currentState", String.valueOf(!MacroHandler.getInstance().getCurrentMacro().isPresent() ? "Macro is not running" : MacroHandler.getInstance().getCurrentMacro().get().getCurrentState()));
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("image", getScreenshot());

        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }
}
