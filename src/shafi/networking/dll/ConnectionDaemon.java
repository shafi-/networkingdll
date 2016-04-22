package shafi.networking.dll;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicLong;

class ConnectionDaemon
{

	public static double SWITCH_PACKET_MISSING_RATE;// = 0.01;
	public static double ERROR_RATE;// = 0.01;

	public static double getDropRate()
	{
		return SWITCH_PACKET_MISSING_RATE;
	}

	public static double getErrorRate()
	{
		return SWITCH_PACKET_MISSING_RATE;
	}

	public static void main(String args[]) throws Exception
	{
		double dropRate=0;
		double errorRate=0;
		int argCount=args.length;

		if (argCount>0)
		{
			dropRate=Double.parseDouble(args[0]);
		}
		if (argCount>1)
		{
			errorRate=Double.parseDouble(args[1]);
		}

		ConnectionDaemon.SWITCH_PACKET_MISSING_RATE=dropRate;
		ConnectionDaemon.ERROR_RATE=errorRate;

		ServerSocket welcomeSocket=new ServerSocket(9009);
		Socket connectionSocket[]=new Socket[2];
		DataInputStream[] inFromClient=new DataInputStream[2];
		DataOutputStream outToClient[]=new DataOutputStream[2];
		
		for (int i=0;i<2;i++)
		{
			connectionSocket[i]=welcomeSocket.accept();
			inFromClient[i]=new DataInputStream(connectionSocket[i].getInputStream());
			outToClient[i]=new DataOutputStream(connectionSocket[i].getOutputStream());
		}
		
		SThread A2B=new SThread("A","B",inFromClient[0],outToClient[1]);
		SThread B2A=new SThread("B","A",inFromClient[1],outToClient[0]);
		
		A2B.join();
		B2A.join();
		
		//close the sockets here
	}
}

class SThread extends Thread
{

	String name;
	String otherName;
	DataInputStream inFromClient;
	DataOutputStream outToClient;
	String clientSentence;

	public SThread(String n,String o,DataInputStream in,DataOutputStream out)
	{
		name=n;
		otherName=o;
		inFromClient=in;
		outToClient=out;
		start();
	}

	@Override
	public void run()
	{
		try
		{
			while (true)
			{

				byte[] temp=SimPhy.readDeStuffed(inFromClient);
				
				Frame f=new Frame(temp);
				
				System.out.println("\tFrame Received from "+name+": "+f.getString()+"\n");
				
				//Drop Packet Randomly
				double dpp=Math.random();
				
				if (dpp<ConnectionDaemon.getDropRate())
				{
					System.out.println("\tDropping Frame:  "+f.getString()+"\n");
					continue;
				}

				//add random error before transmitting
				double fep=Math.random();
				
				if (fep<ConnectionDaemon.getErrorRate())
				{	
					//toggle the checksum
					int changeByteAt=temp.length-1;
					temp[changeByteAt]=(byte)(temp[changeByteAt]^(byte)255);
					
					System.out.println("Error added to Frame \n");
				}

				SimPhy.writeStuffed(outToClient,temp);
				
				System.out.println("Frame Sent to "+otherName+": "+new Frame(temp).getString()+"\n");
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static final AtomicLong LAST_TIME_MS=new AtomicLong();

	public static long uniqueCurrentTimeMS()
	{
		long now=System.currentTimeMillis();
		
		while (true)
		{
			long lastTime=LAST_TIME_MS.get();
			if (lastTime>=now)
			{
				now=lastTime+1;
			}
			if (LAST_TIME_MS.compareAndSet(lastTime,now))
			{
				return now;
			}
		}
	}
}
