package de.goldendeveloper.backup;

import de.goldendeveloper.backup.discord.commands.Backup;
import de.goldendeveloper.backup.discord.commands.Import;
import de.goldendeveloper.dcbcore.DCBotBuilder;
import de.goldendeveloper.dcbcore.interfaces.CommandInterface;

import java.util.LinkedList;

public class Main {

    public static void main(String[] args) {
        DCBotBuilder builder = new DCBotBuilder(args, true);
        builder.registerCommands(registerCommands());
        builder.build();
    }

    public static LinkedList<CommandInterface> registerCommands() {
        LinkedList<CommandInterface> commands = new LinkedList<>();
        commands.add(new Backup());
        commands.add(new Import());
        return commands;
    }
}
