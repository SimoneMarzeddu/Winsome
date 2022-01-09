import java.rmi.server.*;
import java.util.LinkedList;

public class ClientNotify extends RemoteObject implements ClientNotifyInterface {

    private final LinkedList<String> followers;
    //crea un nuovo callback client */
    /**
     *	Costruttore della classe
     * 	@param //
     */
    public ClientNotify( ){
        super();
        followers = new LinkedList<>();
    }
    /**
     *	Metodo utilizzato dal server per notificare la comparsa di un nuovo follower
     * 	@param username nome dell'utente follower
     *  @return //
     */
    public synchronized void someone_followed(String username){
        followers.add(username);
    }
    /**
     *	Metodo utilizzato dal Server per notificare la scomparsa di un follower
     * 	@param username nome dell'utente non più follower
     *  @return //
     */
    public synchronized void someone_unfollowed(String username){
        followers.remove(username);
    }
    /**
     *	Metodo utilizzato dal Server per far ottenere al Client uno stato consistente dei followers al momento del login
     * 	@param list_to_parse lista degli utenti attualmente followers dell'utente corrente
     *  @return //
     */
    public synchronized void consistent_state_Followers (String list_to_parse){
        if (list_to_parse.isEmpty()) return;
        String[] parsed_usernames = list_to_parse.split(" ");
        int i = 0;
        reset();
        while (i<parsed_usernames.length)
        {
            if (!followers.contains(parsed_usernames[i]))followers.add(parsed_usernames[i]);
            i++;
        }
    }
    /**
     *	Metodo utilizzato per l'ottenimento della lista dei followers dell'utente corrente
     * 	@param //
     *  @return una stringa formattata contenente i followers dell'utente corrente
     */
    public synchronized String list_follower() {
        if (followers.isEmpty()) return " Sembra che nessuno abbia ancora iniziato a seguirti :(\n Mettiti in gioco ed inizia a pubblicare qualche post sul tuo blog!";
        StringBuilder output = new StringBuilder();
        output.append(" Hai un totale di ").append(followers.size()).append(" followers!\n Ecco i loro nomi:");
        for (String user: followers) {
            output.append("\n * ").append(user);
        }
        return output.toString();
    }
    /**
     *	Metodo utilizzato per il reset della lista dei followers (utile quando cambia l'utente autenticato)
     * 	@param //
     *  @return //
     */
    public synchronized void reset()
    {
        followers.clear();
    }
}

