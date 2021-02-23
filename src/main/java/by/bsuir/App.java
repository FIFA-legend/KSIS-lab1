package by.bsuir;

import org.apache.commons.codec.binary.Hex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Enumeration;

public class App {

    public static void main(String[] args) throws IOException, InterruptedException {
        Enumeration<NetworkInterface> localNetworks = NetworkInterface.getNetworkInterfaces();
        while (localNetworks.hasMoreElements()) {
            NetworkInterface netInterface = localNetworks.nextElement();
            byte[] mac = netInterface.getHardwareAddress();
            String fullName = netInterface.getDisplayName();
            if (mac != null) {
                System.out.println(addSlashesToString(Hex.encodeHex(mac, false)));
                System.out.println(fullName);
                System.out.println();
            }
        }

        InetAddress localHost = Inet4Address.getLocalHost();
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
        byte[] localAddressInBytes = localHost.getAddress();
        short[] address = convertByteAddressToShort(localAddressInBytes);
        short mask = networkInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength();
        short[] binaryMask = convertMaskToBinary(mask);

        pingAllDevices(binaryMask, address);

        String command = "arp -a";
        Process p = Runtime.getRuntime().exec(command);
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String regex = "^\\d+.\\d+.\\d+.\\d+$";
        while (true) {
            String line = br.readLine();
            if (line != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts[0].matches(regex) && !parts[0].endsWith(".255")) {
                    short[] someIP = convertIPtoShort(parts[0]);
                    if (isInOneNetwork(binaryMask, address, someIP)) {
                        System.out.println(parts[0] + "\t\t" + parts[1]);
                    }
                }
            } else {
                break;
            }
        }
    }

    private static String addSlashesToString(char[] str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length; i++) {
            sb.append(str[i]);
            if (i % 2 == 1 && i != str.length - 1) sb.append("-");
        }
        return sb.toString();
    }

    private static void pingAllDevices(short[] mask, short[] host) throws IOException, InterruptedException {
        int binaryMask = convertShortToInt(mask);
        int amountOfDevices = ~binaryMask;
        short[] temp = { (short) (mask[0] & host[0]), (short) (mask[1] & host[1]), (short) (mask[2] & host[2]), (short) (mask[3] & host[3]) };
        int subnetwork = convertShortToInt(temp);
        for (int i = 1; i < amountOfDevices; i++) {
            String str = Integer.toBinaryString(subnetwork + i);
            short[] array = convertStringToShort(str);
            Runtime.getRuntime().exec("ping " + array[0] + "." + array[1] + "." + array[2] + "." + array[3]);
        }
        Thread.sleep(10000);
    }

    private static int convertShortToInt(short[] array) {
        int result = 0;
        result += array[0];
        result <<= 8;
        result += array[1];
        result <<= 8;
        result += array[2];
        result <<= 8;
        result += array[3];
        return result;
    }

    private static boolean isInOneNetwork(short[] mask, short[] host, short[] someAddress) {
        for (int i = 0; i < mask.length; i++) {
            if ((host[i] & mask[i]) != (someAddress[i] & mask[i])) return false;
        }
        return true;
    }

    private static short[] convertIPtoShort(String ip) {
        short[] result = new short[4];
        String[] strings = ip.split("\\.");
        for (int i = 0; i < strings.length; i++) {
            result[i] = Short.parseShort(strings[i]);
        }
        return result;
    }

    private static short[] convertByteAddressToShort(byte[] byteAddress) {
        if (byteAddress.length != 4) return null;
        short[] shortAddress = new short[4];
        int mask = 255;
        for (int i = 0; i < byteAddress.length; i++) {
            shortAddress[i] = (short) (byteAddress[i] & mask);
        }
        return shortAddress;
    }

    private static short[] convertMaskToBinary(short maskSize) {
        String str = Integer.toBinaryString(createIntegerMask(maskSize));
        return convertStringToShort(str);
    }

    private static int createIntegerMask(short maskSize) {
        int mask = 0;
        for (int i = 0; i < 31; i++) {
            if (i < maskSize) {
                mask++;
            }
            mask <<= 1;
        }
        return mask;
    }

    private static short[] convertStringToShort(String binaryString) {
        short[] result = new short[4];
        result[0] = Short.valueOf(binaryString.substring(0, 8), 2);
        result[1] = Short.valueOf(binaryString.substring(8, 16), 2);
        result[2] = Short.valueOf(binaryString.substring(16, 24), 2);
        result[3] = Short.valueOf(binaryString.substring(24), 2);
        return result;
    }
}