/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dmsassign3;

/**
 *
 * @author j
 */
class Peer {
    // Hold various information about the Peers connected to the system
    private String ipAddress = "";
    private String portNumber = "";
    private int peerID = -1;
    private boolean isLeader = false;

    public Peer(String ipAddress, String portNumber, int peerID, boolean isLeader) {
         this.ipAddress = ipAddress;
         this.portNumber = portNumber;
         this.peerID = peerID;
         this.isLeader = isLeader;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(String portNumber) {
        this.portNumber = portNumber;
    }

    public int getPeerID() {
        return peerID;
    }

    public void setPeerID(int peerID) {
        this.peerID = peerID;
    }

    public boolean isIsLeader() {
        return isLeader;
    }

    public void setIsLeader(boolean isLeader) {
        this.isLeader = isLeader;
    }

    @Override
    public String toString() {
        return ipAddress + "//" + portNumber + "//" + peerID + "//" + isLeader;
    }
                    
}
