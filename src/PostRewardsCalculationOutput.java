import java.util.LinkedList;

public class PostRewardsCalculationOutput {
    private final double raw_reward;
    private final LinkedList<String> rewardable_users;

    /**
     *	Costruttore della classe
     * 	@param reward valore complessivo della ricompensa risultante dal calcolo
     *  @param users utenti che grazie a valutazioni positive o commenti meritano una porzione della ricompensa
     */
    PostRewardsCalculationOutput(double reward, LinkedList<String> users){
        raw_reward = reward;
        rewardable_users = users;
    }
    /**
     *	Metodo getter per l'ottenimento della somma totale di ricompensa
     * 	@param //
     *  @return la somma totale di ricompensa
     */
    public double getRaw_reward(){
        return raw_reward;
    }
    /**
     *	Metodo getter per la lista di utenti meritevoli di una porzione di ricompensa
     * 	@param //
     *  @return un'istanza di LinkedList rappresentante la lista di utenti meritevoli di una porzione di ricompensa
     */
    public LinkedList<String> getRewardable_users(){
        return rewardable_users; // non clonato poiche' e' utilizzato esclusivamente lato server
    }
}
