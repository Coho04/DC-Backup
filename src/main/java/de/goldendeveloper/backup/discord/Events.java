package de.goldendeveloper.backup.discord;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import de.goldendeveloper.backup.Main;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class Events extends ListenerAdapter {

    @Override
    public void onShutdown(@NotNull ShutdownEvent e) {
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setAuthor(new WebhookEmbed.EmbedAuthor(Main.getDiscord().getBot().getSelfUser().getName(), Main.getDiscord().getBot().getSelfUser().getAvatarUrl(), "https://Golden-Developer.de"));
        embed.addField(new WebhookEmbed.EmbedField(false, "[Status]", "OFFLINE"));
        embed.setColor(0xFF0000);
        embed.setFooter(new WebhookEmbed.EmbedFooter("@Golden-Developer", Main.getDiscord().getBot().getSelfUser().getAvatarUrl()));
        if (new WebhookClientBuilder(Main.getConfig().getDiscordWebhook()).build().send(embed.build()).isDone()) {
            System.exit(0);
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent e) {
        User _Coho04_ = e.getJDA().getUserById("513306244371447828");
        User nick = e.getJDA().getUserById("428811057700536331");
        String cmd = e.getName();
        if (cmd.equalsIgnoreCase(Discord.getCmdShutdown)) {
            if (e.getUser() == nick || e.getUser() == _Coho04_) {
                e.getInteraction().reply("Der Bot wird nun heruntergefahren").queue();
                e.getJDA().shutdown();
            } else {
                e.getInteraction().reply("Dazu hast du keine Rechte, du musst für diesen Befehl der Bot inhaber sein!").queue();
            }
        } else if (cmd.equalsIgnoreCase(Discord.getCmdRestart)) {
            if (e.getUser() == nick || e.getUser() == _Coho04_) {
                try {
                    e.getInteraction().reply("Der Discord Bot wird nun neugestartet!").queue();
                    Process p = Runtime.getRuntime().exec("screen -AmdS GD-Backup java -Xms1096M -Xmx1096M -jar GD-Backup-1.0.jar");
                    p.waitFor();
                    e.getJDA().shutdown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                e.getInteraction().reply("Dazu hast du keine Rechte, du musst für diesen Befehl der Bot Inhaber sein!").queue();
            }
        } else if (cmd.equalsIgnoreCase(Discord.getCmdBackup)) {
            File file = ExportImport.Export(e.getGuild());
            e.reply("Bitte das Backup gut aufbewahren!").addFile(file).queue(m -> {
                file.delete();
            });
        } else if (cmd.equalsIgnoreCase(Discord.getCmdImport)) {
            if (e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                ExportImport.Import(e.getGuild(), e.getTextChannel());
            }
        } else if (cmd.equalsIgnoreCase(Discord.getCmdDelete)) {
            if (e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                ExportImport.ClearDiscordServer(e.getGuild());
            }
        }
    }
}