package com.protocol2.location;

import java.math.BigInteger;

public class FriendData {
	String sharedKey;
	String id;
	String name;
	
	boolean isNearby;
	
	BigInteger randomS; //= BigInteger.valueOf(1223425); //5;
	BigInteger randomA0; // = BigInteger.valueOf(232345); //(alice = 10); (bob = 5)
	BigInteger randomS0; // = BigInteger.valueOf(83232532); //8;
	
	//General
	BigInteger s[] = new BigInteger[3];
	BigInteger a[][] = new BigInteger[3][3];
	//BigInteger userLocation[] = new BigInteger[3];
}
