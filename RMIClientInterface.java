/**
 * Created by John on 06/03/2015.
 */
import java.rmi.Remote;
import java.rmi.RemoteException;

//interface to show rmi what functions the client has
public interface RMIClientInterface extends Remote {
    void NotifyMe(String dataItemName, int updatedValue, boolean deleted) throws RemoteException, SubscriptionException;
}
