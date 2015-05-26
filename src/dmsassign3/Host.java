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
import static java.lang.Thread.sleep;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
    JTextArea systemip = new JTextArea();

    JLabel isServerLabel = new JLabel("");

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
    boolean isSelfInitiated = false;
    boolean electionDecided = false;

    PeerInformation peerInfo = new PeerInformation();

    boolean haltUpdates = false;

    private Peer thisPeer = null;

    Thread tcpServer;
    Thread leaderElection;

    public Host(boolean isServer, String ip) {
        String localIP = null;
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
            systemip.setText("local: localIP");
        } catch (UnknownHostException ex) {
            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.isServer = isServer;
        ourIP = localIP;    // We need to change this to represent the outbound
        // network adapter IP
        if (isServer) {
            // Try to create the RMI object
            // Since this is the server create the RMI server and begin listing
            // for TCP and RMI requests.
            isServerLabel.setText("Server");
            processID = 0;
            boolean successful = initRMI();
            boolean tcpSuccessful = initTCPServ();
            leaderIP = ourIP;

            // Add ourselfs to our own peer array
            thisPeer = new Peer(leaderIP, String.valueOf(SERVER_TCP_PORT), processID, true);
            peers.add(thisPeer);

        } else {
            // If this host is a client attempt to connect on TCP to initiate
            // handshake with remote host.
            isServerLabel.setText("Client");
            boolean tcpSuccessful = initTCPServ();
            boolean successful = connectTCP(ip);

            if (successful) {
                // Since we successfully joined the system start our own TCP
                // server to listen for requests.
                boolean rmiSuccessful = connectRMI(leaderIP);

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

        gbc.gridx = 1;
        gbc.gridy = 0;
        p1.add(systemip);
        gbc.gridx = 1;
        gbc.gridy = 1;
        p1.add(isServerLabel);

        container.add(p1);

        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Attempt to add the booking

                openAdd();

            }
        });

        frame.setSize(400, 400);
        frame.add(container);
        frame.setVisible(true);
        frame.setResizable(false);
        //frame.setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Create a timer that continiously checks connection to the main server      
        Timer checkingTimer = new Timer(2000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                checkConnection();
            }
        });
        checkingTimer.setInitialDelay(2000);
        checkingTimer.start();

        // Create a timer that continiously updates the Booking list every second
        Timer updateTimer = new Timer(2000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                updateBookings();
            }
        });
        updateTimer.setInitialDelay(500);
        updateTimer.start();
    }

    private void openAdd() {
        Add add = new Add(rObject);
    }

    private synchronized void updateBookings() {
        if (!haltUpdates) {
            try {
                // Update the bookings
                bookings = rObject.getBookings();
            } catch (RemoteException ex) {
                //Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
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
    }

    private void checkConnection() {
        // We check the status of connected peers
        System.out.println("Beginning connection testing of peers");
        for (int i = 0; i < peers.size(); ++i) {
            // Test a connection to the connected peer.  If connection fails to 
            // a peer they are remove from the peer list, if the disconnected
            // peer is the leader initiate a leader election.
            Peer p = peers.get(i);
            if (p.equals(thisPeer)) {
                // Don't bother PING'ing this peer since we obiously are still alive
                continue;
            }

            Socket socket = null;
            try {
                // Set the time out to 8 seconds before we declare that we cannot reach the client
                SocketAddress sockaddr = new InetSocketAddress(p.getIpAddress(), Integer.parseInt(p.getPortNumber()));
                socket = new Socket();
                System.out.println("Attempting to connect to peer (" + p.toString());
                socket.connect(sockaddr, 7000);
            } catch (IOException e) {
                System.err.println("CHECKING: Client could not make connection: " + e);
                //System.exit(-1);
                // Could not make connection to remote peer remove the peer from
                // the peer list, check if the peer is the leader if so initiate
                // leader election
                peers.remove(i);
                if (p.isIsLeader()) {
                    // The disconnected peer was the leader initiate a leader election
                    // only start if there is not a currently running leader election     
                    // Check to see if there are any ids higher than ours

                    // if our ID is not the largest wait for 20 seconds before initiaiting
                    // a new leader election because another node will more then likely
                    // handle the the election
                    boolean highestID = true;
                    for (int j = 0; j < peers.size(); ++j) {
                        if (thisPeer.getPeerID() < peers.get(j).getPeerID()) {
                            highestID = false;
                        }
                    }

                    if (highestID) {
                        if (leaderElection == null) {
                            System.out.println("Calling leader election from check connection after detail highest id");
                            leaderElection = new LeaderElection();
                            leaderElection.run();
                        }
                    } else {
                        try {
                            // Wait a time for another process to handle the message
                            System.out.println("Waiting...");
                            sleep(2000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        // If there is a new leader ignore
                        boolean newLeader = false;
                        for (int j = 0; j < peers.size(); ++j) {
                            if (peers.get(j).isIsLeader()) {
                                newLeader = true;
                            }
                        }

                        if (newLeader  == false || electionDecided == false) {
                            if (leaderElection == null) {
                                System.out.println("Calling leader election from check connection after detail");
                                leaderElection = new LeaderElection();
                                leaderElection.run();
                            }
                        }
                    }
                }

                // Stop checking peers this round
                break;
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
                if (serverResponse != null) {
                    if (serverResponse.equals("PONG")) {
                        // Correct response from pinging server
                    } else {
                        // Incorrect response from pinging server
                    }
                } else {
                    // We must of lost connection during our transmission
                    // Try the connection another time.
                    continue;
                }
                // Send the server the done message
                pw.println("DONE");

            } catch (IOException e) {
                // Unable to PING server this might mean we are disconnected
                System.err.println("Client error: " + e);
                break;
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

        String[] info = new String[peers.size()];

        for (int i = 0; i < peers.size(); ++i) {
            String result = "";
            Peer p = peers.get(i);
            result += "Host:" + p.getIpAddress() + "   Port:" + p.getPortNumber() + "    PID:" + p.getPeerID() + "   Leader:" + p.isIsLeader();

            info[i] = result;
        }

        peerInfo.setPeers(info);
    }

    private boolean connectRMI(String ip) {

        // This method connects to a remote RMI
        System.out.println("Attemptin to connect to remote RMI host:" + ip);
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
        System.setProperty("java.rmi.server.hostname", ourIP);

        String ip;
        boolean successful = false;
        RMIBookingImpl remoteObject
                = new RMIBookingImpl();

        try {
            ip = InetAddress.getLocalHost().getHostAddress();
            systemip.setText("Local: " + ip);
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

        try {
            remoteObject.setBookings(bookings);
        } catch (RemoteException ex) {
            // Just incase we are creating the RMI server again
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
            try {
                socket = new Socket(ip, CLIENT_TCP_PORT);
            } catch (IOException ee) {
                System.err.println("Client could not make connection: " + ee);
                System.exit(-1);
            }
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
                socket = new Socket(peers.get(i).getIpAddress(), Integer.parseInt(peers.get(i).getPortNumber()));
            } catch (IOException e) {
                System.err.println("Client could not make handshake connection to peer(" + peers.get(i).toString() + "): " + e);
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
            tcpServer = new TCPServer();
            tcpServer.start();
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
                System.out.println("Starting TCP Server on Server Port:" + SERVER_TCP_PORT);
                tcpPort = SERVER_TCP_PORT;
            } else {
                System.out.println("Starting TCP Server on Server Port:" + CLIENT_TCP_PORT);
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
            serverStopped = false;

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
                stopTCPServ = false;
                serverStopped = true;
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
                        if (i == peers.size() - 1) {
                            response += peers.get(i);
                        } else {
                            response += peers.get(i) + ":";
                        }
                    }
                    System.out.println("Prepared Response:" + response);
                    break;
                }
                case "Join": {
                    // A new node wants to join the network
                    // We return return the host address of the current leader
                    int highestID = 0;
                    for (int i = 0; i < peers.size(); ++i) {
                        if (peers.get(i).getPeerID() >= highestID);
                        {
                            highestID = peers.get(i).getPeerID() + 1;
                        }
                    }
                    response = "LEADER:" + leaderIP + ":" + highestID;
                    break;
                }
                case "Handshake": {
                    // Add the calling peer to our peer list
                    String[] newPeer = (tokens[1]).split("//");
                    peers.add(new Peer(newPeer[0], newPeer[1], Integer.parseInt(newPeer[2]), Boolean.parseBoolean(newPeer[3])));
                    response = "HANDSHAKEOk";
                    break;
                }
                case "PING": {
                    response = "PONG";
                    break;
                }
                case "ElectionMessage": {
                    // We have received an election message from a peer
                    int proposingPeerID = Integer.parseInt(tokens[1]);

                    if (thisPeer.getPeerID() > proposingPeerID) {
                        // Our peerID is larger reply ALIVE to the peer and initiate
                        // new leader election
                        // Our peerID is larger initiate a new leader election
                        // Only start a new leader election if we havnt allready started an election.
                        if (leaderElection == null) {
                            System.out.println("Calling leader election from Election message request");
                            leaderElection = new LeaderElection();
                            leaderElection.run();
                        }

                        response = "ALIVE";
                    }
                    break;
                }
                case "LeaderMessage":
                    // We have received an leader message from a peer
                    int proposingPeerID = Integer.parseInt(tokens[1]);
                    String proposingIP = tokens[2];

                    if (thisPeer.getPeerID() > proposingPeerID) {
                        // Our peerID is larger initiate a new leader election
                        System.out.println("Calling leader election from LeaderMessage bully");
                        leaderElection = new LeaderElection();
                        leaderElection.run();
                        response = "Ok-Bully";
                        break;
                    } else {
                        try {
                            // New leader is authentic restart tcp and rmi connections
                            // Add the server as a new peer and let the old client peer die at the
                            // next connection test
                            // wait just in case the server needs more time to initiate

                            wait(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        leaderIP = proposingIP;
                        electionDecided = true;
                        if (leaderElection != null) {
                            leaderElection.interrupt();
                            leaderElection.stop();
                            leaderElection = null;
                        }
                        boolean rmiSuccessful = connectRMI(leaderIP);

                        // Add the leader as a new peer and we can let the old
                        // die 
                        peers.add(new Peer(leaderIP, String.valueOf(SERVER_TCP_PORT), proposingPeerID, true));
                        System.out.println("Adding server peer");
                    }

                    response = "Ok";
                    break;
            }
            return response;
        }
    }

    // Inner class that handles starting a leader election
    public class LeaderElection extends Thread {

        // Set timeouts for connections here
        @Override
        public void run() {
            electionDecided = false;
            // Initiate leader election
            System.out.println("Initiating leader election");

            // Alive messages
            int aliveCount = 0;

            // Broadcast an election message to all connected peers that have a higher process ID.
            for (int i = 0; i < peers.size(); ++i) {
                Socket socket = null;
                Peer p = peers.get(i);
                if (p.getPeerID() > thisPeer.getPeerID()) {

                    try {
                        socket = new Socket(p.getIpAddress(), Integer.parseInt(p.getPortNumber()));
                    } catch (IOException f) {
                        // Double up the try statement and also check the peer with a server
                        // port incase they won the election before we started
                        System.err.println("LEADER ELECTION: Client could not make connection to peer(" + p.toString() + "): " + f);
                        // Couldn't connect to this host,  we will just continue and handle
                        // peer deletion in the checkPeers method
                        continue;
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
                        clientRequest = "ElectionMessage:" + thisPeer.getPeerID();
                        pw.println(clientRequest);  // println flushes itself
                        // then get server response and display it
                        String[] serverResponse = (br.readLine()).split(":"); // blocking

                        System.out.println("Response: " + Arrays.toString(serverResponse));

                        // Send the server the done message
                        pw.println("DONE");

                        if (serverResponse[0].equals("ALIVE")) {
                            ++aliveCount;
                        }

                    } catch (IOException e) {
                        // This means that we likely have crashed.
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

            // Now that we have contacted all our peers
            if (aliveCount > 0) {
                isSelfInitiated = true;

                System.out.println("Entering Sleep while we wait for replys");
                try {
                    sleep(7500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
                }

                System.out.println("Exiting Sleep while we wait for replys");

                // Check to see if the election has been decided\
                // If there is a new leader ignore
                boolean newLeader = false;
                for (int j = 0; j < peers.size(); ++j) {
                    if (peers.get(j).isIsLeader()) {
                        newLeader = true;
                    }
                }

                System.out.println("New Leader = " + newLeader);
                if (!newLeader || !electionDecided) {
                    if (leaderElection == null) {
                        System.out.println("Calling leader election from leader election alive");
                        leaderElection = new LeaderElection();
                        leaderElection.run();
                    }
                }

            } else if (aliveCount == 0) {
                // There are no other peers so elect ourself as leader
                // Initialise the RMI server
                isServer = true;
                stopTCPServ = true;
                while (serverStopped == false) {
                    try {
                        // Loop until the TCP server stops
                        sleep(100);

                    } catch (InterruptedException ex) {
                        Logger.getLogger(Host.class
                                .getName()).log(Level.SEVERE, null, ex);
                    }
                }

                boolean initTCPServ = initTCPServ();
                boolean rmiInit = initRMI();
                isServer = true;
                becomeServer();

                System.out.println("We won leader election, become new leader");
                boolean bully = false;

                for (int i = 0; i < peers.size(); ++i) {

                    Socket socket = null;
                    Peer p = peers.get(i);

                    System.out.println("Sending LeaderMsg:  (" + p.toString() + ")");

                    if (p.equals(thisPeer)) {
                        continue;
                    }

                    try {
                        socket = new Socket(p.getIpAddress(), Integer.parseInt(p.getPortNumber()));
                    } catch (IOException e) {
                        // Server Died as we were sending messages

                        System.err.println("Sending new Leader:  Client could not make connection to peer(" + p.toString() + "): " + e);
                        continue;
                    }

                    PrintWriter pw = null; // output stream to server
                    BufferedReader br = null; // input stream from server
                    try {  // create an autoflush output stream for the socket
                        pw = new PrintWriter(socket.getOutputStream(), true);
                        // create a buffered input stream for this socket
                        br = new BufferedReader(new InputStreamReader(
                                socket.getInputStream()));

                        String clientRequest;

                        // Send the new leader message
                        clientRequest = "LeaderMessage:" + thisPeer.getPeerID() + ":" + thisPeer.getIpAddress();
                        pw.println(clientRequest);  // println flushes itself
                        // then get server response and display it
                        String line = br.readLine();
                        if (line != null) {
                            String[] serverResponse = line.split(":"); // blocking

                            System.out.println("Response: " + Arrays.toString(serverResponse));

                            if (serverResponse[0].equals("Ok-Bully")) {
                                // A process with a higher ID is bullying us out of 
                                // the leader position
                                bully = true;
                                break;
                            }
                        }
                        // Send the server the done message
                        pw.println("DONE");

                    } catch (IOException e) {
                        // This means that we likely have crashed.
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

                if (!bully) {

                }
                leaderElection = null;

            }
        }
    }

    public void becomeServer() {
        isServerLabel.setText("Server");
        leaderIP = ourIP;
        for (int i = 0; i < peers.size(); ++i) {
            if (peers.get(i).equals(thisPeer)) {
                peers.get(i).setPortNumber("14201");
                peers.get(i).setIsLeader(true);
            }
        }
        try {
            rObject.setBookings(bookings);

        } catch (RemoteException ex) {
            Logger.getLogger(Host.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }
}
