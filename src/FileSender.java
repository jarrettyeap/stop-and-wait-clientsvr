/**
 * Author: Yeap Hooi Tong
 * Matric: A0111736M
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class FileSender {

    public DatagramSocket socket;
    public DatagramPacket packet;
    private static final int MAX_SIZE = 1000;

    public static void main(String[] args) {
        // check if the number of command line argument is 4
        if (args.length != 4) {
            System.out.println("Usage: java FileSender <path/filename> "
                               + "<rcvHostName> <rcvPort> <rcvFileName>");
            System.exit(1);
        }

        new FileSender(args[0], args[1], args[2], args[3]);
    }

    public FileSender(String fileToOpen, String host, String port,
                      String rcvFileName) {
        try {
            int portNum = Integer.parseInt(port);
            socket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName(host);
            File fileToTransfer = new File(fileToOpen);

            String header = rcvFileName + ";" + fileToTransfer.length();
            byte[] fileArray = header.getBytes();
            packet = new DatagramPacket(fileArray, fileArray.length, IPAddress,
                                        portNum);
            socket.send(packet);

            FileInputStream fis = new FileInputStream(fileToTransfer);
            BufferedInputStream bis = new BufferedInputStream(fis);
            byte[] byteArray = new byte[MAX_SIZE];
            while (bis.available() > 0) {
                bis.read(byteArray, 0, MAX_SIZE);
                packet = new DatagramPacket(byteArray, byteArray.length,
                                            IPAddress, portNum);
                socket.send(packet);
                Thread.sleep(10);
            }
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
