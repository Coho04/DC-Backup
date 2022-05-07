package de.goldendeveloper.backup.discord;

import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ExportImport {

    public static String PERMISSIONS = "permissions";
    public static String TYPE = "type";
    public static String ROLES = "roles";
    public static String POSITION = "position";
    public static String USERLIMIT = "userlimit";
    public static String NAME = "name";
    public static String DESCRIPTION = "description";
    public static String TOPIC = "topic";
    public static String CATEGORY = "category";
    public static String CHANNELS = "channels";
    public static String AFKCHANNEL = "afkchannel";
    public static String AFKTIMEOUT = "afktimeout";
    public static String SYSTEMMESSAGECHANNEL = "systemmessagechannel";
    public static String DEFAULTNOTIFICATIONLEVEL = "defaultnotificationlevel";
    public static String NSFW = "nsfw";


    public static File Export(Guild guild) {
        JSONObject jsonObject = exportBackup(guild);
        String fileName = "ServerBackup-" + guild.getId() + ".gd";
        try {
            FileWriter file = new FileWriter(fileName);
            file.write(jsonObject.toString());
            file.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return new File(fileName);
    }

    public static void Import(Guild guild, @NotNull TextChannel channel) {
        channel.getHistory().retrievePast(1).queue(messages -> {
            for (Message m : messages) {
                for (Message.Attachment a : m.getAttachments()) {
                    CompletableFuture<File> f = a.downloadToFile();
                    BufferedReader br = null;
                    try {
                        br = new BufferedReader(new FileReader(f.get()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        try {
                            line = br.readLine();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        while (line != null) {
                            sb.append(line);
                            sb.append(System.lineSeparator());
                            line = br.readLine();
                        }
                        ClearDiscordServer(guild);
                        ImportBackup(new JSONObject(sb.toString()), guild);
                    } catch (ExecutionException | InterruptedException | IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            if (br != null) {
                                br.close();
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public static @NotNull JSONObject exportBackup(@NotNull Guild guild) {
        JSONObject category = new JSONObject();
        for (Category c : guild.getCategories()) {
            category.put(c.getName(), getCategory(c));
        }

        JSONObject roles = new JSONObject();
        for (Role role : guild.getRoles()) {
            roles.put(role.getName(), getRole(role));
        }

        JSONObject json = new JSONObject();
        json.put(NAME, guild.getName());
        json.put(DESCRIPTION, guild.getDescription());
        json.put(CATEGORY, category);
        json.put(ROLES, roles);
        json.put(AFKTIMEOUT, guild.getAfkTimeout());
        if (guild.getAfkChannel() != null) {
            json.put(AFKCHANNEL, guild.getAfkChannel().getName());
        }
        if (guild.getSystemChannel() != null) {
            json.put(SYSTEMMESSAGECHANNEL, guild.getSystemChannel().getName());
        }
        json.put(DEFAULTNOTIFICATIONLEVEL, guild.getDefaultNotificationLevel());
        return json;
    }

    public static JSONObject getRole(Role r) {
        JSONObject role = new JSONObject();
        role.put(PERMISSIONS, "comming soon");
        role.put(POSITION, r.getPosition());
        return role;
    }

    public static JSONObject getCategory(Category c) {
        JSONObject category = new JSONObject();
        category.put(CHANNELS, getChannel(c));
        category.put(POSITION, c.getPosition());
        return category;
    }

    public static @NotNull JSONObject getChannel(@NotNull Category c) {
        JSONObject channels = new JSONObject();
        for (Channel channel : c.getChannels()) {
            channels.put(channel.getName(), getGuildChannel(channel));
        }
        return channels;
    }

    public static @NotNull JSONObject getGuildChannel(@NotNull Channel guildChannel) {
        JSONObject channelJson = new JSONObject();
        if (guildChannel.getType().equals(ChannelType.TEXT)) {
            TextChannel textChannel = guildChannel.getJDA().getTextChannelById(guildChannel.getId());
            if (textChannel != null) {
                channelJson.put(TYPE, "text");
                channelJson.put(NSFW, "" + textChannel.isNSFW() + "");
                channelJson.put(POSITION, textChannel.getPosition());
                channelJson.put(DESCRIPTION, textChannel.getTopic());
            }
        } else if (guildChannel.getType().equals(ChannelType.VOICE)) {
            VoiceChannel voiceChannel = guildChannel.getJDA().getVoiceChannelById(guildChannel.getId());
            if (voiceChannel != null) {
                channelJson.put(TYPE, "voice");
                channelJson.put(POSITION, voiceChannel.getPosition());
                channelJson.put(USERLIMIT, voiceChannel.getUserLimit());
            }
        } else if (guildChannel.getType().equals(ChannelType.STAGE)) {
            StageChannel stageChannel = guildChannel.getJDA().getStageChannelById(guildChannel.getId());
            if (stageChannel != null) {
                channelJson.put(TYPE, "stage");
                channelJson.put(POSITION, stageChannel.getPosition());
            }
        } else if (guildChannel.getType().equals(ChannelType.NEWS)) {
            NewsChannel newsChannel = guildChannel.getJDA().getNewsChannelById(guildChannel.getId());
            if (newsChannel != null) {
                channelJson.put(TYPE, "news");
                channelJson.put(POSITION, newsChannel.getPosition());
                channelJson.put(NSFW, "" + newsChannel.isNSFW() + "");
                channelJson.put(TOPIC, newsChannel.getTopic());
            }
        }
        channelJson.put(ROLES, "Hier kommen die Rollen hin");
        channelJson.put(PERMISSIONS, "comming soon");
        return channelJson;
    }

    public static void ClearDiscordServer(@NotNull Guild guild) {
        if (!guild.getCategories().isEmpty()) {
            for (Category category : guild.getCategories()) {
                category.delete().queue();
            }
        }
        if (!guild.getRoles().isEmpty()) {
            for (Role role : guild.getRoles()) {
                if (guild.getSelfMember().canInteract(role) && !role.isManaged()) {
                    try {
                        role.delete().queue();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (!guild.getTextChannels().isEmpty()) {
            for (TextChannel textChannel : guild.getTextChannels()) {
                textChannel.delete().queue();
            }
        }
        if (!guild.getVoiceChannels().isEmpty()) {
            for (VoiceChannel voiceChannel : guild.getVoiceChannels()) {
                voiceChannel.delete().queue();
            }
        }
        if (!guild.getNewsChannels().isEmpty()) {
            for (NewsChannel newsChannel : guild.getNewsChannels()) {
                newsChannel.delete().queue();
            }
        }
        if (!guild.getStageChannels().isEmpty()) {
            for (StageChannel stageChannel : guild.getStageChannels()) {
                stageChannel.delete().queue();
            }
        }
    }

    public static void ImportBackup(@NotNull JSONObject json, @NotNull Guild guild) {
        guild.getManager().setName(json.getString(NAME)).queue();
        guild.getManager().setAfkTimeout(Guild.Timeout.valueOf(json.getString(AFKTIMEOUT))).queue();
        if (guild.getFeatures().contains("COMMUNITY")) {
            guild.getManager().setDescription(json.getString(DESCRIPTION)).queue();
        }
        JSONObject category = json.getJSONObject(CATEGORY);
        JSONArray jsonArray = category.names();
        for (int i = 0; i < jsonArray.length(); i++) {
            String item = jsonArray.getString(i);
            JSONObject categoryJSONObject = category.getJSONObject(item);
            int categoryPosition = categoryJSONObject.getInt(POSITION);
            guild.createCategory(item).setPosition(categoryPosition).queue(msg -> {
                JSONObject jsonObject = categoryJSONObject.getJSONObject(CHANNELS);
                JSONArray channels = jsonObject.names();
                if (channels != null) {
                    if (!channels.toList().isEmpty()) {
                        for (int b = 0; b < channels.length(); b++) {
                            String name = channels.getString(b);
                            JSONObject channelObject = jsonObject.getJSONObject(name);
                            String type = channelObject.getString(TYPE);
                            if (type.equalsIgnoreCase("text")) {
                                msg.createTextChannel(name).queue(channel -> {
                                    if (channelObject.has(DESCRIPTION)) {
                                        channel.getManager().setTopic(channelObject.getString(DESCRIPTION)).queue();
                                        boolean nsfw = false;
                                        if (channelObject.has(NSFW)) {
                                            System.out.println("NSFW: " + channelObject.get(NSFW));
                                            if (channelObject.getString(NSFW).equalsIgnoreCase("true")) {
                                                nsfw = true;
                                            }
                                        }
                                        System.out.println(nsfw);
                                        channel.getManager().setNSFW(nsfw).queue();
                                        channel.getManager().setPosition(channelObject.getInt(POSITION)).queue();
                                    }
                                });
                            } else if (type.equalsIgnoreCase("voice")) {
                                msg.createVoiceChannel(name).queue(channel -> {
                                    channel.getManager().setPosition(channelObject.getInt(POSITION)).queue();
                                    channel.getManager().setUserLimit(channelObject.getInt(USERLIMIT)).queue();
                                });
                            } else if (type.equalsIgnoreCase("stage")) {
                                if (guild.getFeatures().contains("COMMUNITY")) {
                                    msg.createStageChannel(name).queue(channel -> {
                                        channel.getManager().setPosition(channelObject.getInt(POSITION)).queue();
                                    });
                                }
                            } else if (type.equalsIgnoreCase("news")) {
                                if (guild.getFeatures().contains("NEWS")) {
                                    msg.createTextChannel(name).queue(channel -> {
                                        channel.getManager().setType(ChannelType.NEWS).setTopic(channelObject.getString(TOPIC)).queue();
                                        channel.getManager().setPosition(channelObject.getInt(POSITION)).queue();
                                        channel.getManager().setNSFW(channelObject.getBoolean(NSFW)).queue();
                                    });
                                }
                            }
                        }
                    }
                }
            });
        }

        if (json.has(AFKCHANNEL)) {
            List<VoiceChannel> afkchannel = guild.getVoiceChannelsByName(json.getString(AFKCHANNEL), true);
            if (!afkchannel.isEmpty()) {
                guild.getManager().setAfkChannel(afkchannel.get(0)).queue();
            }
        }
        if (json.has(DEFAULTNOTIFICATIONLEVEL)) {
            guild.getManager().setDefaultNotificationLevel(Guild.NotificationLevel.valueOf(json.getString(DEFAULTNOTIFICATIONLEVEL))).queue();
        }
        if (json.has(SYSTEMMESSAGECHANNEL)) {
            List<TextChannel> systemchannel = guild.getTextChannelsByName(json.getString(SYSTEMMESSAGECHANNEL), true);
            if (!systemchannel.isEmpty()) {
                guild.getManager().setSystemChannel(systemchannel.get(0)).queue();
            }
        }

        JSONObject roles = json.getJSONObject(ROLES);
        JSONArray rolesArray = roles.names();
        for (int i = 0; i < rolesArray.length(); i++) {
            String name = rolesArray.get(i).toString();
            guild.createRole().setName(name).queue();
        }
    }
}