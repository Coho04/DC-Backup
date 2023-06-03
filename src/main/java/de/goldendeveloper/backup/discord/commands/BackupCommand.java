package de.goldendeveloper.backup.discord.commands;

import de.goldendeveloper.backup.discord.Export;
import de.goldendeveloper.dcbcore.DCBot;
import de.goldendeveloper.dcbcore.interfaces.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;

public class BackupCommand implements CommandInterface {

    @Override
    public CommandData commandData() {
        return Commands.slash("backup", "Macht ein Backup des Discord Servers").setGuildOnly(true);
    }

    @Override
    public void runSlashCommand(SlashCommandInteractionEvent e, DCBot dcBot) {
        if (e.isFromGuild()) {
            e.reply("Bitte das Backup gut aufbewahren!").addFiles(FileUpload.fromData(new Export(e.getGuild()).getFile())).queue(m -> {
                File f = new File("ServerBackup-" + e.getGuild().getId() + ".gd");
                if (!f.delete()) {
                    System.out.println("ERROR: Failed to delete the file: " + "ServerBackup-" + e.getGuild().getId() + ".gd");
                }
            });
        }
    }
}
