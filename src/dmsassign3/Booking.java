package dmsassign3;


import java.io.Serializable;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author j
 */
public class Booking implements Serializable{
    public String name;
    String date;
    int id;
    
    
    
    
    public Booking(String name, String date, int id)
    {
        this.name = name;
        this.date = date;
        this.id = id;
    }
    
    public String toString()
    {
        return ("Name:"+name+" date:"+date+" ID:"+id);
    }    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    
    
}
