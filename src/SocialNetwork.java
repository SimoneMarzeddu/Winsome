import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SocialNetwork implements ServerRemoteInterface {

    private final ConcurrentHashMap<String,User> winsome_users;
    private final ConcurrentHashMap<String,Post>  winsome_posts;
    private final ConcurrentHashMap<String, ClientNotifyInterface> callbacks_registrations;
    private final int author_rewards_precentage;

    /**
     *	Metodo costruttore della classe
     * 	@param author_rewards_precentage percentuale sopettante all'autore di un dato Post ad ogni calcolo delle ricompense
     */
    SocialNetwork (int author_rewards_precentage) {
        super();
        winsome_users = new ConcurrentHashMap<>();
        winsome_posts = new ConcurrentHashMap<>();
        callbacks_registrations = new ConcurrentHashMap<>();
        this.author_rewards_precentage = author_rewards_precentage;
    }

    /**
     *	Metodo implementante l'operazione di registrazione di un utente
     * 	@param username username del richiedente
     * 	@param password password inserita dall'utente
     * 	@param lista_di_tag lista dei tags inseriti dall'utente
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    @Override
    public String register(String username, String password, String lista_di_tag){

        try {
            if (!winsome_users.containsKey(username)) {
                User new_user = new User(username,password,lista_di_tag);
                winsome_users.put(username,new_user);
                return " SUCCESSO: Registrazione completata";
            }
            return " ERRORE: Username gia' in uso";
        }
        catch (IllegalArgumentException ex) {
            return " ERRORE: Possono essere selezionati al massimo 5 tags";
        }
        catch (NoSuchAlgorithmException ex) {
            return " ERRORE";
        }
    }
    /**
     *	Metodo implementante l'operazione di registrazione di un Client al servizio di Callback
     * 	@param username username del richiedente
     * 	@param password password inserita dall'utente
     * 	@param clientInterface interfaccia per l'interazione remota con il Client
     * 	@return //
     */
    @Override
    public void registerForCallback(ClientNotifyInterface clientInterface, String username, String password) throws RemoteException {
        User tmp;
        try {
            if (username != null && (tmp=winsome_users.get(username))!=null && tmp.verify_identity(password) && !callbacks_registrations.containsKey(username)){
                callbacks_registrations.put(username,clientInterface);
                clientInterface.consistent_state_Followers(tmp.getFollowers_for_request());
            }
        }
        catch(NoSuchAlgorithmException ignored) {
        }
    }
    /**
     *	Metodo implementante l'operazione di deregistrazione di un Client al servizio di Callback
     * 	@param username username del richiedente
     * 	@param password password inserita dall'utente
     * 	@param clientInterface interfaccia per l'interazione remota con il Client
     * 	@return //
     */
    @Override
    public void unregisterForCallback(ClientNotifyInterface clientInterface, String username, String password){
        User tmp;
        try {
            if (username != null && (tmp=winsome_users.get(username))!=null && tmp.verify_identity(password) && callbacks_registrations.containsKey(username)) callbacks_registrations.remove(username,clientInterface);
        }
        catch(NoSuchAlgorithmException ignored) {
        }
    }
    /**
     *	Metodo implementante l'operazione di login
     * 	@param username username del richiedente
     *  @param password password digitata dall'utente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String login(String username, String password, String login_address) {
        try
        {
            if (username == null) return " ERRORE: Username non valido";
            User user = winsome_users.get(username);
            if (user == null) return " ERRORE: Username non valido";
            int try_login = user.login(password,login_address);
            if (try_login == 0) return " SUCCESSO: Login completato";
            if (try_login == -1) return " ERRORE: Password errata";
            else return " ERRORE: Sembra che tu sia gia' attivo su un altro dispositivo o su un'altra pagina :(\n Effettua il logout da questi per proseguire!";
        }
        catch(NullPointerException ex)
        {
            return " ERRORE: Username non valido";
        }
        catch (NoSuchAlgorithmException ex) {
            return " ERRORE";
        }
    }
    /**
     *	Metodo implementante l'operazione di logout
     * 	@param username username del richiedente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String logout(String username, String login_address) {
        try
        {
            User user = winsome_users.get(username);
            if (user == null) return " ERRORE: Username non valido";
            if (user.logout(login_address)) return " SUCCESSO: Logout completato";
            else return " ERRORE: Login necessario";
        }
        catch(NullPointerException ex)
        {
            ex.printStackTrace();
            return " ERRORE: Username non valido";
        }
    }
    /**
     *	Metodo implementante l'operazione per mostrare gli utenti affini al richiedente (registratisi con almeno un tag in comune con il richiedente)
     * 	@param username username del richiedente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String listUsers(String username, String login_address) {
        User user = winsome_users.get(username);
        if (user == null) return " ERRORE: Username non valido";
        if (user.not_authentic(login_address)) return " ERRORE: Login necessario";

        StringBuilder output = new StringBuilder();
        String[] tags = user.getTags();
        int i;
        int j;
        boolean found = false;

        for (User tmp_user : winsome_users.values() ) {

            if (tmp_user.getUsername().equals(username)) continue;
            String[] tmp_tags = tmp_user.getTags();
            i = 0;
            j = 0;

            while(!found && i< tags.length)
            {
                while(!found && j< tmp_tags.length)
                {
                    if (tags[i].equals(tmp_tags[j])) found = true;
                    j++;
                }
                i++;
                j = 0;
            }
            if (found) output.append(tmp_user.toString());
            found = false;
        }
        if (output.toString().isEmpty()) return " Sembra che i tuoi interessi siano unici nel loro genere!\n Ci dispiace, ma nessun utente di Winsome ha mai selezionato i tuoi stessi tags";
        return output.toString();
    }
    /**
     *	Metodo implementante l'operazione per mostrare gli utenti seguiti dall'utente corrente
     * 	@param username username del richiedente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String listFollowing(String username, String login_address) {
        User user = winsome_users.get(username);
        if (user == null) return " ERRORE: Username non valido";
        if (user.not_authentic(login_address)) return " ERRORE: Login necessario";

        return user.getFollowed_for_request();
    }
    /**
     *	Metodo per l'esecuzione di una callback verso il Client su cui è autenticato un utente verso cui è stato richiesto follow o unfollow
     * 	@param username_follower username dell'utente che ha richiesto l'operazione
     * 	@param username_followed username dell'utente che ha subito l'operazione
     *  @param follow true -> se si tratta di un follow, false se si tratta di un unfollow
     * 	@return //
     */
    private void callback (String username_follower, String username_followed, boolean follow) {
        if (callbacks_registrations.containsKey(username_followed)) {
            try {
                ClientNotifyInterface client = callbacks_registrations.get(username_followed);
                if (client != null) {
                    if (follow) client.someone_followed(username_follower);
                    else client.someone_unfollowed(username_follower);
                }

            } catch (RemoteException ignored) {
            }
        }
    }
    /**
     *	Metodo implementante l'operazione di follow
     * 	@param username_follower username del richiedente dell'operazione
     * 	@param username_followed username dell'utente verso cui l'operazione è indirizzata
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String followUser(String username_follower, String username_followed, String login_address) {
        User user_follower = winsome_users.get(username_follower);
        if (user_follower == null) return " ERRORE: Username non valido";
        if (user_follower.not_authentic(login_address)) return " ERRORE: Login necessario";
        if (!winsome_users.containsKey(username_followed)) return " ERRORE: L'utente che vorresti seguire non risulta iscritto a Winsome";
        User user_followed = winsome_users.get(username_followed);
        if (user_follower.follow_someone(user_followed)) {
            callback(username_follower,username_followed,true);
            return " SUCCESSO: L'utente fa ora parte degli utenti che segui";
        }
        return " ERRORE: Non puoi seguire un utente che fa parte dei tuoi seguiti, ne seguire te stesso";
    }
    /**
     *	Metodo implementante l'operazione di unfollow
     * 	@param username_follower username del richiedente dell'operazione
     * 	@param username_followed username dell'utente verso cui l'operazione è indirizzata
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String unfollowUser(String username_follower, String username_followed, String login_address) {
        User user_follower = winsome_users.get(username_follower);
        if (user_follower == null) return " ERRORE: Username non valido";
        if (user_follower.not_authentic(login_address)) return " ERRORE: Login necessario";

        if (!winsome_users.containsKey(username_followed)) return " ERRORE: L'utente che non vorresti seguire non risulta iscritto a Winsome";
        User user_followed = winsome_users.get(username_followed);
        if (user_follower.unfollow_someone(user_followed)) {
            callback(username_follower,username_followed,false);
            return " SUCCESSO: Adesso l'utente non fa parte degli utenti che segui";
        }
        return " ERRORE: Non puoi smettere di seguire un utente che non fa parte dei tuoi seguiti, ne te stesso";
    }
    /**
     *	Metodo per la condivisione di un post pubblicato da un dato utente verso tutti i suoi followers
     * 	@param user username del pubblicatore
     * 	@param tmp post da condividere
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    private void sharePost(User user, Post tmp) {
        winsome_posts.put(tmp.getId(),tmp);
        user.publishPost(tmp);
        String followers = user.getFollowers_for_request();
        if (followers.isEmpty()) return;
        String[] followers_to_reach = followers.split(" ");
        int i = 0;
        while (i<followers_to_reach.length){
            winsome_users.get(followers_to_reach[i]).updateFeed(tmp);
            i++;
        }
    }
    /**
     *	Metodo per la pubblicazione di un post su Winsome
     * 	@param username username del richiedente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@param title titolo del post
     * 	@param body contenuto del post
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String createPost(String username, String title, String body, String login_address) {
        if (username == null) return " ERRORE: Username non valido";
        if (title.length() > 20) return " ERRORE: Il titolo deve avere un massimo di 20 caratteri";
        if (body.length() > 500) return " ERRORE: Il corpo del post deve avere un massimo di 500 caratteri";
        User user = winsome_users.get(username);
        if (user == null) return " ERRORE: Username non valido";
        if (user.not_authentic(login_address)) return " ERRORE: Login necessario";
        Post tmp = new Post(username,title,body);
        sharePost(user, tmp);
        return " SUCCESSO: Il tuo post ha appena iniziato il sua viaggio su Winsome con l'ID: \"" + tmp.getId() + "\", Buona fortuna!";
    }
    /**
     *	Metodo implementante l'operazione di condivisione di un Post
     * 	@param username username del richiedente
     *  @param post_id id del Post
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String rewinPost(String username, String post_id, String login_address) {
        if (username == null) return " ERRORE: Username non valido";
        User user = winsome_users.get(username);
        if (user == null) return " ERRORE: Username non valido";
        if (user.not_authentic(login_address)) return " ERRORE: Login necessario";
        if (!winsome_posts.containsKey(post_id)) return " ERRORE: Ci dispiace, ma il post che hai richiesto non esiste";
        if (user.its_not_in_myFeed(post_id)) return " ERRORE: Puoi effettuare il rewin solo di post presenti sul tuo feed";
        Post existing_post = winsome_posts.get(post_id);
        sharePost(user, existing_post);
        return " SUCCESSO: Hai completato il rewin del post di " +existing_post.getAuthor()+ " su Winsome, Buona fortuna!";
    }
    /**
     *	Metodo implementante l'operazione "show feed" (richiesta da un dato utente per visualizzare una lista di tutti i post presenti sul suo feed)
     * 	@param username username del richiedente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String showFeed(String username, String login_address){
        if (username == null) return " ERRORE: Username non valido";
        User user = winsome_users.get(username);
        if (user == null) return " ERRORE: Username non valido";
        if (user.not_authentic(login_address)) return " ERRORE: Login necessario";
        return user.getFeed_for_request();
    }
    /**
     *	Metodo implementante l'operazione "blog" (richiesta da un dato utente per visualizzare una lista di tutti i post presenti sul suo blog)
     * 	@param username username del richiedente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String showBlog(String username, String login_address){
        if (username == null) return " ERRORE: Username non valido";
        User user = winsome_users.get(username);
        if (user == null) return " ERRORE: Username non valido";
        if (user.not_authentic(login_address)) return " ERRORE: Login necessario";
        return user.getBlog_for_request();
    }
    /**
     *	Metodo implementante l'operazione "show post" (richiesta da un dato utente per visualizzare un dato post)
     * 	@param username username del richiedente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     *  @param post_id id del Post
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String showPost(String username, String login_address, String post_id){
        if (username == null) return " ERRORE: Username non valido";
        User user = winsome_users.get(username);
        if (user == null) return " ERRORE: Username non valido";
        if (user.not_authentic(login_address)) return " ERRORE: Login necessario";
        Post tmp = winsome_posts.get(post_id);
        if (tmp == null) return " ERRORE: Ci dispiace, ma il post che hai richiesto non esiste";
        return tmp.getPost();
    }
    /**
     *	Metodo implementante l'operazione "delete" (richiesta da un dato utente per l'eliminazione di un post da lui pubblicato)
     * 	@param username username del richiedente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     *  @param post_id id del Post
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String deletePost(String username, String login_address, String post_id){
        if (username == null) return " ERRORE: Username non valido";
        User user = winsome_users.get(username);
        if (user == null) return " ERRORE: Username non valido";
        if (user.not_authentic(login_address)) return " ERRORE: Login necessario";
        if (!winsome_posts.containsKey(post_id)) return " ERRORE: Ci dispiace, ma il post che hai richiesto non esiste";
        if (!winsome_posts.get(post_id).getAuthor().equals(username)) return " ERRORE: Puoi rimuovere solo i post pubblicati da te";
        for (User u : winsome_users.values()) {
            u.someone_deletedPost(post_id); // è necessario chiamare il metodo su tutti gli utenti poichè potrebbero esserci state delle particolari catene di rewin
        }
        winsome_posts.remove(post_id);
        return " SUCCESSO: Il Post e' stato rimosso";
    }
    /**
     *	Metodo implementante l'operazione "rate" (richiesta da un dato utente per valutare un dato post)
     * 	@param username username del richiedente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     *  @param vote valutazione indicata dall'utente
     *  @param post_id id del Post
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String ratePost(String username, String post_id, String vote, String login_address) {
        if (username == null) return " ERRORE: Username non valido";
        User user = winsome_users.get(username);
        if (user == null) return " ERRORE: Username non valido";
        if (user.not_authentic(login_address)) return " ERRORE: Login necessario";
        if (!winsome_posts.containsKey(post_id)) return " ERRORE: Ci dispiace, ma il post che hai richiesto non esiste";
        if (user.its_not_in_myFeed(post_id)) return " ERRORE: Puoi valutare solo i post presenti sul tuo feed";
        Post tmp = winsome_posts.get(post_id);
        if (tmp.getAuthor().equals(username)) return " ERRORE: Non puoi valutare il rewin di un tuo stesso post";
        if (vote.equals("-1")){
            if (tmp.ratePost(username,false))return " SUCCESSO: Hai espresso il tuo giudizio negativo verso il post: \""+ post_id + "\"";
            else return " ERRORE: Hai gia' valutato questo post o hai provato a valutare un tuo post";
        }
        if (vote.equals("+1")){
            if (tmp.ratePost(username,true))return " SUCCESSO: Hai espresso il tuo giudizio positivo verso il post: \""+ post_id + "\"";
            else return " ERRORE: Hai gia' valutato questo post o hai provato a valutare un tuo post";
        }
        return " ERRORE: I valori selezionabili per la valutazione sono esclusivamente:\n +1 -> per una valutazione positiva\n -1 -> per una valutazione negativa";
    }
    /**
     *	Metodo implementante l'operazione "comment" (richiesta da un dato utente per commentare un dato post)
     * 	@param username username del richiedente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     *  @param comment contenuto del commento indicato dall'utente
     *  @param post_id id del Post
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String addComment(String username, String post_id, String comment, String login_address) {
        if (username == null) return " ERRORE: Username non valido";
        User user = winsome_users.get(username);
        if (user == null) return " ERRORE: Username non valido";
        if (user.not_authentic(login_address)) return " ERRORE: Login necessario";
        if (!winsome_posts.containsKey(post_id)) return " ERRORE: Ci dispiace, ma il post che hai richiesto non esiste";
        if (user.its_not_in_myFeed(post_id)) return " ERRORE: Puoi commentare solo i post presenti sul tuo feed";
        Post tmp = winsome_posts.get(post_id);
        if (tmp.getAuthor().equals(username)) return " ERRORE: Non puoi commentare il rewin di un tuo stesso post";
        if(tmp.addComment(username,comment))return " SUCCESSO: Hai commentato il post: \""+ post_id + "\"";
        return " ERRORE: Un commento deve avere un estensione massima di 200 caratteri";
    }
    /**
     *	Metodo implementante il calcolo delle ricompense relativo ad ogni post di Winsome
     * 	@param //
     * 	@return //
     */
    public void updateRewards () {
        User tmp_user;
        LinkedList<String> tmp_rewardable;
        double tmp_reward;
        double tmp_author_reward;
        for (Post p: winsome_posts.values()) {

            PostRewardsCalculationOutput raw_result = p.calculateRawRewards();
            tmp_reward = raw_result.getRaw_reward();
            tmp_author_reward = (tmp_reward*author_rewards_precentage/100);
            tmp_rewardable = raw_result.getRewardable_users();
            tmp_reward = (tmp_reward - tmp_author_reward)/(tmp_rewardable.size());

            tmp_user = winsome_users.get(p.getAuthor());
            tmp_user.updateWallet(tmp_author_reward,p.getId());

            for (String username : tmp_rewardable) {
                tmp_user = winsome_users.get(username);
                tmp_user.updateWallet(tmp_reward,p.getId());
            }
        }
    }
    /**
     *	Metodo implementante l'operazione "wallet" o "wallet btc" (richiesta da un dato utente per viualizzare il suo portafoglio ed una cronologia di transazioni)
     * 	@param username username del richiedente
     * 	@param login_address indirizzo remoto del socket del Client che ha richiesto l'operazione
     *  @param btc true -> l'utent desidera la conversione in bitcoins del suo wallet, false altrimenti
     * 	@return una stringa capace di esprimere l'esito dell'operazione
     */
    public String showWallet (String username, String login_address, boolean btc){
        if (username == null) return " ERRORE: Username non valido";
        User user = winsome_users.get(username);
        if (user == null) return " ERRORE: Username non valido";
        if (user.not_authentic(login_address)) return " ERRORE: Login necessario";
        if (btc) return user.getWallet_bitcoin_for_request();
        else return user.getWallet_for_request();
    }

    /**
     *	Metodo per il salvataggio di un backup di tutti i post di Winsome
     * 	@param filename file utilizzato per il backup
     * 	@return true -> se l'operazione ha avuto successo, false altrimenti
     */
    private boolean serializePosts (String filename){
        try(OutputStream out = new FileOutputStream(filename);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

            Gson gs = new GsonBuilder().setPrettyPrinting().create();
            writer.setIndent("    ");
            writer.beginArray();
            for (Post p : winsome_posts.values()) {
                writer.beginObject();
                writer.name("id_post").value(p.getId());
                writer.name("author").value(p.getAuthor());
                writer.name("title").value(p.getTitle());
                writer.name("body").value(p.getBody());
                writer.name("timestamp").value(p.getTimestamp());
                Type comments_hashtable_tt = new TypeToken<ConcurrentHashMap<String, LinkedList<Comment>>>() {
                }.getType();
                writer.name("comments").value(gs.toJson(p.getComments(), comments_hashtable_tt));
                Type feedbacks_hashtable_tt = new TypeToken<ConcurrentHashMap<String, Feedback>>() {
                }.getType();
                writer.name("likes").value(gs.toJson(p.getLikes(), feedbacks_hashtable_tt));
                writer.name("dislikes").value(gs.toJson(p.getDislikes(), feedbacks_hashtable_tt));
                PostRewardsState prs = p.getRewardsState();
                writer.name("rewcalcitnum").value(prs.getReward_calculation_iter_num());
                writer.name("resratesum").value(prs.getResettable_rates_sum());
                Type string_linkedlist_tt = new TypeToken<LinkedList<String>>() {
                }.getType();
                writer.name("rewusrs").value(gs.toJson(prs.getRewardable_users(),string_linkedlist_tt));
                writer.name("resnewcomm").value(gs.toJson(prs.getResettable_new_commentators(),string_linkedlist_tt));
                writer.endObject();
            }
                writer.endArray();
                return true;
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
        return false;
    }
    /**
     *	Metodo per il caricamento di un backup di tutti i post di Winsome
     * 	@param filename file utilizzato per il backup
     * 	@return true -> se l'operazione ha avuto successo, false altrimenti
     */
    private void deserializePosts (String filename){

        try(InputStream in = new FileInputStream(filename);
            JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            Gson gs = new GsonBuilder().setPrettyPrinting().create();
            Post tmp;
            String namenext;
            Type comments_hashtable_tt = new TypeToken<ConcurrentHashMap<String, LinkedList<Comment>>>() {
            }.getType();
            Type feedbacks_hashtable_tt = new TypeToken<ConcurrentHashMap<String, Feedback>>() {
            }.getType();
            Type string_linkedlist_tt = new TypeToken<LinkedList<String>>() {
            }.getType();

            reader.beginArray();
            while(reader.hasNext()){

                String id_post = null;
                String author = null;
                String timestamp = null;
                String title = null;
                String body = null;
                ConcurrentHashMap<String,Feedback> likes = null;
                ConcurrentHashMap<String,Feedback> dislikes = null;
                ConcurrentHashMap<String,LinkedList<Comment>> comments = new ConcurrentHashMap<>();
                int reward_calculation_iter_num = 0;
                int resettable_rates_sum = 0;
                LinkedList<String> rewardable_users = null;
                LinkedList<String> resettable_new_commentators = null;

                reader.beginObject();
                while(reader.hasNext()){
                    namenext = reader.nextName();
                    if(namenext.equals("id_post")) id_post = reader.nextString();
                    else
                        if(namenext.equals("author")) author = reader.nextString();
                        else
                            if(namenext.equals("title")) title = reader.nextString();
                            else
                                if(namenext.equals("body")) body = reader.nextString();
                                else
                                    if(namenext.equals("rewcalcitnum")) reward_calculation_iter_num = reader.nextInt();
                                    else
                                        if(namenext.equals("resratesum")) resettable_rates_sum = reader.nextInt();
                                        else
                                            if(namenext.equals("timestamp")) timestamp = reader.nextString();
                                            else
                                                if(namenext.equals("comments")) comments = gs.fromJson(reader.nextString(),comments_hashtable_tt);
                                                else
                                                    if(namenext.equals("likes")) likes = gs.fromJson(reader.nextString(),feedbacks_hashtable_tt);
                                                    else
                                                        if(namenext.equals("dislikes")) dislikes = gs.fromJson(reader.nextString(),feedbacks_hashtable_tt);
                                                        else
                                                            if(namenext.equals("rewusrs")) rewardable_users = gs.fromJson(reader.nextString(), string_linkedlist_tt);
                                                            else
                                                             if(namenext.equals("resnewcomm")) resettable_new_commentators = gs.fromJson(reader.nextString(), string_linkedlist_tt);
                                                             else reader.skipValue();
                }
                reader.endObject();

                tmp = new Post(id_post,author,timestamp,title,body,likes,dislikes,comments,reward_calculation_iter_num,resettable_rates_sum,rewardable_users,resettable_new_commentators);
                if (id_post != null) {
                    winsome_posts.put(id_post,tmp);
                }
            }
            reader.endArray();
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
    }
    /**
     *	Metodo per il salvataggio di un backup di tutti gli utenti di Winsome
     * 	@param filename file utilizzato per il backup
     * 	@return true -> se l'operazione ha avuto successo, false altrimenti
     */
    private boolean serializeUsers (String filename){
        try(OutputStream out = new FileOutputStream(filename);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

            Gson gs = new GsonBuilder().setPrettyPrinting().create();
            writer.setIndent("    ");
            writer.beginArray();
            for (User u : winsome_users.values()) {
                writer.beginObject();
                writer.name("hashpassvalue").value(u.getHashed_password_value());
                writer.name("seed").value(u.getSeed());
                writer.name("username").value(u.getUsername());
                writer.name("tags").value(gs.toJson(u.getTags(), String[].class));
                writer.name("transhistory").value(u.getTransaction_history());
                Type string_linkedlist_tt = new TypeToken<LinkedList<String>>() {
                }.getType();
                writer.name("followers").value(gs.toJson(u.getFollowers(), string_linkedlist_tt));
                writer.name("followed").value(gs.toJson(u.getFollowed(), string_linkedlist_tt));
                writer.name("feed").value(gs.toJson(u.getFeed(), string_linkedlist_tt));
                writer.name("blog").value(gs.toJson(u.getBlog(), string_linkedlist_tt));
                writer.name("wwcoin").value(u.getWallet_wincoin());
                writer.endObject();
            }
            writer.endArray();
            return true;
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
        return false;
    }
    /**
     *	Metodo per il caricamento di un backup di tutti gli utenti di Winsome
     * 	@param filename file utilizzato per il backup
     * 	@return true -> se l'operazione ha avuto successo, false altrimenti
     */
    private void deserializeUsers (String filename){

        try(InputStream in = new FileInputStream(filename);
            JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            Gson gs = new GsonBuilder().setPrettyPrinting().create();
            User tmp;
            String namenext;
            Type string_linkedlist_tt = new TypeToken<LinkedList<String>>() {
            }.getType();

            reader.beginArray();
            while(reader.hasNext()){

                String hash_pass = null;
                String seed = null;
                String username = null;
                String t_history = null;
                String[] tags = null;
                LinkedList<String> followers = new LinkedList<>();
                LinkedList<String> followed = new LinkedList<>();
                LinkedList<Post> feed = new LinkedList<>();
                LinkedList<Post> blog = new LinkedList<>();
                LinkedList<String> feed_to_extract = new LinkedList<>();
                LinkedList<String> blog_to_extract = new LinkedList<>();
                double wwcoin = 0;

                reader.beginObject();
                while(reader.hasNext()){
                    namenext = reader.nextName();
                    if(namenext.equals("hashpassvalue")) hash_pass = reader.nextString();
                    else
                    if(namenext.equals("seed")) seed = reader.nextString();
                    else
                    if(namenext.equals("username")) username = reader.nextString();
                    else
                    if(namenext.equals("tags")) tags = gs.fromJson(reader.nextString(),String[].class);
                    else
                    if(namenext.equals("transhistory")) t_history = reader.nextString();
                    else
                    if(namenext.equals("followers")) followers = gs.fromJson(reader.nextString(),string_linkedlist_tt);
                    else
                    if(namenext.equals("followed")) followed = gs.fromJson(reader.nextString(),string_linkedlist_tt);
                    else
                    if(namenext.equals("feed")) feed_to_extract = gs.fromJson(reader.nextString(),string_linkedlist_tt);
                    else
                    if(namenext.equals("blog")) blog_to_extract = gs.fromJson(reader.nextString(),string_linkedlist_tt);
                    else
                    if(namenext.equals("wwcoin")) wwcoin = reader.nextDouble();
                    else reader.skipValue();
                }
                reader.endObject();

                for (String pid: Objects.requireNonNull(feed_to_extract)) {
                    feed.add(winsome_posts.get(pid));
                }
                for (String pid: Objects.requireNonNull(blog_to_extract)) {
                    blog.add(winsome_posts.get(pid));
                }
                tmp = new User(hash_pass,seed,username,tags,followers,followed,feed,blog,t_history,wwcoin);
                winsome_users.put(username,tmp);
            }
            reader.endArray();
        }
        catch(IOException | NullPointerException ex){
            ex.printStackTrace();
        }
    }
    /**
     *	Metodo per il salvataggio di un backup di Winsome
     * 	@param posts_bck_filename file utilizzato per il backup dei posts
     * 	@param users_bck_filename file utilizzato per il backup degli utenti
     * 	@return true -> se l'operazione ha avuto successo, false altrimenti
     */
    public boolean serializeWinsome (String posts_bck_filename, String users_bck_filename){
        return (serializePosts(posts_bck_filename) && serializeUsers(users_bck_filename));
    }
    /**
     *	Metodo per il caricamento di un backup di Winsome
     * 	@param posts_bck_filename file utilizzato per il backup dei posts
     * 	@param users_bck_filename file utilizzato per il backup degli utenti
     * 	@return true -> se l'operazione ha avuto successo, false altrimenti
     */
    public void deserializeWinsome (String posts_bck_filename, String users_bck_filename){
        deserializePosts(posts_bck_filename);
        deserializeUsers(users_bck_filename);
    }

}
