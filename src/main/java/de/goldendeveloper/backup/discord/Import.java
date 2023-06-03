package de.goldendeveloper.backup.discord;

import io.sentry.Sentry;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Import {

    public Import(Guild guild, TextChannel channel) {
        channel.getHistory().retrievePast(1).queue(messages -> {
            messages.forEach(message -> {
                message.getAttachments().forEach(attachment -> {
                    CompletableFuture<File> file = attachment.downloadToFile();
                    BufferedReader bufferedReader = null;
                    try {
                        bufferedReader = new BufferedReader(new FileReader(file.get()));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line = bufferedReader.readLine();
                        while (line != null) {
                            stringBuilder.append(line);
                            stringBuilder.append(System.lineSeparator());
                            line = bufferedReader.readLine();
                        }
                        clearDiscordServer(guild);
                        importBackup(new JSONObject(stringBuilder.toString()), guild);
                    } catch (Exception e) {
                        Sentry.captureException(e);
                    } finally {
                        try {
                            if (bufferedReader != null) {
                                bufferedReader.close();
                            }
                        } catch (Exception e) {
                            Sentry.captureException(e);
                        }
                    }
                });
            });
        });
    }

    private void clearDiscordServer(@NotNull Guild guild) {
        guild.getChannels().forEach(guildChannel -> guildChannel.delete().queue());
        guild.getRoles().stream()
                .filter(role -> guild.getSelfMember().canInteract(role) && !role.isManaged())
                .filter(role -> !role.isPublicRole())
                .forEach(role -> role.delete().queue());
    }

    private void importBackup(@NotNull JSONObject json, @NotNull Guild guild) {
        guild.getManager().setName(json.getString(DiscordTags.NAME)).queue();
        guild.getManager().setAfkTimeout(Guild.Timeout.valueOf(json.getString(DiscordTags.AFK_TIMEOUT))).queue();
        if (guild.getFeatures().contains("COMMUNITY")) {
            guild.getManager().setDescription(json.getString(DiscordTags.DESCRIPTION)).queue();
        }
        this.importRoles(json, guild);
        JSONObject category = json.getJSONObject(DiscordTags.CATEGORY);
        JSONArray jsonArray = category.names();
        for (int i = 0; i < jsonArray.length(); i++) {
            String item = jsonArray.getString(i);
            JSONObject categoryJSONObject = category.getJSONObject(item);
            guild.createCategory(item).setPosition(categoryJSONObject.getInt(DiscordTags.POSITION)).queue(newCategory -> {
                JSONObject jsonObject = categoryJSONObject.getJSONObject(DiscordTags.CHANNELS);
                JSONArray channels = jsonObject.names();
                if (channels != null && !channels.toList().isEmpty()) {
                    for (int b = 0; b < channels.length(); b++) {
                        createChannel(channels, b, jsonObject, newCategory);
                    }
                }
            });
        }
        if (json.has(DiscordTags.AFK_CHANNEL)) {
            List<VoiceChannel> afkChannel = guild.getVoiceChannelsByName(json.getString(DiscordTags.AFK_CHANNEL), true);
            if (!afkChannel.isEmpty()) {
                guild.getManager().setAfkChannel(afkChannel.get(0)).queue();
            }
        }
        if (json.has(DiscordTags.DEFAULT_NOTIFICATION_LEVEL)) {
            guild.getManager().setDefaultNotificationLevel(Guild.NotificationLevel.valueOf(json.getString(DiscordTags.DEFAULT_NOTIFICATION_LEVEL))).queue();
        }
        if (json.has(DiscordTags.SYSTEM_MESSAGE_CHANNEL)) {
            List<TextChannel> systemChannel = guild.getTextChannelsByName(json.getString(DiscordTags.SYSTEM_MESSAGE_CHANNEL), true);
            if (!systemChannel.isEmpty()) {
                guild.getManager().setSystemChannel(systemChannel.get(0)).queue();
            }
        }
    }

    private void setChannelRolePermissions(JSONObject channelObject, Guild guild, StandardGuildChannel channel) {
        JSONObject permissions = channelObject.getJSONObject(DiscordTags.PERMISSIONS);
        JSONArray roles = permissions.names();
        if (roles != null && !roles.toList().isEmpty()) {
            for (int c = 0; c < roles.length(); c++) {
                List<Role> roles1 = guild.getRolesByName(roles.getString(c), true);
                if (!roles1.isEmpty()) {
                    Role role = roles1.get(0);
                    if (role != null) {
                        JSONArray allowed = permissions.getJSONObject(roles.getString(c)).getJSONArray(DiscordTags.ALLOWED);
                        allowed.toList().forEach(allow -> {
                            channel.upsertPermissionOverride(role).setAllowed(Permission.valueOf(allow.toString())).queue();
                        });
                        JSONArray denied = permissions.getJSONObject(roles.getString(c)).getJSONArray(DiscordTags.DENIED);
                        denied.toList().forEach(deny -> {
                            channel.upsertPermissionOverride(role).setDenied(Permission.valueOf(deny.toString())).queue();
                        });
                    }
                }
            }
        }
    }

    private void createChannel(JSONArray channels, int b, JSONObject jsonObject, Category category) {
        String name = channels.getString(b);
        JSONObject channelObject = jsonObject.getJSONObject(name);
        String type = channelObject.getString(DiscordTags.TYPE);
        if (type.equalsIgnoreCase("text")) {
            category.createTextChannel(name).queue(channel -> {
                if (channelObject.has(DiscordTags.DESCRIPTION)) {
                    channel.getManager().setTopic(channelObject.getString(DiscordTags.DESCRIPTION)).queue();
                }
                if (channelObject.has(DiscordTags.NSFW) && channelObject.getBoolean(DiscordTags.NSFW)) {
                    channel.getManager().setNSFW(true).queue();
                }
                setChannelRolePermissions(channelObject, category.getGuild(), channel);
                channel.getManager().setPosition(channelObject.getInt(DiscordTags.POSITION)).queue();
            });
        } else if (type.equalsIgnoreCase("voice")) {
            category.createVoiceChannel(name).queue(channel -> {
                channel.getManager().setPosition(channelObject.getInt(DiscordTags.POSITION)).queue();
                channel.getManager().setUserLimit(channelObject.getInt(DiscordTags.USER_LIMIT)).queue();
                setChannelRolePermissions(channelObject, category.getGuild(), channel);
            });
        } else if (type.equalsIgnoreCase("stage")) {
            if (category.getGuild().getFeatures().contains("COMMUNITY")) {
                category.createStageChannel(name).queue(channel -> {
                    channel.getManager().setPosition(channelObject.getInt(DiscordTags.POSITION)).queue();
                    setChannelRolePermissions(channelObject, category.getGuild(), channel);
                });
            }
        } else if (type.equalsIgnoreCase("news")) {
            if (category.getGuild().getFeatures().contains("NEWS")) {
                category.createNewsChannel(name).queue(channel -> {
                    channel.getManager().setType(ChannelType.NEWS).setTopic(channelObject.getString(DiscordTags.TOPIC)).queue();
                    channel.getManager().setPosition(channelObject.getInt(DiscordTags.POSITION)).queue();
                    if (channelObject.has(DiscordTags.NSFW) && channelObject.getBoolean(DiscordTags.NSFW)) {
                        channel.getManager().setNSFW(true).queue();
                    }
                    setChannelRolePermissions(channelObject, category.getGuild(), channel);
                });
            }
        }
    }

    private void importRoles(JSONObject json, Guild guild) {
        JSONObject roles = json.getJSONObject(DiscordTags.ROLES);
        JSONArray rolesArray = roles.names();
        for (int i = 0; i < rolesArray.length(); i++) {
            String name = rolesArray.get(i).toString();
            guild.createRole().setName(name).queue(role -> {
                roles.getJSONObject(name).getJSONArray(DiscordTags.PERMISSIONS).toList().forEach(c -> {
                    role.getManager().setPermissions(Permission.valueOf(c.toString())).queue();
                });
            });
        }
    }
}