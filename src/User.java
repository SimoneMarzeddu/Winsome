import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class User {

    private final String hashed_password_value;
    private final String seed;
    private final String username;
    private final String[] tags;
    private String login_address;

    private final LinkedList<String> followers;
    private final LinkedList<String> followed;
    private final ConcurrentHashMap<String,Post> feed;
    private final ConcurrentHashMap<String,Post> blog;

    private final Lock wallet_lock;
    private String transaction_history;
    private double wallet_wincoin;

    /**
     *	Metodo costruttore della classe
     * 	@param username username selezionato dall'utente
     * 	@param password password selezionata dall'utente
     * 	@param tags_string tags selezionati dall'utente
     */
    public User (String username, String password, String tags_string) throws NoSuchAlgorithmException, IllegalArgumentException {
        tags_string = tags_string.toLowerCase();
        String[] tmp_tags = tags_string.split(" ");
        if (tmp_tags.length > 5) throw new IllegalArgumentException("Non e' possibile selezionare un numero di tags superiore a 5");
        tags = tmp_tags;
        byte[] array = new byte[32];
        ThreadLocalRandom.current().nextBytes(array);
        seed = new String(array, StandardCharsets.UTF_8);
        hashed_password_value = Hash.bytesToHex(Hash.sha256(password+seed));
        this.username = username;
        followers = new LinkedList<>();
        followed = new LinkedList<>();
        feed = new ConcurrentHashMap<>();
        blog = new ConcurrentHashMap<>();
        wallet_lock = new ReentrantLock();
        transaction_history = "";
        wallet_wincoin = 0;
        login_address = null;
    }
    /**
     *	Metodo costruttore della classe
     * 	@param username username selezionato dall'utente
     * 	@param hashed_password risultato dell'hashing di (password selezionata dall'utente in fase di registrazione + seed)
     * 	@param tags tags selezionati dall'utente
     * 	@param seed seed generato alla registrazione dell'utente
     * 	@param followers lista degli usernames di utenti che seguono l'utente
     * 	@param followed lista degli usernames di utenti seguiti dall'utente
     * 	@param feed feed dell'utente
     * 	@param blog blog dell'utente
     * 	@param transaction_history cronologia dei guadagni dell'utente
     * 	@param wallet_wincoin portafogli dell'utente
     */
    public User (String hashed_password, String seed, String username, String[] tags, LinkedList<String> followers, LinkedList<String> followed, LinkedList<Post> feed, LinkedList<Post> blog, String transaction_history, double wallet_wincoin ){
        hashed_password_value = hashed_password;
        this.seed = seed;
        this.username = username;
        this.tags = tags;
        this.followers = followers;
        this.followed = followed;
        this.feed = new ConcurrentHashMap<>();
        this.blog = new ConcurrentHashMap<>();
        for (Post p: feed) {
            this.feed.put(p.getId(),p);
        }
        for (Post p: blog) {
            this.blog.put(p.getId(),p);
        }
        this.transaction_history = transaction_history;
        this.wallet_wincoin = wallet_wincoin;
        wallet_lock = new ReentrantLock();
        login_address = null;
    }
    /**
     *	Metodo utile alla verifica delle credenziali fornite da un utente
     *  @param password password inserita dall'utente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return 0 -> successo, -1 -> password sbagliata, -2 -> l'utente è connesso in un altro processo Client
     */
    public synchronized int login (String password, String login_address) throws NoSuchAlgorithmException {
        if (hashed_password_value.compareTo(Hash.bytesToHex(Hash.sha256(password + seed))) == 0) {
            if (this.login_address != null) return -2;
            this.login_address = login_address;
            return 0;
        }
        return -1;
    }
    /**
     *	Metodo utile al logout dell'utente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return 0 -> successo, -1 -> password sbagliata, -2 -> l'utente è connesso in un altro processo Client
     */
    public synchronized boolean logout (String login_address) {
        if (this.not_authentic(login_address)) return false;
        this.login_address = null;
        return true;
    }
    /**
     *	Metodo utile all'autenticazione di un utente
     *  @param password password inserita dall'utente
     * 	@return true -> successo, false -> fallimento
     */
    public boolean verify_identity (String password) throws NoSuchAlgorithmException {
        return hashed_password_value.compareTo(Hash.bytesToHex(Hash.sha256(password + seed))) == 0;
    }
    /**
     *	Metodo utile all'autenticazione del Client da cui un utente è connesso
     *  @param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return true -> successo, false -> fallimento
     */
    public synchronized boolean not_authentic(String login_address) {
        if (login_address == null || this.login_address == null) return true;
        return !this.login_address.equals(login_address);
    }
    /**
     *	Metodo invocato quanto l'utente corrente ha seguito un utente
     *  @param followed_user username dell'utente seguito
     * 	@return true -> successo, false -> fallimento
     */
    public boolean follow_someone (User followed_user) {
        synchronized (followed){
            if (followed.contains(followed_user.username) || followed_user.username.equals(this.username)) return false;
            followed.add(followed_user.username);
        }
        followed_user.followed_by_someone(this);
        return true;
    }
    /**
     *	Metodo invocato quanto l'utente corrente ha smesso di seguire un utente
     *  @param followed_user username dell'utente seguito
     * 	@return true -> successo, false -> fallimento
     */
    public boolean unfollow_someone (User followed_user) {
        synchronized (followed){
            if (!followed.contains(followed_user.username) || followed_user.username.equals(this.username)) return false;
            followed.remove(followed_user.username);
        }
        followed_user.unfollowed_by_someone(this);
        return true;
    }
    /**
     *	Metodo invocato quanto l'utene corrente è stato seguito da un utente
     *  @param follower_user username dell'utente follower
     * 	@return //
     */
    private void followed_by_someone (User follower_user) {
        synchronized (followers){
            if (followers.contains(follower_user.username) || follower_user.username.equals(this.username)) return;
            followers.add(follower_user.username);
        }
        follower_user.feed.putAll(blog);
    }
    /**
     *	Metodo invocato quanto l'utene corrente non è più seguito da un su followers
     *  @param follower_user username dell'utente non più follower
     * 	@return //
     */
    private void unfollowed_by_someone (User follower_user) {
        synchronized (followers){
            if (!followers.contains(follower_user.username) || follower_user.username.equals(this.username)) return;
            followers.remove(follower_user.username);
        }
        for (Post p : blog.values()) {
            follower_user.feed.remove(p.getId());
        }
    }
    /**
     *	Metodo per la pubblicazione di un post sul blog dell'utente corrente
     *  @param post post pubblicato
     * 	@return //
     */
    public void publishPost (Post post){
        blog.put(post.getId(),post);
    }
    /**
     *	Metodo che controlla la non presenza di un post sul feed dell'utente corrente
     *  @param post_id  id del post ricercato
     * 	@return true -> il post non è presente sul feed, false altrimenti
     */
    public boolean its_not_in_myFeed(String post_id) {
        return !feed.containsKey(post_id);
    }
    /**
     *	Metodo utile all'aggiornamento del feed dell'utente corrente in seguito alla cancellazione di un post
     *  @param post_id id del post eliminato
     * 	@return //
     */
    public void someone_deletedPost (String post_id) {
        feed.remove(post_id);
        blog.remove(post_id);
    }
    /**
     *	Metodo utile all'aggiornamento del feed dell'utente corrente in seguito alla pubblicazione di un post da parte di un utente seguito
     *  @param post post pubblicato
     * 	@return //
     *
     */
    public void updateFeed (Post post){
        feed.put(post.getId(),post);
    }
    /**
     *	Metodo utile all'aggiornamento del wallet e cronologia dei quadagni dell'utente corrente
     *  @param wincoin somma di wincoin costituente la ricompensa
     *  @param id_post post da cui è stata guadagnata la ricompensa
     * 	@return //
     *
     */
    public void updateWallet (double wincoin, String id_post){
        if (wincoin>0){
            try{
                wallet_lock.lock();
                wallet_wincoin+=wincoin;
                transaction_history = " - " + Instant.now().toString().substring(0,19) + " - Hai guadagnato : " + wincoin + " wincoins dal post: "+ "<" + id_post + ">\n" + transaction_history ;
            }
            finally {
                wallet_lock.unlock();
            }
        }
    }
    /**
     *	Metodo per l'ottenimento del valore di hashed_password_value
     * 	@param //
     *  @return una stringa costituente il valore di hashed_password_value
     */
    public String getHashed_password_value () {
        return hashed_password_value;
    }
    /**
     *	Metodo per l'ottenimento del valore di seed
     * 	@param //
     *  @return una stringa costituente il valore di seed
     */
    public String getSeed () {
        return seed;
    }
    /**
     *	Metodo per l'ottenimento dell'username dell'utente corrente
     * 	@param //
     *  @return una stringa costituente l'username dell'utente corrente
     */
    public String getUsername () {
        return username;
    }
    /**
     *	Metodo per l'ottenimento dei tags impostati dall'utente corrente
     * 	@param //
     *  @return un array di stringhe rappresentanti i tags selezionati dall'utente corrente
     */
    public String[] getTags () {
        return tags.clone();
    }
    /**
     *	Metodo per l'ottenimento della collezione di usernames di utenti seguaci dell'utente corrente
     * 	@param //
     *  @return un' istanza di LinkedList contenente gli usernames di utenti seguaci dell'utente corrente
     */
    public LinkedList<String> getFollowers (){
        return followers; // non clonato poiche' e' utilizzato esclusivamente lato server
    }
    /**
     *	Metodo per l'ottenimento della collezione di usernames di utenti seguiti dall'utente corrente
     * 	@param //
     *  @return un' istanza di LinkedList contenente gli usernames di utenti seguiti dall'utente corrente
     */
    public LinkedList<String> getFollowed (){
        return followed; // non clonato poiche' e' utilizzato esclusivamente lato server
    }
    /**
     *	Metodo per l'ottenimento della collezione di post contenuti dal feed dell'utente
     * 	@param //
     *  @return un' istanza di LinkedList contenente i post contenuti dal feed dell'utente
     */
    public LinkedList<String> getFeed (){
        return new LinkedList<>(feed.keySet());
    }
    /**
     *	Metodo per l'ottenimento della collezione di post contenuti dal blog dell'utente
     * 	@param //
     *  @return un' istanza di LinkedList contenente i post contenuti dal blog dell'utente
     */
    public LinkedList<String> getBlog (){
        return new LinkedList<>(blog.keySet());
    }
    /**
     *	Metodo per l'ottenimento della cronologia dei guadagni dell'utente corrente
     * 	@param //
     *  @return la cronologia dei guadagni dell'utente corrente
     */
    public String getTransaction_history () {
        return transaction_history;}
    /**
     *	Metodo per l'ottenimento del valore del portafoglio in Wincoins dell'utente corrente
     * 	@param //
     *  @return il valore del portafoglio in Wincoins dell'utente corrente
     */
    public double getWallet_wincoin () {
        return wallet_wincoin;}

    /**
     *	Metodo per l'ottenimento della collezione di usernames di utenti seguiti dall'utente corrente
     * 	@param //
     *  @return una stringa rappresentante gli usernames di utenti seguiti dall'utente corrente
     */
    public String getFollowed_for_request() {
        StringBuilder out = new StringBuilder();
        synchronized (followed){
            if (followed.isEmpty()) return " Sembra che tu non abbia ancora iniziato a seguire nessuno...\n Prova il comando \"list users\" per scoprire gli utenti affini a te!";
            out.append(" Segui un totale di ").append(followed.size()).append(" utenti!\n Ecco i loro nomi:");
            for (String user: followed) {
                out.append("\n * ").append(user);
            }
        }
        return out.deleteCharAt(0).toString();
    }
    /**
     *	Metodo per l'ottenimento della collezione di usernames di utenti seguaci dell'utente corrente
     * 	@param //
     *  @return una stringa rappresentante gli usernames di utenti seguaci dell'utente corrente
     */
    public String getFollowers_for_request() {
        StringBuilder out = new StringBuilder();
        synchronized (followers){
            if (followers.isEmpty()) return "";
            for (String user: followers) {
                out.append(user).append(" ");
            }
        }
        return out.toString();
    }
    /**
     *	Metodo per l'ottenimento della collezione di post contenuti dal feed dell'utente
     * 	@param //
     *  @return una stringa rappresentante i post contenuti dal feed dell'utente
     */
    public String getFeed_for_request() {
        if (feed.isEmpty()) return " Purtroppo sembra che ci sia poco movimento da queste parti :(\n Segui qualche utente interessante e visualizzerai qui i suoi post!";
        StringBuilder out = new StringBuilder();
        for (Post post: feed.values()) {
            out.append("\n").append(post.toString());
        }
        return out.deleteCharAt(0).toString();
    }
    /**
     *	Metodo per l'ottenimento della collezione di post contenuti dal blog dell'utente
     * 	@param //
     *  @return una stringa rappresentante i post contenuti dal blog dell'utente
     */
    public String getBlog_for_request() {
        if (blog.isEmpty()) return " Non hai ancora pubblicato alcun post, abbandona la timidezza! ;)\n Un mare di utenti la fuori vorrebbero sapere quel che hai da dire!";
        StringBuilder out = new StringBuilder();
        for (Post post: blog.values()) {
            out.append("\n").append(post.toString());
        }
        return out.deleteCharAt(0).toString();
    }
    /**
     *	Metodo per l'ottenimento della conversione in Bitcoins dell'ammontare di Wincoins posseduti dall'utente
     * 	@param //
     *  @return la conversione in Bitcoins dell'ammontare di Wincoins posseduti dall'utente
     */
    private double toBitcoin (){
        try{
            URL random_dot_org = new URL("https://www.random.org/decimal-fractions/?num=1&dec=10&col=2&format=plain&rnd=new");
            InputStream is = random_dot_org.openStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

            String random_value = bufferedReader.readLine();
            bufferedReader.close();
            is.close();

            double bitcoins;
            try{
                wallet_lock.lock();
                bitcoins = wallet_wincoin * Double.parseDouble(random_value);
            }
            finally {
                wallet_lock.unlock();
            }

            return bitcoins;
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
        return 0;
    }
    /**
     *	Metodo per l'ottenimento del valore del portafoglio in Bitcoins dell'utente corrente
     * 	@param //
     *  @return il valore del portafoglio in Bitcoins dell'utente corrente e la sua cronologia dei guadagni (in Wincoins)
     */
    public String getWallet_bitcoin_for_request(){
        try{
            wallet_lock.lock();
            if (wallet_wincoin == 0) return " Sembra che il tuo portafoglio sia vuoto, ma non arrenderti!";
            return " Possiedi un totale di " + toBitcoin() + " Bitcoins!\n\n Ecco un riassunto dei tuoi ultimi guadagni :\n" + transaction_history;
        }
        finally {
            wallet_lock.unlock();
        }
    }
    /**
     *	Metodo per l'ottenimento del valore del portafoglio in Wincoins dell'utente corrente
     * 	@param //
     *  @return il valore del portafoglio in Wincoins dell'utente corrente e la sua cronologia dei guadagni
     */
    public String getWallet_for_request(){
        try{
            wallet_lock.lock();
            if (wallet_wincoin == 0) return " Sembra che il tuo portafoglio sia vuoto, ma non arrenderti!";
            return " Possiedi un totale di " + wallet_wincoin + " Wincoins!\n\n Ecco un riassunto dei tuoi ultimi guadagni:\n" + transaction_history;
        }
        finally {
            wallet_lock.unlock();
        }
    }
    /**
     *	Metodo per l'ottenimento di una stringa rappresetnate l'istanza di User
     * 	@param //
     *  @return una stringa rappresetnate l'istanza di User
     */
    public String toString () {
        int followers_size;
        int followed_size;
        synchronized (followers){
            followers_size = followers.size();
        }
        synchronized (followed){
            followed_size = followed.size();
        }
        return "\n*\t" + username + "\n*\t" + Arrays.toString(tags)  + "\n*\tfollowers: " + followers_size + "\n*\tfollowed: " + followed_size + "\n*\tpost history: " + blog.size() + "\n";
    }
}
