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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
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

    // We make this distinction so that it is possible to test a server and client
    // on the same host.
    boolean isServer;
    static public final int CLIENT_TCP_PORT = 14200;
    static public final int SERVER_TCP_PORT = 14201;

    // Hold a reference to the remote object
    RMIBooking rObject;

    // Hold our own copy of the ArrayList of bookings incase we become the server
    ArrayList<Booking> bookings = new ArrayList<>();

    JFrame frame = new JFrame();

    JPanel p1 = new JPanel(new GridBagLayout());
    JPanel container = new JPanel();
    JTextArea bookingView = new JTextArea();

    JButton add = new JButton("Add");
    JLabel bookingsLabel = new JLabel("Bookings");
    FlowLayout layout = new FlowLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    // Here we will hold various TCP networking variables
    private ArrayList<Peer> peers = new ArrayList<>();
    private boolean stopTCPServ = false;  // Incase we need to restart the server
    private boolean serverStopped = true;
    private String leaderIP;
    private String ourIP;
    private int processID;

    private Peer thisPeer = null;

    public Host(boolean isServer, String ip) {

        this.isServer = isServer;
        ourIP = "192.168.1.11";    // We need to change this to represent the outbound
        // network adapter IP
        if (isServer) {
            // Try to create the RMI object
            // Since this is the server create the RMI server and begin listing
            // for TCP and RMI requests.
            processID = 0;
            boolean successful = initRMI();
            boolean tcpSuccessful = initTCPServ();
            leaderIP = "192.168.1.11";
            // Add ourselfs to our own peer array
            peers.add(new Peer(leaderIP, String.valueOf(SERVER_TCP_PORT), processID, true));

        } else {
            // If this host is a client attempt to connect on TCP to initiate
            // handshake with remote host.
            boolean successful = connectTCP(ip);

            if (successful) {
                // Since we successfully joined the system start our own TCP
                // server to listen for requests.
                boolean rmiSuccessful = connectRMI(leaderIP);
                boolean tcpSuccessful = initTCPServ();
            }
        }

        gbc.insets = new Insets(15, 15, 15, 15);
        container.setLayout(layout);

        gbc.gridx = 1;
        gbc.gridy = 1;
        p1.add(add, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        p1.add(bookingsLabel, gbc);
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
        //frame.setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Create a timer that continiously checks connection to the main server      
        Timer checkingTimer = new Timer(1000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                checkConnection();
            }
        });
        checkingTimer.setInitialDelay(500);
        //checkingTimer.start();

        // Create a timer that continiously updates the Booking list every second
        Timer updateTimer = new Timer(1000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                updateBookings();
            }
        });
        updateTimer.setInitialDelay(500);
        updateTimer.start();
    }

    private synchronized void updateBookings() {
        try {
            // Update the bookings
            bookings = rObject.getBookings();
        } catch (RemoteException ex) {
            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
            // We were unable to update the bookings from the main server
            // we will initialise leader election in the next handshake
            return;
        }
        String result = "";
        for (int i = 0; i < bookings.size(); ++i) {
            result += bookings.get(i).toString() + "\n";
        }
        InetAddress thisIp;

        bookingView.setText(result);
    }

    private void checkConnection() {
        // We check the status of connected peers
        for (int i = 0; i < peers.size(); ++i) {
            // Test a connection to the connected peer.  If connection fails to 
            // a peer they are remove from the peer list, if the disconnected
            // peer is the leader initiate a leader election.
            Peer p = peers.get(i);

            Socket socket = null;
            try {
                socket = new Socket(p.getIpAddress(), Integer.parseInt(p.getPortNumber()));
            } catch (IOException e) {
                System.err.println("Client could not make connection: " + e);
                //System.exit(-1);
                // Could not make connection to remote peer remove the peer from
                // the peer list, check if the peer is the leader if so initiate
                // leader election
                peers.remove(i);
                if(p.isIsLeader())
                {
                    // The disconnected peer was the leader initiate a leader election
                    // new LeaderElection().run();
                }
            }

            PrintWriter pw = null; // output stream to server
            BufferedReader br = null; // input stream from server
            try {  // create an autoflush output stream for the socket
                pw = new PrintWriter(socket.getOutputStream(), true);
                // create a buffered input stream for this socket
                br = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                
                // Send a PING message
                String clientRequest = "PING";;                            
                pw.println(clientRequest);  // println flushes itself
                // then get server response and display it
                String serverResponse = br.readLine(); // blocking

                System.out.println("PING Response: " + serverResponse);
                if(serverResponse.equals("PONG"))
                {
                    // Correct response from pinging server
                }
                else
                {
                    // Incorrect response from pinging server
                }

                // Send the server the done message
                pw.println("DONE");

            } catch (IOException e) {
                // Unable to PING server this might mean we are disconnected
                System.err.println("Client error: " + e);
            } finally {
                try {
                    if (pw != null) {
                        pw.close();
                    }
                    if (br != null) {
                        br.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Failed to close streams: " + e);
                }
            }

        }
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
        } catch (RemoteException | NotBoundException ex) {
            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
        }
        return successful;
    }

    private boolean initRMI() {
        // This method creates a local RMI
        boolean successful = false;
        RMIBookingImpl remoteObject
                = new RMIBookingImpl();

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

    private boolean connectTCP(String ip) {
        // Try to initiate a TCP connect to a remote host to handshake and enter
        // the system
        boolean successful = false;
        Socket socket = null;

        try {
            socket = new Socket(ip, SERVER_TCP_PORT);
        } catch (IOException e) {
            System.err.println("Client could not make connection: " + e);
            System.exit(-1);
        }

        PrintWriter pw = null; // output stream to server
        BufferedReader br = null; // input stream from server
        try {  // create an autoflush output stream for the socket
            pw = new PrintWriter(socket.getOutputStream(), true);
            // create a buffered input stream for this socket
            br = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));

            String clientRequest;

            // start communication by having client connect
            // send the Join request which will return the IP of the current leader
            clientRequest = "Join";
            pw.println(clientRequest);  // println flushes itself
            // then get server response and display it
            String[] serverResponse = (br.readLine()).split(":"); // blocking

            System.out.println("Response: " + Arrays.toString(serverResponse));
            leaderIP = serverResponse[1];
            processID = Integer.parseInt(serverResponse[2]);

            thisPeer = new Peer(ourIP, String.valueOf(CLIENT_TCP_PORT), processID, isServer);

            // Now ask the server for a current listing of all system nodes
            clientRequest = "ListNodes:" + new Peer(ourIP, String.valueOf(CLIENT_TCP_PORT), processID, false).toString();
            pw.println(clientRequest);  // println flushes itself
            serverResponse = br.readLine().split(":"); // blocking
            for (int i = 0; i < serverResponse.length; ++i) {
                // Loop over all the nodes in the system and add them to our own
                // array list
                String[] nodeDetails = serverResponse[i].split("//");
                Peer p = new Peer(nodeDetails[0], nodeDetails[1], Integer.parseInt(nodeDetails[2]), Boolean.parseBoolean(nodeDetails[3]));
                peers.add(p);
            }

            successful = true;

            // Send the server the done message
            pw.println("DONE");

        } catch (IOException e) {
            System.err.println("Client error: " + e);
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (br != null) {
                    br.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Failed to close streams: " + e);
            }
        }

        boolean handshakingSuccessful = handshakePeers();
        peers.add(thisPeer);

        return successful & handshakingSuccessful;
    }

    private boolean handshakePeers() {
        // We loop over all the peers in the peer list and handshake with them
        boolean successful = true;
        for (int i = 0; i < peers.size(); ++i) {
            Socket socket = null;

            try {
                socket = new Socket(peers.get(i).getIpAddress(), SERVER_TCP_PORT);
            } catch (IOException e) {
                System.err.println("Client could not make connection to peer(" + peers.get(i).toString() + "): " + e);
                System.exit(-1);
            }

            PrintWriter pw = null; // output stream to server
            BufferedReader br = null; // input stream from server
            try {  // create an autoflush output stream for the socket
                pw = new PrintWriter(socket.getOutputStream(), true);
                // create a buffered input stream for this socket
                br = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));

                String clientRequest;

                // Handshake with the peer
                clientRequest = "Handshake:" + thisPeer.toString();
                pw.println(clientRequest);  // println flushes itself
                // then get server response and display it
                String[] serverResponse = (br.readLine()).split(":"); // blocking

                System.out.println("Response: " + Arrays.toString(serverResponse));

                successful = successful & true;
                // Send the server the done message
                pw.println("DONE");

            } catch (IOException e) {
                System.err.println("Client error: " + e);
                successful = successful & false;
            } finally {
                try {
                    if (pw != null) {
                        pw.close();
                    }
                    if (br != null) {
                        br.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Failed to close streams: " + e);
                }
            }
        }

        return successful;
    }

    private boolean initTCPServ() {
        // Try to initiate a local TCP server to handle incomming handshakes from
        // other system peers
        boolean successful = false;
        // First check that the server is not allready running
        if (serverStopped == false) {
            // The server was not stopped before trying to create it again.          
        } else {
            // Initialise the TCP server inside its own dedicated thread
            // wait until the server has started
            new TCPServer().start();
            int attempts = 0;
            while (serverStopped == false) {
                try {
                    // Wait
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return successful;
    }

    // Inner class that handles the TCP server
    public class TCPServer extends Thread {

        @Override
        public void run() {
            int tcpPort = 0;
            if (isServer) {
                System.out.println("Starting TCP Server on Server Port");
                tcpPort = SERVER_TCP_PORT;
            } else {
                System.out.println("Starting TCP Server on Server Port");
                tcpPort = CLIENT_TCP_PORT;
            }

            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(tcpPort);
                serverSocket.setSoTimeout(2000); // timeout for accept
                System.out.println("Server started at "
                        + InetAddress.getLocalHost().getHostAddress() + " on port " + tcpPort);
            } catch (IOException e) {
                System.err.println("Server can't listen on port: " + e);
                System.exit(-1);
            }
            while (!stopTCPServ) {  // block until the next client requests a connection
                // or else the server socket timeout is reached
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("Connection made with "
                            + socket.getInetAddress());
                    // start an echo with this connection
                    EchoConnection echo = new EchoConnection(socket);
                    Thread thread = new Thread(echo);
                    thread.start();
                } catch (SocketTimeoutException e) {  // ignore and try again
                } catch (IOException e) {
                    System.err.println("Can't accept client connection: " + e);
                    stopTCPServ = true;
                }
            }
            try {
                serverSocket.close();
            } catch (IOException e) {  // ignore
            }
            System.out.println("Server stopping");
        }

        // inner class that represents a single connection to the server
        private class EchoConnection implements Runnable {

            private Socket socket; // socket for client/server communication
            public static final String DONE = "done"; // terminates echo

            public EchoConnection(Socket socket) {
                this.socket = socket;
            }

            @Override
            public void run() {
                PrintWriter pw = null; // output stream to client
                BufferedReader br = null; // input stream from client
                try {  // create an autoflush output stream for the socket
                    pw = new PrintWriter(socket.getOutputStream(), true);
                    // create a buffered input stream for this socket
                    br = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));

                    // Receive client messages and process them until DONE is 
                    // recieved
                    String clientRequest;
                    do {  // start communication by waiting for client request
                        clientRequest = br.readLine(); // blocking

                        // Process the request and return the response
                        System.out.println("TCPServ: Recieved request: " + clientRequest);
                        String serverResponse = handleRequst(clientRequest, socket.getInetAddress().toString());

                        pw.println(serverResponse);  // println flushes itself
                    } while (clientRequest != null
                            && !DONE.equalsIgnoreCase(clientRequest.trim()));
                    System.out.println("Closing connection with "
                            + socket.getInetAddress());
                } catch (IOException e) {
                    System.err.println("Server error: " + e);
                } finally {
                    try {
                        if (pw != null) {
                            pw.close();
                        }
                        if (br != null) {
                            br.close();
                        }
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to close streams: " + e);
                    }
                }
            }
        }

        // Part of the TCPServer inner class
        private synchronized String handleRequst(String request, String clientIP) {
            // Handle Incomming TCP requests
            // Split the request into an array on the delimter ":"
            String response = "";
            String[] tokens = request.split(":");
            switch (tokens[0]) {
                case "ListNodes": {
                    // Return the the client a string containing all the nodes
                    // in the system
                    for (int i = 0; i < peers.size(); ++i) {
                        if (i == peers.size() - 2) {
                            response += peers.get(i);
                        } else {
                            response += peers.get(i) + ":";
                        }
                    }
                    break;
                }
                case "Join": {
                    // A new node wants to join the network
                    // We return return the host address of the current leader
                    response = "LEADER:" + leaderIP + ":" + peers.size();
                    break;
                }
                case "Handshake": {
                    // Add the calling peer to our peer list
                    String[] newPeer = (tokens[1]).split("//");
                    peers.add(new Peer(newPeer[0], newPeer[1], Integer.parseInt(newPeer[2]), Boolean.parseBoolean(newPeer[3])));
                    response = "Ok";
                    break;
                }
                case "PING": {
                    response = "PONG";
                    break;
                }
                case "ElectionMessage": {

                    break;
                }
                case "LeaderMessage": {

                    break;
                }
            }

            return response;
        }
    }
}
