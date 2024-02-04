import java.io.Serializable;
import java.util.List;

// Serializable Fait fonctionner le transfert de requetes (durant echange CLIENT/SERVER)
public class Request implements Serializable {
    public enum HEADER_REQUEST {
        PUBLISH, RCV_IDS, RCV_MSG, REPLY, REPUBLISH, SUBSCRIBE,
        UNSUBSCRIBE, CONNECT, RCV_USER, SUBSCRIBE_TAG, UNSUBSCRIBE_TAG
    }
    private final List<Object> header;
    private final Message body;


    /*************** CONSTRUCTEURS ***************/
    public Request(List<Object> header, Message message) {
        this.header = header;
        this.body = message;
    }
    public Request(List<Object> header) {
        this.header = header;
        this.body = null;
    }


    /***************** AFFICHAGE *****************/
    public void printRequest() {
        System.out.println("<- Request received: " + this.header.get(0));
    }



    /****************** GETTERS ******************/
    public List<Object> getHeader() {
        return header;
    }
    public Message getBody() {
        return body;
    }
}
