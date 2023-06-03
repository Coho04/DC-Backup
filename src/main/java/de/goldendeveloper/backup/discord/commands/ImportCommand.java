package de.goldendeveloper.backup.discord.commands;

import de.goldendeveloper.backup.discord.Import;
import de.goldendeveloper.dcbcore.DCBot;
import de.goldendeveloper.dcbcore.interfaces.CommandInterface;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class ImportCommand implements CommandInterface {

    @Override
    public CommandData commandData() {
        return Commands.slash("import", "Importiert ein Discord Server Backup!").setGuildOnly(true);
    }

    @Override
    public void runSlashCommand(SlashCommandInteractionEvent e, DCBot dcBot) {
        Member member = e.getMember();
        if (member != null) {
            if (member.hasPermission(Permission.ADMINISTRATOR)) {
                new Import(e.getGuild(), e.getChannel().asTextChannel());
            } else {
                e.reply("Du hast keine Berechtigung, um diesen Befehl auszuführen!").queue();
            }
        }
    }
}
