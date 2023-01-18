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

import java.io.*;
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
    public static final String USERLIMIT = "userlimit";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String TOPIC = "topic";
    public static final String CATEGORY = "category";
    public static final String CHANNELS = "channels";
    public static final String AFKCHANNEL = "afkchannel";
    public static final String AFKTIMEOUT = "afktimeout";
    public static final String SYSTEMMESSAGECHANNEL = "systemmessagechannel";
    public static final String DEFAULTNOTIFICATIONLEVEL = "defaultnotificationlevel";
    public static final String NSFW = "nsfw";


    public static File Export(Guild guild) {
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

    public static void Import(Guild guild, @NotNull TextChannel channel) {
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
        channelJson.put(PERMISSIONS, getRoles(guildChannel.getJDA().getGuildChannelById(guildChannel.getId())));
        return channelJson;
    }

    public static JSONArray getRolePermissions(Role r) {
        JSONArray permissions = new JSONArray();
        for (Object v : r.getPermissions().toArray()) {
            permissions.put(v);
        }
        return permissions;
    }

    public static JSONObject getRoles(GuildChannel channel) {
        JSONObject roles = new JSONObject();
        for (Role role : channel.getGuild().getRoles()) {
            if (!role.isManaged()) {
                roles.put(role.getName(), getChannelPermissions(role, channel));
            }
        }
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
            PermissionOverride perm = ch.getJDA().getTextChannelById(ch.getId()).getPermissionOverride(role);
            if (perm != null) {
                for (Object v : perm.getAllowed().toArray()) {
                    allowed.put(v);
                }
            }
        } else if (ch.getType().equals(ChannelType.VOICE)) {
            PermissionOverride perm = ch.getJDA().getVoiceChannelById(ch.getId()).getPermissionOverride(role);
            if (perm != null) {
                for (Object v : perm.getAllowed().toArray()) {
                    allowed.put(v);
                }
            }
        } else if (ch.getType().equals(ChannelType.NEWS)) {
            PermissionOverride perm = ch.getJDA().getNewsChannelById(ch.getId()).getPermissionOverride(role);
            if (perm != null) {
                for (Object v : perm.getAllowed().toArray()) {
                    allowed.put(v);
                }
            }
        } else if (ch.getType().equals(ChannelType.STAGE)) {
            PermissionOverride perm = ch.getJDA().getStageChannelById(ch.getId()).getPermissionOverride(role);
            if (perm != null) {
                for (Object v : perm.getAllowed().toArray()) {
                    allowed.put(v);
                }
            }
        }
        return allowed;
    }

    public static JSONArray getDenied(Role role, GuildChannel ch) {
        JSONArray denied = new JSONArray();
        if (ch.getType().equals(ChannelType.TEXT)) {
            PermissionOverride perm = ch.getJDA().getTextChannelById(ch.getId()).getPermissionOverride(role);
            if (perm != null) {
                for (Object v : perm.getDenied().toArray()) {
                    denied.put(v);
                }
            }
        } else if (ch.getType().equals(ChannelType.VOICE)) {
            PermissionOverride perm = ch.getJDA().getVoiceChannelById(ch.getId()).getPermissionOverride(role);
            if (perm != null) {
                for (Object v : perm.getDenied().toArray()) {
                    denied.put(v);
                }
            }
        } else if (ch.getType().equals(ChannelType.NEWS)) {
            PermissionOverride perm = ch.getJDA().getNewsChannelById(ch.getId()).getPermissionOverride(role);
            if (perm != null) {
                for (Object v : perm.getDenied().toArray()) {
                    denied.put(v);
                }
            }
        } else if (ch.getType().equals(ChannelType.STAGE)) {
            PermissionOverride perm = ch.getJDA().getStageChannelById(ch.getId()).getPermissionOverride(role);
            if (perm != null) {
                for (Object v : perm.getDenied().toArray()) {
                    denied.put(v);
                }
            }
        }
        return denied;
    }

    public static void ClearDiscordServer(String sb, @NotNull Guild guild) {
        if (!guild.getCategories().isEmpty()) {
            for (Category category : guild.getCategories()) {
                category.delete().queue();
            }
        }
        if (!guild.getRoles().isEmpty()) {
            for (Role role : guild.getRoles()) {
                if (guild.getSelfMember().canInteract(role) && !role.isManaged()) {
                    if (!role.isPublicRole()) {
                        role.delete().queue();
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
        ImportBackup(new JSONObject(sb), guild);
    }

    public static void ImportBackup(@NotNull JSONObject json, @NotNull Guild guild) {
        guild.getManager().setName(json.getString(NAME)).queue();
        guild.getManager().setAfkTimeout(Guild.Timeout.valueOf(json.getString(AFKTIMEOUT))).queue();
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
                                    }
                                    if (channelObject.has(NSFW)) {
                                        if (channelObject.getBoolean(NSFW)) {
                                            channel.getManager().setNSFW(true).queue();
                                        }
                                    }
                                    setChannelRolePermissions(channelObject, guild, channel);
                                    channel.getManager().setPosition(channelObject.getInt(POSITION)).queue();
                                });
                            } else if (type.equalsIgnoreCase("voice")) {
                                msg.createVoiceChannel(name).queue(channel -> {
                                    channel.getManager().setPosition(channelObject.getInt(POSITION)).queue();
                                    channel.getManager().setUserLimit(channelObject.getInt(USERLIMIT)).queue();
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
                                        if (channelObject.has(NSFW)) {
                                            if (channelObject.getBoolean(NSFW)) {
                                                channel.getManager().setNSFW(true).queue();
                                            }
                                        }
                                        setChannelRolePermissions(channelObject, guild, channel);
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
    }

    public static void setChannelRolePermissions(JSONObject channelObject, Guild guild, Channel channel) {
        JSONObject permissions = channelObject.getJSONObject(PERMISSIONS);
        JSONArray roles = permissions.names();
        if (roles != null) {
            if (!roles.toList().isEmpty()) {
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
    }

    public static void ImportRoles(JSONObject json, Guild guild) {
        JSONObject roles = json.getJSONObject(ROLES);
        JSONArray rolesArray = roles.names();
        for (int i = 0; i < rolesArray.length(); i++) {
            String name = rolesArray.get(i).toString();
            JSONObject obj = roles.getJSONObject(name);
            guild.createRole().setName(name).queue(role -> {
                JSONArray permissions = obj.getJSONArray(PERMISSIONS);
                for (Object c : permissions.toList()) {
                    role.getManager().setPermissions(Permission.valueOf(c.toString())).queue();
                }
            });
        }
    }
}