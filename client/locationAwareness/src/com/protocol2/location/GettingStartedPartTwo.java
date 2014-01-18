package com.protocol2.location;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.android.Facebook;
import com.facebook.android.R;

import de.rtner.security.auth.spi.Base64Coder;
import de.rtner.security.auth.spi.PBKDF2Engine;


public class GettingStartedPartTwo extends Activity {
	private EditText mPassword;
	private Button mNewKeyPair;
	private Button mProfile;
	private Button mFinished;
	private TextView mMessage;
	private ProgressDialog diag;
	private Facebook mFacebook;
	
	private String id ="0001"; // initial value of the facebook user id
	
	// key generation variables
	String publicKeyUrl;
	
	 /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.getting_started_two);
    	
    	  //UI Components
	       mPassword = (EditText)findViewById(R.id.password);
	       mNewKeyPair = (Button)findViewById(R.id.newKeyPair);
	       mMessage = (TextView)findViewById(R.id.messageDetails);
	       mProfile = (Button)findViewById(R.id.profile);
	       mFinished = (Button)findViewById(R.id.doneKey);
	       diag = new ProgressDialog(this);
	       
	       mFacebook = new Facebook();
	       SessionStore.restore(mFacebook, getApplicationContext());
	       
	       // get current user id:
	       try { 
	    	
	    	String json_string = mFacebook.request("me");
			
			JSONObject myJSON = new JSONObject(json_string);
			
			id = myJSON.get("id").toString();
			
			}catch (Exception e)
			{
				Log.d("GettingStartedPartTwo.java","There was an error in requesting the user profile.");
			}
			System.err.println("Facebook-my id: "+ id);
	       
	     //To create key pair
	       mNewKeyPair.setOnClickListener(new OnClickListener() {
	    	   @Override
	    	   public void onClick(View v) {
	    		    if (mPassword.getText().toString().length() != 0) {
	    		    	diag = ProgressDialog.show(GettingStartedPartTwo.this, "FB Pki", "Generating Key Pair");
	    		    	new DownloadTask().execute("test");
	    		    }
	    		    else
    		 	    {
    		 	    	System.err.println ("No password inputed.");
    		 	    	new AlertDialog.Builder(GettingStartedPartTwo.this)
    		     			.setTitle("Error")
    		     			.setMessage("Please provide a password for your public key.")
    		     			.setPositiveButton("OK", null)
    		     			.show();
    		 	    }
	    	   }
	       });
	       
	     // To update profile (opens a new browser window)
	       //To open the browser and take the user to the FB profile page
	       mProfile = (Button)findViewById(R.id.profile);
	       mProfile.setVisibility(View.GONE);
	       mProfile.setOnClickListener(new OnClickListener() {
	    	   @Override
	    	   public void onClick(View v) {
	    		   mFinished.setVisibility(View.VISIBLE);
	    		   String updatePageUrl = "http://www.facebook.com/#!/editprofile.php?sk=contact";
	    		   //"http://www.facebook.com/home.php?#/profile.php?v=info&ref=profile&id=" + id;
	    		   Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(updatePageUrl)); 
	    		   startActivity(myIntent);
	    	   }
	       });
	       
	       mFinished.setOnClickListener(new OnClickListener() {
	    	   @Override
	    	   public void onClick(View v) {
	    		   Intent finishPartTwo = new Intent();
	    		   GettingStartedPartTwo.this.setResult(RESULT_OK, finishPartTwo); 
	    		   GettingStartedPartTwo.this.finish();
	    	   }
	       });
    	
    }
	//The key generation task
	private class DownloadTask extends AsyncTask<String, Void, Object> {
		private String[] resultArr;
		@Override
		protected Object doInBackground(String... params) {
			// TODO Auto-generated method stub
			System.err.println("Starting thread");
			resultArr = generateKeys();
			System.err.println(resultArr);
			System.err.println("Finished thread");
			return null;
		}
		
		@Override
		protected void onPostExecute(Object result){
			//_verify.setVisibility(View.VISIBLE);
 		   	//_details.setVisibility(View.VISIBLE);
 		   	mProfile.setVisibility(View.VISIBLE);
 		   
 		   	mMessage.setText("Generated Public Key is " + resultArr[3].substring(0, 38) + "..." +
	    					"\n\nThis key is copied to clipboard. Click the profile button and add the string to your website info." +
	    					"\nIf you previously created a public key for this app, please replace it with this new one.");
			GettingStartedPartTwo.this.diag.dismiss();
		}
	}
	
	/** Note: took out dictionary passphrase portion of the generation of keys due to user friendliness.
	 * 
	 * @returns an array of keys corresponding to each friend (i think -- Naren wrote this code)
	 */
	protected String[] generateKeys() {
		
    	//Generate key pairs
    	PrivateKey priv = null;
		PublicKey pub = null;
		
		String resultArr[] = new String[4];
		
		BigInteger p512 = new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1" +
				"CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6" +
				"DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE" +
				"386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF", 16);
		BigInteger g512 = new BigInteger("2", 10);
    	try{   		

    	    DHParameterSpec dhParams = new DHParameterSpec(p512, g512);
    	    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH", "BC");
    	    
    	    SecureRandom random = new SecureRandom();
    	   
    	    String seedValue = PBKDF2Engine.execute(new String[]{mPassword.getText().toString()});
    	    
    	    seedValue = seedValue.substring(seedValue.lastIndexOf(':')+1);
    	    System.err.println("final seed : " + seedValue);
    	    random.setSeed(seedValue.getBytes());
    	    keyGen.initialize(dhParams, random);
    	    
    	    //keyGen.initialize(espec, random);

    	    KeyPair aPair = keyGen.generateKeyPair();
    	    
    		priv = aPair.getPrivate();
    		System.err.println("Private key encoded LENGTH : " + priv.getEncoded().length);
    		pub = aPair.getPublic();
    		
    	}catch(Exception e){
    		System.err.println(e.toString());
    	}
    	
    	//Write private key to the user file
    	FileOutputStream outputStream = null;
    	ObjectOutputStream objOutput = null;
    	OutputStream os = null;
		
		try {
			System.err.println("ID IS ##" + id + "##");
			os = openFileOutput("user_priv.dat", Context.MODE_PRIVATE);
			objOutput = new ObjectOutputStream(os);
			
			if(objOutput == null || priv == null){
				System.err.println("oops - null vals");
			}else{
				System.err.println("all rite - no null vals");
			}
			
			objOutput.writeObject(priv);
			System.err.println("write LENGTH : " + priv.getEncoded().length);
		} catch(IOException e) {
			System.err.println(e.getStackTrace());
		} finally {
			if (outputStream != null) {
				try {
					objOutput.close();
					 outputStream.close();
					os.close();
				} catch (IOException e) {
					System.err.println(e.getStackTrace());
				}
			}
		}
    	
		//Now write the public key in the text box
		System.err.println("Pub length : " + pub.getEncoded().length);
		
		// converted to base64 
		String base64 = null;
		DHPublicKeySpec keySpec = null;
		try{
			KeyFactory keyFactory = KeyFactory.getInstance("DH", "BC");
			keySpec = keyFactory.getKeySpec(pub, DHPublicKeySpec.class);
			System.err.println("keyspec G  : " + keySpec.getG().toString(16));
			System.err.println("keyspec P  : " + keySpec.getP().toString(16));
			System.err.println("keyspec Y  : " + keySpec.getY().toString(16));
			System.err.println("Y Length : " + keySpec.getY().toByteArray().length);
			base64 = new String (Base64Coder.encode(keySpec.getY().toByteArray()));
			System.err.println("Y Base64 Length : " + base64.length());
		}catch(Exception e){
			e.printStackTrace(System.err);
		}
		
		System.err.println("Pub base64: " + base64);
		
		//Construct the url that needs to be placed in the profile
		String urlText = "https://socialkeys.org/pubkey?alg=DH&keylen=1024&p=oakley&g=2&key="+base64;
		
		resultArr[3] = urlText;
		
		//Copy the url to clipboard
		ClipboardManager clipboard = 
		      (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
		clipboard.setText(urlText);
		publicKeyUrl = base64;
		return resultArr;
	   
	}
}
