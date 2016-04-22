package shafi.networking.dll;

class DataLinkLayer extends Thread
{

	SimPhy simPhy;

	NetworkLayer nl;

	Buffer<MyEvent> eventQueue;
	MyTimer[] timers;
	MyTimer auxtimers;

	public static int frameBufferSize=10;
	public static int packetBufferSize=10;

	// Event Enumeration
	public static final int network_layer_ready=1;
	public static final int frame_arrival=2;
	public static final int cksum_err=3;
	public static final int timeout=4;
	public static final int ack_timeout=5;
	
	public static final int data=0;
	public static final int nak=2;
	public static final int ack=1;
	
	public static final int MAX_SEQ=7;

	///
	public static final int NR_BUFS=((MAX_SEQ+1)/2);
	public static boolean no_nak=true;					// no nak has been sent yet
	
	int oldest_frame=MAX_SEQ+1;			// initial value only for simulator
	///

	int timeout_duration=5;//=10;

	DataLinkLayer(int timeOut,NetworkLayer n)
	{
		nl=n;
		simPhy=new SimPhy(this);
		eventQueue=new Buffer<>("DLL Event Queue",100);
		timers=new MyTimer[MAX_SEQ+1];
		
		auxtimers=new MyTimer(this,ack_timeout);
		
		for (int i=0;i<MAX_SEQ+1;i++)
		{
			timers[i]=new MyTimer(i,this);
		}
		
		timeout_duration=timeOut;
		start();
	}

	public void addNetworkLayerReadyEvent()
	{
		addEvent(new MyEvent(network_layer_ready,0));
	}

	public void addFrameArrivalEvent()
	{
		addEvent(new MyEvent(frame_arrival,0));
	}
	
	//both for ack and timeout
	public void addTimeOutEvent(MyTimer m,int eType)
	{
		MyEvent e=new MyEvent(eType,1);
		e.setArg(m,0);
		addEvent(e);
	}

	public void addChecksumErrorEvent()
	{
		addEvent(new MyEvent(cksum_err,0));
	}
	
	//from simport class
	public void addEvent(MyEvent m)
	{
		try
		{
			synchronized (eventQueue)
			{
				if (eventQueue.full())
				{
					eventQueue.wait();
				}
				eventQueue.store(m);
				eventQueue.notify();
			}
		} catch (Exception e)
		{
		}
	}

	@Override
	public void run()
	{
		protocol6();
	}

	void protocol6()
	{
		try
		{
			int ack_expected;			// lower edge of sender's window
			int next_frame_to_send;		// upper edge of sender's window+1
			int frame_expected;			// lower edge of receivers's window
			int too_far;				// upper edge of receivers's window+1
			int i;						// used to index into the buffer pool

			Frame r;					/* scratch variable */

			Packet out_buf[]=new Packet[NR_BUFS];	/* buffers for the outbound stream */

			Packet in_buf[]=new Packet[NR_BUFS];		/* buffers for the inbound stream */

			boolean arrived[]=new boolean[NR_BUFS];	/* inbound bitmap */

			int nbuffered;							/* # output buffers currently in use */

			MyEvent event;

			enable_network_layer();					/* allow network_layer_ready events */

			ack_expected=0;							/* next ack expected inbound */

			next_frame_to_send=0;					/* next frame going out */

			frame_expected=0;						/* number of frame expected inbound */

			too_far=NR_BUFS;
			nbuffered=0;							/* initially no packets are buffered */

			for (i=0;i<NR_BUFS;i++)					// initially no packets are buffered
			{
				arrived[i]=false;
			}

			while (true)
			{
				event=wait_for_event();				/* four possibilities: see event_type above */

				switch (event.getType())
				{
					
					/* the network layer has a packet to send.
					So Accept, save, and transmit a new frame. */
					case network_layer_ready:
						
						nbuffered=nbuffered+1;	/* expand the sender's window */

						out_buf[next_frame_to_send%NR_BUFS]=from_network_layer();	/* fetch new packet */
						
						// 0 is for data frame
						send_frame(data,next_frame_to_send,frame_expected,out_buf);	/* transmit the frame. */

						next_frame_to_send=inc(next_frame_to_send);					/* advance sender's upper window edge */

						break;

						
					/* a data or control frame has arrived */
					case frame_arrival:

						r=simPhy.from_physical_layer();	/* get incoming frame from physical layer */
						

						if (r.getKind()==data) /* An undamaged frame has arrived */
						{
							if ((r.getSeqNo()!=frame_expected) && no_nak)
							{
								send_frame(nak,0,frame_expected,out_buf);	// we are sending a nak.
							}
							else
							{
								start_ack_timer();
							}
							
							// frames may be accepted in any order
							if (between(frame_expected,r.getSeqNo(),too_far) && (!arrived[r.getSeqNo()%NR_BUFS]))
							{
								arrived[r.getSeqNo()%NR_BUFS]=true;		/* mark buffer as full */

								in_buf[r.getSeqNo()%NR_BUFS]=r.getPayload();
								
								/* pass frames & advance window */
								while (arrived[frame_expected%NR_BUFS])
								{
									nl.to_network_layer(in_buf[frame_expected%NR_BUFS]);
									no_nak=true;
									
									arrived[frame_expected%NR_BUFS]=false;
									
									frame_expected=inc(frame_expected);	/* advance lower edge of receivers window */

									too_far=inc(too_far);				/* advance upper edge of receivers window */

									start_ack_timer();					/* to see if a separate ack is needed */
								}
							}
						}
						
						if ((r.getKind()==nak) && between(ack_expected,(r.getAckNo()+1)%(MAX_SEQ+1),next_frame_to_send))
						{
							send_frame(data,(r.getAckNo()+1)%(MAX_SEQ+1),frame_expected,out_buf);
						}

						/* Ack n implies n-1, n-2, etc. Check for this. */
						while (between(ack_expected,r.getAckNo(),next_frame_to_send))
						{
							/* Handle piggybacked ack. */
							nbuffered=nbuffered-1;	/* one frame fewer buffered */

							stop_timer(ack_expected%NR_BUFS);	/* frame arrived intact; stop timer */

							ack_expected=inc(ack_expected);		/* contract sender's window */
						}
						
						break;

					case cksum_err:
						
						if (no_nak)
						{
							send_frame(nak,0,frame_expected,out_buf);
						}
						
						break;
						
					/* timed out */
					case timeout:

						send_frame(data,oldest_frame,frame_expected,out_buf);
						break;
						
					case ack_timeout:
						send_frame(ack,0,frame_expected,out_buf);
				}

				if (nbuffered<MAX_SEQ)
				{
					enable_network_layer();
				}
				else
				{
					disable_network_layer();
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	int inc(int i)
	{
		if (i<MAX_SEQ)
		{
			i=i+1;
		}
		else
		{
			i=0;
		}
		
		return i;
	}

	void enable_network_layer()
	{
		nl.setEnable();
	}

	void disable_network_layer()
	{
		nl.disable();
	}

	MyEvent wait_for_event()
	{
		MyEvent event=null;
		try
		{
			synchronized (eventQueue)
			{
				if (eventQueue.empty())
				{
					eventQueue.wait();
				}
				event=eventQueue.get();
				eventQueue.notify();
			}
		} catch (Exception e)
		{
		}
		return event;
	}

	Packet from_network_layer()
	{
		return nl.getPacket();
	}
	
	/* Construct and send a frame. */
	void send_frame(int fk,int frame_nr,int frame_expected,Packet buffer[])
	{
		/*
		THE GIVEN CODE IN THIS PART IS PRETTY MUCH
		MISGUIDING.

		SO FILL THIS PART CAREFULLY ON YOUR OWN.
		IN THIS PART YOU WILL BASICALLY HAVE TO:
		1. CREATE A FRAME.
		2. TRANSMIT IT.

		NOW, THE FRAME CAN BE OF THREE KINDS:
		1. DATA FRAME
		2. ACK FRAME
		3. NAK FRAME.

		ACK AND NAK MAY NOT HAVE PAYLOAD, UNLIKE DATA FRAME. SO, WHEN
		CREATING FRAME OBJECT, THINK ABOUT WHICH CONSTRUCTOR
		SHOULD U USE.
		*/

		
		/*
		Packet p=null;
		int kind = fk;
		if(fk == 0) 
			p = buffer[frame_nr % NR_BUFS];	// insert packet into frame 
		int seq = frame_nr;	// insert sequence number into frame
		int ack = (frame_expected + MAX_SEQ) % (MAX_SEQ + 1);	 
		if(fk == 2) 
			no_nak = false;
		Frame s=new Frame(kind,seq, ack, p); 	// scratch variable
		simPhy.to_physical_layer(s);	// transmit the frame
		if(fk == 0) 
			start_timer(frame_nr % NR_BUFS);  //start the timer running
		stop_ack_timer();

		*/
	}
	
	/* Return true if a <= b < c circularly; false otherwise. */
	boolean between(int a,int b,int c)
	{
		return (((a<=b) && (b<c)) || ((c<a) && (a<=b)) || ((b<c) && (c<a)));
	}

	void start_timer(int i)
	{
		timers[i].stopTimer();
		timers[i]=new MyTimer(i,this);
		timers[i].startTimer(timeout,timeout_duration*8);
	}

	void stop_timer(int i)
	{
		timers[i].stopTimer();
	}
	

	// Below two methods were not in protocol-5
	
	void start_ack_timer()
	{
		auxtimers.stopTimer();
		auxtimers=new MyTimer(this);
		auxtimers.startTimer(ack_timeout,timeout_duration);
	}

	void stop_ack_timer()
	{
		auxtimers.stopTimer();
	}
}
