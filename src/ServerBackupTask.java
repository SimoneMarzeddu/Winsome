public class ServerBackupTask implements Runnable{

    private final SocialNetwork winsome;
    private final long autosave_cooldown;

    /**
     *	Metodo costruttore della classe
     * 	@param social istanza di SocialNetwork di riferimento
     *  @param autosave_time intervallo di tempo che intercorrerà tra due backups
     */
    ServerBackupTask (SocialNetwork social, long autosave_time){
        winsome = social;
        autosave_cooldown = autosave_time;
    }
    /**
     *	Metodo per l'esecuzione di un backup di Winsome
     * 	@param //
     *  @return //
     */
    public void backup (){
        if (winsome.serializeWinsome("..\\bck\\winsome_posts.txt","..\\bck\\winsome_users.txt")) System.out.println("Backup completato con successo");
        else System.err.println("Backup fallito");
    }
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(autosave_cooldown);
                System.out.println("Backup automatico avviato...");
                backup();
            }
        }
        catch (InterruptedException ex){
            System.err.println("THREAD TERMINATO: Backup automatico di Winsome");
        }
    }
}
