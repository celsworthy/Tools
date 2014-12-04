/*
 * Copyright 2014 CEL UK
 */
package svgfxml;

import java.io.File;
import java.io.IOException;
import org.jdom2.JDOMException;

/**
 *
 * @author tony
 */
public class SVGFXML
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws JDOMException, IOException
    {
        Parser parser = new Parser();
        File file = new File(args[0]);
        if (file.isDirectory())
        {
            for (File childFile : file.listFiles((File dir, String name) ->
                name.toLowerCase().endsWith(".svg")))
            {
                parser.parse(childFile.getAbsolutePath());
            }
        } else if (file.isFile())
        {
            parser.parse(args[0]);
        }
    }

}
