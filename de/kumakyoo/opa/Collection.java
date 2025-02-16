package de.kumakyoo.opa;

import java.util.List;
import java.io.*;

public class Collection extends ElementWithID
{
    int minlon;
    int minlat;
    int maxlon;
    int maxlat;

    public Collection()
    {
    }

    public Collection(MyDataInputStream in, int features) throws IOException
    {
        minlon = in.readInt();
        minlat = in.readInt();
        maxlon = in.readInt();
        maxlat = in.readInt();

        readTags(in);
        readMembers(in);
        readMetaData(in,features);
    }

    public static Collection readGeo(MyDataInputStream in) throws IOException
    {
        Collection c = new Collection();
        c.minlon = in.readInt();
        c.minlat = in.readInt();
        c.maxlon = in.readInt();
        c.maxlat = in.readInt();
        return c;
    }

    public void writeGeo(MyDataOutputStream out) throws IOException
    {
        out.writeInt(minlon);
        out.writeInt(minlat);
        out.writeInt(maxlon);
        out.writeInt(maxlat);
    }
}
