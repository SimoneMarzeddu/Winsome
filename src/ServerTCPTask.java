import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerTCPTask implements Runnable {

    SocialNetwork winsome;
    Socket socket;
    String who_are_they;
    String actual_username = null;
    String multicast_address;
    int multicast_port;

    /**
     *	Metodo costruttore della classe
     * 	@param socialNetwork istanza di SocialNetwork di riferimento
     *  @param sock socket del Client da servire
     * 	@param mc_address indirizzo per il servizio di Multicast (per comunicare questo al Client)
     *  @param mc_port porta per il servizio di Multicast (per comunicare questa al Client)
     */
    ServerTCPTask (SocialNetwork socialNetwork, Socket sock, String mc_address, int mc_port)
    {
        winsome = socialNetwork;
        socket = sock;
        who_are_they = sock.getRemoteSocketAddress().toString();
        multicast_address = mc_address;
        multicast_port = mc_port;
    }
    /**
     *	Metodo che controlla lo stat di login o meno del Client attualmente connesso
     * 	@param out riferimento allo stream di uscita verso il Client (per la stampa di un errore)
     *  @return true -> se l'utente non è autenticato sul Client | false -> altrimenti
     */
    private boolean needing_login_gstr (DataOutputStream out)throws IOException {
            if (actual_username == null)
            {
                out.writeUTF(" ERRORE: Login necessario");
                out.flush();
                return true;
            }
            return false;
    }
    /**
     *	Metodo interprete della richiesta in arrivo dal client
     * 	@param request richiesta giunta dal Client
     *  @return un array di stringhe rappresentanti gli argomenti del comando giunto dal Client
     */
    private String[] arguments_parsing (String request) {
        String[] arguments = request.split(" <");
        int i = 1;
        while (i < arguments.length) {
            arguments[i] = arguments[i].substring(0, arguments[i].length() - 1);
            i++;
        }
        return arguments;
    }
    /**
     *	Metodo che effettua la scrittura segmentata di un messaggio verso il Client
     * 	@param out riferimento allo stream di uscita verso il Client
     * 	@param to_send contenuto da inviare al Client
     *  @return //
     */
    private void writeUTF_segmented (DataOutputStream out, String to_send){
        int length = to_send.length();
        int it_num = (int)Math.ceil((double)to_send.length()/65000);
        int iteraction;
        int i = 0;
        int j = 65000;
        try{
            out.writeUTF(String.valueOf(it_num));
            out.flush();

            for (iteraction = 0; iteraction<it_num; iteraction++){
                if (j > length) j = length;
                out.writeUTF(to_send.substring(i,j));
                out.flush();
                i += 65000;
                j += 65000;
            }
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }
    @Override
    public void run() {

        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             BufferedOutputStream out_bs = new BufferedOutputStream(socket.getOutputStream());
             DataOutputStream out = new DataOutputStream(out_bs)) {

            String request;
            String[] arguments;

            // il thread condivide al Client le informazioni relativi al servizio di Multicast per le notifiche sul calcolo delle ricompense
            out.writeUTF(multicast_port + " " + multicast_address);
            out.flush();

            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                request = in.readUTF();

                if (request.startsWith("login")) {
                    if (actual_username != null) {
                        out.writeUTF(" ERRORE: Effettua il logout dal tuo attuale account prima di effettuare un login con un nuovo account");
                        out.flush();
                        continue;
                    }
                    arguments = arguments_parsing(request);
                    String result = winsome.login(arguments[1],arguments[2],who_are_they);
                    out.writeUTF(result);
                    out.flush();
                    if (!result.contains("ERRORE")) actual_username = arguments[1];
                }
                else
                    if (request.startsWith("logout")) {
                        if (needing_login_gstr(out)) continue;
                        String result = winsome.logout(actual_username,who_are_they);
                        out.writeUTF(result);
                        out.flush();
                        if (!result.contains("ERRORE")) actual_username = null;
                    }
                    else
                        if (request.equals("list users")) {
                            if (needing_login_gstr(out)) continue;
                            writeUTF_segmented(out,winsome.listUsers(actual_username,who_are_they));
                        }
                        else
                            if (request.equals("list following")) {
                                if (needing_login_gstr(out)) continue;
                                writeUTF_segmented(out,winsome.listFollowing(actual_username,who_are_they));
                            }
                            else
                                if (request.startsWith("follow")) {
                                    if (needing_login_gstr(out)) continue;
                                    arguments = arguments_parsing(request);
                                    String result = winsome.followUser(actual_username,arguments[1],who_are_they);
                                    out.writeUTF(result);
                                    out.flush();
                                }
                                else
                                    if (request.startsWith("unfollow")) {
                                            if (needing_login_gstr(out)) continue;
                                            arguments = arguments_parsing(request);
                                            String result = winsome.unfollowUser(actual_username,arguments[1],who_are_they);
                                            out.writeUTF(result);
                                            out.flush();
                                        }
                                    else
                                        if (request.startsWith("post")) {
                                            if (needing_login_gstr(out)) continue;
                                            arguments = arguments_parsing(request);
                                            String result = winsome.createPost(actual_username,arguments[1],arguments[2],who_are_they);
                                            out.writeUTF(result);
                                            out.flush();
                                        }
                                        else
                                            if (request.startsWith("show feed")) {
                                                if (needing_login_gstr(out)) continue;
                                                writeUTF_segmented(out,winsome.showFeed(actual_username,who_are_they));
                                            }
                                            else
                                                if (request.startsWith("blog")) {
                                                    if (needing_login_gstr(out)) continue;
                                                    writeUTF_segmented(out,winsome.showBlog(actual_username,who_are_they));
                                                }
                                                else
                                                    if (request.startsWith("show post")) {
                                                        if (needing_login_gstr(out)) continue;
                                                        arguments = arguments_parsing(request);
                                                        String result = winsome.showPost(actual_username,who_are_they,arguments[1]);
                                                        out.writeUTF(result);
                                                        out.flush();
                                                    }
                                                    else
                                                        if (request.startsWith("delete")) {
                                                            if (needing_login_gstr(out)) continue;
                                                            arguments = arguments_parsing(request);
                                                            String result = winsome.deletePost(actual_username,who_are_they,arguments[1]);
                                                            out.writeUTF(result);
                                                            out.flush();
                                                        }
                                                        else
                                                            if (request.startsWith("rewin")) {
                                                                if (needing_login_gstr(out)) continue;
                                                                arguments = arguments_parsing(request);
                                                                String result = winsome.rewinPost(actual_username,arguments[1],who_are_they);
                                                                out.writeUTF(result);
                                                                out.flush();
                                                            }
                                                            else
                                                                if (request.startsWith("rate")) {
                                                                if (needing_login_gstr(out)) continue;
                                                                arguments = arguments_parsing(request);
                                                                String result = winsome.ratePost(actual_username,arguments[1],arguments[2],who_are_they);
                                                                out.writeUTF(result);
                                                                out.flush();
                                                            }
                                                                else
                                                                    if (request.startsWith("comment")) {
                                                                        if (needing_login_gstr(out)) continue;
                                                                        arguments = arguments_parsing(request);
                                                                        String result = winsome.addComment(actual_username,arguments[1],arguments[2],who_are_they);
                                                                        out.writeUTF(result);
                                                                        out.flush();
                                                                    }
                                                                    else
                                                                        if (request.equals("wallet")) {
                                                                            if (needing_login_gstr(out)) continue;
                                                                            writeUTF_segmented(out,winsome.showWallet(actual_username,who_are_they,false));
                                                                        }
                                                                        else
                                                                            if (request.equals("wallet btc")) {
                                                                            if (needing_login_gstr(out)) continue;
                                                                            writeUTF_segmented(out,winsome.showWallet(actual_username,who_are_they,true));
                                                                        }
            }
        }
        catch (IOException e) {
            System.err.println("Connessione terminata con il Client \""+ who_are_they +"\" : " + e.getMessage());
        }
        finally {
            if (actual_username != null) winsome.logout(actual_username,who_are_they);
            try {
                socket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
