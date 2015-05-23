package dmsassign3;

/**
   A class that implements a remote object handles bookings from various
   clients.
   @author Andrew Ensor
*/
import java.rmi.RemoteException;
import java.util.ArrayList;

public class RMIBookingImpl implements RMIBooking
{
   private String message;
   private ArrayList<Booking> bookings;
   
   public RMIBookingImpl()
   {  
      bookings = new ArrayList<>();
   }
   
   public String getGreeting()
   {  System.out.println("getGreeting method called");
      return message;
   }
   
    @Override
    public synchronized void addBooking(String date, String name, int id) throws RemoteException {
        bookings.add(new Booking(date,name,id));
        System.out.println("Added Booking");
        
    }
        
    @Override
    public synchronized ArrayList<Booking> getBookings() throws RemoteException {
        return bookings;
    }

    @Override
    public synchronized void cancelBooking(int id) throws RemoteException {
        for(int i = 0; i < bookings.size(); ++i)
        {
            if(bookings.get(i).id == id)
            {
                bookings.remove(i);
            }
        }
    }

    @Override
    public synchronized void setBookings(ArrayList<Booking> bookings) throws RemoteException {
        this.bookings = bookings;
    }
}
