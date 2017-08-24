/**
 * Created by John on 06/03/2015.
 */
import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class RMIClient extends UnicastRemoteObject implements RMIClientInterface {
    //extent uni.. allows it to be a remote class for callback, impliment rmicli.. allows rmi to see NotifyMe callback function

    //subscription global variables
    private static HashMap<String, Integer> publishIds = new HashMap<String, Integer>();            //store publishIds indexed with dataName
    private static HashMap<String, Integer> subscriptionDataItems = new HashMap<String, Integer>(); //store dataValues indexed with dataName
    private static int clientId;                                                      //this clients unique id
    private static RMIServerInterface subscriptionServer = null;                      //class global ref to rmi server
    //gui global variables
    private static JTextPane console;                                                 //gui command window (for printing)
    private static StyledDocument consoleDoc;
    private static JScrollPane scrollContainer;

    //### class init ### - when invoked as object
    public RMIClient() throws RemoteException {
        super();
    }

    //### main class init ### - when invoked normally
    public static  void main(String args[]) {
        //setup gui
        GenGUI();

        //attempt connect to server rmi
        try {
            //save server rmi
            Registry rmiReg = LocateRegistry.getRegistry(1099);
            subscriptionServer = (RMIServerInterface) rmiReg.lookup("rmi://localhost/SubscriptionServer");

            //fetch this clients unique id
            clientId = subscriptionServer.connectTo();

            //callback feature - add notify function to rmi
            RMIClient clientObject = new RMIClient();
            rmiReg.rebind("rmi://localhost/NotifyMe" + clientId, clientObject);
        }
        //catch exception if not found/ other error
        catch (Exception e) {
            println("Client Error: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private static void GenGUI(){
        //create window
        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //window.setPreferredSize(new Dimension(675, 340));
        window.setVisible(true);
        window.setTitle("Client Interface");
        //create/ add text area
        console = new JTextPane();
        console.setBackground(Color.black);
        console.setForeground(Color.white);
        console.setCaretColor(Color.white);
        console.setFont(new Font("Lucida Console", Font.PLAIN, 12));
        console.setEditable(false);
        //buttons and textboxes for each function
        JPanel buttonPanel = new JPanel(new FlowLayout());//GridBagLayout());
        //GridBagConstraints c = new GridBagConstraints();

        //Subscribe - create gui panel, buttons and textboxes
        JPanel subscribePanel = new JPanel(new FlowLayout());
        JButton subscribeButton = new JButton("Subscribe");
        final JTextField subscribeTextBox = new JTextField(10);
        buttonPanel.add(subscribeButton);
        subscribePanel.add(subscribeButton);
        subscribePanel.add(subscribeTextBox);
//        c.fill = GridBagConstraints.HORIZONTAL;
//        c.gridx = 0;
//        c.gridy = 0;
        buttonPanel.add(subscribePanel); //,c);

        //Publish gui
        JPanel publishPanel = new JPanel(new FlowLayout());
        JButton publishButton = new JButton("Publish");
        final JTextField publishNameTextBox = new JTextField(10);
        final JTextField publishValueTextBox = new JTextField(10);
        publishPanel.add(publishButton);
        publishPanel.add(publishNameTextBox);
        publishPanel.add(publishValueTextBox);
        //c.gridx = 1;
       // c.gridy = 0;
        buttonPanel.add(publishPanel);//, c);

        //UnSubscribe gui
        JPanel unsubscribePanel = new JPanel(new FlowLayout());
        final JButton unsubscribeButton = new JButton("UnSubscribe");
        final JTextField unsubscribeTextBox = new JTextField(10);
        unsubscribePanel.add(unsubscribeButton);
        unsubscribePanel.add(unsubscribeTextBox);
//        c.gridx = 0;
//        c.gridy = 1;
        buttonPanel.add(unsubscribePanel);

        //Update gui
        JPanel updatePanel = new JPanel(new FlowLayout());
        JButton updateButton = new JButton("Update");
        final JTextField updateNameTextBox = new JTextField(10);
        final JTextField updateValueTextBox = new JTextField(10);
        updatePanel.add(updateButton);
        updatePanel.add(updateNameTextBox);
        updatePanel.add(updateValueTextBox);
//        c.gridx = 1;
//        c.gridy = 1;
        buttonPanel.add(updatePanel);

        //Delete gui
        JPanel deletePanel = new JPanel(new FlowLayout());
        JButton deleteButton = new JButton("Delete");
        final JTextField deleteNameTextBox = new JTextField(10);
        deletePanel.add(deleteButton);
        deletePanel.add(deleteNameTextBox);
//        c.gridx = 0;
//        c.gridy = 2;
        buttonPanel.add(deletePanel);

        //console gui
        scrollContainer = new JScrollPane(console);
//        c.ipady = 150;
//        c.gridwidth = 2;
//        c.gridx = 0;
//        c.gridy = 3;
        buttonPanel.add(scrollContainer);
        consoleDoc = console.getStyledDocument();

        window.setLayout(new BorderLayout());
        window.getContentPane().add(buttonPanel, BorderLayout.CENTER);
        window.pack();


//##### action listeners ##### - calls server rmi function on button click

        //### Subscribe ###
        subscribeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                //get gui text box text
                String subscribeDataName = subscribeTextBox.getText();

                //if not already subbed
                if (!subscriptionDataItems.containsKey(subscribeDataName)){
                    try {
                        //try rmi server subscribe
                        subscriptionServer.Subscribe(subscribeDataName, "NotifyMe" + clientId);
                        println("Subscribe: Success - '" + subscribeDataName + "'.");
                    }
                    catch (RemoteException ex) {//remote rmi error
                        println("Client Remote Error: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    catch (SubscriptionException ex) {//error
                        println("Subscribe: Error - " + ex.getMessage());
                    }
                }
                else {
                    //else already subbed - print error
                    println("Subscribe Error: Already subscribed");
                }

                //clear text box
                subscribeTextBox.setText("");
            }
        });

        //### UnSubscribe ###
        unsubscribeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                //prepare for exceptions
                try {
                    String unsubscribeDataName = unsubscribeTextBox.getText();      //get text
                    subscriptionServer.UnSubscribe(unsubscribeDataName, clientId);  //rmi unsub
                    println("UnSubscribe: '" + unsubscribeDataName + "'.");         //print feedback

                }
                catch (RemoteException ex) {//remote error (didnt connect rmi)
                    println("Client Remote Error: " + ex.getMessage());
                    ex.printStackTrace();
                }
                catch (SubscriptionException ex) {//function error (already unsubbed)
                    println("UnSubscribe Error: " + ex.getMessage());
                }

                //clear textbox
                unsubscribeTextBox.setText("");
            }
        });

        //### Publish ###
        publishButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {                                                                 //todo check TEXT BOX empty, not int
                //prepare for exceptions
                try{
                    //get text box values and call server publish
                    String publishDataName = publishNameTextBox.getText();
                    int publishDataValue = Integer.parseInt(publishValueTextBox.getText());
                    int publishId = subscriptionServer.Publish(publishDataName, publishDataValue);  //call rmi publish, save returned publish id

                    //save local copy of publisher id with data id name
                    publishIds.put(publishDataName, publishId);
                    println("Publish: Success - '" + publishDataName + "', value '" + publishDataValue + "', id: '" + publishId + "'.");
                }
                catch (RemoteException ex) {//remote error
                    println("Publish: Failed (Client Remote Error) - " + ex.getMessage());
                    ex.printStackTrace();
                }
                catch(SubscriptionException ex){//function error
                    println("Publish: Failed - " + ex.getMessage());
                }

                //clear the gui text boxes
                publishNameTextBox.setText("");
                publishValueTextBox.setText("");
            }
        });

        //### Update ###
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                //get gui text
                String updateDataName = updateNameTextBox.getText();

                //check client owns
                if(publishIds.containsKey(updateDataName))
                {
                    //call rmi update function - prepare for exceptions
                    try {
                        //get text box inputs and local copy of publisher id
                        int updateDataValue = Integer.parseInt(updateValueTextBox.getText());
                        int publishedId = publishIds.get(updateDataName);

                        //call to rmi
                        subscriptionServer.Update(updateDataName, publishedId, updateDataValue);
                        println("Update: Success - '" + updateDataName + "', new value '" + updateDataValue + "'.");
                    } catch (RemoteException ex) {//remote error
                        println("Update: Failed (Client Remote Error) - " + ex.getMessage());
                        ex.printStackTrace();
                    } catch (SubscriptionException ex) {//function error
                        println("Update: Failed - " + ex.getMessage());
                    }
                }
                else {
                    println("Update: Failed - You do not own this data item.");
                }

                //clear textboxes
                updateNameTextBox.setText("");
                updateValueTextBox.setText("");
            }
        });

        //### Delete ###
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                //get local copy of publisher id for that data item
                String deleteDataName = deleteNameTextBox.getText();
                int publishedId = -1;
                if(publishIds.containsKey(deleteDataName)) {
                    publishedId = publishIds.get(deleteDataName);
                }

                //prepare for exceptions
                try{
                    subscriptionServer.Delete(deleteDataName, publishedId);
                    println("Deleted: Success - '" + deleteDataName + "' deleted.");

                    //if subscribed delete local copy
                    if(subscriptionDataItems.containsKey(deleteDataName)){
                        subscriptionDataItems.remove(deleteDataName);
                    }

                    //delete published id
                    if(publishIds.containsKey(deleteDataName)) {
                        publishIds.remove(deleteDataName);
                    }
                }
                catch (RemoteException ex) {//remote error
                    println("Delete: Failed (Client Remote Error) - " + ex.getMessage());
                    ex.printStackTrace();
                }
                catch(SubscriptionException ex){//function error
                    println("Delete: Failed - " + ex.getMessage());
                }

                //clear textbox
                deleteNameTextBox.setText("");
            }
        });
    }

    //### call back function ### - from server on subscription update/delete
    public void NotifyMe(String dataItemName, int updatedValue, boolean deleted){
        if(deleted){
            //item was deleted - remove local
            subscriptionDataItems.remove(dataItemName);
            println("Subscription Deleted: '" + dataItemName + "'.");
        }
        else {
            //update local copy
            if (publishIds.containsKey(dataItemName)) {
                //update the value
                subscriptionDataItems.replace(dataItemName, updatedValue);
            } else {
                //if dont exist add to local copy
                subscriptionDataItems.put(dataItemName, updatedValue);
            }

            println("Subscription Updated: '" + dataItemName + "', new value: '" + updatedValue + "'.");
        }
    }


    //### print ### - function to print to gui output console window
    private static void print(String text){
        try {
            System.out.print(text); //copy print to cmd window
            consoleDoc.insertString(consoleDoc.getLength(), text + ">> ", null);   //append new string
            console.selectAll();                                                   //auto scroll
        } catch(Exception e){ System.out.println(e);}
    }

    private static void println(String text){
        print(text + "\n");                 //invoke print with newline
    }

}