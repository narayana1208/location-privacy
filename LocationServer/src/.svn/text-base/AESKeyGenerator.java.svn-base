/**
 * Generates different keys (k_1, k_2, etc.) from a shared Key (K) between two clients.
 * Also can be used to generate the random value, r, between the server and the client.
 * 
 * Uses the shared key K that is associated with each friend and the time that is passed in as one of the parameters.
 */

import java.math.BigInteger;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESKeyGenerator {
	
	private Cipher mCipher;

	public AESKeyGenerator()
	{
		// default constructor
	}
	
	/**The only public function and is *the* function of this class. It generates the k_1, k_2, etc. from 
	 * parameters Friend, f, and time or whatever it is you want to "encrypt", toEncrypt.
	 * 
	 */
	public byte[] generate_k(String dhkey, String toEncrypt)
	{
		byte[] retVal;
		
		try { // Set up the Cipher class of Android to use AES to generate keys
			byte[] iv = new byte[16];
			for (int i = 0; i < iv.length; i++)
				iv[i] = new Byte("0").byteValue();
			IvParameterSpec ivspec = new IvParameterSpec(iv);
			mCipher = Cipher.getInstance("AES");
			// Set up key to use in algorithm
			MessageDigest hasher = MessageDigest.getInstance("SHA-256"); // Initialize object that will hash my key.
			byte[] key256 = hasher.digest(dhkey.getBytes()); // Hash the key to 256 bits using SHA
			SecretKeySpec K = new SecretKeySpec(key256, "AES");
			mCipher.init(Cipher.ENCRYPT_MODE, K, ivspec);
			// Encrypt the parameter toEncrypt
			retVal = mCipher.doFinal(toEncrypt.getBytes());
			return retVal;
		}
		catch (Exception e) {
			System.err.println("Could not create and initialize object Cipher.");
		}
		
		return null;
		
	}
	/**The only public function and is *the* function of this class. It generates the k_1, k_2, etc. from 
	 * parameters Friend, f, and time or whatever it is you want to "encrypt", toEncrypt.
	 * 
	 */
	public byte[] generate_r(byte[] sharedKey, String toEncrypt)
	{
		byte[] retVal;
		try {
			/*byte[] iv = new byte[16];
			for (int i = 0; i < iv.length; i++)
				iv[i] = new Byte("0").byteValue();
			IvParameterSpec ivspec = new IvParameterSpec(iv);*/
			
			// Set up the Cipher class of Android to use AES to generate keys
			mCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			// Set up key to use in algorithm
			MessageDigest hasher = MessageDigest.getInstance("SHA-256"); // Initialize object that will hash my key.
			byte[] key256 = hasher.digest(sharedKey); // Hash the key to 256 bits using SHA 256
			SecretKeySpec K = new SecretKeySpec(key256, "AES");
			mCipher.init(Cipher.ENCRYPT_MODE, K);
			// Encrypt the parameter toEncrypt
			System.out.println("toEncrypt AES: "+ toEncrypt);
			retVal = mCipher.doFinal(toEncrypt.getBytes());
			return retVal;
		}
		catch (Exception e) {
			System.err.println("exception: "+ e.toString());
			System.err.println("Could not create and initialize object Cipher.");
		}
		
		return null;
		
	}
}
