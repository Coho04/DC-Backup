package io.github.coho04.backup.discord.commands;

import io.github.coho04.backup.discord.Export;
import io.github.coho04.dcbcore.DCBot;
import io.github.coho04.dcbcore.interfaces.CommandInterface;
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
        if (e.isFromGuild() && e.getGuild() != null) {
            e.reply("Bitte das Backup gut aufbewahren!").addFiles(FileUpload.fromData(new Export(e.getGuild()).getFile())).queue(m -> {
                File file = new File("ServerBackup-" + e.getGuild().getId() + ".gd");
                if (!file.delete()) {
                    System.out.println("ERROR: Failed to delete the file: " + "ServerBackup-" + e.getGuild().getId() + ".gd");
                }
            });
        }
    }
}
