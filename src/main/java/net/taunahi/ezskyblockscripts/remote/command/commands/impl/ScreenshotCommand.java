package net.taunahi.ezskyblockscripts.remote.command.commands.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.google.gson.JsonObject;
import net.taunahi.ezskyblockscripts.handler.MacroHandler;
import net.taunahi.ezskyblockscripts.remote.command.commands.ClientCommand;
import net.taunahi.ezskyblockscripts.remote.command.commands.Command;
import net.taunahi.ezskyblockscripts.remote.struct.RemoteMessage;
import net.taunahi.ezskyblockscripts.util.InventoryUtils;
import net.taunahi.ezskyblockscripts.util.PlayerUtils;

import java.util.concurrent.TimeUnit;

@Command(label = "screenshot")

public class ScreenshotCommand extends ClientCommand {

    @Override
    public void execute(RemoteMessage message) {
        JsonObject args = message.args;
        try {
            boolean inventory = args.get("inventory").getAsBoolean();
            if (!inventory) {
                screenshot();
            } else {
                inventory();
            }

        } catch (Exception e) {
            e.printStackTrace();
            screenshot();
        }
    }

    public void screenshot() {
        JsonObject data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("image", getScreenshot());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }

    public void inventory() {
        JsonObject data = new JsonObject();

        boolean wasMacroing;
        if (MacroHandler.getInstance().isMacroToggled()) {
            wasMacroing = true;
            MacroHandler.getInstance().pauseMacro();
        } else {
            wasMacroing = false;
        }

        Multithreading.schedule(() -> {
            try {
                InventoryUtils.openInventory();
                Thread.sleep(1000);
                String screenshot = getScreenshot();
                Thread.sleep(1000);
                PlayerUtils.closeScreen();
                if (wasMacroing) {
                    MacroHandler.getInstance().resumeMacro();
                }

                data.addProperty("username", mc.getSession().getUsername());
                data.addProperty("image", screenshot);
                data.addProperty("uuid", mc.getSession().getPlayerID());
                RemoteMessage response = new RemoteMessage(label, data);
                send(response);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 0, TimeUnit.MILLISECONDS);
    }
}
