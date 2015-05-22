package dmsassign3;

/**
   A remote interface that represents a greeting
   Note this interface should be on both the server and the client
   @see RMIGreetingImpl.java
*/
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RMIBooking extends Remote
{
   public String getGreeting() throws RemoteException;   
   public void addBooking(String date, String name, int id) throws RemoteException;
   public ArrayList<Booking> getBookings() throws RemoteException;
   public void cancelBooking(int id) throws RemoteException;
   
}
