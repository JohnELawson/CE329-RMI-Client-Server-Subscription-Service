/**
 * Created by John on 04/03/2015.
 */
import java.rmi.Remote;
import java.rmi.RemoteException;

//interface to show rmi what functions the server has
public interface RMIServerInterface extends Remote{
    int connectTo() throws RemoteException;
    void Subscribe(String dataItemName, String callBackFunctionName) throws RemoteException, SubscriptionException;
    void UnSubscribe(String dataItemName, int clientId) throws RemoteException, SubscriptionException;
    int Publish(String dataItemName, int initValue) throws RemoteException, SubscriptionException;
    void Update(String dataItemName, int publishedId, int newValue) throws RemoteException, SubscriptionException;
    void Delete(String dataItemName, int publishedId) throws RemoteException, SubscriptionException;
}