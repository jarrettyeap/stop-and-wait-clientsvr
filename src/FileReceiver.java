/**
 * Author: Yeap Hooi Tong
 * Matric: A0111736M
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

class FileReceiver {

  public DatagramSocket socket;
  public DatagramPacket packet;

  private static final int FILE_PACKET_SIZE = 988;
  private static final int HEADER_SIZE = 12;
  private static final int PACKET_SIZE = FILE_PACKET_SIZE + HEADER_SIZE;
  private static final int ACK_SIZE = 8;

  private int noOfSequences;

  private FileOutputStream fos;
  private BufferedOutputStream bos;

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
      /* Initialise Socket and Packet information */
      socket = new DatagramSocket(Integer.parseInt(localPort));
      byte[] receiveData = new byte[PACKET_SIZE];
      packet = new DatagramPacket(receiveData, receiveData.length);

      int currentPacket = -1;
      boolean isCompleted = false;

      while (!isCompleted) {
        /* Get file information */
        socket.receive(packet);
        byte[] fileInfo = packet.getData();
        long receivedCS = ByteBuffer.wrap(fileInfo, 0, 8).getLong();
        int seqNo = ByteBuffer.wrap(fileInfo, 8, 4).getInt();
        System.out.println("Received ACK " + seqNo);
        byte[] content = new byte[packet.getLength() - HEADER_SIZE];
        System.arraycopy(fileInfo, HEADER_SIZE, content, 0, content.length);

      /* Check the checksum & possibility for duplicate packets */
        if (getCRC(content) == receivedCS && seqNo == currentPacket) {
          if (currentPacket == -1) {
            String[] fileContent = new String(content).split(";");
            File fileDest = new File(fileContent[0].trim());
            noOfSequences = Integer.parseInt(fileContent[1].trim());
            fos = new FileOutputStream(fileDest);
            bos = new BufferedOutputStream(fos);
            currentPacket++;
          } else {
            bos.write(content, 0, content.length);
            currentPacket++;
            if (currentPacket == noOfSequences) {
              isCompleted = true;
            }
          }

          /* Return ack */
          InetAddress senderAddress = packet.getAddress();
          int senderPort = packet.getPort();
          int ackNo = currentPacket - 1;
          int purityBit = ackNo % 2 == 0 ? 0 : 1; /* even parity bit */
          /* Put header into buffer */
          ByteBuffer buffer = ByteBuffer.allocate(ACK_SIZE).putInt(ackNo).putInt(purityBit);
          DatagramPacket ackPkt = new DatagramPacket(buffer.array(), buffer.array().length,
                                                     senderAddress, senderPort);
          socket.send(ackPkt);
          System.out.println("Sent ACK");
        } else if (seqNo == currentPacket - 1) {
           /* Return ack */
          InetAddress senderAddress = packet.getAddress();
          int senderPort = packet.getPort();
          int ackNo = currentPacket - 1;
          int purityBit = ackNo % 2 == 0 ? 0 : 1; /* even parity bit */
          /* Put header into buffer */
          ByteBuffer buffer = ByteBuffer.allocate(ACK_SIZE).putInt(ackNo).putInt(purityBit);
          DatagramPacket ackPkt = new DatagramPacket(buffer.array(), buffer.array().length,
                                                     senderAddress, senderPort);
          socket.send(ackPkt);
          System.out.println("Sent ACK");
        }
      }
      bos.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /* Method used to return CRC of a byte array */
  private long getCRC(byte[] byteArray) {
    Checksum cs = new CRC32();
    cs.update(byteArray, 0, byteArray.length);
    return cs.getValue();
  }
}
