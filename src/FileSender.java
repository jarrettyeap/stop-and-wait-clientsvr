/**
 * Author: Yeap Hooi Tong
 * Matric: A0111736M
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

class FileSender {

  public DatagramSocket socket;
  public DatagramPacket packet;

  private static final int FILE_PACKET_SIZE = 988;
  private static final int HEADER_SIZE = 12;
  private static final int PACKET_SIZE = FILE_PACKET_SIZE + HEADER_SIZE;
  private static final int ACK_SIZE = 8;

  private byte[][] packetStorage;
  private int noOfSequences;

  public static void main(String[] args) {
    // check if the number of command line argument is 4
    if (args.length != 3) {
      System.out.println("Usage: java FileSender <path/filename> "
                         + "<unreliNetPort> <rcvFileName>");
      System.exit(1);
    }

    new FileSender(args[0], args[1], args[2]);
  }

  public FileSender(String fileToOpen, String port, String rcvFileName) {
    try {

      /* Setup socket and receiver's information */
      int portNum = Integer.parseInt(port);
      socket = new DatagramSocket();
      socket.setSoTimeout(10);
      InetAddress IPAddress = InetAddress.getByName("localhost");

      /* Pre-process the file into a 2-D byte array for easy reference */
      splitFileIntoByte(fileToOpen);
      System.out.println(fileToOpen);
      /* Generate CRC for fileName and sequence numbers */
      String fileInfo = rcvFileName + ";" + noOfSequences;
      byte[] fileArray = fileInfo.getBytes();

      /* Put header into buffer */
      ByteBuffer buffer = ByteBuffer.allocate(fileArray.length + HEADER_SIZE).
          putLong(getCRC(fileArray)).putInt(-1);

      /* Put content into buffer */
      buffer.put(fileArray);

      /* Send file information packet */
      byte[] packetArray = buffer.array();
      packet = new DatagramPacket(packetArray, packetArray.length, IPAddress, portNum);
      socket.send(packet);

      /* rdt version 3 algorithm */
      boolean isCompleted = false;
      int currentPacket = -1;

      System.out.println("Sending " + currentPacket + " " + getCRC(fileArray));

      while (!isCompleted) {
        /* Receive ACK from previous sequence*/
        byte[] ackData = new byte[ACK_SIZE];
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
        boolean timeOut = false;
        try {
          socket.receive(ackPacket);
          System.out.println("Received ACK");
        } catch (SocketTimeoutException e) {
          timeOut = true;
        }

        /* Logic process for acknowledgement */
        boolean isAck = false;
        if (timeOut) {
          isAck = false;
        } else {
          isAck = isACK(ackPacket, currentPacket);
        }

         /* If its the correct ack packet */
        if (isAck) {
          currentPacket++;
          if (currentPacket == noOfSequences) { /* If finish sending, exit loop */
            isCompleted = true;
          } else {
            /* Send next packet */
            /* Put header into buffer */
            ByteBuffer packetBuffer = ByteBuffer.allocate(packetStorage[currentPacket].length
                                                          + HEADER_SIZE);
            packetBuffer.putLong(getCRC(packetStorage[currentPacket]));
            packetBuffer.putInt(currentPacket);
            packetBuffer.put(packetStorage[currentPacket]);
            packet.setData(packetBuffer.array());
            socket.send(packet);
            System.out.println("Sending " + currentPacket);
            byte[] content = new byte[packet.getLength() - HEADER_SIZE];
            System.arraycopy(packet.getData(), HEADER_SIZE, content, 0, content.length);
          }
        } else {
          socket.send(packet);
          System.out.println("Sending " + currentPacket);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private boolean isACK(DatagramPacket p, int packetNumber) {
    int seqNumber = ByteBuffer.wrap(p.getData(), 0, 4).getInt();
    int purityBit = ByteBuffer.wrap(p.getData(), 4, 4).getInt();

      /* Even parity bit scheme */
    if (((seqNumber + purityBit) % 2) == 0) {
      return seqNumber == packetNumber;
    } else {
      return false;
    }
  }

  private boolean splitFileIntoByte(String fileName) {
    try {
      File fileToTransfer = new File(fileName);
      int iterator = 0;
      noOfSequences = (int) Math.ceil(fileToTransfer.length() / (double) FILE_PACKET_SIZE);
      packetStorage = new byte[noOfSequences][];

      FileInputStream fis = new FileInputStream(fileToTransfer);
      BufferedInputStream bis = new BufferedInputStream(fis);

      long byteToRead = fileToTransfer.length();
      long readBytes = 0;
      while (bis.available() > 0) {
        if (byteToRead - readBytes >= FILE_PACKET_SIZE) {
          packetStorage[iterator] = new byte[FILE_PACKET_SIZE];
          bis.read(packetStorage[iterator++], 0, FILE_PACKET_SIZE);
          readBytes += FILE_PACKET_SIZE;
        } else {
          packetStorage[iterator] = new byte[(int) (byteToRead - readBytes)];
          bis.read(packetStorage[iterator++], 0, (int) (byteToRead - readBytes));
          readBytes += (int) (byteToRead - readBytes);
        }
      }

      bis.close();
      return true;
    } catch (Exception e) {
      System.out.println("Error splitting file into byte arrays");
      e.printStackTrace();
      return false;
    }
  }

  /* Method used to return CRC of a byte array */
  private long getCRC(byte[] byteArray) {
    Checksum cs = new CRC32();
    cs.update(byteArray, 0, byteArray.length);
    return cs.getValue();
  }
}