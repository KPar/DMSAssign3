/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dmsassign3;

import java.awt.CardLayout;
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
import javax.swing.JTextField;

/**
 *
 * @author Ken
 */
public class DmsAssign3 {
    
    JFrame frame = new JFrame("Hotel Booking System");
    JPanel main = new JPanel();
    JPanel p1 = new JPanel(new GridBagLayout());
    JPanel host = new JPanel();
    
    JLabel title = new JLabel("Hotel Booking");
    
    JButton connectbutton = new JButton("Connect");
    JButton hostbutton = new JButton ("Host");
    JButton exitbutton = new JButton("Exit");
    
    
    JTextField ipfield = new JTextField("Enter IP Address here", 15);
    
    FlowLayout c1 = new FlowLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    
    public DmsAssign3() {
        
        gbc.insets = new Insets(10,0,0,0);
        
        main.setLayout(c1);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        p1.add(title,gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        p1.add(ipfield,gbc);
        
        gbc.gridx = 3;
        gbc.gridy = 2;
        p1.add(connectbutton,gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 3;
        p1.add(hostbutton,gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 4;
        p1.add(exitbutton,gbc);
        
        main.add(p1);
        
        hostbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                Host host = new Host();
            }
        });
        
        exitbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                System.exit(0);
            }
        });
        
        frame.setSize(300,200);
        frame.add(main);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        
    }

    
    public static void main(String[] args) {
       
        DmsAssign3 dmsassign3 = new DmsAssign3();
        
    }
    
}
