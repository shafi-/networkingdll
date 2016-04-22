package shafi.networking.dll;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

class NetworkLayer extends Thread
{
	public DataLinkLayer dll;
	public final Buffer<Packet> packetBuffer;		// error khaite pari ekta.
	BufferedReader br;

	boolean enable;

	public static void main(String args[]) throws Exception
	{
		int port=0;
		int timeOut=5;

		int argCount=args.length;
		
		if (argCount>0)
		{
			port=Integer.parseInt(args[0]);
		}
		if (argCount>1)
		{
			timeOut=Integer.parseInt(args[1]);
		}

		NetworkLayer networkLayer=new NetworkLayer(port,timeOut);
		networkLayer.join();
	}

	public NetworkLayer(int swPort,int timeOut)
	{
		br=new BufferedReader(new InputStreamReader(System.in));
		dll=new DataLinkLayer(timeOut,this);
		packetBuffer=new Buffer<>("NL Packet",1);
		enable=false;
		start();
	}

	@Override
	public void run()
	{
		try
		{
			while (true)
			{
				String input=br.readLine();		// Format: 'msg>10:hello' or 'file>myfile'

				int grt=input.indexOf('>');
				String infotype=input.substring(0,grt);

				if (infotype.equals("msg"))
				{
					int colon=input.indexOf(':');
					int numOfTimes=Integer.parseInt(input.substring(grt+1,colon));
					
					String text=input.substring(colon+1);
					
					for (int i=0;i<numOfTimes;i++)
					{
						String temp="msg"+text+"-"+i;
						
						Packet p=new Packet(temp.getBytes());
						storePacket(p);
						
						while (true)
						{
							if (getEnable()) break;
						}
						
						dll.addNetworkLayerReadyEvent();
					}
				}
				else if (infotype.equals("file"))
				{
					String filename=input.substring(grt+1);
					String filepath=""+filename;
					
					// create file object
					File file=new File(filepath);
					FileInputStream in=new FileInputStream(file);
					
					long filesize=file.length();
					int startByte;
					int chunkSize=512;

					for (int i=0;i<filesize;i+=chunkSize)
					{
						startByte=i;

						if (((int)filesize-startByte)<512)
						{
							chunkSize=((int)filesize-startByte);
						}
						
						byte[] bytes=new byte[chunkSize];
						
						in.read(bytes);

						String temp="file "+filename+" "+filesize+" "+startByte+" "+chunkSize+"-"+new String(bytes);
						
						Packet p=new Packet(temp.getBytes());
						storePacket(p);
						
						while (true)
						{
							if (getEnable()) break;
						}
						
						dll.addNetworkLayerReadyEvent();
					}
					
					in.close();
				}
			}
			
		} catch (IOException|NumberFormatException e)
		{
			e.printStackTrace();
		}
	}

	Packet getPacket()	//from DLL
	{
		Packet p=null;
		
		try
		{
			synchronized (packetBuffer)
			{
				p=packetBuffer.get();
				enable=false;			//do not allow to create next event
				packetBuffer.notify();
			}
		} catch (Exception e) {}
		
		return p;
	}

	Packet storePacket(Packet p)	//from DLL
	{
		try
		{
			synchronized (packetBuffer)
			{
				if (packetBuffer.full())
				{
					packetBuffer.wait();
				}
				
				packetBuffer.store(p);
				
				//packetBuffer.notify();
			}
		} catch (Exception e) {}
		
		return p;
	}

	synchronized boolean getEnable()
	{
		return enable;
	}

	synchronized void setEnable()
	{
		enable=true;
	}

	synchronized void disable()
	{
		enable=false;
	}

	public void to_network_layer(Packet b)
	{
		
		/*
		PACKET CAN BE OF TWO TYPES: MSG AND FILE.
		IF MSG, JUST SHOW THE MSG.
		IF FILE, TAKE PROPER STEPS TO STORE THE FILE AND
		SHOW A MSG THAT A FILE OR FILE SEGMENT IS RECEIVED.

		ONE MORE THING, THE CODE BELOW SHOWING THE PACKET INFO
		IS NOT REQUIRED.
		*/
		
		System.out.println("\tShowing Packet: "+new String(b.getHeader())+" "+new String(b.getPayload())+"\n");
	}
}
