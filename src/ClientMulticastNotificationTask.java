import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class ClientMulticastNotificationTask implements Runnable{
    private final String address;
    private final int port;
    private final StringBuilder history;

    /**
     *	Costruttore della classe
     * 	@param mc_address indirizzo per il Multicast
     * 	@param mc_port porta per il Multicast
     */
    ClientMulticastNotificationTask (String mc_address, int mc_port){
        address = mc_address;
        port = mc_port;
        history = new StringBuilder();
    }
    @Override
    public void run() {
        try(MulticastSocket socket = new MulticastSocket(port))
        {
            // il Client si mette in ascolto per la ricezione di pacchetti attraverso Multicast
            socket.joinGroup(InetAddress.getByName(address));
            byte[] buff = new byte[90];
            while(!Thread.currentThread().isInterrupted())
            {
                DatagramPacket dp = new DatagramPacket(buff, buff.length);
                socket.receive(dp);
                String message = new String(dp.getData(), StandardCharsets.UTF_8);
                Instant time = Instant.now();
                // le notifiche ricevute vengono archiviate all'interno della cronolgia
                synchronized(history){
                 history.append(" ").append(time.toString(), 0, 10).append(" - ").append(time.toString(), 11, 19).append(" -> ").append(message).append("\n");   
                }
                
            }
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }
    /**
     *	Metodo per la stampa della cronologia di tutte le notifiche ricevute attraverso Multicast
     * 	@param //
     *  @return //
     */
    public void printHistory () {
        String printable;
        synchronized(history){
           printable = history.toString(); 
        }
        if (printable.isEmpty()) System.out.println(" Sembra che non ci sia nulla da notificare!");
        else System.out.println(printable);
    }
}
