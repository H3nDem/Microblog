import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientHandler extends Thread {
    Socket socket;

    /***** Constructeur *****/
    public ClientHandler(Socket s){
        this.socket = s;
    }
    @Override
    public void run() {
        try {
            // Reception + affichage de la requete
            ObjectInputStream towardsClient = new ObjectInputStream(socket.getInputStream());
            Request args = (Request) towardsClient.readObject();
            args.printRequest();

            // Gestion de la requete
            switch ((Request.HEADER_REQUEST) args.getHeader().get(0)) {
                case PUBLISH -> sendPublishResponse(socket, args);
                case RCV_IDS -> sendMsgIDSList(socket, args);
                case RCV_MSG -> sendMsg(socket, args);
                case REPLY -> sendReplyResponse(socket, args);
                case REPUBLISH -> sendRepublishResponse(socket, args);
                case SUBSCRIBE -> sendFollowResponse(socket, args);
                case UNSUBSCRIBE -> sendUnfollowResponse(socket, args);
                case CONNECT -> sendConnectResponse(socket, args);
                case RCV_USER -> sendUserInfo(socket, args);
                case SUBSCRIBE_TAG -> sendFollowTagResponse(socket,args);
                case UNSUBSCRIBE_TAG -> sendUnfollowTagResponse(socket,args);
                default -> System.out.println("Type de requete non reconnu");
            }

            printServerState(); // Affichage de l'etat du server

            socket.close(); // Fermeture des sockets
            towardsClient.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    // Affiche les messages + utilisateurs stockes dans le server
    private void printServerState() {
        System.out.println("-- Users on the server: " + MicroblogCentral.USERS);
        System.out.println("-- Tags available: " + MicroblogCentral.TAGS);
        System.out.println("-- Messages on the server: ");
        for (Message m : MicroblogCentral.MSG_HISTORY) {
            System.out.println("    " + m.getContent());
        }


        System.out.println("\n-------------------------\n");
    }



    /***** Envoi de reponse au utilisateur *****/
    // Reponse a la connexion
    private void sendConnectResponse(Socket socket, Request request) throws IOException {
        if (isUserExist("@" + (String) request.getHeader().get(1))) { // Si l'user existe deja, renvoie erreur
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "username already exists"));
            System.out.println("-> Returning ERROR: Refuse connexion\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        } else { // sinon on le rajoute au server
            User newUser = new User((String) request.getHeader().get(1));
            MicroblogCentral.USERS.add(newUser);
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.OK, newUser.getUsername()));
            System.out.println("-> Returning OK: Accept connexion\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
    }
    // Reponse a la publication
    private void sendPublishResponse(Socket socket, Request request) throws IOException, ClassNotFoundException {
        if (request.getBody().getContent().length() > Message.MAX_MSG_SIZE) { // Si le message est trop long, erreur
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "messages can't be more than 256 chars"));
            System.out.println("-> Returning ERROR: Cancel publication\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        } else { // sinon c'est bon
            request.getBody().setID(MicroblogCentral.NEXT_ID); // set l'id, pour compenser synchronized ne fonctionnant pas
            MicroblogCentral.incrementNextId(); // Incremente l'id
            User user = getUser(((User) request.getHeader().get(1)).getUsername());
            user.getPostedMessage().add(request.getBody());// On ajoute le message comme etant posté pas l'utilisateur
            MicroblogCentral.MSG_HISTORY.add(request.getBody());

            // TODO : Commenter l'un lors de l'utilisation de l'autre sinon ça bloque
            //  Les 2 pour utiliser Publisher
            //  1ere ligne pour utiliser Repost
            //  2eme ligne pour utiliser Follower
//            notifyFollower(request.getBody());
//            notifyReposter(request.getBody());

            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.OK));
            System.out.println("-> Returning OK: Publish message\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
    }
    // Reponse a la reception d'ids de messages
    private void sendMsgIDSList(Socket socket, Request request) throws IOException {
        List<Long> messageIDS = new ArrayList<>();
        int n = 5;

        List<Message> reversed = new ArrayList<>(List.copyOf(MicroblogCentral.MSG_HISTORY));
        Collections.reverse(reversed);

        for (Message msg : reversed) {
            messageIDS.add(msg.getId()); // Ajoute dans l'ordre antechrnologique
            n--;
            if (n <= 0) {break;}
        }

        ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
        Response response = new Response(List.of(Response.HEADER_RESPONSE.MSG_IDS), messageIDS);
        System.out.println("-> Returning IDS list\n");
        towardsClient.writeObject(response);
        towardsClient.close();
    }
    // Reponse a la reception d'un message
    private void sendMsg(Socket socket, Request request) throws IOException {
        long id = (long) request.getHeader().get(1);
        if (!containMsgID(id)) { // Si le message a l'id "valeur de get(1)" n'existe pas
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "message n." + id + " doesn't exist"));
            System.out.println("-> Returning ERROR: Cancel reception\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        } else { // sinon c'est bon
            Message msg = getMessage(id);
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.MSG),msg);
            System.out.println("-> Returning OK: Send message\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
    }
    // Renvoie au client si sa reponse a un message est ok ou renvoie erreur sinon
    private void sendReplyResponse(Socket socket, Request request) throws IOException {
        if (request.getBody().getContent().length() > Message.MAX_MSG_SIZE) { // Si le message est trop long, erreur
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "messages can't be more than 256 chars"));
            System.out.println("-> Returning ERROR: Cancel reply\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
        else if (!containMsgID((long) request.getHeader().get(2))) { // Si le message auquel on souhaite repondre n'existe pas
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "message n." + (long) request.getHeader().get(2) + " doesn't exist"));
            System.out.println("-> Returning ERROR: Cancel reply\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
        else { // sinon c'est bon
            request.getBody().setID(MicroblogCentral.NEXT_ID); // set l'id, pour compenser synchronized ne fonctionnant pas
            MicroblogCentral.incrementNextId(); // Incremente l'id
            MicroblogCentral.MSG_HISTORY.add(request.getBody());
            getMessage((long) request.getHeader().get(2)).getReplies().add(request.getBody());
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.OK));
            System.out.println("-> Returning OK: Reply to message");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
    }
    // Republie le message d'un certain id
    private void sendRepublishResponse(Socket socket, Request request) throws IOException {
        if (!containMsgID((long) request.getHeader().get(1))) { // Si le message auquel on souhaite repondre n'existe pas
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "message n." + (long) request.getHeader().get(1) + " doesn't exist"));
            System.out.println("-> Returning ERROR: Cancel republication\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
        else { // sinon c'est bon
            MicroblogCentral.MSG_HISTORY.add(getMessage((long) request.getHeader().get(1)));
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.OK));
            System.out.println("-> Returning OK: Republish message\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
    }
    // Recupere les infos d'un utilisateur
    private void sendUserInfo(Socket socket, Request request) throws IOException {
        if (!isUserExist("@" + (String) request.getHeader().get(1))) { // Si l'user n'existe pas, renvoie erreur
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "user doesn't exist"));
            System.out.println("-> Returning ERROR: Cancel reception\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        } else { // sinon on le rajoute au server
            User user = getUser("@" + (String) request.getHeader().get(1));
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.OK, user));
            System.out.println("-> Returning OK: Send user info\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
    }
    // Renvoie a l'abonnement de l'utilisateur
    private void sendFollowResponse(Socket socket, Request request) throws IOException {
        if (isUserExist("@" + (String) request.getHeader().get(2))) { // Si l'utilisateur toFollow existe
            User toFollow = getUser("@" + ((String) request.getHeader().get(2)));
            User follower = getUser(((User) request.getHeader().get(1)).getUsername());
            if (toFollow.getUsername().equals(follower.getUsername())) { // Si l'ustilisateur s'abonne a lui meme, on l'empeche
                ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
                Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "you can't subscribe to yourself"));
                System.out.println("-> Returning ERROR: Cancel follow\n");
                towardsClient.writeObject(response);
                towardsClient.close();
            } else {
                if (toFollow.getFollowers().contains(follower)) { // Si l'utilisateur est déja abonné a l'autre
                    ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
                    Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "you can subscribe only once to someone"));
                    System.out.println("-> Returning ERROR: Cancel follow\n");
                    towardsClient.writeObject(response);
                    towardsClient.close();
                } else {
                    toFollow.getFollowers().add(follower); // ajoute l'user.get(2) au abonnement de l'user.get(1)
                    follower.getFollowing().add(toFollow); // ajoute le user.get(1) au followers de l'user.get(2)
                    ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
                    Response response = new Response(List.of(Response.HEADER_RESPONSE.OK));
                    System.out.println("-> Returning OK: Complete follow\n");
                    towardsClient.writeObject(response);
                    towardsClient.close();
                }

            }
        }
        else { // sinon on renvoie l'erreur
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "user doesn't exist"));
            System.out.println("-> Returning ERROR: Cancel follow\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
    }
    // Renvoie  au desabonnement de l'utilisateur
    private void sendUnfollowResponse(Socket socket, Request request) throws IOException {
        User toUnfollow = getUser("@" + ((String) request.getHeader().get(2)));
        User follower = getUser(((User) request.getHeader().get(1)).getUsername());

        if (toUnfollow.getFollowers().contains(follower)) { //

            //User toUnfollow = getUser(((User) request.getHeader().get(2)).getUsername()); // retire l'user.get(2) des abonnements de l'user.get(1)
            //User follower = getUser(((User) request.getHeader().get(1)).getUsername()); // retire le user.get(1) des followers de l'user.get(2)
            toUnfollow.getFollowers().remove(follower);
            follower.getFollowing().remove(toUnfollow);

            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.OK));
            System.out.println("-> Returning OK: Complete unfollow");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
        else{
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "you weren't following this user"));
            System.out.println("-> Returning ERROR: Cancel unfollow");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
    }




    // Renvoie a l'abonnement de tag
    private void sendFollowTagResponse(Socket socket, Request request) throws IOException {
        String tag = (String) request.getHeader().get(2);
        if (!isTagExist("#" + tag)) { // Si le tag n'existe pas on l'ajoute
            Tag newTag = new Tag(tag);
            MicroblogCentral.TAGS.add(newTag);
            User follower = getUser(((User) request.getHeader().get(1)).getUsername());

            follower.getFollowedTags().add(newTag);

            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.OK, follower.getFollowedTags()));
            System.out.println("-> Returning OK: Complete follow tag\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
        else {
            Tag existingTag = getTag("#" + tag);
            User follower = getUser(((User) request.getHeader().get(1)).getUsername());

            if (follower.getFollowedTags().contains(existingTag)) { // si il est deja abonné a un tag
                ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
                Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "you can subscribe only once to a tag"));
                System.out.println("-> Returning ERROR: Cancel follow tag\n");
                towardsClient.writeObject(response);
                towardsClient.close();
            } else {
                follower.getFollowedTags().add(existingTag);
                ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
                Response response = new Response(List.of(Response.HEADER_RESPONSE.OK, follower.getFollowedTags()));
                System.out.println("-> Returning OK: Complete follow tag\n");
                towardsClient.writeObject(response);
                towardsClient.close();
            }

        }
    }

    // Renvoie au desabonnement de tag
    private void sendUnfollowTagResponse(Socket socket, Request request) throws IOException {
        Tag toUnfollow = getTag("#" + ((String) request.getHeader().get(2)));
        User follower = getUser(((User) request.getHeader().get(1)).getUsername());
        if (follower.getFollowedTags().contains(toUnfollow)) {
            follower.getFollowedTags().remove(toUnfollow);
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.OK, follower.getFollowedTags()));
            System.out.println("-> Returning OK: Complete unfollow tag\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        } else {
            ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
            Response response = new Response(List.of(Response.HEADER_RESPONSE.ERROR), new Message(MicroblogCentral.SERVER, "you weren't following this tag"));
            System.out.println("-> Returning ERROR: Cancel unfollow tag\n");
            towardsClient.writeObject(response);
            towardsClient.close();
        }
    }



    /***** Methodes utiles *****/
    // Recupere un message stocké dans le serv a partir de son id
    private Message getMessage(long id) {
        for (Message msg : MicroblogCentral.MSG_HISTORY) {
            if (msg.getId() == id) return msg;
        }
        return null;
    }
    // voit si l'id d'un message existe bien dans la liste des messages du server
    private boolean containMsgID(long id) {
        for (Message msg : MicroblogCentral.MSG_HISTORY) {
            if (msg.getId() == id) return true;
        }
        return false;
    }
    // Permet de verifier a partir d'un nom si l'user existe
    private boolean isUserExist(String name) {
        for (User user : MicroblogCentral.USERS) {
            if (name.equals(user.getUsername())) {
                return true;
            }
        }
        return false;
    }
    // Recupère l'utilisateur a partir du nom
    private User getUser(String name) {
        for (User user : MicroblogCentral.USERS) {
            if (name.equals(user.getUsername())) {
                return user;
            }
        }
        return new User("ERROR");
    }
    // Previens le Follower lorsqu'un utilisateur qu'il suit afficher les details du message suivi
    public void notifyFollower(Message message) throws IOException, ClassNotFoundException {
        List<Object> header = List.of(Request.HEADER_REQUEST.PUBLISH, message);
        Request request = new Request(header); // Creation de la requete

        Socket socket = new Socket(InetAddress.getLocalHost(), 20000); // Connexion au Follower

        ObjectOutputStream towardsFollower = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Notify Follower");
        towardsFollower.writeObject(request); // Envoi de la requete au Follower

        towardsFollower.close();
        socket.close();
    }
    // Previens le Reposter lorsqu'un utilisateur qu'il suit poste un message sur le server
    public void notifyReposter(Message message) throws IOException, ClassNotFoundException {
        List<Object> header = List.of(Request.HEADER_REQUEST.REPUBLISH, message);
        Request request = new Request(header); // Creation de la requete

        Socket socket = new Socket(InetAddress.getLocalHost(), 30000); // Connexion au serveur

        ObjectOutputStream towardsReposter = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("<- Notify Repost");
        towardsReposter.writeObject(request); // Envoi de la requete au serveur

        towardsReposter.close();
        socket.close();
    }
    // Permet de verifier a partir d'un nom de tag si il existe
    private boolean isTagExist(String tag) {
        for (Tag t : MicroblogCentral.TAGS) {
            if (tag.equals(t.getTagName())) {
                return true;
            }
        }
        return false;
    }
    // Recupère le tag a partir de son nom
    private Tag getTag(String tag) {
        for (Tag t : MicroblogCentral.TAGS) {
            if (tag.equals(t.getTagName())) {
                return t;
            }
        }
        return new Tag("ERROR");
    }

}

/*
    // TODO : Implementer les critères (options) + ajoute le nb de id ajouté dans la liste a 5 par default
    private void sendMsgIDSList(Socket socket, Request request) throws IOException {
        List<Long> messageIDS = new ArrayList<>();
        int n = 5;

        List<Message> reversed = new ArrayList<>(List.copyOf(MicroblogCentral.MSG_HISTORY));
        Collections.reverse(reversed);

        for (Message msg : reversed) {
            messageIDS.add(msg.getId()); // Ajoute dans l'ordre antechrnologique
            n--;
            if (n <= 0) {break;}
        }

        ObjectOutputStream towardsClient = new ObjectOutputStream(socket.getOutputStream());
        Response response = new Response(List.of(Response.HEADER_RESPONSE.MSG_IDS), messageIDS);
        System.out.println("-> Returning IDS list\n");
        towardsClient.writeObject(response);
        towardsClient.close();

        // Variable optionnels
        String author = "";
        //String tag = "";
        int n = 5;
        // TODO : implementer le SINCE_ID et TAG

        if (header.size() > 1) {
            int i = 1;
            while (i != header.size()) {
                String opt = (String) header.get(i);
                if (opt.charAt(0) == '@') {author = opt; i++;}
                //if (opt.charAt(0) == '#') {tag = opt; i++;}
            }
        }

        // TODO : faire ça dans methode appart avec eventuel switch case
        if (author.equals("") && tag.equals("")) { // Si aucune valeur optionnel
            for (Message msg : MSG_HISTORY) {
                messageIDS.add(0, msg.getId());
            }
        } else if (!author.equals("") && tag.equals("")) { // On recupere les message d'un user precis
            for (Message msg : MSG_HISTORY) {
                if (msg.getAuthor().getName().equals(author)) {
                    messageIDS.add(0, msg.getId());
                }
            }
        }

    }


*/