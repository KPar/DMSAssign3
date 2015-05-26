/**
 * Name: Peer.java 
 * Created: 05.2015 (mm/YYYY)
 *
 * @author Ken
 * @author Jony
 *
 */
package dmsassign3;

import java.util.Objects;

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
    private boolean hasLock = false;

    public Peer(String ipAddress, String portNumber, int peerID, boolean isLeader) {
         this.ipAddress = ipAddress;
         this.portNumber = portNumber;
         this.peerID = peerID;
         this.isLeader = isLeader;
    }

    public boolean hasLock() {
        return hasLock;
    }

    public void setHasLock(boolean hasLock) {
        this.hasLock = hasLock;
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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.ipAddress);
        hash = 59 * hash + Objects.hashCode(this.portNumber);
        hash = 59 * hash + this.peerID;
        hash = 59 * hash + (this.isLeader ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Peer other = (Peer) obj;
        if (this.peerID != other.peerID) {
            return false;
        }
        return true;
    }
       
    
}
