package de.goldendeveloper.backup;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ServerCommunicator {

    public enum action {
        ADD,
        REMOVE,
        START
    }

    private final String HOSTNAME;
    private final int PORT;

    public ServerCommunicator(String HOSTNAME, int PORT) {
        this.HOSTNAME = HOSTNAME;
        this.PORT = PORT;
    }

    public void startBot() {
        Socket socket = null;
        try {
            socket = new Socket(this.HOSTNAME, this.PORT);
            OutputStream raus = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(raus, StandardCharsets.UTF_8);
            JSONObject msg = new JSONObject();
            msg.put("name", Main.getDiscord().getBot().getSelfUser().getName());
            msg.put("invite", Main.getDiscord().getBot().getInviteUrl(Permission.ADMINISTRATOR));
            msg.put("type", action.START);
            msg.put("commands", Main.getDiscord().getBot().retrieveCommands().complete());
            msg.put("server", getGuildIDsFromGuilds());
            osw.write(msg.toString());
            osw.flush();
            osw.close();
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host...");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IOProbleme...");
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                    System.out.println("Socket geschlossen...");
                } catch (IOException e) {
                    System.out.println("Socket konnte nicht geschlossen werden...");
                    e.printStackTrace();
                }
            }
        }
    }

    public void addServer(String guild) {
        Socket socket = null;
        try {
            socket = new Socket(this.HOSTNAME, this.PORT);
            OutputStream raus = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(raus, StandardCharsets.UTF_8);
            JSONObject msg = new JSONObject();
            msg.put("name", Main.getConfig().getProjektName());
            msg.put("type", action.ADD);
            msg.put("server", Main.getDiscord().getBot().getGuilds().size());
            msg.put("guild", guild);
            osw.write(msg.toString());
            osw.flush();
            osw.close();
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host...");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IOProbleme...");
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                    System.out.println("Socket geschlossen...");
                } catch (IOException e) {
                    System.out.println("Socket konnte nicht geschlossen werden...");
                    e.printStackTrace();
                }
            }
        }
    }

    public void removeServer(String guild) {
        Socket socket = null;
        try {
            socket = new Socket(this.HOSTNAME, this.PORT);
            OutputStream raus = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(raus, StandardCharsets.UTF_8);
            JSONObject msg = new JSONObject();
            msg.put("name", Main.getConfig().getProjektName());
            msg.put("type", action.REMOVE);
            msg.put("server", Main.getDiscord().getBot().getGuilds().size());
            msg.put("guild", guild);
            osw.write(msg.toString());
            osw.flush();
            osw.close();
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host...");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IOProbleme...");
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                    System.out.println("Socket geschlossen...");
                } catch (IOException e) {
                    System.out.println("Socket konnte nicht geschlossen werden...");
                    e.printStackTrace();
                }
            }
        }
    }

    public List<String> getGuildIDsFromGuilds() {
        List<String> ids = new ArrayList<>();
        for (Guild guild : Main.getDiscord().getBot().getGuilds()) {
            ids.add(guild.getId());
        }
        return ids;
    }
}
