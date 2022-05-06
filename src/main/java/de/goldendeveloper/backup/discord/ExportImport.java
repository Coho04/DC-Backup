package de.goldendeveloper.backup.discord;

import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ExportImport {

    public static String PERMISSIONS = "permissions";
    public static String TYPE = "Type";
    public static String ROLES = "roles";
    public static String POSITION = "position";
    public static String USERLIMIT = "userlimit";


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
            category.put(c.getName(), getChannel(c));
        }

        JSONObject roles = new JSONObject();
        for (Role role : guild.getRoles()) {
            roles.put(role.getName(), getRole(role));
        }

        JSONObject json = new JSONObject();
        json.put("name", guild.getName());
        json.put("description", guild.getDescription());
        json.put("category", category);
        json.put(ROLES, roles);
        return json;
    }

    public static JSONObject getRole(Role r) {
        JSONObject role = new JSONObject();
        role.put(PERMISSIONS, "comming soon");
        role.put(POSITION, r.getPosition());
        return role;
    }

    public static @NotNull JSONObject getChannel(@NotNull Category c) {
        JSONObject channels = new JSONObject();
        for (Channel channel : c.getChannels()) {
            channels.put(channel.getName(), getGuildChannel(channel));
        }
        return channels;
    }

    public static @NotNull JSONObject getGuildChannel(@NotNull Channel guildChannel) {
        JSONObject willkommen = new JSONObject();
        if (guildChannel.getType().equals(ChannelType.TEXT)) {
            willkommen.put(TYPE, "text");
            TextChannel textChannel = guildChannel.getJDA().getTextChannelById(guildChannel.getId());
            if (textChannel != null) {
                willkommen.put("Description", textChannel.getTopic());
                willkommen.put(POSITION, textChannel.getPosition());
            }
        } else if (guildChannel.getType().equals(ChannelType.VOICE)) {
            willkommen.put(TYPE, "voice");
            VoiceChannel voiceChannel = guildChannel.getJDA().getVoiceChannelById(guildChannel.getId());
            if (voiceChannel != null) {
                willkommen.put(POSITION, voiceChannel.getPosition());
                willkommen.put(USERLIMIT, voiceChannel.getUserLimit());
            }
        } else if (guildChannel.getType().equals(ChannelType.STAGE)) {
            willkommen.put(TYPE, "stage");
            StageChannel stageChannel = guildChannel.getJDA().getStageChannelById(guildChannel.getId());
            if (stageChannel != null) {
                willkommen.put(POSITION, stageChannel.getPosition());
            }
        } else if (guildChannel.getType().equals(ChannelType.NEWS)) {
            willkommen.put(TYPE, "news");
            NewsChannel newsChannel = guildChannel.getJDA().getNewsChannelById(guildChannel.getId());
            if (newsChannel != null) {
                willkommen.put(POSITION, newsChannel.getPosition());
            }
        }
        willkommen.put(ROLES, "Hier kommen die Rollen hin");
        willkommen.put(PERMISSIONS, "comming soon");
        return willkommen;
    }

    public static void ClearDiscordServer(@NotNull Guild guild) {
        if (!guild.getCategories().isEmpty()) {
            for (Category category : guild.getCategories()) {
                category.delete().queue();
            }
        }
        if (!guild.getRoles().isEmpty()) {
            for (Role role : guild.getRoles()) {
                if (guild.getSelfMember().canInteract(role)) {
                    role.delete().queue();
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
        guild.getManager().setName(json.get("name").toString()).queue();
        if (guild.getFeatures().contains("COMMUNITY")) {
            guild.getManager().setDescription(json.get("description").toString()).queue();
        }
        JSONObject category = json.getJSONObject("category");
        JSONArray jsonArray = category.names();
        for (int i = 0; i < jsonArray.length(); i++) {
            String item = jsonArray.get(i).toString();
            guild.createCategory(item).queue(msg -> {
                JSONObject jsonObject = category.getJSONObject(item);
                JSONArray channels = jsonObject.names();
                for (int b = 0; b < channels.length(); b++) {
                    String name = channels.get(b).toString();
                    JSONObject channelObject = jsonObject.getJSONObject(name);
                    String type = channelObject.getString(TYPE);
                    if (type.equalsIgnoreCase("text")) {
                        msg.createTextChannel(name).queue(channel -> {
                            if (channelObject.has("Description")) {
                                int position = channelObject.getInt(POSITION);
                                String description = channelObject.get("Description").toString();
                                channel.getManager().setTopic(description).queue();
                                channel.getManager().setPosition(position).queue();
                            }
                        });
                    } else if (type.equalsIgnoreCase("voice")) {
                        msg.createVoiceChannel(name).queue(channel -> {
                            int position = channelObject.getInt(POSITION);
                            int userLimit = channelObject.getInt(USERLIMIT);
                            channel.getManager().setPosition(position).queue();
                            channel.getManager().setUserLimit(userLimit).queue();
                        });
                    } else if (type.equalsIgnoreCase("stage")) {
                        if (guild.getFeatures().contains("COMMUNITY")) {
                            msg.createStageChannel(name).queue(channel -> {
                                int position = channelObject.getInt(POSITION);
                                channel.getManager().setPosition(position).queue();
                            });
                        }
                    } else if (type.equalsIgnoreCase("news")) {
                        if (guild.getFeatures().contains("NEWS")) {
                            msg.createTextChannel(name).queue(m -> {
                                m.getManager().setType(ChannelType.NEWS).queue();
                                int position = channelObject.getInt(POSITION);
                                m.getManager().setPosition(position).queue();
                            });
                        }
                    }
                }
            });
        }

        JSONObject roles = json.getJSONObject(ROLES);
        JSONArray rolesArray = roles.names();
        for (int i = 0; i < rolesArray.length(); i++) {
            String name = rolesArray.get(i).toString();
            guild.createRole().setName(name).queue();
        }
    }
}