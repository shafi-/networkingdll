package protocol6;

import java.util.*;
import java.net.*;
import java.io.*;

//+++++++++++++++++Class: DataLinkLayer+++++++++++++++++++++++++++++++++
class DataLinkLayer extends Thread {

    SimPhy simPhy;
    NetworkLayer nl;
    //Buffer<Integer> eventQueue;
    Buffer<MyEvent> eventQueue;
    MyTimer[] timers;
    MyTimer ack_timer;
    public static int frameBufferSize = 10;
    public static int packetBufferSize = 10;
    //Event Enumeration 
    public static final int network_layer_ready = 1;
    public static final int frame_arrival = 2;
    public static final int cksum_err = 3;
    public static final int timeout = 4;
    public static final int ack_timeout = 5;
    public static final int MAX_SEQ = 7;
    public static final int NR_BUFS = (MAX_SEQ + 1) / 2;
    public static final byte _kind_data = 1;
    public static final byte _kind_ack = 2;
    public static final byte _kind_nak = 0;
    public static boolean no_nak = true;
    public static int oldest_frame;
    public int timeout_duration;//=10;	
    public int ack_timeout_duration=2;

    //=======================================================
    DataLinkLayer(int timeOut, NetworkLayer n) {
        nl = n;
        simPhy = new SimPhy(this);
        eventQueue = new Buffer<MyEvent>("DLL Event Queue", 100);
        timers = new MyTimer[NR_BUFS];
        for (int i = 0; i < NR_BUFS; i++) {
            timers[i] = new MyTimer(i, this);
        }
        timeout_duration = timeOut;
        ack_timer = new MyTimer(this);
        start();
    }
    //=======================================================

    public void addNetworkLayerReadyEvent() {
        addEvent(new MyEvent(network_layer_ready, 0));
    }
    //=======================================================

    public void addFrameArrivalEvent() {
        addEvent(new MyEvent(frame_arrival, 0));
    }
    //=======================================================

    public void addTimeOutEvent(MyTimer m, int eType) {//both for ack and timeout
        
        MyEvent e = new MyEvent(eType, 1);
        e.setArg(m, 0);
        
        oldest_frame=m.frameId;
        addEvent(e);
        
       //System.out.println("in add timeout" + oldest_frame);
    }
    //=======================================================

    public void addChecksumErrorEvent() {
        addEvent(new MyEvent(cksum_err, 0));
    }
    //=======================================================

    public void addEvent(MyEvent m) { //from simport class
        try {
            synchronized (eventQueue) {
                if (eventQueue.full()) {
                    eventQueue.wait();
                }
                eventQueue.store(m);
                eventQueue.notify();
            }
        } catch (Exception e) {
        }
    }
    //=======================================================

    public void run() {
        protocol6();
    }
    //=======================================================

    void protocol6() {
        try {
            int next_frame_to_send;	/*
             * MAX_SEQ > 1; used for outbound stream
             */
            int ack_expected;	/*
             * oldest frame as yet unacknowledged
             */
            int frame_expected;	/*
             * next frame expected on inbound stream
             *
             */
            int too_far;

            Frame r;

            /*
             * scratch variable
             */
            Packet out_buf[] = new Packet[NR_BUFS];

            Packet in_buf[] = new Packet[NR_BUFS];
            boolean[] arrived = new boolean[NR_BUFS];
            /*
             * buffers for the outbound stream
             */
            int nbuffered;	/*
             * # output buffers currently in use
             */
            int i;	/*
             * used to index into the buffer array
             */
            MyEvent event;

            enable_network_layer();	/*
             * allow network_layer_ready events
             */
            ack_expected = 0;	/*
             * next ack expected inbound
             */
            next_frame_to_send = 0;	/*
             * next frame going out
             */
            frame_expected = 0;
            too_far = NR_BUFS;
            /*
             * number of frame expected inbound
             */
            nbuffered = 0;	/*
             * initially no packets are buffered
             */

            for (int k = 0; k < NR_BUFS; k++) {
                arrived[k] = false;
            }

            while (true) {
                event = wait_for_event();

                switch (event.getType()) {

                    case network_layer_ready:

                        nbuffered = nbuffered + 1;
                        out_buf[next_frame_to_send % NR_BUFS] = from_network_layer();
                        send_data(_kind_data, next_frame_to_send, frame_expected, out_buf);
                        next_frame_to_send = inc(next_frame_to_send);
                        break;

                    case frame_arrival:
                        r = simPhy.from_physical_layer();
                       
                        if (r.kind == _kind_data) {

                            if ((r.getSeqNo() != frame_expected) && (no_nak)) {
                                send_data(_kind_nak, 0, frame_expected, out_buf);
                            } else {

                                start_ack_timer();
                            }
                            if (between(frame_expected, r.seqNo, too_far) && (arrived[r.seqNo % NR_BUFS] == false)) {
                                arrived[r.seqNo % NR_BUFS] = true;
                                Packet pak = new Packet();
                                pak.data = r.getPayload();
                                in_buf[r.seqNo % NR_BUFS] = pak;
                                 
                                while (arrived[frame_expected % NR_BUFS]) {
                                    
                                        
                                    nl.to_network_layer(in_buf[frame_expected % NR_BUFS]);

                                    no_nak = true;
                                    arrived[frame_expected % NR_BUFS] = false;

                                    frame_expected = inc(frame_expected);
                                    too_far = inc(too_far);

                                    start_ack_timer();
                                }

                            }
                        }

                        if ((r.kind == _kind_nak) && (between(ack_expected, (r.ackNo + 1) % (MAX_SEQ + 1), next_frame_to_send))) {
                            send_data(_kind_data, (r.ackNo+1) % (MAX_SEQ + 1), frame_expected, out_buf);

                        }
                        while (between(ack_expected, r.ackNo, next_frame_to_send)) {
                            nbuffered = nbuffered - 1;
                            stop_timer(ack_expected % NR_BUFS);
                            ack_expected = inc(ack_expected);

                        }
                        break;


                    case cksum_err:
                        if (no_nak) {
                            send_data(_kind_nak, 0, frame_expected, out_buf);
                        }
                        break;

                    case timeout:
                        System.out.println("time out oldest frame= " + oldest_frame);
                        send_data(_kind_data,oldest_frame, frame_expected, out_buf);
                        break;
                    case ack_timeout:
                        System.out.println("ack time out");
                        send_data(_kind_ack, 0, frame_expected, out_buf);
                        break;



                }

                if (nbuffered < NR_BUFS) {
                    enable_network_layer();
                } else {
                    disable_network_layer();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //=======================================================

    int inc(int i) {
        if (i < (MAX_SEQ)) {
            i = i + 1;
        } else {
            i = 0;
        }
        return i;
    }
    //=======================================================

    void enable_network_layer() {
        nl.setEnable();
    }
    //=======================================================

    void disable_network_layer() {
        nl.disable();
    }
    //=======================================================

    MyEvent wait_for_event() {
        MyEvent event = null;
        try {
            synchronized (eventQueue) {
                if (eventQueue.empty()) {
                    eventQueue.wait();
                }
                event = eventQueue.get();
                eventQueue.notify();
            }
        } catch (Exception e) {
        }
        return event;
    }
    //=======================================================

    Packet from_network_layer() {
        return nl.getPacket();
    }
    //=======================================================

    void send_data(byte fk, int frame_nr, int frame_expected, Packet buffer[]) {

        int seq = frame_nr;
        int ack = (frame_expected + MAX_SEQ) % (MAX_SEQ + 1);
        Frame s;
        

        if (fk == _kind_data) {
            if (buffer[frame_nr % NR_BUFS] == null) {
                s = new Frame(seq, ack, new byte[0], fk); 
                
            } else {
                s = new Frame(seq, ack, buffer[frame_nr % NR_BUFS].getBytes(), fk);
            }
        } else {
            s = new Frame(seq, ack, new byte[0], fk);
        }

        if (fk == _kind_nak) {
            no_nak = false;

        }
        simPhy.to_physical_layer(s); 
        if (fk == _kind_data) {
            start_timer(frame_nr % NR_BUFS);
        }
        stop_ack_timer();      


    }
    //=======================================================

    boolean between(int a, int b, int c) {
        /*
         * Return true if (a <=b < c circularly; false otherwise.
         */
        if (((a <= b) && (b < c)) || ((c < a) && (a <= b)) || ((b < c) && (c < a))) {
            return (true);
        } else {
            return (false);
        }
    }
    //=======================================================

    void start_timer(int i) {
        timers[i].stopTimer();
        timers[i] = new MyTimer(i, this);
        timers[i].startTimer(timeout, timeout_duration);
    }
    //=======================================================

    void stop_timer(int i) {
        timers[i].stopTimer();
    }
    //  MyTimer ack_timer = new MyTimer(this);

    void start_ack_timer() {

         System.out.println("Start ack timer");
        if (ack_timer.running) {
            ack_timer.stopTimer();
        }
        ack_timer = new MyTimer(this);
        ack_timer.startTimer(ack_timeout, ack_timeout_duration);

    }

    void stop_ack_timer() {
        ack_timer.stopTimer();
        System.out.println("Stop ack timer");
     
    }
}
