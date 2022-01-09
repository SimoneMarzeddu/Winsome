import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerTCPAdmin implements Runnable {

    private final SocialNetwork winsome;
    private final int socket_timeout;
    private final int port;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    String multicast_address;
    int multicast_port;

    /**
     *	Metodo costruttore della classe
     * 	@param socialNetwork istanza di SocialNetwork di riferimento
     *  @param socket_timeout timeout adottato verso operazioni sui socket dei Clients
     *  @param tcp_port porta per le connessioni TCP
     * 	@param mc_address indirizzo per il servizio di Multicast (per comunicare questo ai clients)
     *  @param mc_port porta per il servizio di Multicast (per comunicare questa ai clients)
     */
    ServerTCPAdmin(SocialNetwork socialNetwork, int socket_timeout, int tcp_port, String mc_address, int mc_port)
    {
        winsome = socialNetwork;
        this.socket_timeout = socket_timeout;
        port = tcp_port;
        multicast_address = mc_address;
        multicast_port = mc_port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)){

            System.out.printf("Il Server accettera' connessioni TCP dalla porta: %d\n", port);

            while (!serverSocket.isClosed() && !Thread.currentThread().isInterrupted()) {
                Socket client_socket = serverSocket.accept();
                client_socket.setSoTimeout(socket_timeout);
                // una task worker viene creata e sottomessa alla thread_pool
                ServerTCPTask service_task = new ServerTCPTask(winsome,client_socket,multicast_address,multicast_port);
                pool.submit(service_task);
            }
            pool.shutdown();
        }
        catch (IOException ex) {
            System.err.println("Il Server non accettera' ulteriori connessioni : \n"+ex.getMessage());
            System.exit(-1);
        }
    }
}
