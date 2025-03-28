package net.taunahi.ezskyblockscripts.remote.event;


import net.taunahi.ezskyblockscripts.remote.DiscordBotHandler;
import net.taunahi.ezskyblockscripts.remote.command.discordCommands.DiscordCommand;
import net.taunahi.ezskyblockscripts.util.LogUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class InteractionCreate extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();
        ArrayList<DiscordCommand> commands = DiscordBotHandler.getInstance().getCommands();

        Optional<DiscordCommand> optionalCommand = commands.stream().filter(cmd -> cmd.name.equalsIgnoreCase(command)).findFirst();
        if (optionalCommand.isPresent()) {
            DiscordCommand cmd = optionalCommand.get();
            try {
                LogUtils.sendDebug("[Remote Control] Executing command " + cmd.name);
                cmd.execute(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        LogUtils.sendDebug("[Remote Control] Command not found: " + command);
        LogUtils.sendDebug("[Remote Control] Commands list: " + Arrays.toString(commands.toArray()));
        event.reply("Command not found").queue();
    }
}
