package de.kumakyoo.opa;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class OpaToOma
{
    static final byte VERSION = 1;

    private String infile;
    private String outfile;

    private LineNumberReader in;
    private MyDataOutputStream out;

    private String line;
    private String token;
    private String nextline = null;

    private boolean zipped = false;
    private boolean id = false;
    private boolean version = false;
    private boolean timestamp = false;
    private boolean changeset = false;
    private boolean user = false;

    private int chunks;

    private byte[] type;
    private long[] start;
    private int[][] bb;

    private int chunk;

    private int lastx;
    private int lasty;

    public OpaToOma(String infile, String outfile)
    {
        this.infile = infile;
        this.outfile = outfile;
    }

    public void process() throws IOException
    {
        in = new LineNumberReader(new BufferedReader(new FileReader(infile)));
        out = MyDataOutputStream.getMyDataOutputStream(outfile);

        out.writeByte('O');
        out.writeByte('M');
        out.writeByte('A');

        nextLine("Version");

        out.writeByte(VERSION);

        readFeatures();
        readHeaderBB();
        out.writeLong(0);
        readHeaderEntries();
        readChunks();
        in.close();

        long tmp = out.getPosition();
        out.writeInt(chunks);
        for (int i=0;i<chunks;i++)
        {
            out.writeLong(start[i]);
            out.writeByte(type[i]);
            for (int j=0;j<4;j++)
                out.writeInt(bb[i]==null?Integer.MAX_VALUE:bb[i][j]);
        }

        out.setPosition(21);
        out.writeLong(tmp);

        out.close();
    }

    private void readFeatures() throws IOException
    {
        nextLine("Features");

        StringTokenizer t = new StringTokenizer(line,",");
        int count = t.countTokens();
        int features = 0;
        while (t.hasMoreTokens())
        {
            String token = t.nextToken().trim();
            switch (token)
            {
            case "id":
                id = true;
                features += 1;
                break;
            case "version":
                version = true;
                features += 2;
                break;
            case "timestamp":
                timestamp = true;
                features += 4;
                break;
            case "changeset":
                changeset = true;
                features += 8;
                break;
            case "user":
                user = true;
                features += 16;
                break;
            case "once":
                features += 32;
                break;
            case "-":
                if (count==1) break;
                error("'-' feature cannot be combined with other features");
            default:
                error("unknown feature '"+token+"'");
            }
        }
        out.writeByte(features);
    }

    private void readHeaderBB() throws IOException
    {
        nextLine("BoundingBox");

        int[] bb = readBB();
        for (int i=0;i<4;i++)
            out.writeInt(bb==null?Integer.MAX_VALUE:bb[i]);
    }

    private void readHeaderEntries() throws IOException
    {
        while (true)
        {
            nextLineUnfiltered();
            pushBack();

            int pos = line.indexOf(":");
            if (pos==-1) break;
            String token = line.substring(0,pos);

            if ("Compression".equals(token))
                readCompression();
            else if ("Types".equals(token))
                readTypeTable();
            else
                break;
        }
        out.writeByte(0);
    }

    private void readCompression() throws IOException
    {
        nextLine("Compression");

        out.writeByte('c');
        if ("DEFLATE".equals(line))
        {
            zipped = true;
            out.writeInt((int)out.getPosition()+12);
            out.writeString(line);
        }
        else if ("NONE".equals(line))
        {
            out.writeInt((int)out.getPosition()+9);
            out.writeString(line);
        }
        else
            error("Unknown compression method");
    }

    private void readTypeTable() throws IOException
    {
        out.writeByte('t'+(zipped?128:0));
        long pos = out.getPosition();
        out.writeInt(0);
        if (zipped)
            out.writeInt(0);

        MyDataOutputStream orig = out;
        BufferedOutputStream bos = null;
        DeflaterOutputStream dos = null;

        if (zipped)
        {
            dos = new DeflaterOutputStream(out, new Deflater(Deflater.BEST_COMPRESSION));
            bos = new BufferedOutputStream(dos);
            out = new MyDataOutputStream(bos,out);
        }

        nextLine("Types");

        int typeCount = 0;
        try
        {
            typeCount = Integer.parseInt(line);
        }
        catch (NumberFormatException e) { error("invalid number of types"); }
        out.writeSmallInt(typeCount);

        for (int i=0;i<typeCount;i++)
        {
            nextLine("Type");
            out.writeByte(line.charAt(0));

            nextLine("Keys");
            int keyCount = 0;
            try
            {
                keyCount = Integer.parseInt(line);
            }
            catch (NumberFormatException e) { error("invalid number of keys"); }
            out.writeSmallInt(keyCount);

            for (int j=0;j<keyCount;j++)
            {
                nextLine("Key");
                out.writeString(line);

                nextLine("Values");
                int valueCount = 0;
                try
                {
                    valueCount = Integer.parseInt(line);
                }
                catch (NumberFormatException e) { error("invalid number of values"); }
                out.writeSmallInt(valueCount);

                for (int k=0;k<valueCount;k++)
                {
                    nextLineUnfiltered();
                    out.writeString(line);
                }
            }
        }

        if (zipped)
        {
            bos.flush();
            dos.finish();
            out = orig;
        }

        long npos = out.getPosition();
        out.setPosition(pos);
        out.writeInt((int)npos);
        if (zipped)
            out.writeInt((int)(npos-pos-8));
        out.setPosition(npos);
    }

    private void readChunks() throws IOException
    {
        nextLine("Chunks");

        try
        {
            chunks = Integer.parseInt(line);
        }
        catch (NumberFormatException e) { error("invalid number of chunks"); }

        type = new byte[chunks];
        start = new long[chunks];
        bb = new int[chunks][];

        for (chunk=0;chunk<chunks;chunk++)
        {
            start[chunk] = out.getPosition();
            readChunk();
        }
    }

    private void readChunk() throws IOException
    {
        nextLine("Chunk");
        if (!"".equals(line)) error("unknown stuff after 'Chunk:'");

        nextLine("Type");
        if (line.length()!=1) error("unknown type '"+line+"'");
        type[chunk] = (byte)line.charAt(0);

        nextLine("Start");
        bb[chunk] = readChunkBB();

        nextLine("Blocks");
        int blocks = 0;
        try
        {
            blocks = Integer.parseInt(line);
        }
        catch (NumberFormatException e) { error("invalid number of blocks"); }

        long pos = out.getPosition();
        out.writeInt(0);

        int[] start = new int[blocks];
        String[] key = new String[blocks];

        for (int i=0;i<blocks;i++)
        {
            start[i] = (int)(out.getPosition()-pos);
            key[i] = readBlock();
        }

        int tablepos = (int)(out.getPosition()-pos);
        out.writeSmallInt(blocks);
        for (int i=0;i<blocks;i++)
        {
            out.writeInt(start[i]);
            out.writeString(key[i]);
        }
        long tmp = out.getPosition();
        out.setPosition(pos);
        out.writeInt(tablepos);
        out.setPosition(tmp);
    }

    private String readBlock() throws IOException
    {
        nextLine("Block");
        String key = line;
        if ("-".equals(key)) key = "";

        nextLine("Slices");
        int slices = 0;
        try
        {
            slices = Integer.parseInt(line);
        }
        catch (NumberFormatException e) { error("invalid number of slices"); }

        long pos = out.getPosition();
        out.writeInt(0);

        int[] start = new int[slices];
        String[] value = new String[slices];

        for (int i=0;i<slices;i++)
        {
            start[i] = (int)(out.getPosition()-pos);
            value[i] = readSlice();
        }

        int tablepos = (int)(out.getPosition()-pos);
        out.writeSmallInt(slices);
        for (int i=0;i<slices;i++)
        {
            out.writeInt(start[i]);
            out.writeString(value[i]);
        }
        long tmp = out.getPosition();
        out.setPosition(pos);
        out.writeInt(tablepos);
        out.setPosition(tmp);

        return key;
    }

    private String readSlice() throws IOException
    {
        nextLine("Slice");
        String value = line;
        if ("-".equals(value)) value = "";

        nextLine("Elements");
        int elements = 0;
        try
        {
            elements = Integer.parseInt(line);
        }
        catch (NumberFormatException e) { error("invalid number of elements"); }

        out.writeInt(elements);
        long pos = out.getPosition();
        if (zipped)
            out.writeInt(0);
        out.resetDelta();

        MyDataOutputStream orig = out;
        BufferedOutputStream bos = null;
        DeflaterOutputStream dos = null;

        if (zipped)
        {
            dos = new DeflaterOutputStream(out, new Deflater(Deflater.BEST_COMPRESSION));
            bos = new BufferedOutputStream(dos);
            out = new MyDataOutputStream(bos,out);
        }

        for (int i=0;i<elements;i++)
            readElement();

        if (zipped)
        {
            bos.flush();
            dos.finish();
            out = orig;

            long npos = out.getPosition();
            out.setPosition(pos);
            out.writeInt((int)(npos-pos-4));
            out.setPosition(npos);
        }

        return value;
    }

    private void readElement() throws IOException
    {
        nextLine("Element");
        if (!"".equals(line)) error("unknown stuff after 'Element:'");

        switch (type[chunk])
        {
        case 'N': readNode(); break;
        case 'W': readWay(); break;
        case 'A': readArea(); break;
        case 'C': readCollection(); break;
        default: error("unknown type: "+(char)type[chunk]);
        }

        readTags();
        readMembers();
        readMeta(type[chunk]=='C');
    }

    private void readNode() throws IOException
    {
        nextLine("Position");
        if (line.equals("-"))
        {
            out.writeDeltaX(Integer.MAX_VALUE);
            out.writeDeltaY(Integer.MAX_VALUE);
            return;
        }

        StringTokenizer t = new StringTokenizer(line,",");
        if (t.countTokens()!=2) error("geo position expected");
        out.writeDeltaX(convCoord(t.nextToken()));
        out.writeDeltaY(convCoord(t.nextToken()));
    }

    private void readWay() throws IOException
    {
        positions("Positions");
    }

    private void readArea() throws IOException
    {
        positions("Positions");

        nextLine("Holes");
        int holes = 0;
        try
        {
            holes = Integer.parseInt(line);
        }
        catch (NumberFormatException e) { error("invalid number of holes"); }

        out.writeSmallInt(holes);

        for (int i=0;i<holes;i++)
            positions("Hole");
    }

    private void readCollection() throws IOException
    {
        nextLine("ID");

        nextLine("Slices");
        int slices = 0;
        try
        {
            slices = Integer.parseInt(line);
        }
        catch (NumberFormatException e) { error("invalid number of slices"); }

        out.writeSmallInt(slices);

        for (int i=0;i<slices;i++)
        {
            nextLine("Type");
            if (line.length()!=1) error("unknown type '"+line+"'");
            out.writeByte((byte)line.charAt(0));

            nextLine("BoundingBox");
            int[] bb = readBB();
            for (int j=0;j<4;j++)
                out.writeInt(bb==null?Integer.MAX_VALUE:bb[j]);

            nextLine("Key");
            line = replaceEscapeSequences(line.trim());
            out.writeString(line);

            nextLine("Value");
            line = replaceEscapeSequences(line.trim());
            out.writeString(line);
        }
    }

    private void positions(String key) throws IOException
    {
        nextLine(key);
        List<Integer> pos = new ArrayList<>();
        while (true)
        {
            nextLineUnfiltered();
            if (line.indexOf(":")!=-1)
            {
                out.writeSmallInt(pos.size()/2);
                for (int i=0;i<pos.size();i+=2)
                {
                    out.writeDeltaX(pos.get(i));
                    out.writeDeltaY(pos.get(i+1));
                }
                pushBack();
                break;
            }

            StringTokenizer t = new StringTokenizer(line,",");
            if (line.equals("-"))
            {
                pos.add(Integer.MAX_VALUE);
                pos.add(Integer.MAX_VALUE);
                continue;
            }

            if (t.countTokens()!=2) error("geo position expected");
            pos.add(convCoord(t.nextToken()));
            pos.add(convCoord(t.nextToken()));
        }
    }

    private void readMeta(boolean force_id) throws IOException
    {
        if (id || force_id)
        {
            nextLine("ID");
            try
            {
                out.writeLong(Long.parseLong(line));
            }
            catch (NumberFormatException e) { error("invalid ID"); }
        }

        if (version)
        {
            nextLine("Version");
            try
            {
                out.writeSmallInt(Integer.parseInt(line));
            }
            catch (NumberFormatException e) { error("invalid version"); }
        }

        if (timestamp)
        {
            nextLine("Timestamp");
            try
            {
                out.writeLong(Long.parseLong(line));
            }
            catch (NumberFormatException e) { error("invalid timestamp"); }
        }

        if (changeset)
        {
            nextLine("Changeset");
            try
            {
                out.writeLong(Long.parseLong(line));
            }
            catch (NumberFormatException e) { error("invalid changeset"); }
        }

        if (user)
        {
            nextLine("User");
            int pos = line.indexOf(" ");
            try
            {
                out.writeInt(Integer.parseInt(line.substring(0,pos)));
            }
            catch (NumberFormatException e) { error("invalid user id"); }
            String name = line.substring(pos).trim();
            if (name.charAt(0)!='(' || name.charAt(name.length()-1)!=')')
                error("name in parenthesis expected");
            out.writeString(name.substring(1,name.length()-1));
        }
    }

    private void readTags() throws IOException
    {
        nextLine("Tags");

        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        while (true)
        {
            nextLineUnfiltered();
            int pos = line==null?-1:line.indexOf(" =");
            if (pos==-1)
            {
                out.writeSmallInt(keys.size());
                for (int i=0;i<keys.size();i++)
                {
                    out.writeString(keys.get(i));
                    out.writeString(values.get(i));
                }
                pushBack();
                return;
            }

            keys.add(replaceEscapeSequences(line.substring(0,pos).trim()));
            values.add(replaceEscapeSequences(line.substring(pos+2).trim()));
        }
    }

    private void readMembers() throws IOException
    {
        nextLine("Members");

        int members = 0;
        try
        {
            members = Integer.parseInt(line);
        }
        catch (NumberFormatException e) { error("invalid number of members"); }

        out.writeSmallInt(members);
        for (int i=0;i<members;i++)
        {
            nextLineUnfiltered();
            String[] split = line.split(" ",3);
            long id = 0;
            int nr = 0;
            try
            {
                id = Long.parseLong(split[0]);
                nr = Integer.parseInt(split[1]);
            }
            catch (NumberFormatException e) { error("invalid number"); }
            String role=replaceEscapeSequences(split[2]);

            out.writeLong(id);
            out.writeString(role);
            out.writeSmallInt(nr);
        }
    }

    private int[] readChunkBB() throws IOException
    {
        nextLine("BoundingBox");
        return readBB();
    }

    private int[] readBB() throws IOException
    {
        StringTokenizer t = new StringTokenizer(line,",");
        if (t.countTokens()==1)
        {
            String token = t.nextToken().trim();
            if (!"-".equals(token))
                error("invalid bounding box");
            return null;
        }
        else if (t.countTokens()==4)
        {
            int[] bb = new int[4];
            for (int i=0;i<4;i++)
            {
                try
                {
                    bb[i] = convCoord(t.nextToken());
                }
                catch (NoSuchElementException e) { error("invalid bounding box"); }
                catch (NumberFormatException e) { error("invalid bounding box"); }
            }
            return bb;
        }
        else
            error("invalid bounding box");
        return null;
    }

    private int convCoord(String s)
    {
        try
        {
            s = s.trim();
            double delta = s.charAt(0)=='-'?-0.5:0.5;
            return (int)(Double.parseDouble(s)*1e7+delta);
        }
        catch (NumberFormatException e) { error("invalid coordinate number"); }

        return Integer.MAX_VALUE;
    }

    private int delta(int last, int val) throws IOException
    {
        int delta = val-last;
        if (delta>=-32767 && delta<=32767)
            out.writeShort(delta);
        else
        {
            out.writeShort(-32768);
            out.writeInt(val);
        }

        return val;
    }

    private String replaceEscapeSequences(String s)
    {
        if (s==null || s.length()==0) error("empty string");
        if (s.charAt(0)=='\"')
        {
            if (s.length()<2) error("quote without match");
            s = s.substring(1,s.length()-1);
        }

        StringBuffer b = new StringBuffer();
        int i=0;
        while (i<s.length())
        {
            char c = s.charAt(i);
            i++;
            if (c=='\\')
            {
                if (i>=s.length()) error("trailing backslash");
                c = s.charAt(i);
                i++;

                if (c=='e')
                    b.append('=');
                else if (c=='x')
                    b.append('#');
                else if (c=='b')
                    b.append('\\');
                else if (c=='n')
                    b.append('\n');
                else if (c=='r')
                    b.append('\r');
                else if (c!='u')
                    error("wrong character after backslash");
                else
                {
                    if (i>s.length()-4) error("incomplete unicode character");
                    b.append((char)Integer.parseInt(s.substring(i,i+4),16));
                    i+=4;
                }
            }
            else
                b.append(c);
        }
        return b.toString();
    }

    //////////////////////////////////////////////////////////////////

    private void nextLine(String expectedToken) throws IOException
    {
        nextLineUnfiltered();

        int pos = line.indexOf(":");
        if (pos==-1) error("token expected");
        if (expectedToken==null)
            token = line.substring(0,pos);
        else
            if (!line.substring(0,pos).equals(expectedToken)) error("'"+expectedToken+"' expected");
        line = line.substring(pos+1).trim();
    }

    private void nextLineUnfiltered() throws IOException
    {
        while (true)
        {
            if (nextline!=null)
            {
                line = nextline;
                nextline = null;
            }
            else
                line = in.readLine();
            if (line==null) return;

            if (line.indexOf('#')>=0)
                line = line.substring(0,line.indexOf('#'));
            line = line.trim();
            if ("".equals(line)) continue;

            return;
        }
    }

    private void pushBack()
    {
        nextline = line;
    }

    private void error(String msg)
    {
        new Throwable().printStackTrace();
        System.err.println(infile+":"+in.getLineNumber()+": "+msg);
        System.exit(-1);
    }
}
