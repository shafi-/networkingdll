package protocol6;

import java.util.*;
import java.net.*;
import java.io.*;

//+++++++++++++++++Class: NetworkLayer+++++++++++++++++++++++++++++++++
class NetworkLayer extends Thread {

    public DataLinkLayer dll;
    public Buffer<Packet> packetBuffer;
    BufferedReader br;
    boolean enable;
    //=======================================================

    public static void main(String args[]) throws Exception {
        int port = 0;
        int timeOut = 5;

        int argCount = args.length;
        if (argCount > 0) {
            port = Integer.parseInt(args[0]);
        }
        if (argCount > 1) {
            timeOut = Integer.parseInt(args[1]);
        }


        NetworkLayer networkLayer = new NetworkLayer(port, timeOut);
        networkLayer.join();
    }
    //=======================================================

    public NetworkLayer(int swPort, int timeOut) {
        br = new BufferedReader(new InputStreamReader(System.in));
        dll = new DataLinkLayer(timeOut, this);
        packetBuffer = new Buffer<Packet>("NL Packet", 1);
        enable = false;
        start();
    }
    //=======================================================	

    public void run() {
        try {
            while (true) {
                String input = br.readLine();

                int gt = input.indexOf(">");
                String Type = input.substring(0, gt);
                
                if (Type.equals("msg")) {


                    int colon = input.indexOf(':');
                    int numOfTimes = Integer.parseInt(input.substring(gt + 1, colon));
                    String text = input.substring(colon + 1);
                    for (int i = 0; i < numOfTimes; i++) {
                        String temp = new String(text + "-" + i);
                        Packet p = new Packet(temp.getBytes());
                        storePacket(p);
                        while (true) {
                            if (getEnable() == true) {
                                break;
                            }
                        }
                        dll.addNetworkLayerReadyEvent();
                    }
                }
                if (Type.equals("file")) {
                    String filename = input.substring(gt + 1);
                    File file = new File("Shared//" + filename);
     
                    File_ByteArray FBA = new File_ByteArray();
                    byte[] b = FBA.toByteArray(file.getAbsolutePath());

                    int lastpos;
                    for (lastpos = 511; lastpos < b.length; lastpos = lastpos + 512) {
                        byte[] a = new byte[512];
                        System.arraycopy(b, lastpos - 512 + 1, a,0 , 512);
                       Packet pak = new Packet(a, 1, file, lastpos - 512 + 1, 512);

                        storePacket(pak);
                        while (true) {
                            if (getEnable() == true) {
                                break;
                            }
                        }
                        dll.addNetworkLayerReadyEvent();

                    }

                    byte[] a = new byte[512];
                    System.arraycopy(b, lastpos-512+1, a,0 ,b.length-lastpos+512-1);
                    Packet pak = new Packet(a, 1, file, lastpos-512+1,b.length-lastpos+512-1);

                    storePacket(pak);
                    while (true) {
                        if (getEnable() == true) {
                            break;
                        }
                    }
                    dll.addNetworkLayerReadyEvent();



                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //=================Call From others====================

    Packet getPacket() {//from DLL
        Packet p = null;
        try {
            synchronized (packetBuffer) {
                p = packetBuffer.get();
                enable = false; //do not allow to create next event
                packetBuffer.notify();
            }
        } catch (Exception e) {
        }
        return p;
    }
    //======================================================

    Packet storePacket(Packet p) {//from DLL		
        try {
            synchronized (packetBuffer) {
                if (packetBuffer.full()) {
                    packetBuffer.wait();
                }
                packetBuffer.store(p);
                //packetBuffer.notify();
            }
        } catch (Exception e) {
        }
        return p;
    }
    //======================================================

    synchronized boolean getEnable() {
        return enable;
    }
    //======================================================

    synchronized void setEnable() {
        enable = true;
    }
    //======================================================

    synchronized void disable() {
        enable = false;
    }
    //======================================================

    public void to_network_layer(byte[] b) {
        System.out.println("Showing Packet: " + new String(b) + "\n");
    }

    public void to_network_layer(Packet p) {
        System.out.println("Showing Packet: " + new String(p.getBytes()) + "\n");
    }
}
