package de.kumakyoo.opa;

import java.util.Arrays;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Tools
{
    static final byte[] OMA_SIGNATUR = {0x4f,0x4d,0x41};
    static final byte[] O5M_SIGNATUR = {(byte)0xff,(byte)0xe0,0x04,0x6f,0x35,0x6d,0x32};

    static long getFileSize(String filename) throws IOException
    {
        return Files.size(Paths.get(filename));
    }

    static long memavail()
    {
        return Runtime.getRuntime().maxMemory()-(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
    }

    static String humanReadable(long l)
    {
        if (l<1000) return l+"";

        int digits = (""+l).length();
        return String.format("%."+(2-(digits-1)%3)+"f",l/Math.pow(10.0,3*((digits-1)/3)))+" KMGTE".charAt((digits-1)/3);
    }

    static DataInputStream getInStream(String filename) throws IOException
    {
        return new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
    }

    static DataOutputStream getOutStream(String filename) throws IOException
    {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
    }

    static boolean isOma(String filename) throws IOException
    {
        DataInputStream in = Tools.getInStream(filename);
        byte[] data = new byte[3];
        in.readFully(data);
        in.close();

        return Arrays.compare(data,OMA_SIGNATUR)==0;
    }

    static boolean isO5M(String filename) throws IOException
    {
        DataInputStream in = Tools.getInStream(filename);
        byte[] data = new byte[7];
        in.readFully(data);
        in.close();

        return Arrays.compare(data,O5M_SIGNATUR)==0;
    }
}
