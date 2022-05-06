package de.goldendeveloper.backup;

import de.goldendeveloper.backup.discord.Discord;

public class Main {

    private static Discord discord;
    private static Config config;

    public static void main(String[] args) {
        config = new Config();
        discord = new Discord(config.getDiscordToken());
    }

    public static Config getConfig() {
        return config;
    }

    public static Discord getDiscord() {
        return discord;
    }
}
