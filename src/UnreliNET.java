// Author: Hong Hande honghand@comp.nus.edu.sg

// [zlf]: Place this program in the same directory as your sender
// and receiver programs. Command to compile all three: javac *.java

import java.net.*;
import java.util.*;

public class UnreliNET {
    
    static int buf_size = 1500;
    int returnPort_sk1;
    private DatagramSocket sk1, sk2;
    int port_sk1, port_sk2;
    
    // corruption/loss rate
    static float data_loss_pct;
    static float ack_loss_pct;
    static float data_corrupt_pct;
    static float ack_corrupt_pct;
    
    // define thread which is used to handle one-direction of communication
    public class UnreliThreadProcessData extends Thread {
        
        private Random rnd = new Random();
        private Random rnd_byte = new Random();
        int corruptionCounter = 0;
        int dropCounter = 0;
        
        public void run() {
            try {
                byte[] in_data = new byte[buf_size];
                InetAddress dst_addr = InetAddress.getByName("127.0.0.1");
                DatagramPacket in_pkt = new DatagramPacket(in_data, in_data.length);
                
                while (true) {
                    // read data from the incoming socket
                    sk1.receive(in_pkt);
                    returnPort_sk1 = in_pkt.getPort();
                    
                    // check the length of the packet
                    if (in_pkt.getLength() > 1000) {
                        System.err.println("Error: packet length is more than 1000 bytes");
                        System.exit(-1);
                    }
                    
                    // decide if to drop the packet or not
                    if (rnd.nextFloat() <= data_loss_pct) {
                        dropCounter++;
                        System.out.println(dropCounter + " Packet dropped");
                        continue;
                    }
                    
                    // decide if to corrupt the packet or not
                    if (rnd.nextFloat() <= data_corrupt_pct) {
                        for (int i = 0; i < in_pkt.getLength(); ++i)
                            if (rnd_byte.nextFloat() <= 0.3)  //decide if to corrupt a byte
                                in_data[i] = (byte) ((in_data[i] + 1) % 10);
                        corruptionCounter++;
                        System.out.println(corruptionCounter + " Packet corrupted");
                    }
                    
                    // write data to the outgoing socket
                    DatagramPacket out_pkt =
                        new DatagramPacket(in_data, in_pkt.getLength(), dst_addr, port_sk2);
                    sk2.send(out_pkt);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
    
    public class UnreliThreadProcessAck extends Thread {
        
        private Random rnd = new Random();
        private Random rnd_byte = new Random();
        int ackcorruptionCounter = 0;
        int ackdropCounter = 0;
        
        public void run() {
            try {
                byte[] in_data = new byte[buf_size];
                InetAddress dst_addr = InetAddress.getByName("127.0.0.1");
                DatagramPacket in_pkt = new DatagramPacket(in_data, in_data.length);
                
                while (true) {
                    // read data from the incoming socket
                    sk2.receive(in_pkt);
                    
                    // check the length of the packet
                    if (in_pkt.getLength() > 1000) {
                        System.err.println("Error: packet length is more than 1000 bytes");
                        System.exit(-1);
                    }
                    
                    // decide if to drop the packet or not
                    if (rnd.nextFloat() <= ack_loss_pct) {
                        ackdropCounter++;
                        System.out.println(ackdropCounter + " ACK/NAK dropped");
                        continue;
                    }
                    
                    // decide if to corrupt the packet or not
                    if (rnd.nextFloat() <= ack_corrupt_pct) {
                        for (int i = 0; i < in_pkt.getLength(); ++i)
                            if (rnd_byte.nextFloat() <= 0.3) //decide if to corrupt a byte
                                in_data[i] = (byte) ((in_data[i] + 1) % 10);
                        ackcorruptionCounter++;
                        System.out.println(ackcorruptionCounter + " ACK/NAK corrupted");
                    }
                    
                    // write data to the outgoing socket
                    DatagramPacket out_pkt =
                        new DatagramPacket(in_data, in_pkt.getLength(), dst_addr, returnPort_sk1);
                    sk1.send(out_pkt);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
    
    public UnreliNET(float data_corrupt_rate, float ack_corrupt_rate,
                     float data_loss_rate, float ack_loss_rate, int unreliNetPort, int rcvPort) {
        
        System.out.println("unreliNetPort = " + unreliNetPort
                               + "\nrcvPort = " + rcvPort 
                               + "\ndata corruption rate = " + data_corrupt_rate
                               + "\nack/nak corruption rate = " + ack_corrupt_rate
                               + "\ndata loss rate = " + data_loss_rate
                               + "\nack/nak loss rate = " + ack_loss_rate);
        
        try {
            // Create socket sk1 and sk2
            
            data_corrupt_pct = data_corrupt_rate;
            ack_corrupt_pct = ack_corrupt_rate;
            data_loss_pct = data_loss_rate;
            ack_loss_pct = ack_loss_rate;
            
            sk1 = new DatagramSocket(unreliNetPort);
            sk2 = new DatagramSocket();
            
            port_sk1 = unreliNetPort;
            port_sk2 = rcvPort;
            
            // create threads to process sender's incoming data
            UnreliThreadProcessData th1 = new UnreliThreadProcessData();
            th1.start();
            
            // create threads to process receiver's incoming data
            UnreliThreadProcessAck th2 = new UnreliThreadProcessAck();
            th2.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    public static void main(String[] args) {
        // parse parameters
        if (args.length != 6) {
            System.err.println("Usage: java UnreliNET <P_DATA_CORRUPT> <P_ACK_CORRUPT> " +
                               "<P_DATA_LOSS> <P_ACK_LOSS> <unreliNetPort> <rcvPort>");
            System.exit(-1);
        } else {
            new UnreliNET(Float.parseFloat(args[0]), Float.parseFloat(args[1]),
                          Float.parseFloat(args[2]), Float.parseFloat(args[3]),
                          Integer.parseInt(args[4]), Integer.parseInt(args[5]) );
        }
    }
}
