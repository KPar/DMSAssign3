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
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author Ken
 */
public class Add {
    RMIBooking booking;
    
    String[] capacity = {"1","2","3","4","5","6+"};
    
    JFrame frame = new JFrame("Enter Details");
    
    JPanel p1 = new JPanel(new GridBagLayout());
    JPanel container = new JPanel();
    
    JButton submit = new JButton("Submit");
    
    JTextField name = new JTextField("Enter name",20);
    JTextField checkindate = new JTextField("Check In date", 10);
    JTextField checkoutdate = new JTextField("Check Out date", 10);
    JLabel nameLabel = new JLabel("Name");
    JLabel checkinLabel = new JLabel("Check-In Date");
    JLabel checkoutLabel = new JLabel("Check-Out Date");
    JLabel people = new JLabel("Room Capacity");
    
    JComboBox roomCapacity = new JComboBox(capacity);
   
    FlowLayout layout = new FlowLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    
    public Add(RMIBooking rObject) {
        booking = rObject;
        gbc.insets = new Insets(5,0,0,0); 
        container.setLayout(layout);
        
        gbc.gridx =1;
        gbc.gridy =1;
        p1.add(nameLabel,gbc);
        
        gbc.gridx =1;
        gbc.gridy =2;
        p1.add(name,gbc);
        
        gbc.gridx =1;
        gbc.gridy =3;
        p1.add(checkinLabel,gbc);
        
        gbc.gridx =1;
        gbc.gridy =4;
        p1.add(checkindate,gbc);
        
        gbc.gridx =1;
        gbc.gridy =5;
        p1.add(checkoutLabel,gbc);
        
        gbc.gridx =1;
        gbc.gridy =6;
        p1.add(checkoutdate,gbc);
        
        gbc.gridx =1;
        gbc.gridy =7;
        p1.add(people,gbc);
        
        gbc.gridx =1;
        gbc.gridy =8;
        p1.add(roomCapacity,gbc);
        
        gbc.gridx =1;
        gbc.gridy =9;
        p1.add(submit,gbc);
        
       container.add(p1);
       
       submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addBooking();
            }
        });
       
       frame.setSize(400,400);
       frame.add(container);
       frame.setVisible(true);
       frame.setResizable(true);
       frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        
    }
    
    public void addBooking()
    {
        try {
            booking.addBooking(nameLabel.getText(), checkindate.getText(), 1);
        } catch (RemoteException ex) {
            Logger.getLogger(Add.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
