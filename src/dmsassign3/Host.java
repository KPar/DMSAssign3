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
import javax.swing.JButton;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Ken
 */
public class Host {
    
    JFrame frame = new JFrame();
    
    JPanel p1 = new JPanel(new GridBagLayout());
    JPanel container = new JPanel();
    
    JButton add = new JButton("Add");
    JLabel bookings = new JLabel("Bookings");
    FlowLayout layout = new FlowLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    
    public Host() {
       
       gbc.insets = new Insets(15,15,15,15); 
       container.setLayout(layout);
       
       gbc.gridx =1;
       gbc.gridy =1;
       p1.add(add,gbc);
       gbc.gridx =1;
       gbc.gridy =2;
       p1.add(bookings,gbc);
       
       container.add(p1);
       
       add.addActionListener(new ActionListener() {
           @Override
           public void actionPerformed(ActionEvent e) {
             Add add = new Add();
           }
       });
       
       frame.setSize(400,400);
       frame.add(container);
       frame.setVisible(true);
       frame.setResizable(false);
       frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        
    }
}
