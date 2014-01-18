package com.protocol2.location;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.R;
import com.protocol2.location.SessionEvents.AuthListener;
import com.protocol2.location.SessionEvents.LogoutListener;

public class GettingStarted extends Activity{
    public static final String APP_ID = "106215252764932";
    
    private static final String[] PERMISSIONS =
        new String[] {"offline_access", "user_website", "friends_website", "user_photos", "friends_photos"};
    private LoginButton mLoginButton;
    private TextView mText;
    
    private SharedPreferences loggedIn;
 
    private Facebook mFacebook;
    private AsyncFacebookRunner mAsyncRunner;

	
	 /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.getting_started_one);
    	
	    // Set up app-wide global that will set whether or not user is logged into facebook (basically whether or not to display getting started screen)
	    loggedIn = getSharedPreferences("loggedIn", MODE_PRIVATE);
	    if ( loggedIn.getBoolean("loggedIn", false) ) {
	    	start_mainLocation_Activity();
	    }
    	        
    	mFacebook = new Facebook(null);
       	mAsyncRunner = new AsyncFacebookRunner(mFacebook);
       
        SessionStore.restore(mFacebook, getApplicationContext());
        SessionEvents.addAuthListener(new SampleAuthListener());
        SessionEvents.addLogoutListener(new SampleLogoutListener());

        // Set up UI components
    	mLoginButton = (LoginButton) findViewById(R.id.startFB);
        mText = (TextView) GettingStarted.this.findViewById(R.id.txt_error);

        mLoginButton.init(mFacebook, PERMISSIONS);         
    }
    
    
    @Override
    public void onBackPressed() {
    	Intent finishPartOne = new Intent();
    	GettingStarted.this.setResult(2, finishPartOne); 
		GettingStarted.this.finish();
    return;
    }
    
  public class SampleAuthListener implements AuthListener {
        
        public void onAuthSucceed() {
            mText.setText("You have logged in! ");
            boolean createdKey = false;
            boolean privateKeyFound = false;
            try{
            	JSONObject me = new JSONObject(mFacebook.request("me"));  // request my info
            	String website;
    			try { website = me.getString("website"); } 
    			catch (Exception e) { System.err.println("Error getting my wesbite"); website = "www.fail.com"; }
    			String websites[] = website.split("\n");
    			for(String w:websites){
    				if(w.contains("key=")){
    					createdKey = true;
    				}
    			}
            }
            catch (Exception e)
            {
            	System.err.println ("Error retreiving my information");
            }
            if (createdKey) {
            	  try {
      			    ObjectInputStream objInput = null;
      			    InputStream ip = null;
      		    	ip = openFileInput("user_priv.dat");
      		    	privateKeyFound = true;
                  }
                  catch (FileNotFoundException ef)
                  {
                  	Log.d("GettingStarted.java", "Private key not found so the user must create a new public key.");
                  	privateKeyFound = false;
                  }
            }
          
            if (!createdKey || !privateKeyFound)
            {
            	Intent partTwoIntent = new Intent(GettingStarted.this, GettingStartedPartTwo.class);
            	GettingStarted.this.startActivityForResult(partTwoIntent, 102);
            }
            else
            {
            	start_mainLocation_Activity();
            }
      }

        public void onAuthFail(String error) {
            mText.setText("Login Failed: " + error);
        }
    }
    
    public class SampleLogoutListener implements LogoutListener {
        public void onLogoutBegin() {
        	// Clear stored friends
        	ExtendedCheckBoxListAdapter e = ExtendedCheckBoxListAdapter.getInstance(GettingStarted.this.getApplicationContext());
        	e.clearFriends();
        	
        	// Clear whether or not we have logged in
        	SharedPreferences.Editor loggedInEditor = loggedIn.edit();
	    	loggedInEditor.putBoolean("loggedIn", false);
	    	loggedInEditor.commit();
        	
	    	// Delete private key file
/*	    	try {
	    		File f = new File("user_priv.dat");
	    		if (f.exists())
	    		{
	    			f.delete();
	    			Log.d("GettingStarted.java: logout", "Private key file deleted.");
	    		}
	    	}
	    	catch (Exception except)
	    	{
	    		except.printStackTrace();
	    	}
*/	    	
		    // Stop B-side service
	    	Intent bSide = new Intent(getApplicationContext(), BServiceSide.class);
	    	stopService(bSide);
	    	
            mText.setText("Logging out...");
        }
        
        public void onLogoutFinish() {
            mText.setText("You have logged out! ");
        }
    }
    
    private void start_mainLocation_Activity()
    {
    	SharedPreferences.Editor loggedInEditor = loggedIn.edit();
    	loggedInEditor.putBoolean("loggedIn", true);
    	loggedInEditor.commit();
    	Intent finishPartOne = new Intent();
    	GettingStarted.this.setResult(1, finishPartOne); 
		GettingStarted.this.finish();
    }
    

    
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if (requestCode == 102)
    	{
    		if (resultCode == RESULT_OK)
    		{
    	    	start_mainLocation_Activity();
    		}
    	}
    }
}

