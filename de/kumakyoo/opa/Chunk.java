package de.kumakyoo.opa;

public class Chunk
{
    long start;
    byte type;
    int minlon, minlat, maxlon, maxlat;

    public Chunk(long start, byte type, int minlon, int minlat, int maxlon, int maxlat)
    {
        this.start = start;
        this.type = type;
        this.minlon = minlon;
        this.minlat = minlat;
        this.maxlon = maxlon;
        this.maxlat = maxlat;
    }

    public String toString()
    {
        return (char)type+" "+(minlon/1e7)+","+(minlat/1e7)+","+(maxlon/1e7)+","+(maxlat/1e7);
    }
}
