package de.goldendeveloper.backup.discord;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import de.goldendeveloper.backup.Main;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class Discord {

    private JDA bot;

    public static String cmdShutdown = "shutdown";
    public static String cmdRestart = "restart";
    public static String cmdHelp = "help";
    public static String cmdBackup = "discord-backup";
    public static String cmdImport = "discord-import";

    public Discord(String token) {
        try {
            bot = JDABuilder.createDefault(token)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.ROLE_TAGS, CacheFlag.EMOTE, CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
                    .enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_EMOJIS,
                            GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_PRESENCES,
                            GatewayIntent.GUILD_BANS, GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                            GatewayIntent.GUILD_INVITES, GatewayIntent.DIRECT_MESSAGE_TYPING,
                            GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.GUILD_VOICE_STATES,
                            GatewayIntent.GUILD_WEBHOOKS, GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_MESSAGE_TYPING)
                    .addEventListeners(new Events())
                    .setAutoReconnect(true)
                    .setContextEnabled(true)
                    .build().awaitReady();
            registerCommands();
            if (!System.getProperty("os.name").split(" ")[0].equalsIgnoreCase("windows")) {
                Online();
            }
            bot.getPresence().setActivity(Activity.playing("/help | " + bot.getGuilds().size() + " Servern"));
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void Online() {
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setAuthor(new WebhookEmbed.EmbedAuthor(getBot().getSelfUser().getName(), getBot().getSelfUser().getAvatarUrl(), "https://Golden-Developer.de"));
        embed.setColor(0x00FF00);
        embed.addField(new WebhookEmbed.EmbedField(false, "[Status]", "ONLINE"));
        embed.addField(new WebhookEmbed.EmbedField(false, "Gestartet als", bot.getSelfUser().getName()));
        embed.addField(new WebhookEmbed.EmbedField(false, "Server", Integer.toString(bot.getGuilds().size())));
        embed.addField(new WebhookEmbed.EmbedField(false, "Status", "\uD83D\uDFE2 Gestartet"));
        embed.addField(new WebhookEmbed.EmbedField(false, "Version", getProjektVersion()));
        embed.setFooter(new WebhookEmbed.EmbedFooter("@Golden-Developer", getBot().getSelfUser().getAvatarUrl()));
        embed.setTimestamp(new Date().toInstant());
        new WebhookClientBuilder(Main.getConfig().getDiscordWebhook()).build().send(embed.build());
    }

    public String getProjektVersion() {
        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties.getProperty("version");
    }
    public String getProjektName() {
        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties.getProperty("name");
    }

    private void registerCommands() {
        bot.upsertCommand(cmdShutdown, "Fährt den " + bot.getSelfUser().getName() + " Bot herunter").queue();
        bot.upsertCommand(cmdBackup, "Macht ein Backup des Discord Servers").queue();
        bot.upsertCommand(cmdImport, "Importiert ein Discord Server Backup!").queue();
        bot.upsertCommand(cmdRestart, "Startet den Bot neu").queue();
        bot.upsertCommand(cmdHelp, "Zeigt dir eine Liste möglicher Befehle an!").queue();
    }

    public JDA getBot() {
        return bot;
    }
}