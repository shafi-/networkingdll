package protocol6;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.nio.*;

//+++++++++++++++++Class: ByteArray++++++++++++++++++++++++++++++++++++
class ByteArray {

    byte[] bArray;
    //-----------------------------------------------------------------	

    ByteArray(int size) {
        bArray = new byte[size];
    }
    //-----------------------------------------------------------------	

    ByteArray(byte[] b) {
        bArray = new byte[b.length];
        System.arraycopy(b, 0, bArray, 0, b.length);
    }
    //-----------------------------------------------------------------	

    void setAt(int index, byte[] b) {
        System.arraycopy(b, 0, bArray, index, b.length);
    }
    //-----------------------------------------------------------------	

    byte getByteVal(int index) {
        return bArray[index];
    }

    void setByteVal(int index, byte b) {
        bArray[index] = b;
    }
    //-----------------------------------------------------------------	

    byte[] getAt(int index, int length) {
        byte[] temp = new byte[length];
        System.arraycopy(bArray, index, temp, 0, length);
        return temp;
    }
    //-----------------------------------------------------------------	

    byte[] getBytes() {
        return bArray;
    }

    int getSize() {
        return bArray.length;
    }
}

//+++++++++++++++++Class: Packet++++++++++++++++++++++++++++++++++++
class Packet {

    byte data[];
    int header_length;
    byte[] header;
    int size;
    byte b[];
    String fileName;
    long fileSize;
    int startByte;
    int chunkSize;
    //=======================================================	

    Packet() {
    }

    Packet(byte[] a, int type, File file, int start, int chunksize) {
       
        
            fileName=file.getName();
            fileSize=file.length();
            startByte=start;
            chunkSize=chunksize;            
            
            size = a.length;
            if (type == 0) {
                header = new String("msg").getBytes();
            }
            if (type == 1) {
                header = new String("file" + " " + fileName + " " + fileSize + " " + startByte + " " + chunkSize).getBytes();
            }
            header_length = header.length;
            b = new byte[4];
            b[3] = (byte) header_length;
            header_length = header_length >>> 8;
            b[2] = (byte) header_length;
            header_length = header_length >>> 8;
            b[1] = (byte) header_length;
            header_length = header_length >>> 8;
            b[0] = (byte) header_length;
            data = new byte[size + 4 + header.length];
            System.arraycopy(a, 0, data, 4 + header.length, size);
            System.arraycopy(b, 0, data, 0, 4);
            System.arraycopy(header, 0, data, 4, header.length);
      
     
    }

    Packet(byte[] a) {

        size = a.length;
        header = new String("msg").getBytes();
        header_length = header.length;
        b = new byte[4];
        b[3] = (byte) header_length;
        header_length = header_length >>> 8;
        b[2] = (byte) header_length;
        header_length = header_length >>> 8;
        b[1] = (byte) header_length;
        header_length = header_length >>> 8;
        b[0] = (byte) header_length;
        data = new byte[size + 4 + header.length];
        System.arraycopy(a, 0, data, 4 + header.length, size);
        System.arraycopy(b, 0, data, 0, 4);
        System.arraycopy(header, 0, data, 4, header.length);

    }
    
    public Packet makePacket(byte[] b)
    {
        Packet p= new Packet();
        byte[] header_len=new byte[4];
        header_len[3]=b[3];
        header_len[2]=b[2];
        header_len[1]=b[1];
        header_len[0]=b[0];
        int header_lenth=0;
        header_lenth=header_lenth|b[0];
        header_lenth = header_lenth <<8;
        
        header_lenth=header_lenth|b[1];
        header_lenth = header_lenth << 8;
        
        header_lenth=header_lenth|b[2];
        header_lenth = header_lenth << 8;  
        
        
        return p;
    }


    //=======================================================			

    byte[] getBytes() {
        return data;
    }
}
//+++++++++++++++++Class: Frame++++++++++++++++++++++++++++++++++++
class Frame {

    byte kind;
    byte seqNo;
    byte ackNo;
    byte payload[];
    byte checksum;
    //=======================================================	

    Frame(byte[] a) {

        kind = a[0];
        seqNo = a[1];
        ackNo = a[2];
        payload = new byte[a.length - 4];
        System.arraycopy(a, 3, payload, 0, payload.length);

        checksum = a[a.length - 1];
    }
    //=======================================================	

    Frame(int sNo, int aNo, byte[] a, byte kind) {
        this.kind = kind;
        seqNo = (byte) sNo;
        ackNo = (byte) aNo;

        payload = new byte[a.length];

        try {
            System.arraycopy(a, 0, payload, 0, a.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        checksum = calcChecksum();
    }
    //=======================================================	

    Frame(int sNo, int aNo, byte kind) {
        this.kind = kind;
        seqNo = (byte) sNo;
        ackNo = (byte) aNo;
        payload = new byte[0];
        checksum = calcChecksum();
    }
    //=======================================================

    int getSeqNo() {
        return seqNo;
    }

    int getAckNo() {
        return ackNo;
    }

    byte[] getPayload() {
        return payload;
    }

    int getChecksum() {
        return checksum;
    }

    byte calcChecksum() {
        byte cSum = 0;
        cSum = (byte) (kind^seqNo ^ ackNo);
        for (int i = 0; i < payload.length; i++) {
            cSum = (byte) (cSum ^ payload[i]);
        }     
        return cSum;
    }
    //=======================================================

    byte[] getBytes() {
        byte[] frame = new byte[payload.length + 4];
        frame[0] = kind;
        frame[1] = seqNo;
        frame[2] = ackNo;
        try {
            System.arraycopy(payload, 0, frame, 3, payload.length);
            frame[payload.length + 3] = checksum;
            return frame;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //=======================================================

    String getString() {
        return new String("Kind " + kind + " Seq No=" + seqNo + " | Ack No=" + ackNo + " | Payload=" + new String(payload) + " | Checksum=" + checksum);
    }
    //=======================================================

    boolean hasCheckSumError() {
        if (checksum != calcChecksum()) {
            return true;
        } else {
            return false;
        }
    }
}
//+++++++++++++++++Class: Buffer+++++++++++++++++++++++++++++++++
class Buffer<T> {

    T data[];
    int size;
    int head;
    int tail;
    String name;
    //=======================================================	

    Buffer(String n, int sz) {
        name = n;
        size = sz + 1;
        data = (T[]) new Object[size];
        head = 0;
        tail = 0;
    }
    //=======================================================

    synchronized boolean empty() {
        if (head == tail) {
            return true;
        } else {
            return false;
        }
    }
    //=======================================================

    synchronized boolean full() {
        if ((tail + 1) % size == head) {
            return true;
        } else {
            return false;
        }
    }
    //=======================================================		

    public synchronized boolean store(T t) {
        if (full()) {
            System.out.println(name + " Buffer Full. Dropping Packet ...");
            return false;
        } else {
            tail = (tail + 1) % size;
            data[tail] = t;
            return true;
        }
    }
    //=======================================================		

    synchronized T get() {
        if (empty()) {
            System.out.println(name + " Buffer Empty.");
            return null;
        } else {
            head = (head + 1) % size;
            return data[head];
        }
    }
    //=======================================================	
}
//+++++++++++++++++Class: MyEvent++++++++++++++++++++++++++++++++++++
class MyEvent {

    int eventType; //use inner class to eliminate this.
    int numOfArgs;
    Object[] args;

    public MyEvent(int eType, int numArgs) {
        eventType = eType;
        numOfArgs = numArgs;
        args = new Object[numArgs];
    }

    public void setArg(Object arg, int num) {
        args[num] = arg;
    }

    public Object getArg(int num) {
        return args[num];
    }

    public int getType() {
        return eventType;
    }
}
//+++++++++++++++++Class: MyTimer++++++++++++++++++++++++++++++++++++
class MyTimer extends Thread {

    int frameId;
    DataLinkLayer dll;
    boolean running;
    int event;
    int duration; //for thread based timer

    public MyTimer(int id, DataLinkLayer d) {
        frameId = id;
        dll = d;
        running = false;
    }

    public MyTimer(DataLinkLayer d) {
        dll = d;
        running = false;
    }

    public void startTimer(int eType, int timeout_duration) {
        running = true;
        event = eType;
        duration = timeout_duration * 1000;
        start();
        //System.out.println("Scheduling Timer: "+timerId+"\n");			
    }

    public void start_ack_timer(int eType, int timeout_duration) {
        running = true;
        event = eType;
        duration = timeout_duration * 1000;
        start();
        //System.out.println("Scheduling Timer: "+timerId+"\n");			
    }

    synchronized public void stopTimer() {
        running = false;
        this.interrupt();
        //System.out.println("Stopping Timer: "+timerId+"\n");
    }

    public int getFrameId() {
        return frameId;
    }

    synchronized public boolean isRunning() {
        return running;
    }

    public void run() {
        //--------Using delay------------------
        try {
            Thread.sleep(duration);
            //------------------------------------------
            System.out.println("Time's up for frame seq no: " + frameId + "\n");
            //------------------------------------------
            dll.addTimeOutEvent(this, event);
        } catch (InterruptedException e) {
            System.out.println("Timer for frame seq no: " + frameId + " stopped\n");
            //e.printStackTrace();
        }
    }
}
