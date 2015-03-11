/**
 * Author: Yeap Hooi Tong
 * Matric: A0111736M
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

class FileReceiver {

    public DatagramSocket socket;
    public DatagramPacket packet;
    private static final int MAX_SIZE = 1000;

    public static void main(String[] args) {

        // check if the number of command line argument is 1
        if (args.length != 1) {
            System.out.println("Usage: java FileReceiver port");
            System.exit(1);
        }

        new FileReceiver(args[0]);
    }

    public FileReceiver(String localPort) {
        try {
            socket = new DatagramSocket(Integer.parseInt(localPort));
            byte[] receiveData = new byte[MAX_SIZE];
            packet = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(packet);
            String[] header = new String(packet.getData()).split(";");
            String fileDest = header[0].trim();
            int fileLength = Integer.parseInt(header[1].trim());
            int writtenBytes = 0;
            File destination = new File(fileDest);
            FileOutputStream fos = new FileOutputStream(destination);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            while (writtenBytes < fileLength) {
                int byteToWrite = fileLength - writtenBytes < 1000 ? fileLength
                                                                     - writtenBytes
                                                                   : 1000;
                packet = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(packet);
                bos.write(packet.getData(), 0, byteToWrite);
                writtenBytes += byteToWrite;
            }
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
