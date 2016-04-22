package shafi.networking.dll;

class ByteArray
{

	byte[] bArray;

	//-----------------------------------------------------------------
	ByteArray(int size)
	{
		bArray=new byte[size];
	}

	//-----------------------------------------------------------------
	ByteArray(byte[] b)
	{
		bArray=new byte[b.length];
		System.arraycopy(b,0,bArray,0,b.length);
	}

	//-----------------------------------------------------------------
	void setAt(int index,byte[] b)
	{
		System.arraycopy(b,0,bArray,index,b.length);
	}

	//-----------------------------------------------------------------
	byte getByteVal(int index)
	{
		return bArray[index];
	}

	void setByteVal(int index,byte b)
	{
		bArray[index]=b;
	}

	//-----------------------------------------------------------------
	byte[] getAt(int index,int length)
	{
		byte[] temp=new byte[length];
		System.arraycopy(bArray,index,temp,0,length);
		return temp;
	}

	//-----------------------------------------------------------------
	byte[] getBytes()
	{
		return bArray;
	}

	int getSize()
	{
		return bArray.length;
	}
}


// YOU CAN RE-STRUCTURE THE PACKET CLASS IF YOU WISH. I ASKED SIR REGARDING THIS
// AS THIS STRUCTURED SEEMED SOMEWHAT WEIRD TO ME.
class Packet
{
	byte headerlength[];
	byte header[];
	byte payld[];

	Packet(Packet p)
	{
		headerlength=new byte[4];
		headerlength=p.headerlength;
		header=p.header;
		payld=p.payld;
	}

	Packet(byte[] a)
	{
		String str=new String(a);
		
		// PACKET MAY CONTAIN MSG, FILE OR NOTHING. SO HANDLE ACCORDINGLY.
		if(a.length==0)								
		{
			
		}
		else if (str.startsWith("msg"))
		{
			
		}
		else if (str.startsWith("file"))
		{
			// ACCORDING TO THE FORMAT WE ARE USING IN NETWORK LAYER
			// LEFT OF '-' IS HEADER, RIGHT OF '-' IS PAYLOAD

			int dash=str.indexOf("-");		
			String headerstring=str.substring(0,dash+1);
			header=headerstring.getBytes();
			payld=str.substring(dash+1).getBytes();
		}
	}
	
	byte[] getHeader()
	{
		return header;
	}

	byte[] getPayload()
	{
		return payld;
	}
	
	
	byte[] getBytes()
	{
		// THIS FUNCTION WASN'T ORIGINALLY IN THE SKELETON CODE.
		// I ADDED BECAUSE IT SEEMED REQUIRED. IT MUST RETURN ALL THE BYTES OF THE PACKET.
		// YOU CAN DISCARD IT IF YOU DON'T NEED.

		return null;
	}
}

final class Frame
{

	byte kind;  //protocol6 frame has a new field 'kind'
	byte seqNo;
	byte ackNo;
	
	Packet payload;

	byte checksum;

	Frame(byte[] a)
	{
		kind=a[0];		// field 'kind' is at first byte
		seqNo=a[1];
		ackNo=a[2];
		
		byte payldbyte[]=new byte[a.length-4];
		
		System.arraycopy(a,3,payldbyte,0,payldbyte.length);
		
		payload=new Packet(payldbyte);
		
		checksum=a[a.length-1];
	}

	Frame(int knd,int sNo,int aNo,Packet a)
	{
		kind=(byte)knd;
		seqNo=(byte)sNo;
		ackNo=(byte)aNo;

		payload=a;
		
		checksum=calcChecksum();
	}

	Frame(int knd,int sNo,int aNo)
	{
		kind=(byte)knd;
		seqNo=(byte)sNo;
		ackNo=(byte)aNo;
		
		byte payldbyte[]=new byte[0];
		payload=new Packet(payldbyte);

		checksum=calcChecksum();
	}

	int getKind()
	{
		return kind;
	}

	int getSeqNo()
	{
		return seqNo;
	}

	int getAckNo()
	{
		return ackNo;
	}
	
	Packet getPayload()
	{
		return payload;
	}

	int getChecksum()
	{
		return checksum;
	}

	byte calcChecksum()
	{
		byte cSum;
		
		cSum=(byte)(seqNo^ackNo);		// update the checksum by incorporating kind as xor
		
		if(payload==null) return cSum;
		
		byte[] temp=payload.getBytes();
		
		for (int i=0;i<payload.getBytes().length;i++)
		{
			cSum=(byte)(cSum^temp[i]);
		}
		
		return cSum;
	}

	byte[] getBytes()
	{
		// THIS FUNCTION IS SUPPOSED TO RETURN ALL THE BYTES OF THE FRAME AS A BYTE ARRAY.
		// BUT THE GIVEN CODE IN THE SKELETON DOESN'T SEEM TO DO SO. SO DO IT IN YOUR OWN WAY.
		
		/*

		byte[] frame=new byte[(payload.getHeader().length)+4]; // payload charao aro 4 ta byte ase so +4
		frame[0]=kind;
		frame[1]=seqNo;
		frame[2]=ackNo;
		try{
			System.arraycopy(payload, 0, frame, 3, payload.getHeader().length);
			frame[payload.getHeader().length+3]=checksum;
			//frame e kind,seqno,ackno (3 ta byte-so +3) ,payload.length ei koi bit er por checksum dhukabo
			return frame;
		}
		catch(Exception e){
			e.printStackTrace();
		}

		*/

		return null;
	}

	String getString()
	{
		return "Kind="+kind+" | Seq No="+seqNo+" | Ack No="+ackNo+" | Payload="+new String(payload.getBytes())+" | Checksum="+checksum;
	}

	// tells us if there is any checksum error that is calculated & (given in argument checksum ) doesn't match
	boolean hasCheckSumError()
	{
		return checksum!=calcChecksum();
	}

}

class Buffer<T>
{

	T data[];
	int size;
	int head;
	int tail;
	String name;

	Buffer(String n,int sz)
	{
		name=n;
		size=sz+1;
		data=(T[])new Object[size];
		head=0;
		tail=0;
	}

	synchronized boolean empty()
	{
		return head==tail;
	}

	synchronized boolean full()
	{
		return (tail+1)%size==head;
	}

	synchronized boolean store(T t)
	{
		if (full())
		{
			System.out.println(name+" Buffer Full. Dropping Packet ...");
			return false;
		}
		else
		{
			tail=(tail+1)%size;
			data[tail]=t;
			return true;
		}
	}

	synchronized T get()
	{
		if (empty())
		{
			System.out.println(name+" Buffer Empty.");
			return null;
		}
		else
		{
			head=(head+1)%size;
			return data[head];
		}
	}
}

class MyEvent
{

	int eventType; //use inner class to eliminate this.
	int numOfArgs;
	Object[] args;

	public MyEvent(int eType,int numArgs)
	{
		eventType=eType;
		numOfArgs=numArgs;
		args=new Object[numArgs];
	}

	public void setArg(Object arg,int num)
	{
		args[num]=arg;
	}

	public Object getArg(int num)
	{
		return args[num];
	}

	public int getType()
	{
		return eventType;
	}
}

class MyTimer extends Thread
{

	int frameId;
	DataLinkLayer dll;

	boolean running;
	int event;
	int duration; //for thread based timer

	//new constructor for protocol 6 leaving the frameid as no need for separate ack frame
	public MyTimer(DataLinkLayer d)
	{
		frameId=0;
		dll=d;
		running=false;
	}
	
	public MyTimer(DataLinkLayer d,int etype)
	{
		dll=d;
		frameId=0;
		running=false;
		event=etype;
	}
	
	public MyTimer(int id,DataLinkLayer d)
	{
		frameId=id;
		dll=d;
		running=false;
	}

	public void startTimer(int eType,int timeout_duration)
	{
		running=true;
		event=eType;
		duration=timeout_duration*1000;
		start();
		//System.out.println("Scheduling Timer: "+timerId+"\n");
	}

	synchronized public void stopTimer()
	{
		running=false;
		this.interrupt();
		//System.out.println("Stopping Timer: "+frameId+"\n");
	}

	public int getFrameId()
	{
		return frameId;
	}

	synchronized public boolean isRunning()
	{
		return running;
	}

	@Override
	public void run()
	{
		//--------Using delay------------------
		try
		{
			Thread.sleep(duration);
			
			/*
			WHAT SHOULD BE DONE IF A TIMER TIMES OUT (APART FROM
			WHAT'S ALREADY DONE IN THIS FUNCTION)? 
			DO IT HERE.
			
			HINT: THIS PART WILL PROBABLY MAKE YOUR BRAIN GO DRY.:-P
			*/
			
			System.out.println("Time's up for frame seq no: "+frameId+"\n");
			
			dll.addTimeOutEvent(this,event);
			
		} catch (InterruptedException e)
		{
			System.out.println("\tTimer for frame seq no: "+frameId+" stopped\n");
		}
	}
}