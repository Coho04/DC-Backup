package de.goldendeveloper.backup;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class ServerCommunicator {

    public enum action {
        ADD,
        REMOVE
    }

    private final String HOSTNAME;
    private final int PORT;

    public ServerCommunicator(String HOSTNAME, int PORT) {
        this.HOSTNAME = HOSTNAME;
        this.PORT = PORT;
    }

    public void sendToServer(Enum action, String serverId) {
        Socket socket = null;
        try {
            socket = new Socket(this.HOSTNAME, this.PORT);
            OutputStream raus = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(raus, StandardCharsets.UTF_8);
            JSONObject msg = new JSONObject();
            msg.put("name", Main.getDiscord().getProjektName());
            msg.put("action", action);
            msg.put("server", serverId);
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
}
