import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerRemoteInterface extends Remote {

    String register (String username, String password, String lista_di_tag) throws RemoteException;
    // registrazione per la callback
    void registerForCallback(ClientNotifyInterface clientInterface, String username, String password) throws RemoteException;
    // cancella registrazione per la callback
    void unregisterForCallback(ClientNotifyInterface clientInterface, String username, String password) throws RemoteException;
}

