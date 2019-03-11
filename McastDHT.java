/*

1. Jeff Wiand / 3-10-19
2. Java 1.8
3. Compilation Instructions:
    > javac McastDHT.java

4. Run Instructions  (Open up as many terminals as need to launch new nodes: 1 terminal = 1 node)
    > java McastDHT

   List of files needed for running the program
    - McastDHT.java
    - McastDHTLog.txt

5. My Notes
    - This version of McastDHT can handle any # nodes being added to a DHT. The first node detects if its the original and adds itself to the DHT,
    prints its attributes to the console, and opens up a DatagramSocket at port 40001.
    - The next node detects that it's not the original node, generates a new random node ID between 2 and 999. Sends that message to the original node
    using UDP, then prints its attributes to the console and adds itself to the DHT (which isn't multicasting correctly, it only returns the DHT once to the current newest node added terminal)

    - This version can also handle some console commands after the OriginalNodeWorker run() method is concluded. The survey command is the only command
    that will currently return an dynamic data (DHT).
    - To add additional nodes, type add in the original node terminal, then launch a new terminal and run > java McastDHT

*/


import java.io.BufferedReader;                          //pull in libraries for program
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;


class OriginalNodeWorker extends Thread{                            //worker class to handle the UDP connections and DHT additions
    public static int port;                                         // local variable definitions port, nodeID, receivedMessage
    public static int nodeID;
    public static String receivedMessage = null;

    byte[] addToDHTbuffer = new byte[1024];                         //local instantiation of DHTbuffer
    DatagramSocket comPortOneSocket;                                //local variable definitions of socket and packet and DHT
    DatagramPacket comPortOnePacket;

    Hashtable<Integer, Integer> DHT;

    ArrayList<Integer> ports = new ArrayList<>();               // define an arrayList to hold our ports to be used in the multicast method


    public OriginalNodeWorker(int port, int nodeID, Hashtable<Integer, Integer> DHT) {              //constructor to set local variables passed in from main
        this.port = port;
        this.nodeID = nodeID;
        this.DHT = DHT;
    }

    public void run() {                                                                                 // run method executed from main on OriginalNodeWorker.start()

        try {
            comPortOneSocket = new DatagramSocket(40001);                                           //first node opens port at 40001
            System.out.println("Node: "+nodeID + ", predecessor: "+ nodeID+ ", successor: "+nodeID);      // if successful, prints this message to console

            int originalPort = port+nodeID;                                                               // set the original port to 40001 and add that to the DHT with nodeID=1
            //add nodeID = 1 to the DHT
            DHT.put(nodeID, originalPort);
            ports.add(originalPort);

            while(receivedMessage==null) {                                                                  //block waiting for new nodes to come online.
                comPortOnePacket = new DatagramPacket(addToDHTbuffer, addToDHTbuffer.length);
                comPortOneSocket.receive(comPortOnePacket);                                                         //receive messages with UDP
                receivedMessage = new String(comPortOnePacket.getData(),0,comPortOnePacket.getLength());
            }

            //add the port from new nodeID to the arraylist
            ports.add(comPortOnePacket.getPort());

            //add the new nodeID to the DHT at the newPort
            DHT.put(Integer.parseInt(receivedMessage), comPortOnePacket.getPort());

            //call multicast method block here once new node is added to the DHT
            MultiCastDHT(comPortOneSocket, DHT, ports);

            receivedMessage = null;

        } catch (IOException e) {                                                                               // if nodeID !=1. enter this block
            nodeID = (int) (Math.random() *999+2);                                                              //generate random int between 2-999
            int newPort = nodeID+port;                                                                          // set new port to 40000+ nodeID
            addToDHTbuffer = (String.valueOf(nodeID)).getBytes();

            try {                                                                                               //create a new UDP socket at newPort
                comPortOneSocket = new DatagramSocket(newPort);
                comPortOnePacket = new DatagramPacket(addToDHTbuffer, addToDHTbuffer.length, InetAddress.getByName("localhost"),40001 );       // send packet with new NodeID to original node to be added to DHT
                comPortOneSocket.send(comPortOnePacket);

                // add a receive multicast block here

                byte[] receiveDHTbuffer = new byte[1024];

                while(receivedMessage==null) {                                                                          //block waiting for new nodes to come online.
                    comPortOnePacket = new DatagramPacket(receiveDHTbuffer, receiveDHTbuffer.length);
                    comPortOneSocket.receive(comPortOnePacket);                                                         //receive messages with UDP
                    receivedMessage = new String(comPortOnePacket.getData(),0,comPortOnePacket.getLength());
                }
            } catch (IOException e1) {
                // should we launch a new "non-original Node Worker here instead of handling everything in try/catch blocks (current setup doesn't work for 3 terminals, only 2)
            }

            System.out.println("Node: "+nodeID + ", predecessor: "+ 1 +", successor: "+nodeID);                     //need to do multicast here and call DHT.get(xxx) for predecessor and successor

            System.out.println(receivedMessage);            //this is the multicasted DHT in string form. Don't know how to add it back to a HashTable format. So survey won't work on non original node terminals

            //next refactor MUST recreate DHT with the multicast "received message" string above === our multicasted DHT

            receivedMessage = null;                                                                                 //reset and wait for more nodes to come online
        }

        comPortOneSocket.close();                                               //close the current connection
    }

    private void MultiCastDHT(DatagramSocket comPortOneSocket, Hashtable<Integer, Integer> dht, ArrayList<Integer> ports) throws IOException {

        byte[] mcastBuffer = new byte[1024];

        String dhtString = DHT.toString();
        mcastBuffer =  dhtString.getBytes();


        // send the new DHT to all the instances listening at their respective ports
        for(int i = 0; i< this.ports.size(); i++){
            //System.out.println(ports.get(i));
            DatagramPacket mcastPacket = new DatagramPacket(mcastBuffer, mcastBuffer.length,InetAddress.getByName("localhost"),ports.get(i) );
            comPortOneSocket.send(mcastPacket);
        }
    }
}


public class McastDHT {

    public static void main(String args[]) throws IOException, InterruptedException {                   // McastDHT main
        int port = 40000;                                                                               // set base port to 40000
        int nodeID = 1;                                                                                 // set first nodeID =1
        String input;

        Hashtable<Integer, Integer> DHT = new Hashtable<>();                                            //create our HashTable object DHT here

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));                       //create BufferedReader object in here. will wait for console commands after OriginalNodeWorker completes its work

        new OriginalNodeWorker(port, nodeID, DHT).start();                                               // launches a new worker object with the incoming connection

        Thread.sleep(1000);

        System.out.println("Available Console Commands: Status, Ping, Loopring, Survey, File");         // print available commands to the console

        // wait for our console commands here
        while(true) {
            input = in.readLine();

            if(input.toUpperCase().equals(("STATUS"))){
                System.out.println("Status Console command not implemented yet");
            }

            else if(input.toUpperCase().equals(("PING"))){
                System.out.println("Ping Console command not implemented yet");
            }

            else if(input.toUpperCase().equals(("LOOPPING"))){
                System.out.println("LoopRing Console command not implemented yet");
            }

            else if(input.toUpperCase().equals(("SURVEY"))){
                System.out.println("Full DHT = "+ DHT);                 // this only works for the original node = 1 terminal. Need to put multicasted DHT string back into DHT as a hashtable (ran out of time)
            }

            else if(input.toUpperCase().equals(("FILE"))){
                System.out.println("File Console command not implemented yet");
            }

            //custom console input to add new node to the DHT
            else if(input.toUpperCase().equals("ADD")){
                new OriginalNodeWorker(port,nodeID,DHT).start();
            }

            /*
            else if(input.toUpperCase().equals("MULTICAST")){

            }
            */

        }
    }
}