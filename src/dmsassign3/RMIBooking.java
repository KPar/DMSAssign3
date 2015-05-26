/**
 * Name: RMIBooking.java 
 * Created: 05.2015 (mm/YYYY)
 *
 * @author Ken
 * @author Jony
 *
 */

package dmsassign3;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RMIBooking extends Remote
{
   public String getGreeting() throws RemoteException;   
   public void addBooking(String date, String name, int id) throws RemoteException;
   public ArrayList<Booking> getBookings() throws RemoteException;
   public void cancelBooking(int id) throws RemoteException;

   public void setBookings(ArrayList<Booking> bookings) throws RemoteException;

   
}
