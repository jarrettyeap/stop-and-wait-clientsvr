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
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

class FileSender {

  private static final int FILE_PACKET_SIZE = 992;
  private static final int HEADER_SIZE = 8;
  private static final int ACK_SIZE = 8;

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
    long startTime = System.currentTimeMillis();
    try {
      File fileToTransfer = new File(fileToOpen);
      long byteToRead = fileToTransfer.length();
      int noOfSequences = (int) Math.ceil(byteToRead / (double) FILE_PACKET_SIZE);

      FileInputStream fis = new FileInputStream(fileToTransfer);
      BufferedInputStream bis = new BufferedInputStream(fis);

      /* Setup socket and receiver's information */
      int portNum = Integer.parseInt(port);
      DatagramSocket socket = new DatagramSocket();
      socket.setSoTimeout(1);
      InetAddress IPAddress = InetAddress.getByName("localhost");
      byte[] packetBacker = new byte[FILE_PACKET_SIZE + HEADER_SIZE];
      byte[] ackData = new byte[ACK_SIZE];
      byte[] seqBuffer = new byte[4];
      DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
      int currentPacket = -1;

      /* Generate CRC for fileName and sequence numbers */
      String fileInfo = rcvFileName + ";" + noOfSequences;
      byte[] fileArray = fileInfo.getBytes();
      ByteBuffer buffer = ByteBuffer.wrap(packetBacker);
      buffer.put(getCRC(fileArray, 0, fileArray.length)).putInt(-1).put(fileArray);
      DatagramPacket packet = new DatagramPacket(packetBacker, fileArray.length + HEADER_SIZE,
                                                 IPAddress, portNum);

      while (true) {
        /* Send Packet */
        socket.send(packet);

        /* Receive Packet */
        try {
          socket.receive(ackPacket);
          /* If its the correct ack packet */
          if (isACK(ackPacket, currentPacket)) {
            currentPacket++;
            if (currentPacket == noOfSequences) { /* If last ack packet is correct, exit */
              long stopTime = System.currentTimeMillis();
              long elapsedTime = stopTime - startTime;
              System.out.println(elapsedTime);
              bis.close();
              System.exit(0);
            } else {
              /* Prepare payload and send next packet */
              int byteRead = bis.read(packetBacker, HEADER_SIZE, FILE_PACKET_SIZE);

              if (byteRead == -1) {
                byteRead = (int) byteToRead;
              }

              System.arraycopy(getCRC(packetBacker, HEADER_SIZE, byteRead), 0, packetBacker, 0, 4);
              byte[] seqByte = ByteBuffer.wrap(seqBuffer).putInt(currentPacket).array();
              System.arraycopy(seqByte, 0, packetBacker, 4, 4);
              packet.setData(packetBacker, 0, byteRead + HEADER_SIZE);
            }
          }
        } catch (SocketTimeoutException e) {
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private boolean isACK(DatagramPacket p, int packetNumber) {
    int seqNumber = ByteBuffer.wrap(p.getData(), 0, 4).getInt();
    byte[] receivedCS = Arrays.copyOfRange(p.getData(), 4, 8);
    return Arrays.equals(getCRC(p.getData(), 0, 4), receivedCS) && seqNumber == packetNumber;
  }

  /* Method used to return CRC of a byte array */
  private byte[] getCRC(byte[] byteArray, int offset, int length) {
    Checksum cs = new CRC32();
    cs.update(byteArray, offset, length);
    return longToBytes(cs.getValue());
  }

  private static ByteBuffer tempBuffer = ByteBuffer.allocate(8);

  public static byte[] longToBytes(long x) {
    tempBuffer.putLong(0, x);
    return Arrays.copyOfRange(tempBuffer.array(), 4, 8);
  }
}
