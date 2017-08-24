/**
 * Created by John on 06/03/2015.
 */
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

public class RMIServer extends UnicastRemoteObject implements RMIServerInterface {
    //server class global variables
    private static HashMap<String,Integer> dataItems = new HashMap<String, Integer>();              //data item values, indexed with dataName
    private static HashMap<String, Integer> dataPublisherIds = new HashMap<String, Integer>();      //data item publishers, indexed with dataName
    private static ArrayList<String[]> dataSubscribers = new ArrayList<String[]>();                 //clients subscriptions
    private static Registry rmiReg;                                                                 //class global ref to rmi
    private static int clientIdCount = 0;                                                           //client unique id counter
    private static int dataPublishIdCount = 0;                                                      //publish unique id counter

    //### class init ### when invoked as object for rmi
    public RMIServer() throws RemoteException {
        super();
    }

    //### main class init ### when invoked normally
    public static void main(String args[])
    {
        //### Setup Server Object on RMI ###
        try{
            //create rmi reg on port 1199
            rmiReg = LocateRegistry.createRegistry(1099);

            //create server class obj
            RMIServer RMIServerObject = new RMIServer();

            //bind server object instance to the name
            rmiReg.rebind("rmi://localhost/SubscriptionServer", RMIServerObject);

            System.out.println("\nServer bound in localhost port: " + 1099);
            System.out.println("Server Ready!\n");
        }
        //catch exception if rmi bound failed
        catch (Exception e) {
            System.out.println("Server Error: " + e.getMessage()); //print error
            e.printStackTrace();
        }
    }

    //### connect to ### - gives the new client a unique id
    public int connectTo(){
        clientIdCount++;        //increments id counter, so no two clients get the same id
        return clientIdCount;
    }

    //## Subscribe ###                                                      //expects to have a subscription exception error
    public void Subscribe(String dataItemName, String callBackFunctionName) throws SubscriptionException
    {
        //check if data item exists
        if(dataItems.containsKey(dataItemName))
        {
            //check if already subscribed
            int j = dataSubscribers.size();     //loop all subscribers

            for(int i=0; i<j; i++)
            {
                if(dataSubscribers.get(i)[0].equals(dataItemName) && dataSubscribers.get(i)[1].equals(callBackFunctionName))
                {
                    //already subscribed
                    System.out.println("Subscribe: '" + dataItemName + "'. Failed - already subscribed.");
                    throw new SubscriptionException("You have already subscribed.");            //breaks loop and ends function
                }
            }

            //if not already subscribed
            //add subscription
            dataSubscribers.add(new String[]{dataItemName, callBackFunctionName});
            System.out.println("Subscribe: '" + dataItemName + "'.");

            //update the client value
            try{
                //get the value of the subscription
                int dataItemValue = dataItems.get(dataItemName);

                //find callback function on rmi AND send the value to the client
                ((RMIClientInterface) rmiReg.lookup("rmi://localhost/" + callBackFunctionName)).NotifyMe(dataItemName, dataItemValue, false);
            }
            catch (Exception e) {
                System.out.println("Client Error: " + e.getMessage());
                e.printStackTrace();
                throw new SubscriptionException("Could not connect.");
            }
        }
        else {
            //error dont exist - cant subscribe
            System.out.println("Subscribe: '" + dataItemName + "'. Failed - data item does not exist.");
            throw new SubscriptionException("This item does not exist.");
        }
    }

    //### UnSubscribe ###
    public void UnSubscribe(String dataItemName, int clientId) throws SubscriptionException
    //needs another parameter to specify which client is UnSubscribing from what data item
    {
        //check if already subscribed
        int j = dataSubscribers.size();
        boolean found = false;
        for(int i=0; i<j; i++) //loop subscribers
        {
            if(dataSubscribers.get(i)[0].equals(dataItemName) && dataSubscribers.get(i)[1].equals("NotifyMe"+clientId)){
                //unsubscribe client
                dataSubscribers.remove(i);
                System.out.println("UnSubscribe: '" + dataItemName + "'.");
                found = true;
                break;      //end search
            }
        }

        //if didnt find subscription
        if(!found) {
            //you are not subscribed
            System.out.println("UnSubscribe: '" + dataItemName + "'. Failed - data item already unsubscribed.");
            throw new SubscriptionException("You are not subscribed.");
        }
    }

    //### Publish ###
    public int Publish(String dataItemName, int initValue) throws SubscriptionException
    {
        //if data item already exists
        if(dataItems.containsKey(dataItemName))
        {
            //error already exists
            System.out.println("Publish: '" + dataItemName + "'. Failed: data item already exists.");
            throw new SubscriptionException("A data item already exists with this name.");
        }
        else {
            //create new data item
            dataItems.put(dataItemName, initValue);                     //save data item name with value
            dataPublisherIds.put(dataItemName, ++dataPublishIdCount);   //save data item name with publisher id
            System.out.println("Publish: '" + dataItemName + "', value: " + initValue + ", id: '" + dataPublishIdCount + "'.");
        }

        //return publisher id
        return dataPublishIdCount;
    }

    //### Update ###
    public void Update(String dataItemName, int publishedId, int newValue) throws SubscriptionException
    {
        //check if data item exists
        if(dataItems.containsKey(dataItemName))
        {
            //check client owns data item
            if(dataPublisherIds.get(dataItemName) == publishedId)
            {
                //perform update
                dataItems.replace(dataItemName, newValue);
                System.out.println("Update: '" + dataItemName + "', value: " + newValue);

                //update all clients who are subscribed
                updateSubscribers(dataItemName, newValue, false);
            }
            else {
                //error client does not own data item
                System.out.println("Update: '" + dataItemName + "'. Failed - client does not own item.");
                throw new SubscriptionException("You do not own this data item.");
            }
        }
        else {
            //error dont exist
            System.out.println("Update: '" + dataItemName + "'. Failed - data item does not exist.");
            throw new SubscriptionException("This item does not exist.");
        }
    }

    //### Delete ###
    public void Delete(String dataItemName, int publishedId) throws SubscriptionException
    {
        //check if data item exists
        if(dataItems.containsKey(dataItemName))
        {
            //check client owns data item
            if(dataPublisherIds.get(dataItemName) == publishedId)
            {
                //perform delete
                dataItems.remove(dataItemName);
                dataPublisherIds.remove(dataItemName);
                System.out.println("Delete: '" + dataItemName + "'.");

                //update subscribers of delete
                updateSubscribers(dataItemName, 0, true);
            }
            else {
                //error client does not own data item
                System.out.println("Update: '" + dataItemName + "'. Failed - client does not own item.");
                throw new SubscriptionException("You do not own this data item.");
            }
        }
        else {
            //error dont exist
            System.out.println("Delete: '" + dataItemName + "'. Failed - data item does not exist.");
            throw new SubscriptionException("This item does not exist.");
        }
    }

    //### update client subscriber ###
    private void updateSubscribers(String dataItemName, int newValue, boolean deleted) throws SubscriptionException
    {
        //loop every subscribers
        int j = dataSubscribers.size();
        for(int i=0; i<j; i++)
        {
            //if a subscription entry is for the updated item
            if(dataSubscribers.get(i)[0].equals(dataItemName))
            {
                //update the client value
                try {
                    //callback function on rmi to client to update their data subscription value
                    ((RMIClientInterface) rmiReg.lookup("rmi://localhost/" + dataSubscribers.get(i)[1])).NotifyMe(dataItemName, newValue, deleted);
                }
                catch (Exception e) {//connect error
                    System.out.println("Client Error: " + e.getMessage());
                    e.printStackTrace();
                    throw new SubscriptionException("Could not connect.");
                }
            }
        }
    }
}
