package com.protocol2.location;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.StreamCorruptedException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHPublicKeySpec;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.facebook.android.Facebook;


public class mainLocation extends ListActivity {
    // Called when the activity is first created. */
	
	// --- For facebook logging in --- //
    public static final String APP_ID = "106215252764932";
    private static final String[] PERMISSIONS =
        new String[] {"offline_access", "user_website", "friends_website", "user_photos", "friends_photos"};    
	// --- End for facebook loogin in --- //
    
    private static final int DEFAULT_GRIDSIZE_FEET = 1500; // in feet
	private ExtendedCheckBoxListAdapter friendsList;
	private Facebook mFacebook = new Facebook();
	private boolean displayedCorrectly = false; // to keep track if friends was properly displayed or not
	private SharedPreferences loggedIn;
	
	private Intent bSide;
	
	private SharedPreferences.Editor meEditor;
	
	final Handler messageHandler = new Handler() {
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{
				case(1):
					// Generate main key (K) between client and client
			    	fillValues();

			    	// Start the B service side -- sending my encrypted location
			    	startService(bSide);
				
			    	final Button button = (Button) findViewById(R.id.donebutton);
			    	button.setOnClickListener(new View.OnClickListener() {
			    		public void onClick(View v) {
			    			// Start intent to execute Protocol 2
			    			Intent protocolIntent = new Intent(mainLocation.this, ExecuteProtocol.class);
			    			mainLocation.this.startActivity(protocolIntent);
			    		}
			    	});
				break;
				
			}
			
		}
	};
	
	/** Called when the activity is first created. */

	@Override

	public void onCreate(Bundle savedInstanceState) {

	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.friendscreen);
	    bSide = new Intent(getApplicationContext(), BServiceSide.class);
	    
	    
	    // --- STORAGE: Saved Preferences method of storing variables ----
	   
	    
	    // place in random locations for gps locations
	    // set up a "preference" that will store the latitude and longitude and grid size
	    SharedPreferences latitude = getSharedPreferences("savedLatitude", MODE_PRIVATE); 
		String tempL = latitude.getString("savedLatitude", "FAIL_LAT");
		if (tempL == "FAIL_LAT")
		{
			 SharedPreferences.Editor latitudeEditor = latitude.edit();
			 latitudeEditor.putString("savedLatitude", new String("5555555"));
			 latitudeEditor.commit();
		}
	   
	    SharedPreferences longitude = getSharedPreferences("savedLongitude", MODE_PRIVATE);
	    tempL = longitude.getString("savedLatitude", "FAIL_LONG");
	    if (tempL == "FAIL_LONG")
		{
	    	SharedPreferences.Editor longitudeEditor = longitude.edit();
		    longitudeEditor.putString("savedLongitude", new String("55555555"));
		    longitudeEditor.commit();
		}
	    
	    SharedPreferences gridProximity = getSharedPreferences("gridProximity", MODE_PRIVATE); 
	    SharedPreferences.Editor gridEditor = gridProximity.edit();
	    gridEditor.putInt("gridProximity", DEFAULT_GRIDSIZE_FEET); // save the default grid size as 1500
	    gridEditor.commit();
	    
	    SharedPreferences myID = getSharedPreferences("myID", MODE_PRIVATE);
	    meEditor = myID.edit();
	    long tempID = myID.getLong("myID", -1);
	    if (tempID == -1)
	    {
	    	meEditor.putLong("myID", 0); // save the default user id as 0
	    	meEditor.commit();
	    }
	   

	    
	}
	
	public void onRestart()
	{
		super.onRestart();
    	// Clear stored friends
    	if (friendsList != null)
    		friendsList.clearFriends();
	}
	
	public void onStart()
	{
		super.onStart();

	    loggedIn = getSharedPreferences("loggedIn", MODE_PRIVATE);
	    if ( !(loggedIn.getBoolean("loggedIn", false)) )
	    {
	    	//System.err.println("Did not log in to facebook.");
	    	loginFacebook();
	    }
	    else 
	    { 
	    	displayAll();
	    } // END of IF statement that checks if you're logged in or not
	    
	}
	
	public void onResume()
	{
		super.onResume();
		// So nothing will be executed
	}

	public void onStop()
	{
		super.onStop();
    	// Clear stored friends
    	//if (friendsList != null)
    		//friendsList.clearFriends();
	}
	
	public void onDestroy()
	{
		super.onDestroy();
    	// Clear stored friends
    	if (friendsList != null)
    		friendsList.clearFriends();
	}
	
    @Override
    public void onBackPressed() {
    	exitApp();
    return;
    }

	/**
	 * This method displays a dialog (pop up window) that tells the user to log in to Facebook. 
	 * Then once the user clicks login, a call to Facebook.java and all associated files is made
	 * and a window is displayed where the user can log into Facebook.
	 */
	private void loginFacebook()
	{
    	Intent partOneIntent = new Intent(mainLocation.this, GettingStarted.class);
    	mainLocation.this.startActivityForResult(partOneIntent, 101);
	}
	
	/**
	 * This is the BIG function -- it displays your list of friends and from there, you can execute the
	 * protocol on any or all of them. This is the main chunk of this activity.    
	 */
	private void displayAll()
	{
		// Build the list adapter
	    friendsList = ExtendedCheckBoxListAdapter.getInstance(getApplicationContext());
	   
	    // Add friends from facebook:
	    SessionStore.restore(mFacebook, getApplicationContext());
	    
	    
		runOnUiThread(new Runnable() {
		@Override  
		public void run() {
		    
			//Public keys of friends are placed in a file that is accessed after friend list is displayed
			// THIS FILE IS REALLY NEEDED ANYMORE SINCE INFORMATION IS STORED IN GLOBAL ARRAY OF FRIEND OBJECTS, BUT THIS FILE IS ACCESS LATER
			// THUS, GOAL WOULD BE TO MOVE IT TO GLOBAL ARRAY OF FRIEND OBJECTS 
			FileOutputStream fOut = null;
		    OutputStreamWriter osw = null;
		    try
		   	{
		   		Log.d("MAIN", "Building friend array and doing key exchanges.");
		   		// set up file that will hold the websites of all the friends
		   		fOut = openFileOutput("public.dat", Context.MODE_PRIVATE);
				osw = new OutputStreamWriter(fOut);
				
		   		// get the friends who have registered with this application
		   		Bundle friends_params = new Bundle();
		        friends_params.putString("method", "friends.getAppUsers");
		        String response = mFacebook.request(friends_params);
		        Log.d("mainLocation.java", response);
	  			
		        // get the name and website of the friends who we just retrieved
		        Bundle urls_params = new Bundle();
		        urls_params.putString("method", "users.getInfo");
		        urls_params.putString("uids", response);
		        urls_params.putString("fields", "name,website");
		        String resp = mFacebook.request(urls_params);
		        Log.d("mainLocation.java", "Facebook friends information: " + resp);

		        // convert response into an array so we can extract the fields
		        JSONArray appFriendArray = new JSONArray(resp);

		        // store the number of friends we have
		        int entries = 1 + appFriendArray.length();
				osw.write(Integer.toString(entries));
				
				
				//Extracting the public key from the website info of the friends.
				//Currently the app looks for the first url that has a 'key' field and extracts its value
				// this is extracting my info/key:
				JSONObject me = new JSONObject(mFacebook.request("me")); // get the user's (me) info
				Log.d("mainLocation.java", me.toString());
				// Save my id in a shared preference:
				long idNum = ( Long.valueOf(me.getString("id")) ).longValue();
				meEditor.putLong("myID", idNum);
				meEditor.commit();
				
				String website;
				try { website = me.getString("website"); } 
				catch (Exception e) { System.err.println("Error getting my wesbite"); website = "www.fail.com"; }
				String websites[] = website.split("\n");
				String key = null;
				for(String w:websites){
					if(w.contains("key=")){
						key = (String) w.subSequence(w.indexOf("key=")+4, w.length());
					}
				}
				
				osw.write("\n" + me.getString("id") + "," + me.getString("name") + "," + key); // store my own public key
				
				Log.d("mainLocation.java", "Number of friends retrieved: " + appFriendArray.length());
		        // add each friend to the file that will hold each friend's id, name, and public key so that later
				// on keys can be exchanged
	  			for( int i = 0; i < appFriendArray.length(); i++ )
	  			{
		   	    	String newFriendName = (appFriendArray.getJSONObject(i)).getString("name");
		   	    	long newFriendID =  Long.valueOf( appFriendArray.getJSONObject(i).getString("uid") , 10 ) ;
		   	    	Friend friend = friendsList.getFriend(newFriendID);
		   	    	if ( friend == null)
		   	    	{
		   	    		friend = new Friend(newFriendName, newFriendID); // create a new Friend.class
		   	    		friendsList.addFriend(friend);
		   	    	}
		   	    	
		   	    	
		   	    	try { website = (appFriendArray.getJSONObject(i)).getString("website"); } 
					catch (Exception e) { System.err.println("Error getting a wesbite"); website = "www."+i+".com"; }
					websites = website.split("\n");
					
					key = null;
					for(String w:websites){
						if(w.contains("key=")){
							key = (String) w.subSequence(w.indexOf("key=")+4, w.length());
							osw.write("\n" + (appFriendArray.getJSONObject(i)).getString("uid") + "," + newFriendName + "," + key);
							friend.setPublicKey(key); // set the new friend's website (save his/her public key)
						}
					}
					
		   	    	
		   	    }
	  			
	  		
	  			// Bind it to the activity! (to display the list)
	   		    setListAdapter(friendsList);
	   		 
	   		    osw.write("\n");
				osw.close();
				fOut.close();
		    		  
				doRest();
		 	}
		    catch (Exception e)
		    {
		    	Log.d("Facebook: get friends", "error getting friends");
		    	new AlertDialog.Builder(mainLocation.this)
     			.setTitle("Error")
     			.setMessage("We could not retreive your friends. Please log in again.")
     			.setPositiveButton("OK", null)
     			.show();
		    	e.printStackTrace();
		    	// Restart the logged in process and everything.
	        	if (friendsList != null) { friendsList.clearFriends(); }
	        	try { 
	        		mFacebook.logout(getApplicationContext()); // Logout of facebook 
	        	}
	        	catch ( Exception logout_e )
	        	{
	        		System.err.println("Could not log out.");
	        		logout_e.printStackTrace();
	        	}
		    	SharedPreferences loggedIn = getSharedPreferences("loggedIn", MODE_PRIVATE);
		    	SharedPreferences.Editor loggedInEditor = loggedIn.edit();
		    	loggedInEditor.putBoolean("loggedIn", false);
		    	loggedInEditor.commit();
		    	Intent restart = new Intent(mainLocation.this, GettingStarted.class);
		    	startActivity(restart);
		    }
			}
		      
			}); // end of runOnUI thread
	}
	
	/**
	 * This method reads the user and friends public key from the file and
	 * computes the shared secret keys and stores them for other apps to access.
	 */
	private void fillValues() {

		FileInputStream fIn = null;
	    InputStreamReader isr = null;
		try{
		    char[] inputBuffer = new char[10000];
		    String data = null;
		    
		    //Access the public key of each user in the file
		    fIn = openFileInput("public.dat");
		    isr = new InputStreamReader(fIn);
		    isr.read(inputBuffer);
	        data = new String(inputBuffer);
	        Log.d("mainLocation.java", data);
	        String users[] = data.split("\n");
	        String appResultString = "";
	        String publicKeyString = "";
	        if(users.length > 0){
	        	if(users[0].length()<4){
	        		int count = Integer.parseInt(users[0]);
	        		count = users.length-2; 	        		//The first line is the current user's public key and the last line is a carriage return
	        		if(count>0){
	        			//Access the first line and set the user info
	        			String vals[] = users[1].split(",");
	        			StringBuffer userInfo = new StringBuffer();
	        			userInfo.append(userInfo + "Facebook User Name: " + vals[1]+ "\nKey: ");
	        			if(vals[2].length()>0){
	        				userInfo.append("Available"); 
	        			}else{
	        				userInfo.append("Not Available");
	        			}
	        			Log.d("mainLocation.java", "Your info: " + userInfo);

	        			// Line from old version: FBContent.getInstance().setCurrentUserId(vals[0]);
	   
	        			//Shared key generation with each friend
	        			//Access the stored user private key
        			    ObjectInputStream objInput = null;
        			    InputStream ip = null;
        			    
        			    PrivateKey user_priv = null;
        			    try {
        			    	ip = openFileInput("user_priv.dat");
        					objInput = new ObjectInputStream(ip);
        					user_priv = (PrivateKey)objInput.readObject();
        					ip.close();
        					objInput.close();
        				} catch (StreamCorruptedException e1) {
        					// TODO Auto-generated catch block
        					e1.printStackTrace();
        				} catch (IOException e1) {
        					// TODO Auto-generated catch block
        					e1.printStackTrace();
        				} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

        				//Now for each of the friends' public key, generate a shared key
	        			// Line from old version: _friends.setText("Friends\n");
        				Log.d("mainLocation.java","====================== Freind-key-generation starts here. ======================");
        				Log.d("mainLocation.java", "The count of friends is: " + count);
	        			for(int i=2; i<(count+1); i++){
	        				vals = users[i].split(",");
	        				// Line from old verision: _friends.setText(_friends.getText()+"\n\nName : " + vals[1] + "\nURL:" + vals[2]);
	        				appResultString += vals[1]+",";
	        				publicKeyString += vals[1]+",";
	        				publicKeyString += vals[2]+"#";
	        				
	        				try {
	        					BigInteger p1024 = new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1" +
		        						"CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6" +
		        						"DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE" +
		        						"386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF", 16);
		        				BigInteger g = new BigInteger("2", 10);
		        				BigInteger y = new BigInteger(Base64Coder.decode(vals[2]));
		        				
		        				DHPublicKeySpec pubspec = new DHPublicKeySpec(y, p1024, g);
		        				PublicKey friend_pub = null;
		        				
	        					KeyFactory keyF = KeyFactory.getInstance("DH","BC");
	        					friend_pub = keyF.generatePublic(pubspec);
	        					
		        				if(friend_pub != null)
		        					Log.d("mainLocation.java", "Derived Friend Pub : " + new String(friend_pub.getEncoded()));
		        				else
		        					Log.d("mainLocation.java", "Derived Pub : NULL");
		        				
		        				KeyAgreement keyAgreement = KeyAgreement.getInstance("DH", "BC");
		        				keyAgreement.init(user_priv);
		        				keyAgreement.doPhase(friend_pub, true);
		        				String sharedsecretBase64 = new String(Base64Coder.encode(keyAgreement.generateSecret()));
		        				Log.d("mainLocation.java", "vals[1]: " + vals[1]);
		        				Log.d("mainLocation.java", "Friend: " + friendsList.getFriend(vals[1]).getName() + "\tSharedSecret : " + sharedsecretBase64);
		        				appResultString += sharedsecretBase64+"#";
		        				Friend temp = friendsList.getFriend(vals[1]); 
		        				if ( temp  != null )
		        				{
		        					temp.setDHKey(sharedsecretBase64);
		        				}
		        				else
		        					Log.d("mainLocation.java", "Could not find " + vals[1] + ". So could not store DH key in mainLocation.java.");
	        				} catch (NoSuchAlgorithmException e) {
	        					// TODO Auto-generated catch block
	        					e.printStackTrace();
	        				} catch (NoSuchProviderException e) {
	        					// TODO Auto-generated catch block
	        					e.printStackTrace();
	        				} catch (InvalidKeySpecException e) {
	        					// TODO Auto-generated catch block
	        					e.printStackTrace();
	        				} catch (InvalidKeyException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
	        				
	        			}
	        		}
	        	}
	        }
	        /* OLD VERSION LINES:
	        //The public keys and shared keys are set in FBContent singleton object 
	        //that is delivered to consuming apps 
	        FBContent.getInstance().setAppInterfaceResult(appResultString);
	        FBContent.getInstance().setPublicKeyResult(publicKeyString);
	        */
			isr.close();
			fIn.close();
		}catch(IOException e){
			e.printStackTrace(System.err);
		}catch(Exception e){
			e.printStackTrace(System.err);
		}
		Log.d("mainLocation.java", "====================== Freind-key-generation ends here. ======================");
	}
	/**
	 * If a list item is clicked
	 * we need to toggle the checkbox too
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// Toggle the checkbox state!
		if ( v != null ){
			ExtendedCheckBoxListView CurrentView = (ExtendedCheckBoxListView)v;
			if ( CurrentView != null ){
				CurrentView.toggleCheckBoxState();
			}
	    }
	   	super.onListItemClick(l, v, position, id);
	}
	
	
	private void doRest()
	{
		Message msg = Message.obtain(messageHandler, 1);
		
		messageHandler.sendMessage(msg);
	}
	
	
	private void exitApp()
	{
    	friendsList.clearFriends();
    	stopService(bSide);
    	finish();
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if (requestCode == 101)
    	{
    		if (resultCode == 1)
    		{
    	    	displayAll();
    		}
    		else if (resultCode == 2)
    		{
    			finish();
    		}
    		
    	}
    }
	
	// --- MENU --- //
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.main_menu, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.exit:
	    	exitApp();
	        return true;
	    case R.id.settings:
	    	Intent settingsIntent = new Intent(mainLocation.this, Settings.class);
	    	mainLocation.this.startActivity(settingsIntent);
	    	return true;
	    case R.id.logout:
        	// Clear stored friends
        	friendsList.clearFriends();
        	
        	// Clear whether or not we have logged in
        	SharedPreferences.Editor loggedInEditor = loggedIn.edit();
	    	loggedInEditor.putBoolean("loggedIn", false);
	    	loggedInEditor.commit();

		    // Stop B-side service
	    	bSide = new Intent(getApplicationContext(), BServiceSide.class);
	    	stopService(bSide);
	    	
	    	loginFacebook();
        	try { 
        		mFacebook.logout(getApplicationContext()); // Logout of facebook 
        	}
        	catch ( Exception logout_e )
        	{
        		System.err.println("Could not log out.");
        		logout_e.printStackTrace();
        	}
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
}
