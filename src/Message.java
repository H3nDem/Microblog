import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// TODO : syncroniser les next_ID pour eviter de revenir a 0 avec un autre user
public class Message extends Thread implements Serializable {
    private final User author;
    private final String message;
    private long ID; // Tous messages ont un ID unique
    public static final int MAX_MSG_SIZE = 256;
    private final List<Message> replies = new ArrayList<>();
    // private final List<Tag> tags = new ArrayList<>();

    /**************** CONSTRUCTEUR ***************/
    public Message(User author, String message) {
        this.author = author;
        this.message = message;
    }


    /****************** GETTERS ******************/
    public User getAuthor() {
        return this.author;
    }
    public String getContent() {
        return this.message;
    }
    public long getId() {
        return this.ID;
    }
    public List<Message> getReplies() {
        return this.replies;
    }

    public void setID(long ID) {
        this.ID = ID;
    }

    @Override
    public String toString() {
        return "[Author: " + author +
                ", message: " + message +
                ", ID: " + ID + "]";
    }
}
