package com.protocol2.location;

// not sure if this class is needed. may just store all data in one large file.

public class Friend {
	private ExtendedCheckBox checkbox;
	private String name;
	private Long mID;
	private String key;
	private String mURL;
	
	
	public Friend(String n, Long id){
		name = n;
		mID = id;
		checkbox = new ExtendedCheckBox(n, true);
	}
	
	/* ================================================ MUTATORS ================================================ */
	public void setPublicKey (String url)
	{
		mURL = url;
	}
	// Set Diffie Helman key
	public void setDHKey (String k){
		key = k;
	}
	
	/* ================================================ ACCESSORS ================================================ */
	
	// Return Diffie Helman key
	public String getDHKey(){
		return key;
	}
	public String getName(){
		return name;
	}
	
	public Long getID ()
	{
		return mID;
	}
	
	public ExtendedCheckBox getCheckBox(){
		return checkbox;
	}
	
	//needs to be changed but currently compares name strings to see if two friends are the same
	public int compareTo(Friend f){
		if(f != null)
            return mID.compareTo(f.getID());
        else
             throw new IllegalArgumentException();
	}
}
