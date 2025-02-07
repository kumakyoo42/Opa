package de.kumakyoo.opa;

import java.util.List;
import java.io.*;

public class Collection extends ElementWithID
{
    public String[] noderole;
    public int[] nlon;
    public int[] nlat;

    public String[] wayrole;
    public int[][] wlon;
    public int[][] wlat;

    public Collection()
    {
    }

    public Collection(MyDataInputStream in, int features) throws IOException
    {
        int naz = in.readSmallInt();
        noderole = new String[naz];
        nlon = new int[naz];
        nlat = new int[naz];

        for (int k=0;k<naz;k++)
        {
            noderole[k] = in.readString();
            nlon[k] = in.readDeltaX();
            nlat[k] = in.readDeltaY();
        }

        int waz = in.readSmallInt();
        wayrole = new String[waz];
        wlon = new int[waz][];
        wlat = new int[waz][];
        for (int k=0;k<waz;k++)
        {
            wayrole[k] = in.readString();
            int az = in.readSmallInt();
            wlon[k] = new int[az];
            wlat[k] = new int[az];
            for (int i=0;i<az;i++)
            {
                wlon[k][i] = in.readDeltaX();
                wlat[k][i] = in.readDeltaY();
            }
        }

        int aaz = in.readSmallInt();

        readTags(in);
        readMetaData(in,features);
    }

    public static Collection readGeo(MyDataInputStream in) throws IOException
    {
        Collection c = new Collection();

        int naz = in.readSmallInt();
        c.noderole = new String[naz];
        c.nlon = new int[naz];
        c.nlat = new int[naz];

        for (int k=0;k<naz;k++)
        {
            c.noderole[k] = in.readString();
            c.nlon[k] = in.readDeltaX();
            c.nlat[k] = in.readDeltaY();
        }

        int waz = in.readSmallInt();
        c.wayrole = new String[waz];
        c.wlon = new int[waz][];
        c.wlat = new int[waz][];
        for (int k=0;k<waz;k++)
        {
            c.wayrole[k] = in.readString();
            int az = in.readSmallInt();
            c.wlon[k] = new int[az];
            c.wlat[k] = new int[az];
            for (int i=0;i<az;i++)
            {
                c.wlon[k][i] = in.readDeltaX();
                c.wlat[k][i] = in.readDeltaY();
            }
        }

        int aaz = in.readSmallInt();

        return c;
    }

    public void writeGeo(MyDataOutputStream out) throws IOException
    {
        out.writeSmallInt(nlon.length);
        for (int i=0;i<nlon.length;i++)
        {
            out.writeString(noderole[i]);
            out.writeDeltaX(nlon[i]);
            out.writeDeltaY(nlat[i]);
        }

        out.writeSmallInt(wlon.length);
        for (int i=0;i<wlon.length;i++)
        {
            out.writeString(wayrole[i]);
            out.writeSmallInt(wlon[i].length);
            for (int j=0;j<wlon[i].length;j++)
            {
                out.writeDeltaX(wlon[i][j]);
                out.writeDeltaY(wlat[i][j]);
            }
        }

        out.writeSmallInt(0);
    }
}
