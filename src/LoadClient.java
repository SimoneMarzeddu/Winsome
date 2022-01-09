import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class LoadClient implements Runnable{

    private final String configuration_file_name;

    /**
     *	Costruttore della classe, invocato durante il caricamento
     * 	@param args il secondo argomento eventualmente ricevuto dal ThinClient
     */
    LoadClient (String args) {
        configuration_file_name = args;

    }
    public void run ()
    {
        File config_file;
        String registry_host = "localhost";
        int tcp_server_port = 6666;
        int rmi_port = 7777;
        long socket_timeout = 100000;

        try
        {
            config_file = new File(configuration_file_name);
            Scanner scanner = new Scanner(config_file);
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                if (data.isEmpty() || data.startsWith("#")) continue;
                try {
                    String[] data_split = data.split("=");
                    if (data.startsWith("TCPPORT")) tcp_server_port = Integer.parseInt(data_split[1]);
                    else {
                        if (data.startsWith("REGPORT")) rmi_port = Integer.parseInt(data_split[1]);
                        else {
                            if (data.startsWith("TIMEOUT")) socket_timeout = Long.parseLong(data_split[1]);
                            else {
                                if (data.startsWith("REGHOST")) registry_host = data_split[1];
                            }
                        }
                    }
                }
                catch (NumberFormatException ignored) {
                }
            } // Estrazione dei parametri per la configurazione
        }
        catch (NullPointerException | FileNotFoundException | ArrayIndexOutOfBoundsException ex) {
            System.out.println("CLIENT : Avvio con configurazione standard");
        }
        System.out.println("CLIENT : Configurazione selezionata :\n*\t Registry_Host -> "+ registry_host + "\n*\t TCP_Port -> "+ tcp_server_port + "\n*\t Registry_Port -> "+ rmi_port + "\n*\t Socket_Timeout -> "+ socket_timeout);
        
        try {
            Registry registry = LocateRegistry.getRegistry(rmi_port);
            ServerRemoteInterface serverInterface = (ServerRemoteInterface) registry.lookup(registry_host);

            Socket socket = new Socket("localhost",tcp_server_port);
            socket.setSoTimeout(Math.toIntExact(socket_timeout));
            try(DataInputStream in = new DataInputStream(socket.getInputStream());
                BufferedOutputStream out_bs = new BufferedOutputStream(socket.getOutputStream());
                DataOutputStream out = new DataOutputStream(out_bs)){

                String[] multicast_info = in.readUTF().split(" ");
                String multicast_address = multicast_info[1];
                int multicast_port = Integer.parseInt(multicast_info[0]);

                ClientMulticastNotificationTask rewards_notifications = new ClientMulticastNotificationTask(multicast_address,multicast_port);
                Thread rewards_notification_thread = new Thread(rewards_notifications);
                rewards_notification_thread.setDaemon(true);
                rewards_notification_thread.start();

                Scanner scanner = new Scanner(System.in);
                ClientNotify follower_gstr = new ClientNotify();
                ClientNotifyInterface stub = (ClientNotifyInterface) UnicastRemoteObject.exportObject(follower_gstr, 0);
                String request = "";
                String[] arguments;
                String username = null;
                String password = null;
                int i;
                boolean error = false;

                System.out.println("\n Siamo pronti a partire, inizia quando vuoi scrivendo la tua prima richiesta a WINSOME!");
                System.out.println(" Ricordati che digitando il comando \"help\" potrai visualizzare il manuale :)");

                // nel ciclo si andranno ad ottenere i comandi digitati dall'utente, questi verranno analizzati e se corretti trasmessi al Server
                while (!request.equals("quit")) {

                    request = scanner.nextLine().strip();

                    if (request.startsWith("register")) {
                        arguments = request.split(" <");
                        if (arguments.length != 4) System.err.println(" ERRORE: la sintassi corretta e' -> register <username> <password> <tags>");
                        else {
                            i = 1;
                            if (arguments[i].contains(" ")){
                                System.err.println(" ERRORE: L'username non deve contenere il carattere \" \" (white space)");
                                continue;
                            }
                            while (i< arguments.length) {
                                if (!arguments[i].endsWith(">"))
                                {
                                    System.err.println(" ERRORE: la sintassi corretta e' -> register <username> <password> <tags>");
                                    error = true;
                                    break;
                                }
                                else {
                                    arguments[i] = arguments[i].substring(0,arguments[i].length()-1);
                                    i++;
                                }
                            }
                            if (!error) System.out.println(serverInterface.register(arguments[1],arguments[2],arguments[3]));
                        }
                    }
                    else
                        if (request.startsWith("login")) {
                            arguments = request.split(" <");
                            if (arguments.length != 3) System.err.println(" ERRORE: la sintassi corretta e' -> login <username> <password>");
                            else {
                                i = 1;
                                while (i< arguments.length) {
                                    if (!arguments[i].endsWith(">"))
                                    {
                                        System.err.println(" ERRORE: la sintassi corretta e' -> login <username> <password>");
                                        error = true;
                                        break;
                                    }
                                    i++;
                                }

                                if(!error){
                                    out.writeUTF(request);
                                    out.flush();
                                    String result = in.readUTF();
                                    if (!result.contains("ERRORE"))
                                    {
                                        username = arguments[1].substring(0, arguments[1].length() - 1);
                                        password = arguments[2].substring(0, arguments[2].length() - 1);
                                        // Registrazione del Client per la callback del Server
                                        follower_gstr.reset();
                                        serverInterface.registerForCallback(stub,username,password);
                                    }
                                    System.out.println(result);
                                }
                            }
                        }
                        else
                            if (request.startsWith("follow")) {
                                arguments = request.split(" <");
                                if (arguments.length != 2) System.err.println(" ERRORE: la sintassi corretta e' -> follow <username>");
                                else {
                                    i = 1;
                                    while (i< arguments.length) {
                                        if (!arguments[i].endsWith(">"))
                                        {
                                            System.err.println(" ERRORE: la sintassi corretta e' -> follow <username>");
                                            error = true;
                                            break;
                                        }
                                        i++;
                                    }
                                    if (!error) {
                                        out.writeUTF(request);
                                        out.flush();
                                        System.out.println(in.readUTF());
                                    }
                                }
                            }
                            else
                                if (request.startsWith("unfollow")) {
                                    arguments = request.split(" <");
                                    if (arguments.length != 2) System.err.println(" ERRORE: la sintassi corretta e' -> unfollow <username>");
                                    else {
                                        i = 1;
                                        while (i< arguments.length) {
                                            if (!arguments[i].endsWith(">"))
                                            {
                                                System.err.println(" ERRORE: la sintassi corretta e' -> unfollow <username>");
                                                error = true;
                                                break;
                                            }
                                            i++;
                                        }
                                        if(!error)
                                        {
                                            out.writeUTF(request);
                                            out.flush();
                                            System.out.println(in.readUTF());
                                        }
                                    }
                                }
                                else
                                    if (request.equals("logout")) {
                                        out.writeUTF(request);
                                        out.flush();
                                        String result = in.readUTF();
                                        if (!result.contains("ERRORE"))
                                        {
                                            // Deregistrazione del Client per la callback del Server
                                            serverInterface.unregisterForCallback(stub,username,password);
                                            password = null;
                                            username = null;
                                        }
                                        System.out.println(result);
                                    }
                                    else
                                        if (request.equals("list followers")) {
                                            System.out.println(follower_gstr.list_follower());
                                        }
                                        else
                                            if (request.startsWith("post")) {
                                                arguments = request.split(" <");
                                                if (arguments.length != 3) System.err.println(" ERRORE: la sintassi corretta e' -> post <title> <content>");
                                                else {
                                                    i = 1;
                                                    while (i< arguments.length) {
                                                        if (!arguments[i].endsWith(">")) {
                                                            System.err.println(" ERRORE: la sintassi corretta e' -> post <title> <content>");
                                                            error = true;
                                                            break;
                                                        }
                                                        i++;
                                                    }

                                                    if(!error){
                                                        out.writeUTF(request);
                                                        out.flush();
                                                        System.out.println(in.readUTF());
                                                    }
                                                }
                                            }
                                            else
                                                if (request.startsWith("rate")) {
                                                    arguments = request.split(" <");
                                                    if (arguments.length != 3) System.err.println(" ERRORE: la sintassi corretta e' -> rate <idPost> <vote> (con vote = +/- 1)");
                                                    else {
                                                        i = 1;
                                                        while (i< arguments.length) {
                                                            if (!arguments[i].endsWith(">")) {
                                                                System.err.println(" ERRORE: la sintassi corretta e' -> rate <idPost> <vote> (con vote = +/- 1)");
                                                                error = true;
                                                                break;
                                                            }
                                                            i++;
                                                        }
                                                        if(!error){
                                                            out.writeUTF(request);
                                                            out.flush();
                                                            System.out.println(in.readUTF());
                                                        }
                                                    }
                                                }
                                                else
                                                    if (request.startsWith("comment")) {
                                                        arguments = request.split(" <");
                                                        if (arguments.length != 3) System.err.println(" ERRORE: la sintassi corretta e' -> comment <idPost> <comment>");
                                                        else {
                                                            i = 1;
                                                            while (i< arguments.length) {
                                                                if (!arguments[i].endsWith(">")) {
                                                                    System.err.println(" ERRORE: la sintassi corretta e' -> comment <idPost> <comment>");
                                                                    error = true;
                                                                    break;
                                                                }
                                                                i++;
                                                            }
                                                            if(!error){
                                                                out.writeUTF(request);
                                                                out.flush();
                                                                System.out.println(in.readUTF());
                                                            }
                                                        }
                                                    }
                                                    else
                                                        if (request.startsWith("show post")) {
                                                            arguments = request.split(" <");
                                                            if (arguments.length != 2) System.err.println(" ERRORE: la sintassi corretta e' -> show post <idPost>");
                                                            else {
                                                                i = 1;
                                                                while (i< arguments.length) {
                                                                    if (!arguments[i].endsWith(">"))
                                                                    {
                                                                        System.err.println(" ERRORE: la sintassi corretta e' -> show post <idPost>");
                                                                        error = true;
                                                                        break;
                                                                    }
                                                                    i++;
                                                                }
                                                                if (!error) {
                                                                    out.writeUTF(request);
                                                                    out.flush();
                                                                    System.out.println(in.readUTF());
                                                                }
                                                            }
                                                        }
                                                        else
                                                            if (request.startsWith("delete")) {
                                                                arguments = request.split(" <");
                                                                if (arguments.length != 2) System.err.println(" ERRORE: la sintassi corretta e' -> delete <idPost>");
                                                                else {
                                                                    i = 1;
                                                                    while (i< arguments.length) {
                                                                        if (!arguments[i].endsWith(">"))
                                                                        {
                                                                            System.err.println(" ERRORE: la sintassi corretta e' -> delete <idPost>");
                                                                            error = true;
                                                                            break;
                                                                        }
                                                                        i++;
                                                                    }
                                                                    if (!error) {
                                                                        out.writeUTF(request);
                                                                        out.flush();
                                                                        System.out.println(in.readUTF());
                                                                    }
                                                                }
                                                            }
                                                            else
                                                                if (request.startsWith("rewin")) {
                                                                    arguments = request.split(" <");
                                                                    if (arguments.length != 2) System.err.println(" ERRORE: la sintassi corretta e' -> rewin <idPost>");
                                                                    else {
                                                                        i = 1;
                                                                        while (i< arguments.length) {
                                                                            if (!arguments[i].endsWith(">"))
                                                                            {
                                                                                System.err.println(" ERRORE: la sintassi corretta e' -> rewin <idPost>");
                                                                                error = true;
                                                                                break;
                                                                            }
                                                                            i++;
                                                                        }
                                                                        if (!error) {
                                                                            out.writeUTF(request);
                                                                            out.flush();
                                                                            System.out.println(in.readUTF());
                                                                        }
                                                                    }
                                                                }
                                                                else
                                                                    if (request.equals("list users") || request.equals("list following") || request.equals("blog") || request.equals("show feed") || request.equals("wallet") || request.equals("wallet btc")) {

                                                                        out.writeUTF(request);
                                                                        out.flush();

                                                                        String received = in.readUTF();
                                                                        if (received.contains("ERRORE")){
                                                                            System.out.println(received);
                                                                            continue;
                                                                        }
                                                                        int it_num = Integer.parseInt(received);
                                                                        for (i=0; i<it_num; i++){
                                                                            System.out.println(in.readUTF());
                                                                        }
                                                                    }
                                                                    else
                                                                        if (request.equals("show notifications")) rewards_notifications.printHistory();
                                                                        else
                                                                            if(request.equals("help")){
                                                                                System.out.println("\n Hai bisogno di aiuto? non ti preoccupare!\n Troverai di seguito una lista completa dei comandi di Winsome:\n");
                                                                                System.out.println("* register <username> <password> <tags> : ti permettera' di registrarti a Winsome!");
                                                                                System.out.println("* login <username> <password> : ti permettera' di accedere al tuo account, e' il primo passo di ogni sessione qui su Winsome");
                                                                                System.out.println("* logout : ti permettera' di uscire in modo sicuro dal tuo account");
                                                                                System.out.println("* list users : e' un comando con cui potrai visualizzare tutti gli utenti di Winsome affini a te");
                                                                                System.out.println("* list following : e' un comando con cui potrai visualizzare tutti gli utenti di Winsome che hai deciso di seguire");
                                                                                System.out.println("* list followers : e' un comando con cui potrai visualizzare tutti gli utenti di Winsome che hanno deciso di seguirti");
                                                                                System.out.println("* follow <user> : ti permettera' di seguire un utente di Winsome, in questo modo i suoi post appariranno sul tuo feed");
                                                                                System.out.println("* unfollow <user> : stanco di avere a che fare con un utente? Con questo comando non visualizzerai piu' cio' che ha da dire");
                                                                                System.out.println("* post <title> <content> : ti permettera' di pubblicare un post visibile a tutti i tuoi followers");
                                                                                System.out.println("* rate <idPost> <vote> : e' un comando con cui potrai valutare un post identificato dal suo ID (+1 -> voto positivo, -1 -> voto negativo)");
                                                                                System.out.println("* comment <idPost> <comment> : e' un comando con cui potrai commentare pubblicamente un post identificato dal suo ID");
                                                                                System.out.println("* show post <idPost> : con questo comando potrai avere una visione completa di un post");
                                                                                System.out.println("* delete <idPost> : e' un comando con cui potrai eliminare un post di cui sei autore");
                                                                                System.out.println("* rewin <idPost> : con questo comando puoi effettuare il \"rewin\" di un post in modo che tutti i tuoi followers possano visualizzarlo come un post condiviso sul tuo blog");
                                                                                System.out.println("* show feed : e' il comando con cui potrai visualizzare tutti i post presenti sul tuo feed (una raccolta dei post pubblicati dagli utenti che segui)");
                                                                                System.out.println("* blog : e' il comando con cui potrai visualizzare tutti i post presenti sul tuo blog (una raccolta dei post che hai pubblicato)");
                                                                                System.out.println("* wallet : per esplorare il tuo portafoglio di Wincoins");
                                                                                System.out.println("* wallet btc : per esplorare il tuo portafoglio di Wincoins convertiti in Bitcoins");
                                                                                System.out.println("* show notifications : per mostrare tutte le notifiche inviate dal sistema");
                                                                                System.out.println("* quit : per far terminare il Client");
                                                                            }
                                                                            else if(!request.equals("quit"))System.err.println(" Ci dispiace, ma Winsome non ha capito il comando che hai richiesto :(\n Ricorda che puoi sempre visualizzare il manuale con \"help\"");
                                                                            error = false;
                }

                System.out.println(" Grazie per aver trascorso del tempo con noi, a presto! :)");
            }
            catch (ConnectException e){
                System.err.println(" Ci dispiace, ma il Server non e' disponibile");
            }
            catch (NoSuchElementException | NullPointerException e) {
                System.err.println(" Client Terminato");
            }
            finally {
                try {
                    socket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (IOException e){
            System.err.println(" Ci dispiace, ma il Server non e' disponibile :(\n Riprova piu' tardi");
            System.err.println(" "+ e.getMessage());
        }
        catch (NotBoundException ex) {
        System.err.println(" ERRORE: " + ex.getMessage());
        System.exit(-1);
        }
        System.exit(0);
    }
}
