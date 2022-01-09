import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class ServerMain
{
    /**
     *	Metodo invocato alla terminazione del Server
     * 	@param bck task del thread per i backup
     *  @param tcp_main thread gestore del socket del Server
     * 	@param rew_admin thread calcolatore delle ricompense
     *  @return //
     */
    private static void closeServer(ServerBackupTask bck, Thread tcp_main, Thread rew_admin){
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Chiusura del Server avviata...");
                tcp_main.interrupt();
                rew_admin.interrupt();
                bck.backup();
                System.out.println("Server terminato correttamente");
            }
        });
    }
    public static void main (String[]Args) {
        File config_file;
        String server_address = "192.168.1.2";
        String multicast_address = "239.255.32.32";
        String registry_host = "localhost";


        int tcp_server_port = 6666;
        int udp_server_port = 33333;
        int multicast_port = 44444;
        int rmi_port = 7777;
        int socket_timeout = 100000;
        long rewards_timelapse = 10000;
        long autosave_timelapse = 30000;
        int rewards_author_percentage = 70;

        // segue il parsing del file di configurazione
        try {
            config_file = new File(Args[0]);
            Scanner scanner = new Scanner(config_file);
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                if (data.isEmpty() || data.startsWith("#")) continue;
                try {
                    String[] data_split = data.split("=");
                    if (data.startsWith("SERVER")) server_address = data_split[1];
                    else {
                        if (data.startsWith("TCPPORT")) tcp_server_port = Integer.parseInt(data_split[1]);
                        else {
                            if (data.startsWith("UDPPORT")) udp_server_port = Integer.parseInt(data_split[1]);
                            else {
                                if (data.startsWith("MCASTPORT")) multicast_port = Integer.parseInt(data_split[1]);
                                else {
                                    if (data.startsWith("REGPORT")) rmi_port = Integer.parseInt(data_split[1]);
                                    else {
                                        if (data.startsWith("TIMEOUT")) socket_timeout = Integer.parseInt(data_split[1]);
                                        else {
                                            if (data.startsWith("AUTOSTL")) autosave_timelapse = Integer.parseInt(data_split[1]);
                                            else {
                                                if (data.startsWith("MULTICAST")) multicast_address = data_split[1];
                                                else {
                                                    if (data.startsWith("REGHOST")) registry_host = data_split[1];
                                                    else {
                                                        if (data.startsWith("REWTL")) rewards_timelapse = Long.parseLong(data_split[1]);
                                                        else {
                                                            if (data.startsWith("REWAUT")) rewards_author_percentage = Integer.parseInt(data_split[1]);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                catch (NumberFormatException ignored) {
                }
            } // Estrazione dei parametri per la configurazione
        }
        catch (NullPointerException | FileNotFoundException | ArrayIndexOutOfBoundsException ex) {
            System.out.println("SERVER : Avvio con configurazione standard");
        }
        System.out.println("SERVER : Configurazione selezionata :\n*\t Server_Address -> "+ server_address + "\n*\t Multicast_Address -> "+ multicast_address + "\n*\t Registry_Host -> "+ registry_host + "\n*\t TCP_Port -> "+ tcp_server_port + "\n*\t UDP_Port -> "+ udp_server_port + "\n*\t Multicast_Port -> "+ multicast_port + "\n*\t Registry_Port -> "+ rmi_port + "\n*\t Socket_Timeout -> "+ socket_timeout + "\n*\t Author's_Reward_Percentage -> "+ rewards_author_percentage + "%" + "\n*\t Rewards_Time_Lapse -> "+ rewards_timelapse + "\n*\t Autosave_Time_Lapse -> " + autosave_timelapse);

        SocialNetwork winsome = new SocialNetwork(rewards_author_percentage);
        winsome.deserializeWinsome("..\\bck\\winsome_posts.txt","..\\bck\\winsome_users.txt");
        try {
            ServerRemoteInterface stub = (ServerRemoteInterface) UnicastRemoteObject.exportObject(winsome,0);
            LocateRegistry.createRegistry(rmi_port);
            Registry registry = LocateRegistry.getRegistry(rmi_port);
            registry.rebind(registry_host,stub);
        }
        catch (RemoteException ex) {
            System.err.println("ERRORE: " + ex.getMessage());
            System.exit(-1);
        }

        // si costruiscono di seguito le task per i thread inerenti a: amministrazione della connessione TCP, calcolo delle ricompense e backup automatico dello stato di Winsome
        ServerTCPAdmin tcpAdmin_task = new ServerTCPAdmin(winsome,socket_timeout,tcp_server_port,multicast_address,multicast_port);
        ServerRewardsTask rewards_task = new ServerRewardsTask(winsome,rewards_timelapse,multicast_address,multicast_port);
        ServerBackupTask backup_task = new ServerBackupTask(winsome,autosave_timelapse);

        Thread backup_thread = new Thread(backup_task);
        Thread rewards_thread = new Thread(rewards_task);
        Thread tcpAdmin = new Thread(tcpAdmin_task);

        backup_thread.setDaemon(true);
        rewards_thread.setDaemon(true);

        closeServer(backup_task,tcpAdmin,rewards_thread);

        backup_thread.start();
        rewards_thread.start();
        tcpAdmin.start();

    }
}
