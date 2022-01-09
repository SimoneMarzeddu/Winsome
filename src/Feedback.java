import java.time.*;

public class Feedback {
    private final String author;
    private final String timestamp;

    /**
     *	Costruttore della classe
     * 	@param autore autore del feedback
     */
    Feedback (String autore)
    {
        author = autore;
        timestamp = Instant.now().toString();
    }
    /**
     *	Metodo utilizzato per l'ottenimento del nominativo dell'autore del feedback
     * 	@param //
     *  @return una stringa con valore equivalente all'autore del feedback
     */
    public String getAuthor () {
        return author;
    }
    /**
     *	Metodo utilizzato per l'ottenimento del timestamp di pubblicazione del feedback
     * 	@param //
     *  @return una stringa con valore rappresentante il timestamp del feedback
     */
    public String getTimestamp () {
        return timestamp;
    }
}
