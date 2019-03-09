import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;


class OriginalNodeWorker extends Thread{
    public static int port;
    public static int nodeID;
    public static String receivedMessage = null;

    byte[] addToDHTbuffer = new byte[1024];
    DatagramSocket comPortOneSocket;
    DatagramPacket comPortOnePacket;

    ArrayList<Integer> fakeDHT = new ArrayList<>();
    Hashtable<Integer, Integer> DHT;


    public OriginalNodeWorker(int port, int nodeID, Hashtable<Integer, Integer> DHT) {
        this.port = port;
        this.nodeID = nodeID;
        this.DHT = DHT;
    }

    public void run() {

        try {
            comPortOneSocket = new DatagramSocket(40001);
            System.out.println("Node: "+nodeID + ", predecessor: "+ nodeID+ ", successor: "+nodeID);

            int originalPort = port+nodeID;
            //add nodeID = 1 to the DHT
            DHT.put(nodeID, originalPort);


            while(receivedMessage==null) {
                comPortOnePacket = new DatagramPacket(addToDHTbuffer, addToDHTbuffer.length);
                comPortOneSocket.receive(comPortOnePacket);
                receivedMessage = new String(comPortOnePacket.getData(),0,comPortOnePacket.getLength());
            }

            System.out.println(receivedMessage);

            receivedMessage = null;

            while(receivedMessage==null) {
                comPortOnePacket = new DatagramPacket(addToDHTbuffer, addToDHTbuffer.length);
                comPortOneSocket.receive(comPortOnePacket);
                receivedMessage = new String(comPortOnePacket.getData(),0,comPortOnePacket.getLength());
            }


            // add a send multicast block here once new node is added to the DHT


        } catch (IOException e) {
            nodeID = (int) (Math.random() *999+2);
            int newPort = nodeID+port;
            addToDHTbuffer = (String.valueOf(nodeID)).getBytes();

            try {
                comPortOneSocket = new DatagramSocket(newPort);
                comPortOnePacket = new DatagramPacket(addToDHTbuffer, addToDHTbuffer.length, InetAddress.getByName("localhost"),40001 );
                comPortOneSocket.send(comPortOnePacket);

            } catch (IOException e1) {
                // should we launch a new "non-original Node Worker here instead of handling everything in try/catch blocks (current setup doesn't work for 3 terminals, only 2)
            }

            // add a receive multicast block here


            System.out.println("Node: "+nodeID + ", predecessor: "+ 1 +", successor: "+nodeID);             //need to do multicast here and call DHT.get(xxx) for predecessor and successor

            //add the new nodeID to the DHT
            //fakeDHT.add(nodeID);
            DHT.put(nodeID, newPort);

            //then add a multi cast send block again here once it's added  (write multicast function like blockchain asssigment)


        }

        comPortOneSocket.close();
    }
}


public class McastDHT {

    public static void main(String args[]) throws IOException, InterruptedException {                                        // McastDHT main
        int port = 40000;
        int nodeID = 1;
        String input;

        Hashtable<Integer, Integer> DHT = new Hashtable<>();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        new OriginalNodeWorker(port, nodeID, DHT).start();                                               // launches a new worker object with the incoming connection

        while(true) {
            input = in.readLine();
            System.out.println("input = " + input);

            if(input.toUpperCase().equals(("STATUS"))){
                System.out.println("Status Console command not implemented yet");
            }

            if(input.toUpperCase().equals(("PING"))){
                System.out.println("Ping Console command not implemented yet");
            }

            if(input.toUpperCase().equals(("LOOPPING"))){
                System.out.println("LoopRing Console command not implemented yet");
            }

            if(input.toUpperCase().equals(("SURVEY"))){
                System.out.println("Full DHT = "+ DHT);
            }

            if(input.toUpperCase().equals(("FILE"))){
                System.out.println("File Console command not implemented yet");
            }



        }
    }
}