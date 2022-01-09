import java.util.LinkedList;

public class PostRewardsState {
    private final int reward_calculation_iter_num;
    private final int resettable_rates_sum;
    private final LinkedList<String> rewardable_users;
    private final LinkedList<String> resettable_new_commentators;

    /**
     *	Metodo costruttore della classe
     * 	@param rcin il numero di iterazioni del calcolo delle ricompesne subite dal Post
     *  @param rrs variabile ausiliaria al calcolo della ricompensa
     * 	@param ru lista di utenti meritevoli di una porzione della ricompensa
     * 	@param rnc variabile ausiliaria al calcolo della ricompensa
     */
    PostRewardsState (int rcin, int rrs, LinkedList<String> ru, LinkedList<String> rnc){
        resettable_new_commentators = rnc;
        reward_calculation_iter_num = rcin;
        resettable_rates_sum = rrs;
        rewardable_users = ru;
    }
    /**
     *	Metodo getter per il numero di iterazioni del calcolo delle ricompesne subite dal Post
     * 	@param //
     *  @return il numero di iterazioni del calcolo delle ricompesne subite dal Post
     */
    public int getReward_calculation_iter_num () {
        return reward_calculation_iter_num;
    }
    /**
     *	Metodo getter per il valore di una variabile ausiliaria al calcolo delle ricompense
     * 	@param //
     *  @return il valore di una variabile ausiliaria al calcolo delle ricompense
     */
    public int getResettable_rates_sum () {
        return resettable_rates_sum;
    }
    /**
     *	Metodo getter per la lista di utenti ad ora meritevoli delle ricompense derivanti da un Post
     * 	@param //
     *  @return un'istanza di LinkedList rappresentante la lista di utenti ad ora meritevoli delle ricompense derivanti da un Post
     */
    public LinkedList<String> getRewardable_users () {
        return rewardable_users; // non clonato poiche' e' utilizzato esclusivamente lato server
    }
    /**
     *	Metodo getter per il valore di una variabile ausiliaria al calcolo delle ricompense
     * 	@param //
     *  @return il valore di una variabile ausiliaria al calcolo delle ricompense
     */
    public LinkedList<String> getResettable_new_commentators () {
        return resettable_new_commentators; // non clonato poiche' e' utilizzato esclusivamente lato server
    }
}
