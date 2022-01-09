import java.time.Instant;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.lang.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Post {
    private final String id_post;
    private final String author;
    private final String timestamp;
    private final String title;
    private final String body;
    private final ConcurrentHashMap<String,Feedback> likes;
    private final ConcurrentHashMap<String,Feedback> dislikes;
    private final ConcurrentHashMap<String,LinkedList<Comment>> comments;

    private final Lock rewards_calculation_lock;
    private int reward_calculation_iter_num;
    private int resettable_rates_sum;
    private final LinkedList<String> rewardable_users;
    private final LinkedList<String> resettable_new_commentators;

    /**
     *	Metodo costruttore della classe
     * 	@param autore username dell'autore del Post
     *  @param titolo titolo del Post
     * 	@param contenuto contenuto del Post
     */
    Post (String autore, String titolo, String contenuto) {
        author = autore;
        Instant tmp_time = Instant.now();
        timestamp = tmp_time.toString();
        id_post = author + ThreadLocalRandom.current().nextInt(500,999) + tmp_time.toEpochMilli();
        likes = new ConcurrentHashMap<>();
        dislikes = new ConcurrentHashMap<>();
        comments = new ConcurrentHashMap<>();
        StringBuilder formatted_body = new StringBuilder("  ");
        int i = 0;
        int j = 50;
        while (j<contenuto.length())
        {
            formatted_body.append(contenuto, i, j).append("\n  ");
            i = i + 50;
            j = j + 50;
        }
        formatted_body.append(contenuto,i,contenuto.length());
        title = titolo;
        body = formatted_body.toString();

        reward_calculation_iter_num = 0;
        resettable_rates_sum = 0;
        resettable_new_commentators = new LinkedList<>();
        rewardable_users = new LinkedList<>();
        rewards_calculation_lock = new ReentrantLock();
    }
    /**
     *	Metodo costruttore alternativo della classe (utile per ricostruire un post da backup)
     *  @param id id del post
     * 	@param athr username dell'autore del Post
     *  @param ttl titolo del Post
     * 	@param tmstmp timestamp del Post
     * 	@param athr username dell'autore del Post
     *  @param lks likes ricevuti dal Post
     * 	@param dslks dislikes ricevuti dal Post
     *  @param cmmnts commenti ricevuti dal Post
     *  @param rcin numero di iterazioni di calcolo delle ricompense subite dal Post
     * 	@param rrs variabile ausiliaria per il calcolo delle ricompense
     * 	@param ru variabile ausiliaria per il colcolo delle ricompense
     *  @param rnc variabile ausiliaria per il colcolo delle ricompense
     */
    Post (String id, String athr, String tmstmp, String ttl, String cnt, ConcurrentHashMap<String,Feedback> lks, ConcurrentHashMap<String,Feedback> dslks, ConcurrentHashMap<String,LinkedList<Comment>> cmmnts, int rcin, int rrs, LinkedList<String> ru, LinkedList<String> rnc ) {
        author = athr;
        timestamp = tmstmp;
        id_post = id;
        likes = lks;
        dislikes = dslks;
        comments = cmmnts;
        title = ttl;
        body = cnt;
        reward_calculation_iter_num = rcin;
        resettable_rates_sum = rrs;
        resettable_new_commentators = rnc;
        rewardable_users = ru;
        rewards_calculation_lock = new ReentrantLock();
    }
    /**
     *	Metodo getter per l'ID del Post
     * 	@param //
     *  @return una stringa rappresentante l'ID del Post
     */
    public String getId (){
        return id_post;
    }
    /**
     *	Metodo per l'ottenimento di una stringa formattata rappresentante il Post
     * 	@param //
     *  @return una stringa rappresentante il Post
     */
    public String getPost () {
        StringBuilder output = new StringBuilder("* Titolo: <" + title + ">\n  Data di pubblicazione: " + timestamp.substring(0,10) + " alle ore: " + timestamp.substring(11,19)  + "\n  Valutazioni Positive: " + likes.size() + "\n  Valutazioni Negative: " + dislikes.size() + "\n\n" + body + "\n\n  Commenti:");
        if (comments.isEmpty()){
            output.append(" -Nessun utente ha commentato questo post\n");
            return output.toString();
        }
        output.append("\n");
        for (LinkedList<Comment> list : comments.values()) {
            for (Comment c: list) {
                output.append("\n").append(c.toString());
            }
        }
        return output.toString();
    }
    /**
     *	Metodo getter per l'autore del Post
     * 	@param //
     *  @return una stringa rappresentante l'autore del Post
     */
    public String getAuthor () {
        return author;
    }
    /**
     *	Metodo getter per il titolo del Post
     * 	@param //
     *  @return una stringa rappresentante il titolo del Post
     */
    public String getTitle () {
        return title;
    }
    /**
     *	Metodo getter per il contenuto del Post
     * 	@param //
     *  @return una stringa rappresentante il contenuto del Post
     */
    public String getBody () {
        return body;
    }
    /**
     *	Metodo getter per il timestamp di pubblicazione del Post
     * 	@param //
     *  @return una stringa rappresentante il timestamp di pubblicazione del Post
     */
    public String getTimestamp () {
        return timestamp;
    }
    /**
     *	Metodo getter per la collezione di commenti ricevuti dal Post
     * 	@param //
     *  @return una stringa rappresentante la collezione di commenti ricevuti dal Post
     */
    public ConcurrentHashMap<String,LinkedList<Comment>> getComments () {
        return comments; // non clonato poiche' e' utilizzato esclusivamente lato server
    }
    /**
     *	Metodo getter per la collezione di feedbacks postivi ricevuti dal Post
     * 	@param //
     *  @return una stringa rappresentante la collezione di feedbacks positivi ricevuti dal Post
     */
    public ConcurrentHashMap<String,Feedback> getLikes () {
        return likes; // non clonato poiche' e' utilizzato esclusivamente lato server
    }
    /**
     *	Metodo getter per la collezione di feedbacks negativi ricevuti dal Post
     * 	@param //
     *  @return una stringa rappresentante la collezione di feedbacks negativi ricevuti dal Post
     */
    public ConcurrentHashMap<String,Feedback> getDislikes () {
        return dislikes; // non clonato poiche' e' utilizzato esclusivamente lato server
    }
    /**
     *	Metodo getter per lo stato interno del post per quanto riguarda il calcolo delle ricompense
     * 	@param //
     *  @return un'istanza di PostRewardsState rappresentante lo stato interno del post per quanto riguarda il calcolo delle ricompense
     */
    public PostRewardsState getRewardsState () {
        try{
            rewards_calculation_lock.lock();
            return new PostRewardsState(reward_calculation_iter_num,resettable_rates_sum,new LinkedList<>(rewardable_users),new LinkedList<>(resettable_new_commentators));
        }
        finally {
            rewards_calculation_lock.unlock();
        }
    }
    /**
     *	Metodo per l'aggiunta di una valutazione al post
     * 	@param username nome dell'utente che vorrebbe valutare il post
     * 	@param voto true -> voto positivo | false -> voto negativo
     *  @return true -> successo | false -> fallimento
     */
    public boolean ratePost (String username, Boolean voto) {
        if (likes.containsKey(username) || dislikes.containsKey(username)) return false;
        if (voto) {
            likes.put(username,new Feedback(username));
            try{
                rewards_calculation_lock.lock();
                resettable_rates_sum+=1;
                if (!rewardable_users.contains(username)) rewardable_users.add(username);
            }
            finally{
                rewards_calculation_lock.unlock();
            }
        }
        else {
            dislikes.put(username,new Feedback(username));
            try{
                rewards_calculation_lock.lock();
                resettable_rates_sum-=1;
            }
            finally{
                rewards_calculation_lock.unlock();
            }
        }
        return true;
    }
    /**
     *	Metodo per l'aggiunta di un commento al post
     * 	@param username nome dell'utente che vorrebbe commentare il post
     * 	@param corpo contenuto del commento
     *  @return true -> successo | false -> fallimento
     */
    public boolean addComment (String username, String corpo){
        if (corpo.length() <= 200){

            try{
                rewards_calculation_lock.lock();
                if (comments.containsKey(username))
                {
                        LinkedList<Comment> tmp = comments.get(username);
                        tmp.add(new Comment(username,corpo));
                }
                else{
                    LinkedList<Comment> tmp = new LinkedList<>();
                    tmp.add(new Comment(username,corpo));
                    comments.put(username,tmp);
                }
                if (!resettable_new_commentators.contains(username)) resettable_new_commentators.add(username);
                if (!rewardable_users.contains(username)) rewardable_users.add(username);
            }
            finally {
                rewards_calculation_lock.unlock();
            }
            return true;
        }
        return false;
    }
    /**
     *	Metodo per il calcolo delle ricompense relative al post
     * 	@param //
     *  @return un istanza di PostRewardsCalculationOutput costituente il risultato del calcolo
     */
    public PostRewardsCalculationOutput calculateRawRewards (){
        PostRewardsCalculationOutput output;
        try{
            rewards_calculation_lock.lock();
            reward_calculation_iter_num++;
            int second_sum = 0;
            int cp;
            LinkedList<Comment> tmp;
            for (String username : resettable_new_commentators) {
                tmp = comments.get(username);
                cp = tmp.size();
                second_sum += (2 / (1 + Math.pow(Math.E, (1 - cp))));
            }
            output = new PostRewardsCalculationOutput ((Math.log(Math.max(resettable_rates_sum,0) + 1) + Math.log(second_sum + 1))/reward_calculation_iter_num,rewardable_users);
        }
        finally {
            resettable_rates_sum = 0;
            resettable_new_commentators.clear();
            rewards_calculation_lock.unlock();
        }
        return output;
    }
    /**
     *	Metodo per l'ottenimento di una stringa rappresentante il Post
     * 	@param //
     *  @return una stringa rappresentante il Post
     */
    public String toString () {
        return "* Id: " + id_post +"\t Autore: "+ author + "\n  Titolo: " + title;
    }
}