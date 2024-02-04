import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class User extends Thread implements Serializable {
    private final String name;
    private final List<User> followers = new ArrayList<>(); // users suivant this
    private final List<User> following = new ArrayList<>(); // users suivi par this
    private List<Tag> followedTags = new ArrayList<>();
    private final List<Message> postedMessage = new ArrayList<>();

    /***** Constructeur *****/
    public User(String name) {
        this.name = "@" + name;
    }



    /***** Connexion au serveur *****/
    public static boolean connect(String username) throws IOException, ClassNotFoundException {
        List<Object> header = List.of(Request.HEADER_REQUEST.CONNECT, username);
        Request request = new Request(header);

        // on se co au serv
        Socket socketClient = new Socket(InetAddress.getLocalHost(), 12345);
        //System.out.println("\n-- Connected to server");

        // Envoi d'un objet dans le clientHandler
        ObjectOutputStream towardsServer = new ObjectOutputStream(socketClient.getOutputStream());
        System.out.println("<- Sending connexion request");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socketClient.getInputStream());
        Response response = (Response) towardsClient.readObject();
        //response.printResponse();

        if (response.getHeader().get(0) == Response.HEADER_RESPONSE.OK) {
            System.out.println("-> Connexion accepted\n");
            towardsClient.close();
            towardsServer.close();
            socketClient.close();
            return true;
        } else {
            System.out.print("-> Connexion refused, ");
            response.printResponse();
            towardsClient.close();
            towardsServer.close();
            socketClient.close();
            return false;
        }

    }



    /***** Gestion des arguments donnés *****/
    public void handleArgs(String[] args) throws IOException, ClassNotFoundException {
        switch (args[0]) {
            case "publish" : checkPublishArgs(args); break;
            case "rcvIds" : checkReceiveIdsArgs(args); break;
            case "rcvMsg" : checkReceiveMsgArgs(args); break;
            case "reply" : checkReplyArgs(args); break;
            case "republish" : checkRepublishArgs(args); break;
            case "follow" : checkFollowArgs(args); break;
            case "unfollow" : checkUnfollowArgs(args); break;
            case "rcvUser" : checkReceiveUserArgs(args); break;
            case "followTag" : checkFollowTagArgs(args); break;
            case "unfollowTag" : checkUnfollowTagArgs(args); break;
            case "help" : printHelp(); break;
            case "exit" : System.exit(0); break;
            default : System.out.println("Incorrect arguments, enter 'help' for more info\n");
        }
    }

    private void checkPublishArgs(String[] args) throws IOException, ClassNotFoundException {
        if (args.length <= 1) {
            System.out.println("Incorrect arguments, USAGE -> publish [message]\n");
        } else {
            StringBuilder s = new StringBuilder();
            for (int i=1; i < args.length; i++) {
                s.append(args[i]);
                s.append(' ');
            }
            publish(s.toString());
        }
    }
    private void checkReceiveIdsArgs(String[] args) throws IOException, ClassNotFoundException {
        receiveMsgIDS();
    }
    public void checkReceiveMsgArgs(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 2) {
            System.out.println("Incorrect arguments, USAGE -> rcvMsg [id]\n");
        } else {
            receiveMessage(Long.parseLong(args[1]));
        }
    }
    private void checkReplyArgs(String[] args) throws IOException, ClassNotFoundException {
        if (args.length <= 2) {
            System.out.println("Incorrect arguments, USAGE -> reply [id] [message]\n");
        } else {
            StringBuilder s = new StringBuilder();
            for (int i=2; i < args.length; i++) {
                s.append(args[i]);
                s.append(' ');
            }
            reply(Long.parseLong(args[1]), s.toString());
        }
    }
    private void checkRepublishArgs(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 2) {
            System.out.println("Incorrect arguments, USAGE -> republish [id]\n");
        } else {
            republish(Long.parseLong(args[1]));
        }
    }
    private void checkReceiveUserArgs(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 2 || args[1].charAt(0) == '@') {
            System.out.println("Incorrect arguments, USAGE -> rcvUser [username without @]\n");
        } else {
            receiveUser(args[1]);
        }
    }
    private void checkFollowArgs(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 2) {
            System.out.println("Incorrect arguments, USAGE -> follow [user without @]\n");
        } else {
            follow(args[1]);
        }
    }
    private void checkUnfollowArgs(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 2) {
            System.out.println("Incorrect arguments, USAGE -> unfollow [user without @]\n");
        } else {
            unfollow(args[1]);
        }
    }
    private void checkFollowTagArgs(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 2) {
            System.out.println("Incorrect arguments, USAGE -> followTag [tag without #]\n");
        } else {
            followTag(args[1]);
        }
    }
    private void checkUnfollowTagArgs(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 2) {
            System.out.println("Incorrect arguments, USAGE -> unfollowTag [tag without #]\n");
        } else {
            unfollowTag(args[1]);
        }
    }



    /***** Toutes les commandes *****/
    // Affiche les commandes et ce qu'elles font
    private void printHelp() {
        System.out.println(
                """
                        Commands list:
                         - publish [message]    : Publishes the given message on the server
                         - rcvIds               : Receives the last 5 messages ids
                         - rcvMsg [id]          : Receives the message with the given id
                         - reply [id] [message] : Replies to the given message id with the given message
                         - republish [id]       : Republishes the message with the given id
                         - follow [user]        : Follows the given user
                         - unfollow [user]      : Unfollows the given user
                         - rcvUser [username]   : Receives info about the given username
                         - followTag [tag]      : Follows the given tag, creates it if not existing
                         - unfollowTag [tag]    : Unfollows the given tag
                         - exit                 : Quits the server and stops current session
                        """
        );
    }
    // Publie un message
    public void publish(String messageBody) throws IOException, ClassNotFoundException {
        // Creation de la requete
        List<Object> header = List.of(Request.HEADER_REQUEST.PUBLISH, this);
        Request request = new Request(header, new Message(this, messageBody));

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 12345);
        //System.out.println("Connected to server");

        // Envoi de la requete au serveur
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Sending publish request");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();
        //response.printResponse();

        if (response.getHeader().get(0) == Response.HEADER_RESPONSE.OK) {
            System.out.println("-> Message published\n");
        } else {
            System.out.print("-> Publication canceled, ");
            response.printResponse();
        }

        // Fermeture des cannaux
        towardsClient.close();
        towardsServer.close();
        socket.close();
    }
    // Recoit les ids des messages
    public void receiveMsgIDS() throws IOException, ClassNotFoundException {
        // Creation de la requete
        List<Object> header = List.of(Request.HEADER_REQUEST.RCV_IDS);
        Request request = new Request(header);

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 12345);
        //System.out.println("Connected to server");

        // Envoi de la requete au serveur
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Sending receive ids request");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();
        //response.printResponse();

        System.out.println("-> Ids list received: " + response.getMsgIds() + '\n');

        // Fermeture les cannaux
        //System.out.println("Disconnecting from server");
        towardsClient.close();
        towardsServer.close();
        socket.close();
    }
    // Recoit le message id si il existe
    public void receiveMessage(long id) throws IOException, ClassNotFoundException {
        // Creation de la requete
        List<Object> header = List.of(Request.HEADER_REQUEST.RCV_MSG, id);
        Request request = new Request(header);

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 12345);
        //System.out.println("Connected to server");

        // Envoi de la requete au serveur
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Sending receive message request");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();
        response.printResponse();

        // Fermeture les cannaux
        //System.out.println("Disconnecting from server");
        towardsClient.close();
        towardsServer.close();
        socket.close();
    }
    // Reponds a un message id
    public void reply(long idToReply, String message) throws IOException, ClassNotFoundException {
        // Creation de la requete
        List<Object> header = List.of(Request.HEADER_REQUEST.REPLY, this.name, idToReply);
        Request request = new Request(header, new Message(this,message));

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 12345);
        //System.out.println("Connected to server");

        // Envoi de la requete au serveur
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Sending reply request");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();
        //response.printResponse();

        if (response.getHeader().get(0) == Response.HEADER_RESPONSE.OK) {
            System.out.println("-> Message replied\n");
        } else {
            System.out.print("-> Reply canceled, ");
            response.printResponse();
        }

        // Fermeture les cannaux
        //System.out.println("Disconnecting from server");
        towardsClient.close();
        towardsServer.close();
        socket.close();
    }
    // Republie le message id
    public void republish(long messageId) throws IOException, ClassNotFoundException {
        // Creation de la requete
        List<Object> header = List.of(Request.HEADER_REQUEST.REPUBLISH, messageId);
        Request request = new Request(header);

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 12345);
        //System.out.println("Connected to server");

        // Envoi de la requete au serveur
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Sending republish request");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();

        if (response.getHeader().get(0) == Response.HEADER_RESPONSE.OK) {
            System.out.println("-> Message republished\n");
        } else {
            System.out.print("-> Republication canceled, ");
            response.printResponse();
        }

        // Fermeture les cannaux
        towardsClient.close();
        towardsServer.close();
        socket.close();
    }
    // Recoit les infos d'un utilisateur
    public void receiveUser(String user) throws IOException, ClassNotFoundException {
        // Creation de la requete
        List<Object> header = List.of(Request.HEADER_REQUEST.RCV_USER, user);
        Request request = new Request(header);

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 12345);
        //System.out.println("Connected to server");

        // Envoi de la requete au serveur
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Sending receive user info request");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();

        if (response.getHeader().get(0) == Response.HEADER_RESPONSE.OK) {
            printInfo((User)response.getHeader().get(1));
        } else {
            System.out.print("-> Receive user info canceled, ");
            response.printResponse();
        }


        // Fermeture les cannaux
        towardsClient.close();
        towardsServer.close();
        socket.close();
    }
    // S'abonne a un utilisateur
    public void follow(String toFollow) throws IOException, ClassNotFoundException {
        List<Object> header = List.of(Request.HEADER_REQUEST.SUBSCRIBE,this,toFollow);
        Request request = new Request(header);

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 12345);

        // Envoi de la requete au serveur
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Sending follow request");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();

        if (response.getHeader().get(0) == Response.HEADER_RESPONSE.OK) {
            System.out.println("-> Follow completed\n");
        } else {
            System.out.print("-> Follow canceled, ");
            response.printResponse();
        }
    }
    // Se desabonne d'un utilisateur
    public void unfollow(String toUnfollow) throws IOException, ClassNotFoundException {
        List<Object> header = List.of(Request.HEADER_REQUEST.UNSUBSCRIBE,this,toUnfollow);
        Request request = new Request(header);

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 12345);

        // Envoi de la requete au serveur
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Sending unfollow request");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();

        if (response.getHeader().get(0) == Response.HEADER_RESPONSE.OK) {
            System.out.println("-> Unfollow completed\n");
        } else {
            System.out.print("-> Unfollow canceled, ");
            response.printResponse();
        }

    }
    // S'abonne a un tag
    public void followTag(String tagToFollow) throws IOException, ClassNotFoundException {
        List<Object> header = List.of(Request.HEADER_REQUEST.SUBSCRIBE_TAG,this,tagToFollow);
        Request request = new Request(header);

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 12345);

        // Envoi de la requete au serveur
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Sending follow tag request");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();

        if (response.getHeader().get(0) == Response.HEADER_RESPONSE.OK) {
            System.out.println("-> Follow tag completed\n");
            setFollowedTags((List<Tag>) response.getHeader().get(1));
        } else {
            System.out.print("-> Follow tag canceled, ");
            response.printResponse();
        }
    }
    // Se desabonne d'un tag
    public void unfollowTag(String tagToUnfollow) throws IOException, ClassNotFoundException {
        List<Object> header = List.of(Request.HEADER_REQUEST.UNSUBSCRIBE_TAG,this,tagToUnfollow);
        Request request = new Request(header);

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 12345);

        // Envoi de la requete au serveur
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Sending unfollow tag request");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();

        if (response.getHeader().get(0) == Response.HEADER_RESPONSE.OK) {
            System.out.println("-> Unfollow tag completed\n");
            setFollowedTags((List<Tag>) response.getHeader().get(1));
        } else {
            System.out.print("-> Unfollow tag canceled, ");
            response.printResponse();
        }
    }



    /***** Methode pour la presentation *****/
    // Envoi ou non par l'intermediaire d'un publisher
    public void sendToPublisher(String message) throws IOException, ClassNotFoundException {
        // Creation de la requete
        List<Object> header = List.of(Request.HEADER_REQUEST.PUBLISH, this);
        Request request = new Request(header, new Message(this, message));

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 10000);

        // Envoi de la requete au publisher
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Sending message to publisher");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();

        if (response.getHeader().get(0) == Response.HEADER_RESPONSE.OK) {
            System.out.println("-> Requete accepté par le publisher\n");
        } else {
            System.out.print("-> Requete rejeté par le publisher\n");
        }

        // Fermeture des cannaux
        towardsClient.close();
        towardsServer.close();
        socket.close();
    }



    /***** Getters / Setters *****/
    public String getUsername() {
        return name;
    }
    public List<User> getFollowers() {
        return this.followers;
    }
    public List<User> getFollowing() {
        return this.following;
    }
    public List<Message> getPostedMessage() {
        return postedMessage;
    }
    public List<Tag> getFollowedTags() {
        return followedTags;
    }

    public void setFollowedTags(List<Tag> followedTags) {
        this.followedTags = followedTags;
    }


    @Override
    public String toString() {
        return name;
    }

    public void printInfo(User user) {
        System.out.println(
                "-> Username: " + user.name +
                        "\n   Followers: " + user.followers +
                        "\n   Followed users: " + user.following +
                        "\n   Followed Tags: "  + this.followedTags/* +
                        "\n   Posted messages: " + this.postedMessage*/ + '\n'

        );
    }


}

/*
    // On suppose que l'on ne peut pas changer n (il restera a 5) pour le moment
    public void receiveMsgIDS(List<String> optional) throws IOException, ClassNotFoundException {
        // Creation de la requete
        List<Object> header = List.of(Request.HEADER_REQUEST.RCV_IDS);
        header.addAll(optional);
        Request request = new Request(header);

        // Connexion au serveur
        Socket socket = new Socket(InetAddress.getLocalHost(), 12345);
        System.out.println("Connected to server");

        // Envoi de la requete au serveur
        ObjectOutputStream towardsServer = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("Sending request to server");
        towardsServer.writeObject(request);

        // Lecture de la reponse du server
        ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
        Response response = (Response) towardsClient.readObject();
        response.printResponse();

        // Fermeture les cannaux
        System.out.println("Disconnecting from server");
        towardsClient.close();
        towardsServer.close();
        socket.close();
    }

*/