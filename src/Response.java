import java.io.Serializable;
import java.util.List;

// Serializable Fait fonctionner le transfert de reponses (durant echange SERVER/CLIENT)
public class Response implements Serializable {
    public enum HEADER_RESPONSE {OK, ERROR, MSG_IDS, MSG}
    private final List<Object> header;
    private final Message body;
    private final List<Long> msgIds;


    /*************** CONSTRUCTEURS ***************/
    public Response(List<Object> header, Message message) {
        this.header = header;
        this.body = message;
        this.msgIds = null;
    }
    public Response(List<Object> header) {
        this.header = header;
        this.body = null;
        this.msgIds = null;
    }
    public Response(List<Object> header, List<Long> msgIds) {
        this.header = header;
        this.body = null;
        this.msgIds = msgIds;
    }


    /***************** AFFICHAGE *****************/
    public void printResponse() {
        switch ((Response.HEADER_RESPONSE) this.header.get(0)) {
            case OK:
                printPublishResponse(); break;
            case ERROR:
                printError(); break;
            case MSG_IDS:
                printMsgIds(); break;
            case MSG:
                printMessage(); break;
        }
    }
    private void printPublishResponse() {
        System.out.println("Response received from server: "  + HEADER_RESPONSE.OK);
    }
    private void printError() {
        System.out.println(this.body.getContent() + '\n');
    }
    private void printMsgIds() {
        System.out.println("Response received from server: "  + HEADER_RESPONSE.MSG_IDS);
        for (Long msgIds: this.msgIds) {
            System.out.println("ID : " + msgIds);
        }
    }
    private void printMessage() {
        System.out.println("-> " + HEADER_RESPONSE.MSG +
                ", from: " + this.body.getAuthor().getUsername() +
                ", content: " + this.body.getContent() +
                ", length: " + this.body.getContent().length() +
                ", ID: " + this.body.getId() +
                ", replies: " + this.body.getReplies() + '\n'
        );
    }


    /****************** GETTERS ******************/
    public List<Object> getHeader() {
        return header;
    }
    public Message getBody() {
        return body;
    }
    public List<Long> getMsgIds() {
        return this.msgIds;
    }
}
