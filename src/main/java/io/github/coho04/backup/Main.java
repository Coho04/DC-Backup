package io.github.coho04.backup;

import io.github.coho04.backup.discord.commands.BackupCommand;
import io.github.coho04.backup.discord.commands.ImportCommand;
import io.github.coho04.dcbcore.DCBotBuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {

    public static void main(String[] args) {
        DCBotBuilder builder = new DCBotBuilder(args, true);
        builder.registerGatewayIntents(GatewayIntent.MESSAGE_CONTENT);
        builder.registerCommands(new BackupCommand(), new ImportCommand());
        builder.build();
    }
}
