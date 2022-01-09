import java.rmi.*;

public interface ClientNotifyInterface extends Remote {

    void someone_followed(String username) throws RemoteException;
    void someone_unfollowed(String username) throws RemoteException;
    void consistent_state_Followers (String list_to_parse) throws RemoteException;
}
