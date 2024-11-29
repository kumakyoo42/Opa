package de.kumakyoo.opa;

import java.io.*;
import java.nio.channels.*;
import java.util.*;

public class MyDataOutputStream extends DataOutputStream
{
    private FileOutputStream fos;
    private BufferedOutputStream bos;
    private FileChannel fc;

    private int lastx;
    private int lasty;

    public static MyDataOutputStream getMyDataOutputStream(String filename) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(filename);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        MyDataOutputStream s = new MyDataOutputStream(bos);

        s.fos = fos;
        s.bos = bos;
        s.fc = fos.getChannel();
        return s;
    }

    public MyDataOutputStream(OutputStream s)
    {
        super(s);
        resetDelta();
    }

    public MyDataOutputStream(OutputStream s, MyDataOutputStream orig)
    {
        super(s);
        fos = orig.fos;
        bos = orig.bos;
        fc = orig.fc;
        resetDelta();
    }

    //////////////////////////////////////////////////////////////////

    public void writeSmallInt(int value) throws IOException
    {
        if (value<255)
            writeByte(value);
        else
        {
            writeByte(255);
            if (value<65535)
                writeShort(value);
            else
            {
                writeShort(65535);
                writeInt(value);
            }
        }
    }

    public void writeString(String s) throws IOException
    {
        byte[] bytes = s.getBytes("UTF-8");
        writeSmallInt(bytes.length);
        write(bytes,0,bytes.length);
    }

    public void resetDelta()
    {
        lastx = lasty = 0;
    }

    public void writeDeltaX(int val) throws IOException
    {
        lastx = delta(lastx,val);
    }

    public void writeDeltaY(int val) throws IOException
    {
        lasty = delta(lasty,val);
    }

    public int delta(int last, int val) throws IOException
    {
        int delta = val-last;
        if (delta>=-32767 && delta<=32767)
            writeShort(delta);
        else
        {
            writeShort(-32768);
            writeInt(val);
        }

        return val;
    }

    //////////////////////////////////////////////////////////////////

    public long getPosition() throws IOException
    {
        bos.flush();
        return fc.position();
    }

    public void setPosition(long pos) throws IOException
    {
        bos.flush();
        fc.position(pos);
    }
}
