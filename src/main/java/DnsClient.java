import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DnsClient {

    private static final String DNS_SERVER_ADDRESS = "81.180.223.1";
    private static final int DNS_SERVER_PORT = 53;

    public String getIpAdress(String domeniu) throws IOException {
        InetAddress ipAddress = InetAddress.getByName(DNS_SERVER_ADDRESS);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Construieste cerere DNS

        //Identificator: Camp pe 16 biti (2 octeti)
        dos.writeShort(0xABCD);

        // Query Flags
        dos.writeShort(0x0000);

        // Question Count
        dos.writeShort(0x0001);

        // Answer Record Count
        dos.writeShort(0x0000);

        // Authority Record Count
        dos.writeShort(0x0000);

        // Additional Record Count`
        dos.writeShort(0x0000);

        String[] labels = domeniu.split("\\.");
        //  System.out.println("Cele " + labels.length + " componente ale domeniului " + domeniu + ": ");

        for (int i = 0; i < labels.length; i++) {
            //System.out.println(labels[i]);
            byte[] domainBytes = labels[i].getBytes("UTF-8");
            dos.writeByte(domainBytes.length);
            dos.write(domainBytes);
        }

        // Limita care specifica terminarea partilor domeniului
        dos.writeByte(0x00);

        // QType
        dos.writeShort(0x0001);

        // QClass
        dos.writeShort(0x0001);

        byte[] dnsFrame = baos.toByteArray();


//        System.out.println("Trimite: " + dnsFrame.length + " octeti");
//        for (int i = 0; i < dnsFrame.length; i++) {
//            System.out.print("0x" + String.format("%x", dnsFrame[i]) + " ");
//        }

        // Trimite cererea
        DatagramSocket socket = new DatagramSocket();
        DatagramPacket dnsReqPacket = new DatagramPacket(dnsFrame, dnsFrame.length, ipAddress, DNS_SERVER_PORT);
        socket.send(dnsReqPacket);

        // Asteapta raspunsul intr-un buffer de maxim 512 octeti
        byte[] buf = new byte[512];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        //System.out.println("\n\nS-au primit: " + packet.getLength() + " octeti");

//        for (int i = 0; i < packet.getLength(); i++) {
//            System.out.print(i + " = 0x" + String.format("%x", buf[i]) + " ");
//        }
//        System.out.println("\n");

        DataInputStream din = new DataInputStream(new ByteArrayInputStream(buf));

        byte[] a = new byte[12];
        din.read(a, 0, 12);
//        System.out.println("Transaction ID: 0x" + String.format("%x", din.readShort()));
//        System.out.println("Flags: 0x" + String.format("%x", din.readShort()));
//        System.out.println("Questions: 0x" + String.format("%x", din.readShort()));
//        System.out.println("Answers RRs: 0x" + String.format("%x", din.readShort()));
//        System.out.println("Authority RRs: 0x" + String.format("%x", din.readShort()));
//        System.out.println("Additional RRs: 0x" + String.format("%x", din.readShort()));

        int labelLength = 0;
        while ((labelLength = din.readByte()) > 0) {
            byte[] record = new byte[labelLength];

            for (int i = 0; i < labelLength; i++) {
                record[i] = din.readByte();
            }

            // System.out.println("Record: " + new String(record, "UTF-8"));
        }

        byte[] b = new byte[14];
        din.read(b, 0, 14);
//        System.out.println("Record Type: 0x" + String.format("%x", din.readShort()));
//        System.out.println("Class: 0x" + String.format("%x", din.readShort()));
//
//        System.out.println("Field: 0x" + String.format("%x", din.readShort()));
//        System.out.println("Type: 0x" + String.format("%x", din.readShort()));
//        System.out.println("Class: 0x" + String.format("%x", din.readShort()));
//        System.out.println("TTL: 0x" + String.format("%x", din.readInt()));

        short addrLen = din.readShort();
        //System.out.println("Len: 0x" + String.format("%x", addrLen));

        StringBuilder adress = new StringBuilder();
        //System.out.print("Address: ");
        for (int i = 0; i < addrLen - 1; i++) {
            // System.out.print("" + String.format("%d", (din.readByte() & 0xFF)) + ".");
            adress.append("" + String.format("%d", (din.readByte() & 0xFF)) + ".");
        }
        //  System.out.print("" + String.format("%d", (din.readByte() & 0xFF)));
        adress.append("" + String.format("%d", (din.readByte() & 0xFF)));
        return String.valueOf(adress);
    }
}
