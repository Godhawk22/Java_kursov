import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Простая серверная заготовка для будущего сетевого матча против других игроков. */
public class GameServer {
    private static final int DEFAULT_PORT = 1990;
    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new GameServer().start(port);
    }

    void start(int port) throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Tanky server started on port " + port);
            while (true) {
                Socket socket = server.accept();
                Thread clientThread = new Thread(() -> handleClient(socket), "tank-client-" + socket.getPort());
                clientThread.setDaemon(true);
                clientThread.start();
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            clients.add(out);
            out.println("WELCOME Tanky1990Game server placeholder");
            String line;
            while ((line = in.readLine()) != null) {
                broadcast("CLIENT " + socket.getPort() + ": " + line);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }

    private void broadcast(String message) {
        for (PrintWriter client : clients) {
            if (client.checkError()) {
                clients.remove(client);
            } else {
                client.println(message);
            }
        }
    }
}
