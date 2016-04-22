package shafi.networking.dll;
import java.net.*;
import java.io.*;

class SimPhy extends Thread
{

	DataLinkLayer dll;

	Socket sock;

	DataInputStream br;
	OutputStream bw;

	Buffer<Frame> portBuffer;
	boolean hasConnection=false;

	public static final int portBufferSize=10;

	SimPhy(DataLinkLayer d)
	{
		dll=d;
		
		try
		{
			sock=new Socket("127.0.0.1",9009);
			System.out.println("Debug: Physical Layer Connected"+"\n");
			hasConnection=true;
			br=new DataInputStream(sock.getInputStream());
			bw=sock.getOutputStream();
			portBuffer=new Buffer<>("SimPhy",portBufferSize);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		start();
	}

	public void to_physical_layer(Frame f)
	{
		try
		{
			if (hasConnection)
			{
				writeStuffed(bw,f.getBytes());
				System.out.println("Frame Sent: "+f.getString()+"\n");
			}
			
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public Frame from_physical_layer()
	{
		Frame f=null;
		try
		{
			synchronized (portBuffer)
			{
				f=portBuffer.get();
				portBuffer.notify();
			}
		} catch (Exception e)
		{
		}
		return f;
	}

	public void storeInPortBuffer(Frame f)
	{
		try
		{
			synchronized (portBuffer)
			{
				if (portBuffer.full())
				{
					portBuffer.wait();
				}
				portBuffer.store(f);
				portBuffer.notify();
			}
		} catch (Exception e)
		{
		}
	}

	@Override
	public void run()
	{
		try
		{
			while (true)
			{
				if (hasConnection)
				{
					byte[] temp=readDeStuffed(br);
					Frame f=new Frame(temp);
					
					System.out.println("\tFrame Received: "+f.getString()+"\n");

					if (f.hasCheckSumError())
					{
						System.out.println("\tChecksum Error in Frame: "+f.getString()+"\n");
						dll.addChecksumErrorEvent();
					}
					else
					{
						storeInPortBuffer(f);
						dll.addFrameArrivalEvent();
					}
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void writeStuffed(OutputStream bw,byte[] f)
	{
		try
		{
			byte[] temp=SimPhy.bitStuff(f);
			
			ByteArray b=new ByteArray(temp.length+2);
			
			b.setByteVal(0,(byte)126);				//starting flag byte
			b.setAt(1,temp);
			b.setByteVal(temp.length+1,(byte)126);	//ending flag byte
			bw.write(b.getBytes());
			
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static byte[] readDeStuffed(DataInputStream br)
	{
		byte[] b=new byte[1000]; //this size is arbitrary.
		int count=0;
		try
		{
			byte i=br.readByte();
			
			while (i!=126)		//skip as long as there is no preamble
			{
				// EITAR AGA MATHA KSU BUJHINAI.
			}

			i=br.readByte();
			
			while (i!=126)
			{
				b[count++]=i;
				i=br.readByte();
			}
			
			byte[] temp=new byte[count];
			System.arraycopy(b,0,temp,0,count);

			return SimPhy.bitDeStuff(temp);

		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	public static byte[] bitStuff(byte[] b)
	{
		// WRITE BIT STUFFING CODE HERE
		return b;
	}

	public static byte[] bitDeStuff(byte[] b)
	{
		// WRITE BIT DESTUFFING CODE HERE
		return b;
	}
}
