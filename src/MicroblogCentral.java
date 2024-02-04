import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MicroblogCentral {
    private static ServerSocket microblogamu;
    private final Socket clientSocket;
    private static final int port = 12345;
    public static User SERVER = new User("_SERVER");
    public static List<User> USERS = new ArrayList<>();
    public static List<Tag> TAGS = new ArrayList<>();

    public static final List<Message> MSG_HISTORY = new ArrayList<>(); // Contient tous les message envoyés depuis le lancement du server

    public static long NEXT_ID = 0; // Pour conpenser que synchronised ne fonctionne pas

    public MicroblogCentral(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public static void main(String[] args) throws IOException {
        microblogamu = new ServerSocket(port); // On cree le server
        ExecutorService executor = Executors.newCachedThreadPool(); // Pool de thread dynamique
        try {
            while (true) {
                Socket socket = microblogamu.accept(); // On accepte le client
                executor.submit(new Thread(new ClientHandler(socket))); // Gere la connexion du client
            }
        } catch (IOException e) {
            System.out.println("Trop de thread");
        }
    }

    public static void incrementNextId() {
        NEXT_ID++;
    }

}
