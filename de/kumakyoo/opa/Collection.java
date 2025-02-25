package de.kumakyoo.opa;

import java.util.*;
import java.io.*;

public class Collection extends ElementWithID
{
    List<Slice> slices;

    public Collection()
    {
    }

    public Collection(MyDataInputStream in, int features) throws IOException
    {
        int count = in.readSmallInt();
        slices = new ArrayList<>(count);
        for (int i=0;i<count;i++)
            slices.add(new Slice(in.readByte(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readString(),in.readString()));

        readTags(in);
        readMembers(in);
        readMetaData(in,features);
    }

    public static Collection readGeo(MyDataInputStream in) throws IOException
    {
        Collection col = new Collection();
        int count = in.readSmallInt();
        col.slices = new ArrayList<>(count);
        for (int i=0;i<count;i++)
            col.slices.add(new Slice(in.readByte(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readString(),in.readString()));
        return col;
    }

    public void writeGeo(MyDataOutputStream out) throws IOException
    {
        out.writeSmallInt(slices.size());
        for (Slice s:slices)
        {
            out.writeByte(s.type);
            out.writeInt(s.minlon);
            out.writeInt(s.minlat);
            out.writeInt(s.maxlon);
            out.writeInt(s.maxlat);
            out.writeString(s.key);
            out.writeString(s.value);
        }
    }
}
