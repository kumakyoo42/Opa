package de.kumakyoo.opa;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class OpaToOma
{
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

        readFeatures();
        readHeaderBB();
        out.writeLong(0);

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

        out.setPosition(20);
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
            case "zipped":
                zipped = true;
                features += 1;
                break;
            case "id":
                id = true;
                features += 2;
                break;
            case "version":
                version = true;
                features += 4;
                break;
            case "timestamp":
                timestamp = true;
                features += 8;
                break;
            case "changeset":
                changeset = true;
                features += 16;
                break;
            case "user":
                user = true;
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
        default: error("unknown type: "+(char)type[chunk]);
        }

        readTags();
        readMeta();
    }

    private void readNode() throws IOException
    {
        nextLine("Position");
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
            if (t.countTokens()!=2) error("geo position expected");
            pos.add(convCoord(t.nextToken()));
            pos.add(convCoord(t.nextToken()));
        }
    }

    private void readMeta() throws IOException
    {
        if (id)
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
            int pos = line==null?-1:line.indexOf(" = ");
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

            keys.add(line.substring(0,pos).trim().replaceAll("\\\\x","#").replaceAll("\\\\n","\n").replaceAll("\\\\=","=").replaceAll("\\\\\\\\","\\\\"));
            values.add(line.substring(pos+3).trim().replaceAll("\\\\x","#").replaceAll("\\\\n","\n").replaceAll("\\\\=","=").replaceAll("\\\\\\\\","\\\\"));
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
        System.err.println(infile+":"+in.getLineNumber()+": "+msg);
        System.exit(-1);
    }
}