/**
 * Author: Yeap Hooi Tong
 * Matric: A0111736M
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

class FileReceiver {

  public DatagramSocket socket;
  public DatagramPacket packet;

  private static final int FILE_PACKET_SIZE = 992;
  private static final int HEADER_SIZE = 8;
  private static final int PACKET_SIZE = FILE_PACKET_SIZE + HEADER_SIZE;
  private static final int ACK_SIZE = 8;

  private static InetAddress senderAddress;
  private static int senderPort;

  private int noOfSequences;
  private byte[] ackBacker;

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
      socket.setSoTimeout(1);
      ackBacker = new byte[ACK_SIZE];
      byte[] receiveData = new byte[PACKET_SIZE];
      packet = new DatagramPacket(receiveData, receiveData.length);
      int currentPacket = -1;
      byte[] fileInfo, receivedCS;
      int packetLength, seqNo;

      while (true) {
        /* Get file information */
        try {
          socket.receive(packet);
          fileInfo = packet.getData();
          packetLength = packet.getLength();
          receivedCS = Arrays.copyOfRange(fileInfo, 0, 4);
          seqNo = ByteBuffer.wrap(fileInfo, 4, 4).getInt();

        /* Check the checksum & possibility for duplicate packets */
          if (Arrays.equals(getCRC(fileInfo, HEADER_SIZE, packetLength - HEADER_SIZE), receivedCS)
              && seqNo == currentPacket) {
            if (currentPacket != -1) {
              bos.write(fileInfo, HEADER_SIZE, packetLength - HEADER_SIZE);
            } else {
              senderAddress = packet.getAddress();
              senderPort = packet.getPort();
              byte[] content = new byte[packetLength - HEADER_SIZE];
              System.arraycopy(fileInfo, HEADER_SIZE, content, 0, content.length);
              String[] fileContent = new String(content).split(";");
              File fileDest = new File(fileContent[0].trim());
              noOfSequences = Integer.parseInt(fileContent[1].trim());
              fos = new FileOutputStream(fileDest);
              bos = new BufferedOutputStream(fos);
            }

            currentPacket++;
            sendAck(currentPacket);
            if (currentPacket == noOfSequences) {
              break;
            }
          }
        } catch (SocketTimeoutException e) {
          if (currentPacket != -1) {
            sendAck(currentPacket);
          }
        }
      }
      bos.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void sendAck(int currentPacket) throws IOException {
    /* Return ack */
    int ackNo = currentPacket - 1;

    /* Put header into buffer */
    ByteBuffer buffer = ByteBuffer.wrap(ackBacker).putInt(ackNo).put(getCRC(ackBacker, 0, 4));
    DatagramPacket ackPkt = new DatagramPacket(buffer.array(), buffer.array().length,
                                               senderAddress, senderPort);
    socket.send(ackPkt);
  }

  /* Method used to return CRC of a byte array */
  private byte[] getCRC(byte[] byteArray, int offset, int length) {
    Checksum cs = new CRC32();
    cs.update(byteArray, offset, length);
    return longToBytes(cs.getValue());
  }

  private static ByteBuffer tempBuffer = ByteBuffer.allocate(HEADER_SIZE);

  public static byte[] longToBytes(long x) {
    tempBuffer.putLong(0, x);
    return Arrays.copyOfRange(tempBuffer.array(), 4, 8);
  }
}
