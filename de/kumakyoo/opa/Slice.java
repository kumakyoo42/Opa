package de.kumakyoo.opa;

public class Slice
{
    byte type;
    int minlon;
    int minlat;
    int maxlon;
    int maxlat;
    String key;
    String value;

    public Slice(byte type, int minlon, int minlat, int maxlon, int maxlat, String key, String value)
    {
        this.type = type;
        this.minlon = minlon;
        this.minlat = minlat;
        this.maxlon = maxlon;
        this.maxlat = maxlat;
        this.key = key;
        this.value = value;
    }
}
