/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dmsassign3;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;

/**
 *
 * @author Ken
 */
public class Host {

    RMIBooking rObject;
    
    JFrame frame = new JFrame();

    JPanel p1 = new JPanel(new GridBagLayout());
    JPanel container = new JPanel();
    JTextArea bookingView = new JTextArea();
    JTextArea systemip = new JTextArea();
    JLabel hostip = new JLabel("Host IP");

    JButton add = new JButton("Add");
    JLabel bookings = new JLabel("Bookings");
    FlowLayout layout = new FlowLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    public Host(boolean isServer, String ip) {
        

        if(isServer)
        {                
            // Try to create the RMI object
            boolean successful = initRMI();
        }
        else
        {
            boolean successful = connectRMI(ip);
        }
        
        gbc.insets = new Insets(15, 15, 15, 15);
        container.setLayout(layout);
        gbc.gridx = 1;
        gbc.gridy = 1;
        p1.add(add, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        p1.add(bookings, gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        p1.add(bookingView, gbc);
        
        
        container.add(p1);

        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {                
                // Attempt to add the booking
                Add add = new Add(rObject);
                
                
            }
        });

        frame.setSize(400, 400);
        frame.add(container);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        



        Timer timer = new Timer(1000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                updateBookings();
            }
        });
        timer.setInitialDelay(500);
        timer.start(); 
   }
    
    private void updateBookings()
    {                
                ArrayList<Booking> bookings = null;
                try {
                    // Update the bookings
                    bookings = rObject.getBookings();
                } catch (RemoteException ex) {
                    Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
                }
                String result = "";
                for(int i = 0; i < bookings.size(); ++i)
                {
                    result += bookings.get(i).toString()+"\n";
                }
                InetAddress thisIp;

                
                bookingView.setText(result);        
    }

    private boolean connectRMI(String ip) {

            // This method connects to a remote RMI
            boolean successful = false;
        try {            
            Registry registry = LocateRegistry.getRegistry(ip);
            RMIBooking remoteProxy
                    = (RMIBooking) registry.lookup("greeting");
            System.out.println("Client is up");
            rObject = remoteProxy;
            
            successful = true;
        } catch (RemoteException ex) {
            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotBoundException ex) {
            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
        }
        return successful;
    }

    private boolean initRMI() {
        // This method creates a local RMI
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        p1.add(hostip,gbc);
        
        gbc.gridx = 2;
        gbc.gridy = 0;
        p1.add(systemip);
        
        String ip;
        boolean successful = false;
        RMIBookingImpl remoteObject
                = new RMIBookingImpl();
        
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
            systemip.setText(ip);
        } catch (UnknownHostException ex) {
            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
        }


        try {  // create stub (note prior to Java 5.0 must use rmic utility)
            RMIBooking stub = (RMIBooking) UnicastRemoteObject.exportObject(remoteObject, 0);
            // get the registry which is running on the default port 1099
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("greeting", stub);//binds if not already
            // display the names currently bound in the registry
            System.out.println("Names bound in RMI registry");
            try {
                String[] bindings = Naming.list("localhost"); // no URL
                for (String name : bindings) {
                    System.out.println(name);
                }

                successful = true;
                rObject = stub;
            } catch (MalformedURLException e) {
                System.err.println("Unable to see names: " + e);
            }
        } catch (RemoteException e) {
            System.err.println("Unable to bind to registry: " + e);
        }
        // note that separate thread created to keep remoteObject alive
        System.out.println("Main method of RMIGreetingImpl done");
        return successful;
    }
    
        
       
        
        
    }

