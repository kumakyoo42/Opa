package de.kumakyoo.opa;

import java.util.Locale;
import java.io.IOException;

public class Opa
{
    public static Locale locale;

    public static void main(String[] args) throws IOException
    {
        locale = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);

        if (args.length==0 || args.length>2) usage();

        String infile = args[0];

        if (Tools.isOma(infile))
        {
            if (args.length!=1) usage();
            new OmaToOpa(infile).process();
        }
        else
        {
            if (args.length!=2) usage();
            new OpaToOma(infile, args[1]).process();
        }
    }

    private static void usage()
    {
        System.err.println("Usage: From oma to opa: java -jar opa.jar <oma input file>");
        System.err.println("       From opa to oma: java -jar opa.jar <opa input file> <oma output file>");
        System.exit(-1);
    }
}
