package de.kumakyoo.opa;

import java.io.*;
import java.util.*;
import java.util.zip.InflaterInputStream;
import java.util.zip.DataFormatException;

public class OmaToOpa
{
    private String infile;

    private byte version;
    private int features;
    private Chunk[] chunks;

    private Map<Integer,String> wayKeys;
    private Map<Integer,String> wayValues;

    private PrintWriter out;

    public OmaToOpa(String infile)
    {
        this.infile = infile;
    }

    public void process() throws IOException
    {
        out = new PrintWriter(System.out);

        MyDataInputStream in = new MyDataInputStream(infile);
        in.readByte();
        in.readByte();
        in.readByte();
        out.println("#OPA: from "+infile);
        out.println();

        version = in.readByte();
        out.println("Version: "+version);
        if (version>OpaToOma.VERSION)
            throw new IOException("unsupported version ("+version+") of oma file");

        features = in.readByte();
        out.println("Features: "+features(features));
        out.println("BoundingBox: "+bb(in.readInt(),in.readInt(),in.readInt(),in.readInt()));

        long chunktablePos = in.readLong();
        readTypeTable(in);
        in.close();

        in = new MyDataInputStream(infile);
        in.skip(chunktablePos);

        int chunk_count = in.readInt();
        out.println("Chunks: "+chunk_count);

        chunks = new Chunk[chunk_count];
        for (int i=0;i<chunk_count;i++)
            chunks[i] = new Chunk(in.readLong(),in.readByte(),
                                  in.readInt(),in.readInt(),in.readInt(),in.readInt());
        in.close();

        for (int i=0;i<chunk_count;i++)
            readChunk(i);

        out.println();
        out.println("# end of file");
        out.close();
    }

    public String features(int f)
    {
        List<String> features = new ArrayList<>();

        if ((f&1)!=0) features.add("zipped");
        if ((f&2)!=0) features.add("id");
        if ((f&4)!=0) features.add("version");
        if ((f&8)!=0) features.add("timestamp");
        if ((f&16)!=0) features.add("changeset");
        if ((f&32)!=0) features.add("user");
        if ((f&64)!=0) features.add("once");

        return features.size()==0?"-":String.join(", ",features);
    }

    public String bb(int minlon, int minlat, int maxlon, int maxlat)
    {
        if (minlon==Integer.MAX_VALUE) return "-";
        return (minlon/1e7)+", "+(minlat/1e7)+", "+(maxlon/1e7)+", "+(maxlat/1e7);
    }

    public void readTypeTable(MyDataInputStream in) throws IOException
    {
        if ((features&1)!=0)
            in = new MyDataInputStream(new BufferedInputStream(new InflaterInputStream(in)));

        int typeCount = in.readSmallInt();
        out.println("Types: "+typeCount);
        for (int i=0;i<typeCount;i++)
        {
            byte type = in.readByte();
            out.println("  Type: "+(char)type);
            int keyCount = in.readSmallInt();
            out.println("  Keys: "+keyCount);
            for (int j=0;j<keyCount;j++)
            {
                String key = in.readString();
                out.println("    Key: "+key);
                int valueCount = in.readSmallInt();
                out.println("    Values: "+valueCount);
                for (int k=0;k<valueCount;k++)
                {
                    String value = in.readString();
                    out.println("      "+value);
                }
            }
        }
    }

    public void readChunk(int nr) throws IOException
    {
        out.println();
        out.println("Chunk: # "+(nr+1));
        out.println("  Type: "+(char)chunks[nr].type);
        out.println("  Start: "+chunks[nr].start);
        out.println("  BoundingBox: "+bb(chunks[nr].minlon,chunks[nr].minlat,chunks[nr].maxlon,chunks[nr].maxlat));

        MyDataInputStream in = new MyDataInputStream(infile);

        in.setPosition(chunks[nr].start);
        long blockStartPosition = chunks[nr].start+in.readInt();

        in.setPosition(blockStartPosition);
        int blockCount = in.readSmallInt();
        int[] start = new int[blockCount];
        String[] key = new String[blockCount];
        for (int i=0;i<blockCount;i++)
        {
            start[i] = in.readInt();
            key[i] = in.readString();
        }

        out.println("  Blocks: "+blockCount);

        for (int i=0;i<blockCount;i++)
            readBlock(in,chunks[nr].type,start[i]+chunks[nr].start,key[i]);
    }

    public void readBlock(MyDataInputStream in, byte type, long start, String key) throws IOException
    {
        out.println("  Block: "+(key.equals("")?"-":key));

        in.setPosition(start);
        long sliceStartPosition = start+in.readInt();

        in.setPosition(sliceStartPosition);
        int sliceCount = in.readSmallInt();

        int[] sstart = new int[sliceCount];
        String[] svalue = new String[sliceCount];
        for (int i=0;i<sliceCount;i++)
        {
            sstart[i] = in.readInt();
            svalue[i] = in.readString();
        }

        out.println("    Slices: "+sliceCount);

        for (int i=0;i<sliceCount;i++)
            readSlice(in,type,sstart[i]+start,key,svalue[i]);
    }

    public void readSlice(MyDataInputStream in, byte type, long start, String key, String value) throws IOException
    {
        out.println("    Slice: "+(value.equals("")?"-":value));

        in.setPosition(start);
        int count = in.readInt();
        out.println("      Elements: "+count);

        if ((features&1)!=0)
            in = new MyDataInputStream(new BufferedInputStream(new InflaterInputStream(in)));

        for (int i=0;i<count;i++)
        {
            out.println("      Element: # "+i);

            ElementWithID e = type=='N'?Node.readGeo(in):(type=='W'?Way.readGeo(in):Area.readGeo(in));
            e.readTags(in);
            e.readMetaData(in,features);
            if (type=='N')
                out.println("        Position: "+(((Node)e).lon/1e7)+", "+(((Node)e).lat/1e7));
            else
            {
                out.println("        Positions: # "+((Way)e).lon.length);
                for (int j=0;j<((Way)e).lon.length;j++)
                    out.println("          "+(((Way)e).lon[j]/1e7)+", "+(((Way)e).lat[j]/1e7));
                if (type=='A')
                {
                    out.println("        Holes: "+((Area)e).h_lon.length);
                    for (int k=0;k<((Area)e).h_lon.length;k++)
                    {
                        out.println("          Hole: # "+k);
                        for (int j=0;j<((Area)e).h_lon[k].length;j++)
                            out.println("            "+(((Area)e).h_lon[k][j]/1e7)+", "+(((Area)e).h_lat[k][j]/1e7));
                    }
                }
            }

            out.println("        Tags: # "+e.tags.size());
            for (String tag: e.tags.keySet())
                out.println("          "+escapeEqual(tag)+" = "+escapeEqual(e.tags.get(tag)));

            printMetaData(e,out);
        }
    }

    public void readTags(MyDataInputStream in, PrintWriter out) throws IOException
    {
        int az = in.readSmallInt();
        out.println("    Tags: # "+az);
        for (int i=0;i<az;i++)
            out.println("      "+escapeEqual(in.readString())+" = "+escapeEqual(in.readString()));
    }

    public String escapeEqual(String s)
    {
        return s.replaceAll("\\\\","\\\\\\\\").replaceAll("=","\\\\=").replaceAll("\n","\\\\n").replaceAll("#","\\\\x");
    }

    public int delta(int last, MyDataInputStream in) throws IOException
    {
        int delta = in.readShort();
        return delta==-32768?in.readInt():(last+delta);
    }

    public void printMetaData(ElementWithID e, PrintWriter out) throws IOException
    {
        if ((features&2)!=0)
            out.println("        ID: "+e.id);
        if ((features&4)!=0)
            out.println("        Version: "+e.version);
        if ((features&8)!=0)
            out.println("        Timestamp: "+e.timestamp+" # "+(new Date(e.timestamp*1000)));
        if ((features&16)!=0)
            out.println("        Changeset: "+e.changeset);
        if ((features&32)!=0)
            out.println("        User: "+e.uid+" ("+e.user+")");
    }
}
