package de.goldendeveloper.backup;

import de.goldendeveloper.backup.discord.commands.BackupCommand;
import de.goldendeveloper.backup.discord.commands.ImportCommand;
import de.goldendeveloper.dcbcore.DCBotBuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {

    public static void main(String[] args) {
        DCBotBuilder builder = new DCBotBuilder(args, false);
        builder.registerGatewayIntents(GatewayIntent.MESSAGE_CONTENT);
        builder.registerCommands(new BackupCommand(), new ImportCommand());
        builder.build();
    }
}
