/*
 * File: PeerInformation.java
 * Description:  This Panel is used to display information about connected peers
 * and who the current leader is
 */
package dmsassign3;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author j
 */
public class PeerInformation {

    RMIBooking booking;

    JFrame frame = new JFrame("Connected Peers");

    JPanel p1 = new JPanel(new FlowLayout());

    JList peers = new JList();

    FlowLayout layout = new FlowLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    public PeerInformation() {

        gbc.insets = new Insets(5, 0, 0, 0);

        p1.add(peers);

        frame.setSize(525, 350);
        frame.add(p1);
        frame.setVisible(true);
        frame.setResizable(true);
       //frame.setDefaultCloseOperation(EXIT_ON_CLOSE);

    }

    public void setPeers(String[] peers) {
        this.peers.setListData(peers);
    }

}
