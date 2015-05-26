/**
 * Name: PeerInformation.java 
 * Created: 05.2015 (mm/YYYY)
 *
 * @author Ken
 * @author Jony
 *
 */
package dmsassign3;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;

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
