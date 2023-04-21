package de.goldendeveloper.backup;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.util.Properties;

public class Config {

    private final String discordToken;
    private final String discordWebhook;
    private final String serverHostname;
    private final int serverPort;
    private final String sentryDNS;

    public Config() {
        Dotenv dotenv = Dotenv.load();
        discordToken = dotenv.get("DISCORD_TOKEN");
        discordWebhook = dotenv.get("DISCORD_WEBHOOK");
        serverHostname = dotenv.get("SERVER_HOSTNAME");
        serverPort = Integer.parseInt(dotenv.get("SERVER_PORT"));
        sentryDNS = dotenv.get("SENTRY_DNS");
    }


    public String getDiscordWebhook() {
        return discordWebhook;
    }

    public String getDiscordToken() {
        return discordToken;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getServerHostname() {
        return serverHostname;
    }

    public String getSentryDNS() {
        return sentryDNS;
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
}