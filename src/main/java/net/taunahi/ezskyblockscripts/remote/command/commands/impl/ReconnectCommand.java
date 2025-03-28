package net.taunahi.ezskyblockscripts.remote.command.commands.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.google.gson.JsonObject;
import net.taunahi.ezskyblockscripts.feature.impl.AutoReconnect;
import net.taunahi.ezskyblockscripts.remote.command.commands.ClientCommand;
import net.taunahi.ezskyblockscripts.remote.command.commands.Command;
import net.taunahi.ezskyblockscripts.remote.struct.RemoteMessage;

import java.util.concurrent.TimeUnit;

@Command(label = "reconnect")
public class ReconnectCommand extends ClientCommand {
    public static boolean isEnabled = false;


    @Override
    public void execute(RemoteMessage event) {
        JsonObject args = event.args;
        int delay;
        if (args.has("delay")) {
            delay = args.get("delay").getAsInt();
        } else {
            delay = 5_000;
        }
        AutoReconnect.getInstance().getReconnectDelay().schedule(delay);
        AutoReconnect.getInstance().start();
        isEnabled = true;
        Multithreading.schedule(() -> {
            JsonObject data = new JsonObject();
            data.addProperty("username", mc.getSession().getUsername());
            data.addProperty("image", getScreenshot());
            data.addProperty("delay", delay);
            data.addProperty("uuid", mc.getSession().getPlayerID());
            RemoteMessage response = new RemoteMessage(label, data);
            send(response);
        }, 1_500, TimeUnit.MILLISECONDS);
    }
}
