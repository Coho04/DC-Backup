package de.goldendeveloper.backup.discord;

import io.sentry.Sentry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.Objects;

public class Export {

    private final JSONObject jsonObject;
    private final Guild guild;

    public Export(@NotNull Guild guild) {
        this.guild = guild;
        JSONObject categoryJson = new JSONObject();
        guild.getCategories().forEach(category -> categoryJson.put(category.getName(), getCategoryAsJson(category)));
        JSONObject roles = new JSONObject();
        guild.getRoles().stream().filter(role -> !role.isManaged()).forEach(role -> roles.put(role.getName(), getRoleAsJson(role)));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(DiscordTags.NAME, guild.getName());
        jsonObject.put(DiscordTags.DESCRIPTION, guild.getDescription());
        jsonObject.put(DiscordTags.CATEGORY, categoryJson);
        jsonObject.put(DiscordTags.ROLES, roles);
        jsonObject.put(DiscordTags.AFK_TIMEOUT, guild.getAfkTimeout());
        if (guild.getAfkChannel() != null) {
            jsonObject.put(DiscordTags.AFK_CHANNEL, guild.getAfkChannel().getName());
        }
        if (guild.getSystemChannel() != null) {
            jsonObject.put(DiscordTags.SYSTEM_MESSAGE_CHANNEL, guild.getSystemChannel().getName());
        }
        jsonObject.put(DiscordTags.DEFAULT_NOTIFICATION_LEVEL, guild.getDefaultNotificationLevel());
        this.jsonObject = jsonObject;
    }

    public File getFile() {
        String fileName = "ServerBackup-" + this.guild.getId() + ".gd";
        try {
            FileWriter fileWriter = new FileWriter(fileName);
            fileWriter.write(jsonObject.toString());
            fileWriter.close();
        } catch (Exception e) {
            Sentry.captureException(e);
        }
        return new File(fileName);
    }

    @SuppressWarnings("unused")
    public JSONObject getJsonObject() {
        return jsonObject;
    }

    private JSONObject getRoleAsJson(Role role) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(DiscordTags.PERMISSIONS, getRolePermissionsAsJson(role));
        jsonObject.put(DiscordTags.POSITION, role.getPosition());
        return jsonObject;
    }

    private JSONObject getCategoryAsJson(Category c) {
        JSONObject category = new JSONObject();
        category.put(DiscordTags.CHANNELS, getChannelAsJson(c));
        category.put(DiscordTags.POSITION, c.getPosition());
        return category;
    }

    private JSONObject getChannelAsJson(@NotNull Category category) {
        JSONObject channels = new JSONObject();
        category.getChannels().forEach(channel -> channels.put(channel.getName(), getGuildChannelAsJson(channel)));
        return channels;
    }

    private JSONObject getGuildChannelAsJson(@NotNull Channel guildChannel) {
        JSONObject channelJson = new JSONObject();
        if (guildChannel.getType().equals(ChannelType.TEXT)) {
            TextChannel textChannel = guildChannel.getJDA().getTextChannelById(guildChannel.getId());
            if (textChannel != null) {
                channelJson.put(DiscordTags.TYPE, "text");
                channelJson.put(DiscordTags.NSFW, textChannel.isNSFW());
                channelJson.put(DiscordTags.POSITION, textChannel.getPosition());
                channelJson.put(DiscordTags.DESCRIPTION, textChannel.getTopic());
            }
        } else if (guildChannel.getType().equals(ChannelType.VOICE)) {
            VoiceChannel voiceChannel = guildChannel.getJDA().getVoiceChannelById(guildChannel.getId());
            if (voiceChannel != null) {
                channelJson.put(DiscordTags.TYPE, "voice");
                channelJson.put(DiscordTags.POSITION, voiceChannel.getPosition());
                channelJson.put(DiscordTags.USER_LIMIT, voiceChannel.getUserLimit());
            }
        } else if (guildChannel.getType().equals(ChannelType.STAGE)) {
            StageChannel stageChannel = guildChannel.getJDA().getStageChannelById(guildChannel.getId());
            if (stageChannel != null) {
                channelJson.put(DiscordTags.TYPE, "stage");
                channelJson.put(DiscordTags.POSITION, stageChannel.getPosition());
            }
        } else if (guildChannel.getType().equals(ChannelType.NEWS)) {
            NewsChannel newsChannel = guildChannel.getJDA().getNewsChannelById(guildChannel.getId());
            if (newsChannel != null) {
                channelJson.put(DiscordTags.TYPE, "news");
                channelJson.put(DiscordTags.POSITION, newsChannel.getPosition());
                channelJson.put(DiscordTags.NSFW, newsChannel.isNSFW());
                channelJson.put(DiscordTags.TOPIC, newsChannel.getTopic());
            }
        }
        channelJson.put(DiscordTags.PERMISSIONS, getRolesAsJson(Objects.requireNonNull(guildChannel.getJDA().getGuildChannelById(guildChannel.getId()))));
        return channelJson;
    }

    public JSONArray getRolePermissionsAsJson(Role role) {
        JSONArray permissions = new JSONArray();
        role.getPermissions().forEach(permissions::put);
        return permissions;
    }

    public JSONObject getRolesAsJson(GuildChannel channel) {
        JSONObject roles = new JSONObject();
        channel.getGuild().getRoles().stream()
                .filter(role -> !role.isManaged())
                .forEach(role -> roles.put(role.getName(), getChannelPermissionsAsJson(role, channel)));
        return roles;
    }

    private JSONObject getChannelPermissionsAsJson(Role role, GuildChannel channel) {
        JSONObject permissions = new JSONObject();
        permissions.put(DiscordTags.ALLOWED, getAllowedAsJson(role, channel));
        permissions.put(DiscordTags.DENIED, getDeniedAsJson(role, channel));
        return permissions;
    }

    private JSONArray getAllowedAsJson(Role role, GuildChannel channel) {
        JSONArray allowed = new JSONArray();
        if (getGuildStandardChannel(channel) != null) {
            PermissionOverride permissionOverride = getGuildStandardChannel(channel).getPermissionOverride(role);
            if (permissionOverride != null) {
                permissionOverride.getAllowed().forEach(allowed::put);
            }
        }
        return allowed;
    }

    private JSONArray getDeniedAsJson(Role role, GuildChannel channel) {
        JSONArray denied = new JSONArray();
        if (getGuildStandardChannel(channel) != null) {
            PermissionOverride permissionOverride = getGuildStandardChannel(channel).getPermissionOverride(role);
            if (permissionOverride != null) {
                permissionOverride.getDenied().forEach(denied::put);
            }
        }
        return denied;
    }

    private StandardGuildChannel getGuildStandardChannel(GuildChannel channel) {
        return switch (channel.getType()) {
            case TEXT ->  channel.getJDA().getTextChannelById(channel.getId());
            case VOICE -> channel.getJDA().getVoiceChannelById(channel.getId());
            case NEWS -> channel.getJDA().getNewsChannelById(channel.getId());
            case STAGE -> channel.getJDA().getStageChannelById(channel.getId());
            default -> null;
        };
    }
}