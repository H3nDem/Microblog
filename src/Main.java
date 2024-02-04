import java.io.IOException;
import java.util.Scanner;

// Classe utilisé pour simuler les utilisateurs
public class Main extends Thread {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        boolean isConnected = false;
        String username = null;
        String[] arguments;

        while (!isConnected) { // Connexion
            Scanner input = new Scanner(System.in);
            System.out.print("Choose a username: ");
            username = input.nextLine();
            isConnected = User.connect(username);
        }

        User user = new User(username);

        while (true) { // Entree de commandes
            Scanner input2 = new Scanner(System.in);
            System.out.print("Enter command: ");
            String arg0 = input2.nextLine();
            arguments = arg0.split(" ");

            user.handleArgs(arguments);
        }
    }


}
