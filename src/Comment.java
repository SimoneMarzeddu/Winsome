public class Comment extends Feedback{
    private final String text;

    /**
     *	Costruttore della classe
     * 	@param autore autore del commento
     *  @param testo contenuto del commento
     */
    Comment (String autore, String testo) {
        super(autore);
        StringBuilder formatted_body = new StringBuilder("  ");
        int i = 0;
        int j = 50;
        while (j<testo.length())
        {
            formatted_body.append(testo, i, j).append("\n  ");
            i+=50;
            j+=50;
        }
        formatted_body.append(testo,i,testo.length());
        text = formatted_body.toString();
    }
    /**
     *	Metodo per trasformare un istanza di commento una stringa
     * 	@param //
     *  @return una stringa formattata rappresentante un'istanza di "Comment"
     */
    public String toString () {
        return "- <" + this.getAuthor() + ">  in data: " + this.getTimestamp().substring(0,10) + "\t ha commentato:\n" + text;
    }
}
