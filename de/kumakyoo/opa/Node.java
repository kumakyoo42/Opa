package de.kumakyoo.opa;

import java.io.*;

public class Node extends ElementWithID
{
    int lon;
    int lat;

    public Node()
    {
    }

    public Node(MyDataInputStream in, int features) throws IOException
    {
        lon = in.readDeltaX();
        lat = in.readDeltaY();
        readTags(in);
        readMetaData(in,features);
    }

    public static Node readGeo(MyDataInputStream in) throws IOException
    {
        Node n = new Node();
        n.lon = in.readDeltaX();
        n.lat = in.readDeltaY();
        return n;
    }

    public void writeGeo(MyDataOutputStream out) throws IOException
    {
        out.writeDeltaX(lon);
        out.writeDeltaY(lat);
    }

}
