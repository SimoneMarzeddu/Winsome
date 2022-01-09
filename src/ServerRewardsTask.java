import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class ServerRewardsTask implements Runnable{

    private final SocialNetwork winsome;
    private final long calculation_cooldown;
    private final String multicast_address;
    private final int multicast_port;

    /**
     *	Metodo costruttore della classe
     * 	@param socialNetwork istanza di SocialNetwork di riferimento
     *  @param cooldown intervallo di tempo che intercorrerà tra due calcoli delle ricompense
     * 	@param multicast_address indirizzo per il servizio di Multicast
     *  @param multicast_port porta per il servizio di Multicast
     */
    ServerRewardsTask(SocialNetwork socialNetwork, long cooldown, String multicast_address, int multicast_port) {
        winsome = socialNetwork;
        calculation_cooldown = cooldown;
        this.multicast_address = multicast_address;
        this.multicast_port = multicast_port;
    }
    @Override
    public void run() {

        byte[] buff;

        try(DatagramSocket socket = new DatagramSocket()) {
            socket.setReuseAddress(true);

            String message = "ATTENZIONE utenti di Winsome: Le ricompense sono state aggiornate!";
            buff = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buff, buff.length, InetAddress.getByName(multicast_address), multicast_port);

            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(calculation_cooldown);
                winsome.updateRewards();
                socket.send(packet);
            }
        }
        catch (InterruptedException ex){
            System.err.println("THREAD TERMINATO: Calcolo periodico delle ricompense");
        }
        catch (IOException ignored){
        }
    }
}
