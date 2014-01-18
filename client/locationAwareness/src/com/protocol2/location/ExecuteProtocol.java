package com.protocol2.location;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class ExecuteProtocol extends Activity {
	
	// ---- KEY and Facebook values .... //
	private BigInteger k_1 = BigInteger.valueOf(0); // temporary value TODO: this key is equal to the AES encryption of ( time concatenated with 1 )
	private BigInteger k_2 = BigInteger.valueOf(0); // temporary value TODO: this key is equal to the AES encryption of ( time concatenated with 2 )
	private Long myID = 0L; // temporary value TODO: retreive my facebook ID from the facebook API
	
	
	// ----- CONSTANTS ------//
	private static final BigInteger PRIME = BigInteger.valueOf(28147497699961L);
	
	//-----VARIABLES------//
	private Map<Long, ArrayList<BigInteger>> myLocationsMap = new HashMap<Long, ArrayList<BigInteger>>();
	private ExtendedCheckBoxListAdapter friendsList;
	

	// the client class that will interact with the server (self implemented class)
	private ClientClass mClient; 
	
	// Display object -- used for helping to display items on the screen
	private TextView mDisplay;
	private TextView mDisplay2;
	private TextView mNearby;
	private TextView mNotNearby;
	private TextView mNotAvail;
	
	
	public void onCreate(Bundle savedInstanceState) {
		
	    super.onCreate(savedInstanceState);
	    
	      // set up GUI
	    setContentView(R.layout.main);
	    setTitle("Nearby Friends");
	    mDisplay = (TextView) findViewById(R.id.textDisplay);
	    mDisplay2 = (TextView) findViewById(R.id.textDisplay2);
	    mNearby = (TextView) findViewById(R.id.nearbyPeople);
	    //mNearby.setTextSize(12);
	    mNotNearby = (TextView) findViewById(R.id.unknownLocPeople);
	    //mNotNearby.setTextSize(12);
	    mNotAvail = (TextView) findViewById(R.id.notAvailPeople);
	    //mNotAvail.setTextSize(12);
	      
	    mClient = new ClientClass(this.getString(R.string.IP_ADDRESS), 8080);
	      
	    friendsList = ExtendedCheckBoxListAdapter.getInstance(getApplicationContext());
	      
	    SharedPreferences mySavedID = getSharedPreferences("myID", MODE_PRIVATE); 
	    myID = mySavedID.getLong("myID", 0L);
	      
		/* display the latitude and longitude on the screen (mostly for debugging purposes)
	     mDisplay.setText(
	              new StringBuilder()
	                      .append("Latitude: ").append(pad(latitude)).append("\nLongitude: ").append(pad(longitude)));
		 */
   
	    Thread AThread = new Thread() {
	    	public void run() {
	    	
	    		   // Get friend IDs from the friend database
	    	    ArrayList<Friend> checkedFriends = friendsList.getChecked();
	    	    // Generate an array of friend ids (Longs) to send to the server
	    	    long[] checkedFriendIDs = new long[checkedFriends.size()];
	    	    int ci = 0;
	    	    System.err.println("Checked friends to send to server. Number of checked friends: " + checkedFriends.size());
	    	    for (Friend f:checkedFriends){
	    	    	System.err.println(f.getName() + "\t" + f.getID());
	    	    	checkedFriendIDs[ci] = f.getID();
	    	    	ci++;
	    	    }
	    	      
	    	    // Send friend IDs to the server
	    	    mClient.sendReadyPacket(myID, checkedFriendIDs);
	    			
	    	    // Receive info from the server -- get time byte and grid size corresponding to each friend ID:
	    	    Map<Long, ArrayList<Byte>> response = mClient.receiveInitialResponse();
	    	    
	    	    if (response != null)
	    	    {
	    		    Map<Long, ArrayList<BigInteger>> k_2Array = new HashMap<Long, ArrayList<BigInteger>>();      
	    		    // Set up helper variables
	    		    GridHandler gridHandler = new GridHandler();
	    		    Long friendID = 0L; // temporary value
	    		    ArrayList<Byte> timeGridBytes;
	    		    int gridsize;
	    		    Byte timeByte;
	    		    AESKeyGenerator keygen = new AESKeyGenerator();  // Create a key generator (that I created)
	    		    String dhkey; // declare the string that will hold the diffie helman key
	    		    Date curDate;
	    		    Long toEncrypt;     

	    	    	for(int i = 0; i < checkedFriends.size(); i++){
	    	    		Friend f = checkedFriends.get(i);
	    	    		friendID = f.getID();
	    		    	//check if friend is in the response --> if not, friend is not available, so show as not nearby
	    	    		//only put into list if friend is in map
	    	    		if (response.containsKey(friendID)){
	    	    			// Retrieve raw value of time and grid
	    	    			timeGridBytes = response.get(friendID);
	    	    			System.err.println("Friend that I'm about to send a location for: id=" + friendID);
	    	    		
	    	    			// Separate bytes of time and grid
	    	    			timeByte = timeGridBytes.get(0);
	    	    			gridsize = timeGridBytes.get(1);
	    	    			int grid_toOR;
	    	    			for (int j = 2; j < 5; j++) {
	    	    				gridsize = gridsize << 8;
	    	    				grid_toOR = timeGridBytes.get(j);
	    	    				grid_toOR = grid_toOR & 0x000000FF;
	    	    				gridsize = gridsize | grid_toOR;
	    	    			}
	    	    			System.err.println("Gridsize of this friend: " + gridsize + "\tTime byte: " + timeByte);
	    	    	  
	    	    			// Create the locations converted to the grid size and for every overlapping grid color/type
	    	    			ArrayList<BigInteger> myLocs = new ArrayList<BigInteger>();
	    	    			myLocs = gridHandler.getGridCoords(getApplicationContext(), gridsize);
	    		      
	    	    			// Create a place to store all three k_2's
	    	    			ArrayList<BigInteger> three_k_2 = new ArrayList<BigInteger>();
	    	    			
	    	    			// Get Diffie-Helman key corresponding to each friend id
	    	    			dhkey = (friendsList.getFriend(friendID)).getDHKey();
	    		      
	    	    			// Calculate what to encrypt (time that B-side also encrypted). So here, we match the times of A and B
	    	    			curDate = new Date();
	    	    			toEncrypt = ((curDate.getTime()) >> 12);
	    	    			toEncrypt = toEncrypt & 0xFFFFFFFFFFFFFF00L; // Get rid of the least significant byte
	    	    			long toOR = timeByte.longValue();
	    	    			toOR = toOR & 0x00000000000000FFL; 
	    	    			toEncrypt = toEncrypt | toOR; // Add on the time byte that B sent to completely synchronize the times
	    	    			Log.d("ExecuteProtocol.java","A-side, time to encrypt: " + toEncrypt);
	    	    			
	    	    			// k_1 is different for each friend and each overlapping grid
	    	    			for (int j = 0; j < 3; j++){
	    	    				k_1 = new BigInteger (keygen.generate_k(  dhkey, (Long.toString( (toEncrypt*10) + (2*j + 1)) )  )   ); // generate k_1 by encrypting time concatenated with 1
	    	    				BigInteger k_2_temp = new BigInteger (keygen.generate_k(dhkey, (Long.toString( (toEncrypt*10) + (2*j + 2) )) ) );
	    	    				k_2_temp = k_2_temp.mod(PRIME);
	    	    				three_k_2.add( k_2_temp ); // generate k_2 and store it in an array for later usage
	    	    				Log.d("ExecuteProtocol.java","k_1: " + k_1 + "\tk_2: " + k_2_temp);
	    	    				myLocs.set( j, (k_1.add(myLocs.get(j))).mod(PRIME) );  
	    	    			}
	    	    			
	    	    			k_2Array.put(friendID, three_k_2); // Store all three k_2s that correspond to the friend ID.
	    		      
	    	    			// Put the converted and encrypted overlapping grid in the map that will be sent to the server 
	    	    			myLocationsMap.put(friendID, myLocs);
	    	    		}
	    	    		else{
	    	    			// Update list of friends who aren't available since that particular ID could not be found
	    	    			updateNotAvailable(f.getName());
	    	    		}
	    	    	}
	    	    
	    	    	System.err.println ("About to send the server packet A3");
	    	    	// Send server my info corresponding to the grid size of each friend
	    	    	mClient.sendClosenessPacket(myLocationsMap, myID);
	    	    	
	    	    	// Receive response from server
	    	    	Map<Long, ArrayList<BigInteger>> finalResponse = mClient.receiveFinalResponse();
	    	    	boolean nearby = false;
	    	    	int keyIndex = 0;
	    	    	if (finalResponse != null)
	    	    	{
	    	    		for (Long key: myLocationsMap.keySet())
	    	    		{
	    	    			if (finalResponse.containsKey(key))
	    	    			{
	    	    				ArrayList<BigInteger> tempGrids = finalResponse.get(key);
	    	    				if (tempGrids != null)
	    	    				{
	    	    					keyIndex = 0;
	    	    					for (Iterator<BigInteger> g = tempGrids.iterator(); g.hasNext() && !nearby;)
	    	    					{
	    	    						BigInteger loc = g.next();
	    	    						ArrayList<BigInteger> keys = k_2Array.get(key);
	    	    						Log.d("ExecuteProtocol.java", "Final response to match up with k_2: loc: " + loc + "\t key: " + keys.get(keyIndex));
	    	    						
	    	    	    				if (  (loc).equals( keys.get(keyIndex) )  ) // If the final response is k_2, then the friend is nearby
	    	    	    				{
	    	    	    					nearby = true;
	    	    	    					Log.d("ExecuteProtocol.java", "Final response that matched up: " + loc);
	    	    	    				}
	    	    	    				else
	    	    	    					nearby = false;
	    	    	    			
	    	    	    				keyIndex++;
	    	    					}
	    	    				}
	    	    				else { nearby = false; }
	    	    			}
	    	    			else { nearby = false; }
	    	    			
	    	    			// Before moving on to the next friend, update the UI to display current friend's availability
	    	    			if (nearby)
	    	    				updateNearby( (friendsList.getFriend(key)).getName() );
	    	    			else
	    	    				updateNotNearby( (friendsList.getFriend(key)).getName() );
	    	    		}
	    	    	} // end of if statement with finalResponse
	    	    	else
	    	    	{
	    	    		for (Long key: myLocationsMap.keySet())
	    	    			updateNotNearby( friendsList.getFriend(key).getName() );
	    	    	}
	    	    } // end of if statement with initial response
	    	    
	    	    else // If initial response message is null, it means all friends are not available
	    	    { 
		    	    for (Friend friend:checkedFriends){
		    	    	System.err.println("Friend who isn't available: " + friend.getName());
		    	    	updateNotAvailable(friend.getName());
		    	    }
	    	    }
	    	    checkedFriends.clear(); // Clear out array list of checked friends so that resuming this activity won't create duplicate friends
	    	}
	    	
	    };
		
	    AThread.start();
	    
	}
	// --- HANDLER --- //
	final Handler messageHandler = new Handler() {
		public void handleMessage(Message msg)
		{
			Bundle b;
			switch(msg.what)
			{
				case(1):
					b = msg.getData();
					mNearby.append( b.getString("nearby") + "\n" );
					System.err.println("setting nearby");
				break;
				case(2):
					b = msg.getData();
					mNotNearby.append( b.getString("notNearby") + "\n" );
					System.err.println("setting not nearby");
				break;
				case (3):
					b = msg.getData();
					mNotAvail.append( b.getString("notAvail") + "\n" );
					System.err.println("setting not available");
				break;
				
			}
			
		}
	};
	
	private void updateNearby(String name)
	{
		Message msg = Message.obtain(messageHandler, 1, name);
		Bundle bundle = new Bundle(1);
		bundle.putString("nearby", name);
		msg.setData(bundle);
		messageHandler.sendMessage(msg);
	}
	
	private void updateNotNearby(String name)
	{
		Message msg = Message.obtain(messageHandler, 2, name);
		Bundle bundle = new Bundle(1);
		bundle.putString("notNearby", name);
		msg.setData(bundle);
		messageHandler.sendMessage(msg);
	}
	
	private void updateNotAvailable(String name)
	{
		Message msg = Message.obtain(messageHandler, 3, name);
		Bundle bundle = new Bundle(1);
		bundle.putString("notAvail", name);
		msg.setData(bundle);
		messageHandler.sendMessage(msg);
	}
	


	
	// --- MENU --- //
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.protocol_menu, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.back:
	    	 finish();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
}


