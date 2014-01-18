import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class DetectionServer
 */
public class DetectionServer extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//store arraylist, etc. in servletcontext
	
	private static final int TRANSMIT_SIZE = 6;
	private static final BigInteger secretKey = new BigInteger("21391726361020645630756384383360465269220145" +
			"2840477570062524601013601847417378730632625059484363481499620714956751744627527893861" +
			"1049415014554369159620864169199031696067210293953048032860" +
			"8726746175591273963745185527976542245047422955014704090972" +
			"189901636690585182016393063085275305215928250809320504424323073");
	private static final BigInteger RSAmod = new BigInteger("12694219218781329705748652330064241" +
			"32876567060377252671965562728778379597445920850187463" +
			"64753954450262954027491132190217804197573118748199754481792479239949" +
			"265338550227737039760729449292181953594614276814295738904266460590930337991640368023335931" +
			"284127132839615665571953261183977860758358803259102062236595411");
	private static final BigInteger prime = BigInteger.valueOf(28147497699961L);
	
    public void init(ServletConfig config) throws ServletException{
    	super.init(config);

    }
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Security.addProvider(new BouncyCastleProvider());
		//use while loop because input stream might be able to fill byte array. 
		int numBytesRead = -1;
		int bufferLen = request.getContentLength();
		InputStream input = request.getInputStream();
		byte[] buffer = new byte[bufferLen];
		ByteArrayOutputStream output = new ByteArrayOutputStream(bufferLen);
		while((numBytesRead = input.read(buffer, 0, bufferLen)) != -1){
			output.write(buffer, 0, numBytesRead);
		}
		byte[] result = output.toByteArray();
		/*BufferedReader in = request.getReader();
		byte[] result = IOUtils.toByteArray(in);*/
		
		
		ServletOutputStream out = response.getOutputStream();
		//start session for specific request
		HttpSession session = request.getSession(true);
		ServletContext context = getServletContext();

		//index to keep track of place in request packet
		
		int firstByte = new Byte(result[0]).intValue();
		int startIndex = 1;
		//next 8 bytes is id of user because all packets have id 
		byte[] convert = new byte[8];
		for (int j = 0; j < convert.length; j++)
			convert[j] = result[startIndex+j];
		long id = new BigInteger(convert).longValue();
		System.out.println("Packet: " + firstByte);
		System.out.println("userid: " + id);
		//first 9 bytes are packet type and id in all packets
		startIndex += 8;
		//check first byte of packet to see what type, send back nothing if it is not an acceptable (1,3,5) packet number  
		//first packet says it is ready to request location of list of friends
		if (firstByte == 1){
			out.write(handleFirstPacketType(id, result, startIndex, session, context));
		}
		//handles second response from A to ask for masked locations of friends in map because that's packet type 3
		else if (firstByte == 3){
			out.write(handleSecondPacketType(id, result, context,  session, startIndex));
			// since accessed, invalidate the session so the information in attributes cannot be used again (flushing buffer)
			session.invalidate();
		}

		//service packet from B because packet type is 5
		else if (firstByte == 5){
			storeGridInformation(id, result, startIndex, context);
		}
		//exchange of keys packet is type 7
		else if (firstByte == 7){
			shareKey(id,startIndex, result, context);
		}
		else{
			out.write(null);
		}
		out.close();
	}
	private byte[] handleFirstPacketType(long id, byte[] result, int startIndex, HttpSession session, ServletContext context)
	{
		//numberfriends is all that is left in the byte array after the first 9 bytes. they are longs, so in intervals of 8 bytes.
		int numberFriends = (result.length - startIndex)/8;
		ArrayList<Byte> tempInformation = new ArrayList<Byte>();
		for (int i = 0; i < numberFriends; i++){
			byte[] convertLong = new byte[8];
			for (int j = 0; j < convertLong.length; j++){
				convertLong[j] = result[startIndex+j];
			}
			long friend = new BigInteger(convertLong).longValue();
			String key = Long.toString(friend) + "tg";
			ArrayList<Byte> timegridInfo;
			synchronized(context){
				timegridInfo = (ArrayList<Byte>)context.getAttribute(key);
			}
			System.out.println("friend:" + friend);
			if (timegridInfo != null){
				//add friend id (long) to byte array
				for (int j = 0; j < 8; j++)
					tempInformation.add(result[startIndex+j]);
				
				for (int j = 0; j < timegridInfo.size(); j++){
					tempInformation.add(timegridInfo.get(j));
					
					String keyForRVal = Long.toString(friend) + "rvalues";
					HashMap<Long,ArrayList<BigInteger>> rValues = null;
					synchronized(context){
						rValues = (HashMap<Long,ArrayList<BigInteger>>) context.getAttribute(keyForRVal);
					}
					ArrayList<BigInteger> myrValues = rValues.get(id);
					/* store time with t concatenated at the end for each friend
					 * The server will use the same time to get its shared key with the friend.
					 */

					String timeKeyforHttpSession = Long.toString(friend) + "t";
					//store r value so it is the same used for the given time when the next packet comes
					String rValueforSession = Long.toString(friend) + "rvalues";
					synchronized(session){
						session.setAttribute(timeKeyforHttpSession, timegridInfo.get(0));
						session.setAttribute(rValueforSession, myrValues);
					}
					//get hashmap of friend to arraylist of location values for your id from that friend
					key = Long.toString(friend) + "loc"; 
					HashMap<Long,ArrayList<BigInteger>> locTemp;
					synchronized(context){
						locTemp = (HashMap<Long,ArrayList<BigInteger>>) context.getAttribute(key);
					}
					if (locTemp != null){
						/*store information about location for specific grid and time in session to keep it constant until next round ("buffer")
						 * In other words, look in other friend's information map for masked location information for your id
						 */
						ArrayList<BigInteger> loc = locTemp.get(id);
						//store arraylist in session with the friend as the key
						synchronized(session){
							session.setAttribute(Long.toString(friend),loc);
						}
					}
				}
			}
			startIndex += 8;
		}
		//size of bytes stored in arraylist. temp arraylist is necessary because not all friends might have information, so send less bytes back to client
		byte[] responsePacket = new byte[tempInformation.size()+1];
		//first byte is response packet type (Packet Type 2). Then, send friendID and time/grid information associated with that friend.
		responsePacket[0] = new Byte("2").byteValue();
		for (int i = 0; i < tempInformation.size(); i++){
			//add 1 to index because index 0 of response packet represents packet type
			responsePacket[i+1] = tempInformation.get(i).byteValue();
		}
		return responsePacket;
	}
	private byte[] handleSecondPacketType(long id, byte[] result, ServletContext context, HttpSession session, int startIndex)
	{
		//first 5 bytes are id and packet type. each friend has an id (8 bytes) and 3 masked locations (3*6 = 18 bytes)
		int numberFriends = (result.length - startIndex)/(TRANSMIT_SIZE*3 +8);
		int size = 1 + numberFriends*(TRANSMIT_SIZE*3 + 8);
		//responsePacket has 1 byte for packet type (Packet Type 4) and for each friend, friendID 4 bytes and 3*6 bytes of finalLocation
		byte[] responsePacket = new byte[size];
		/*index for response packet off by 4 bytes from result because the response has the same type of 
		 * information except it does not have the user's id
		 */
		int responseIndex = 1;
		for (int i = 0; i < numberFriends; i++){
			byte[] convertLong = new byte[8];
			for (int j = 0; j < convertLong.length; j++){
				convertLong[j] = result[startIndex+j];
			}
			long friend = new BigInteger(convertLong).longValue();
			System.out.println("friend: " + friend);
			startIndex += 8;
			//3 locations from A for each friend
			BigInteger[] aLocs = new BigInteger[3];
			//each big integer is 6 bytes (TRANSMIT_SIZE)
			byte[] tempLoc = new byte[TRANSMIT_SIZE];
			for (int j = 0; j < TRANSMIT_SIZE*3; j++){
				tempLoc[(int)(j%TRANSMIT_SIZE)] = result[startIndex];
				if ((j%TRANSMIT_SIZE) == (TRANSMIT_SIZE-1)){
					aLocs[(int)(j/TRANSMIT_SIZE)] = new BigInteger(tempLoc);
					System.out.println("aLoc" + (int)(j/TRANSMIT_SIZE) + ": " + new BigInteger(tempLoc).toString());
				}
				startIndex++;
			}
			ArrayList<BigInteger> bLocation;
			ArrayList<BigInteger> myrValues;
			synchronized(session){
				 bLocation = (ArrayList<BigInteger>) session.getAttribute(Long.toString(friend));
				 myrValues = (ArrayList<BigInteger>) session.getAttribute(Long.toString(friend)+ "rvalues");
			}
			if (bLocation != null && myrValues != null){
				responsePacket[0] = new Byte("4").byteValue();
				for (int j =0; j < convertLong.length; j++)
					responsePacket[responseIndex +j] = convertLong[j];
				responseIndex += 8;
				BigInteger[] finalLoc = new BigInteger[3];
				for (int j = 0; j < 3; j++){
					System.out.println("randomValue" + j + ": " + myrValues.get(j).toString());
					//calculate final locations using shared key and locations of A and B
					finalLoc[j] = (bLocation.get(j)).subtract(aLocs[j].multiply(myrValues.get(j)));
					finalLoc[j] = finalLoc[j].mod(prime);
					System.out.println("final A side sending " + j + ": " + finalLoc[j].toString());
				}
				for (int j = 0; j < finalLoc.length; j++){
					//change big integers to bytes to put into response packet
					byte[] byteRepresentation = finalLoc[j].toByteArray();
					//pad biginteger if not 6 bytes with 0 in front because it is big endian
					if (byteRepresentation.length < TRANSMIT_SIZE){
						for (int k = 0; k < (TRANSMIT_SIZE - byteRepresentation.length); k++){
							responsePacket[responseIndex] = new Byte("0").byteValue();
							responseIndex++;
						}
					}
					for (int k = 0; k < byteRepresentation.length; k++){
						responsePacket[responseIndex] = byteRepresentation[k];
						responseIndex++;
					}
				}
			}
		}
		return responsePacket;
	}
	private void storeGridInformation(long id, byte[] result, int startIndex, ServletContext context)
	{
		/*order of bytes in packet is packet type (1 byte), userId (8 bytes), lsb (1 byte), 
		 * gridsize (4 bytes), and 8 bytes for each friendId and 6 bytes for every element in the arraylist
		 * associated with the friendID (should be 3 elements for each grid size)
		 */
		ArrayList<Byte> gridtime = new ArrayList<Byte>();
		//next 5 bytes in array is lsb and gridsize in that order
		for (int i =0 ; i < 5; i++){
			System.out.println("gridarray" + i + ": " + result[startIndex+i]);
			gridtime.add(result[startIndex+ i]);
		}
		String keyTemp = Long.toString(id) + "sk";
		byte[] sharedKey = null;
		synchronized(context){
			sharedKey = (byte[]) context.getAttribute(keyTemp);
		}
		if (sharedKey == null)
			return;
		System.out.println("server key is: " + new BigInteger(sharedKey).toString());
		Date curDate = new Date();
		Long sharedTime = (curDate.getTime()) >> 12;
		sharedTime &= 0xFFFFFFFFFFFFFF00L; // Get rid of the least significant byte
		long toOr = gridtime.get(0).longValue();
		toOr = toOr & 0x00000000000000FFL;
		sharedTime |= toOr; // add on stored least significant bit to sync
		System.out.println("time to encrypt: "+ sharedTime);
		startIndex += 5;
		//set grid and time in my servletcontext with key of id + "tg"
		String key = Long.toString(id) + "tg";
		synchronized(context){
			context.setAttribute(key,gridtime);
		}

		HashMap<Long, ArrayList<BigInteger>> locationInfo = new HashMap<Long,ArrayList<BigInteger>>();
		HashMap<Long, ArrayList<BigInteger>> keyInformation = new HashMap<Long, ArrayList<BigInteger>>();
		int numberFriends = (result.length - startIndex)/(TRANSMIT_SIZE*3 +8);
		for (int i = 0; i < numberFriends; i++){
			AESKeyGenerator aes = new AESKeyGenerator();
			ArrayList<BigInteger> randomKey = new ArrayList<BigInteger>();
			for (int j = 0; j < 3; j++){
				byte[] randomVal = aes.generate_r(sharedKey, Long.toString(sharedTime) + Long.toString(3*i + j + 1));
				randomKey.add(new BigInteger(randomVal));
				System.out.println("randomValue" + j + ": " + randomKey.get(j).toString());
			}
			byte[] convertLong = new byte[8];
			for (int j = 0; j < convertLong.length; j++)
				convertLong[j] = result[startIndex+j];
			startIndex += 8;
			long friend = new BigInteger(convertLong).longValue();
			ArrayList<BigInteger> storedLocations= new ArrayList<BigInteger>();
			byte[] tempLoc = new byte[TRANSMIT_SIZE];
			for (int j = 0; j < TRANSMIT_SIZE*3; j++){
				tempLoc[(int)(j%TRANSMIT_SIZE)] = result[startIndex];
				if ((j%TRANSMIT_SIZE) == (TRANSMIT_SIZE-1)){
					storedLocations.add(new BigInteger(tempLoc));
				}
				startIndex++;
			}
			System.out.println("friend id: " + Long.toString(friend));
			locationInfo.put(friend, storedLocations);
			keyInformation.put(friend, randomKey);
		}
		//set hashmap of location information for specific friends into servlet context with key id + "loc"
		String key1 = Long.toString(id) +"loc";
		//store random values ahead of time to be grabbed when A3 (second packet from A client) is sent
		String key2 = Long.toString(id) + "rvalues";
		synchronized(context){
			context.setAttribute(key1,locationInfo);
			context.setAttribute(key2, keyInformation);
		}
	}
	private void shareKey(long id, int startIndex, byte[] result, ServletContext context)
	{
		byte[] encryptedSharedKey = new byte[result.length - 9];
		for (int i = 0; i < encryptedSharedKey.length; i++){
			encryptedSharedKey[i] = result[startIndex];
			startIndex++;
		}
		try{
			RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(RSAmod,secretKey);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			PrivateKey sk = fact.generatePrivate(keySpec);
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, sk);
			byte[] sharedKey = cipher.doFinal(encryptedSharedKey);
			System.out.println("sharedkey: " + new BigInteger(sharedKey).toString());
			String mapKey = id + "sk";
			synchronized(context){
				context.setAttribute(mapKey,sharedKey);
			}
		}
		catch(Exception e){
			System.out.println("key could not be shared be shared because " + e.toString());
		}
	}
}
