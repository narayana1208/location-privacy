package com.protocol2.location;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class ExtendedCheckBoxListAdapter extends BaseAdapter {
	private static ExtendedCheckBoxListAdapter c = null;
	private Context context;
	// probably should use something else other than arraylist because it is so slow
	private List<Friend> friends = new ArrayList<Friend>();
	

	public ExtendedCheckBoxListAdapter(Context c) 
	{
		context = c;            
	}

	public static ExtendedCheckBoxListAdapter getInstance(Context context)
	{
		if ( c == null ){
			c = new ExtendedCheckBoxListAdapter(context);
		}
		return c;
	}
	
	// Clear the list of friends completely to basically set everything clean.
	public void clearFriends()
	{
		friends.clear();
	}
	
    public void addFriend(Friend friend) 
    {
        c.friends.add(friend);
    }
    
    public void removeFriend(Friend friend)
    {
    	int pos = getPosition(friend);
    	c.friends.remove(pos);
    }
    
    // Overloaded
    public Friend getFriend(String name)
    {
    	int i = 0;
    	int loc = -1;
    	Friend retVal = null;
    	while ( loc == -1 && i < c.friends.size() )
    	{
    		retVal = c.friends.get(i);
    		if ( (retVal.getName()).equalsIgnoreCase(name) )
    			return retVal;
    		i++;
    	}

    	return null;
    }
    //Overloaded
    public Friend getFriend(Long id)
    {
    	int i = 0;
    	int loc = -1;
    	Friend retVal = null;
    	while ( loc == -1 && i < c.friends.size() )
    	{
    		retVal = c.friends.get(i);
    		if ( (retVal.getID()).equals(id) )
    			return retVal;
    		i++;
    	}

    	return null;
    }
    
    
    public void setFriendsList(List<Friend> lit) 
    {
    	c.friends = lit;
    }

    /** 
     * @return The number of items this adapter offers
     */
    public int getCount() 
    {
        return c.friends.size();
    }
    /**
     * Return item at a specific position
     */
    public Friend getItem(int position) 
    {
        return c.friends.get(position);
    }

    public Long getFriendID(int position)
    {
    	return c.friends.get(position).getID();
    }
    
    /** 
     * Returns the position of an element
     */
    public int getPosition(Friend f) 
    {
        int count = getCount();
        for ( int i = 0; i < count; i++ ){
        	if (f.compareTo(getItem(i)) == 0)
        		return i;
        }
        return -1;
    }
    /**
     * Set selection of an item
     * @param value - true or false
     * @param position - position
     */
 
    public void setChecked(boolean value, int position) 
    {
        c.friends.get(position).getCheckBox().setChecked(value);
    }
    /**
     * Decides if all items are selectable
     * @return - true or false
     */
    public boolean areAllItemsSelectable() 
    {
        return false;
    }
 
    // need this as inherited method for BaseAdapter
    public long getItemId(int position) 
    {
         return position;
    }
    
    public ArrayList<Friend> getChecked()
    {
    	ArrayList<Friend> checkedFriends = new ArrayList<Friend>();
    	for (Friend f:c.friends){
    		if(f.getCheckBox().getChecked()){
    			checkedFriends.add(f);
    		}
    	}
    	return checkedFriends;
    }
 
    /**
     * Do not recycle a view if one is already there, if not the data could get corrupted and
     * the checkbox state could be lost.
     * @param convertView The old view to overwrite
     * @returns a CheckBoxifiedTextView that holds wraps around an CheckBoxifiedText */
    public View getView(int position, View convertView, ViewGroup parent )
    {
        return new ExtendedCheckBoxListView(context, c.friends.get(position).getCheckBox());
    }
}
 
 