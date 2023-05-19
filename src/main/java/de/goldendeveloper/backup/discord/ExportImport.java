package de.goldendeveloper.backup.discord;

import io.sentry.Sentry;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ExportImport {

    public static final String PERMISSIONS = "permissions";
    public static final String TYPE = "type";
    public static final String ROLES = "roles";
    public static final String POSITION = "position";
    public static final String ALLOWED = "allowed";
    public static final String DENIED = "denied";
    public static final String USER_LIMIT = "userlimit";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String TOPIC = "topic";
    public static final String CATEGORY = "category";
    public static final String CHANNELS = "channels";
    public static final String AFK_CHANNEL = "afkchannel";
    public static final String AFK_TIMEOUT = "afktimeout";
    public static final String SYSTEM_MESSAGE_CHANNEL = "systemmessagechannel";
    public static final String DEFAULT_NOTIFICATION_LEVEL = "defaultnotificationlevel";
    public static final String NSFW = "nsfw";


    public static File export(Guild guild) {
        JSONObject jsonObject = exportBackup(guild);
        String fileName = "ServerBackup-" + guild.getId() + ".gd";
        try {
            FileWriter file = new FileWriter(fileName);
            file.write(jsonObject.toString());
            file.close();
        } catch (IOException e) {
            Sentry.captureException(e);
        }
        return new File(fileName);
    }

    public static void importBackup(Guild guild, @NotNull TextChannel channel) {
        channel.getHistory().retrievePast(1).queue(messages -> {
            for (Message m : messages) {
                for (Message.Attachment a : m.getAttachments()) {
                    CompletableFuture<File> f = a.downloadToFile();
                    BufferedReader br = null;
                    try {
                        br = new BufferedReader(new FileReader(f.get()));
                        StringBuilder sb = new StringBuilder();
                        String line = null;
                        try {
                            line = br.readLine();
                        } catch (IOException e) {
                            Sentry.captureException(e);
                        }
                        while (line != null) {
                            sb.append(line);
                            sb.append(System.lineSeparator());
                            line = br.readLine();
                        }
                        ClearDiscordServer(sb.toString(), guild);
                    } catch (ExecutionException | InterruptedException | IOException e) {
                        Sentry.captureException(e);
                    } finally {
                        try {
                            if (br != null) {
                                br.close();
                            }
                        } catch (IOException e) {
                            Sentry.captureException(e);
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
            if (!role.isManaged()) {
                roles.put(role.getName(), getRole(role));
            }
        }

        JSONObject json = new JSONObject();
        json.put(NAME, guild.getName());
        json.put(DESCRIPTION, guild.getDescription());
        json.put(CATEGORY, category);
        json.put(ROLES, roles);
        json.put(AFK_TIMEOUT, guild.getAfkTimeout());
        if (guild.getAfkChannel() != null) {
            json.put(AFK_CHANNEL, guild.getAfkChannel().getName());
        }
        if (guild.getSystemChannel() != null) {
            json.put(SYSTEM_MESSAGE_CHANNEL, guild.getSystemChannel().getName());
        }
        json.put(DEFAULT_NOTIFICATION_LEVEL, guild.getDefaultNotificationLevel());
        return json;
    }

    public static JSONObject getRole(Role r) {
        JSONObject role = new JSONObject();
        role.put(PERMISSIONS, getRolePermissions(r));
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
                channelJson.put(NSFW, textChannel.isNSFW());
                channelJson.put(POSITION, textChannel.getPosition());
                channelJson.put(DESCRIPTION, textChannel.getTopic());
            }
        } else if (guildChannel.getType().equals(ChannelType.VOICE)) {
            VoiceChannel voiceChannel = guildChannel.getJDA().getVoiceChannelById(guildChannel.getId());
            if (voiceChannel != null) {
                channelJson.put(TYPE, "voice");
                channelJson.put(POSITION, voiceChannel.getPosition());
                channelJson.put(USER_LIMIT, voiceChannel.getUserLimit());
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
                channelJson.put(NSFW, newsChannel.isNSFW());
                channelJson.put(TOPIC, newsChannel.getTopic());
            }
        }
        channelJson.put(PERMISSIONS, getRoles(guildChannel.getJDA().getGuildChannelById(guildChannel.getId())));
        return channelJson;
    }

    public static JSONArray getRolePermissions(Role r) {
        JSONArray permissions = new JSONArray();
        r.getPermissions().forEach(permissions::put);
        return permissions;
    }

    public static JSONObject getRoles(GuildChannel channel) {
        JSONObject roles = new JSONObject();
        channel.getGuild().getRoles().stream()
                .filter(role -> !role.isManaged())
                .forEach(role -> roles.put(role.getName(), getChannelPermissions(role, channel)));
        return roles;
    }

    public static JSONObject getChannelPermissions(Role role, GuildChannel ch) {
        JSONObject permissions = new JSONObject();
        permissions.put(ALLOWED, getAllowed(role, ch));
        permissions.put(DENIED, getDenied(role, ch));
        return permissions;
    }

    public static JSONArray getAllowed(Role role, GuildChannel ch) {
        JSONArray allowed = new JSONArray();
        if (ch.getType().equals(ChannelType.TEXT)) {
            ch.getJDA().getTextChannelById(ch.getId()).getPermissionOverride(role).getAllowed().forEach(allowed::put);
        } else if (ch.getType().equals(ChannelType.VOICE)) {
            ch.getJDA().getVoiceChannelById(ch.getId()).getPermissionOverride(role).getAllowed().forEach(allowed::put);
        } else if (ch.getType().equals(ChannelType.NEWS)) {
            ch.getJDA().getNewsChannelById(ch.getId()).getPermissionOverride(role).getAllowed().forEach(allowed::put);
        } else if (ch.getType().equals(ChannelType.STAGE)) {
            ch.getJDA().getStageChannelById(ch.getId()).getPermissionOverride(role).getAllowed().forEach(allowed::put);
        }
        return allowed;
    }

    public static JSONArray getDenied(Role role, GuildChannel ch) {
        JSONArray denied = new JSONArray();
        if (ch.getType().equals(ChannelType.TEXT)) {
            ch.getJDA().getTextChannelById(ch.getId()).getPermissionOverride(role).getDenied().forEach(denied::put);
        } else if (ch.getType().equals(ChannelType.VOICE)) {
            ch.getJDA().getVoiceChannelById(ch.getId()).getPermissionOverride(role).getDenied().forEach(denied::put);
        } else if (ch.getType().equals(ChannelType.NEWS)) {
            ch.getJDA().getNewsChannelById(ch.getId()).getPermissionOverride(role).getDenied().forEach(denied::put);
        } else if (ch.getType().equals(ChannelType.STAGE)) {
            ch.getJDA().getStageChannelById(ch.getId()).getPermissionOverride(role).getDenied().forEach(denied::put);
        }
        return denied;
    }

    public static void ClearDiscordServer(String sb, @NotNull Guild guild) {
        if (!guild.getCategories().isEmpty()) {
            guild.getCategories().forEach(category -> category.delete().queue());
        }
        if (!guild.getRoles().isEmpty()) {
            guild.getRoles().stream()
                    .filter(role -> guild.getSelfMember().canInteract(role) && !role.isManaged())
                    .filter(role -> !role.isPublicRole())
                    .forEach(role -> role.delete().queue());
        }
        if (!guild.getTextChannels().isEmpty()) {
            guild.getTextChannels().forEach(textChannel -> textChannel.delete().queue());
        }
        if (!guild.getVoiceChannels().isEmpty()) {
            guild.getVoiceChannels().forEach(voiceChannel -> voiceChannel.delete().queue());
        }
        if (!guild.getNewsChannels().isEmpty()) {
            guild.getNewsChannels().forEach(newsChannel -> newsChannel.delete().queue());
        }
        if (!guild.getStageChannels().isEmpty()) {
            guild.getStageChannels().forEach(stageChannel -> stageChannel.delete().queue());
        }
        ImportBackup(new JSONObject(sb), guild);
    }

    public static void ImportBackup(@NotNull JSONObject json, @NotNull Guild guild) {
        guild.getManager().setName(json.getString(NAME)).queue();
        guild.getManager().setAfkTimeout(Guild.Timeout.valueOf(json.getString(AFK_TIMEOUT))).queue();
        if (guild.getFeatures().contains("COMMUNITY")) {
            guild.getManager().setDescription(json.getString(DESCRIPTION)).queue();
        }
        ImportRoles(json, guild);
        JSONObject category = json.getJSONObject(CATEGORY);
        JSONArray jsonArray = category.names();
        for (int i = 0; i < jsonArray.length(); i++) {
            String item = jsonArray.getString(i);
            JSONObject categoryJSONObject = category.getJSONObject(item);
            int categoryPosition = categoryJSONObject.getInt(POSITION);
            guild.createCategory(item).setPosition(categoryPosition).queue(msg -> {
                JSONObject jsonObject = categoryJSONObject.getJSONObject(CHANNELS);
                JSONArray channels = jsonObject.names();
                if (channels != null && !channels.toList().isEmpty()) {
                    for (int b = 0; b < channels.length(); b++) {
                        String name = channels.getString(b);
                        JSONObject channelObject = jsonObject.getJSONObject(name);
                        String type = channelObject.getString(TYPE);
                        if (type.equalsIgnoreCase("text")) {
                            msg.createTextChannel(name).queue(channel -> {
                                if (channelObject.has(DESCRIPTION)) {
                                    channel.getManager().setTopic(channelObject.getString(DESCRIPTION)).queue();
                                }
                                if (channelObject.has(NSFW) && channelObject.getBoolean(NSFW)) {
                                    channel.getManager().setNSFW(true).queue();
                                }
                                setChannelRolePermissions(channelObject, guild, channel);
                                channel.getManager().setPosition(channelObject.getInt(POSITION)).queue();
                            });
                        } else if (type.equalsIgnoreCase("voice")) {
                            msg.createVoiceChannel(name).queue(channel -> {
                                channel.getManager().setPosition(channelObject.getInt(POSITION)).queue();
                                channel.getManager().setUserLimit(channelObject.getInt(USER_LIMIT)).queue();
                                setChannelRolePermissions(channelObject, guild, channel);
                            });
                        } else if (type.equalsIgnoreCase("stage")) {
                            if (guild.getFeatures().contains("COMMUNITY")) {
                                msg.createStageChannel(name).queue(channel -> {
                                    channel.getManager().setPosition(channelObject.getInt(POSITION)).queue();
                                    setChannelRolePermissions(channelObject, guild, channel);
                                });
                            }
                        } else if (type.equalsIgnoreCase("news")) {
                            if (guild.getFeatures().contains("NEWS")) {
                                msg.createTextChannel(name).queue(channel -> {
                                    channel.getManager().setType(ChannelType.NEWS).setTopic(channelObject.getString(TOPIC)).queue();
                                    channel.getManager().setPosition(channelObject.getInt(POSITION)).queue();
                                    if (channelObject.has(NSFW) && channelObject.getBoolean(NSFW)) {
                                        channel.getManager().setNSFW(true).queue();
                                    }
                                    setChannelRolePermissions(channelObject, guild, channel);
                                });
                            }
                        }
                    }
                }
            });
        }
        if (json.has(AFK_CHANNEL)) {
            List<VoiceChannel> afkChannel = guild.getVoiceChannelsByName(json.getString(AFK_CHANNEL), true);
            if (!afkChannel.isEmpty()) {
                guild.getManager().setAfkChannel(afkChannel.get(0)).queue();
            }
        }
        if (json.has(DEFAULT_NOTIFICATION_LEVEL)) {
            guild.getManager().setDefaultNotificationLevel(Guild.NotificationLevel.valueOf(json.getString(DEFAULT_NOTIFICATION_LEVEL))).queue();
        }
        if (json.has(SYSTEM_MESSAGE_CHANNEL)) {
            List<TextChannel> systemChannel = guild.getTextChannelsByName(json.getString(SYSTEM_MESSAGE_CHANNEL), true);
            if (!systemChannel.isEmpty()) {
                guild.getManager().setSystemChannel(systemChannel.get(0)).queue();
            }
        }
    }

    public static void setChannelRolePermissions(JSONObject channelObject, Guild guild, Channel channel) {
        JSONObject permissions = channelObject.getJSONObject(PERMISSIONS);
        JSONArray roles = permissions.names();
        if (roles != null && !roles.toList().isEmpty()) {
            for (int c = 0; c < roles.length(); c++) {
                List<Role> roles1 = guild.getRolesByName(roles.getString(c), true);
                if (!roles1.isEmpty()) {
                    Role role = roles1.get(0);
                    if (role != null) {
                        JSONArray allowed = permissions.getJSONObject(roles.getString(c)).getJSONArray(ALLOWED);
                        for (Object allow : allowed.toList()) {
                            if (channel.getType().equals(ChannelType.TEXT)) {
                                TextChannel ch = channel.getJDA().getTextChannelById(channel.getId());
                                if (ch != null) {
                                    ch.upsertPermissionOverride(role).setAllowed(Permission.valueOf(allow.toString())).queue();
                                }
                            } else if (channel.getType().equals(ChannelType.VOICE)) {
                                VoiceChannel ch = channel.getJDA().getVoiceChannelById(channel.getId());
                                if (ch != null) {
                                    ch.upsertPermissionOverride(role).setAllowed(Permission.valueOf(allow.toString())).queue();
                                }
                            } else if (channel.getType().equals(ChannelType.NEWS)) {
                                NewsChannel ch = channel.getJDA().getNewsChannelById(channel.getId());
                                if (ch != null) {
                                    ch.upsertPermissionOverride(role).setAllowed(Permission.valueOf(allow.toString())).queue();
                                }
                            } else if (channel.getType().equals(ChannelType.STAGE)) {
                                StageChannel ch = channel.getJDA().getStageChannelById(channel.getId());
                                if (ch != null) {
                                    ch.upsertPermissionOverride(role).setAllowed(Permission.valueOf(allow.toString())).queue();
                                }
                            }
                        }
                        JSONArray denied = permissions.getJSONObject(roles.getString(c)).getJSONArray(DENIED);
                        for (Object deny : denied.toList()) {
                            if (channel.getType().equals(ChannelType.TEXT)) {
                                TextChannel ch = channel.getJDA().getTextChannelById(channel.getId());
                                if (ch != null) {
                                    ch.upsertPermissionOverride(role).setDenied(Permission.valueOf(deny.toString())).queue();
                                }
                            } else if (channel.getType().equals(ChannelType.VOICE)) {
                                VoiceChannel ch = channel.getJDA().getVoiceChannelById(channel.getId());
                                if (ch != null) {
                                    ch.upsertPermissionOverride(role).setDenied(Permission.valueOf(deny.toString())).queue();
                                }
                            } else if (channel.getType().equals(ChannelType.NEWS)) {
                                NewsChannel ch = channel.getJDA().getNewsChannelById(channel.getId());
                                if (ch != null) {
                                    ch.upsertPermissionOverride(role).setDenied(Permission.valueOf(deny.toString())).queue();
                                }
                            } else if (channel.getType().equals(ChannelType.STAGE)) {
                                StageChannel ch = channel.getJDA().getStageChannelById(channel.getId());
                                if (ch != null) {
                                    ch.upsertPermissionOverride(role).setDenied(Permission.valueOf(deny.toString())).queue();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void ImportRoles(JSONObject json, Guild guild) {
        JSONObject roles = json.getJSONObject(ROLES);
        JSONArray rolesArray = roles.names();
        for (int i = 0; i < rolesArray.length(); i++) {
            String name = rolesArray.get(i).toString();
            guild.createRole().setName(name).queue(role -> {
                roles.getJSONObject(name).getJSONArray(PERMISSIONS).toList().forEach(c -> {
                    role.getManager().setPermissions(Permission.valueOf(c.toString())).queue();
                });
            });
        }
    }
}