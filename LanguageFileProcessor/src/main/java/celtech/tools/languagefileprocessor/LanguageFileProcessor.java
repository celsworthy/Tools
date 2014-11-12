package celtech.tools.languagefileprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Ian
 */
public class LanguageFileProcessor
{

    private static Stenographer steno = StenographerFactory.getStenographer(LanguageFileProcessor.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        boolean localeFound = false;
        HashMap<Integer, List<PrintWriter>> localeFiles = new HashMap<>();

        if (args.length > 0)
        {
            File sourceFile = new File(args[0]);

            String path = FilenameUtils.getFullPath(sourceFile.getAbsolutePath());

            String filename = sourceFile.getName().replaceFirst("\\..*", "");
            Charset charset = Charset.forName("US-ASCII");

            int defaultLanguageColumn = -1;

            try
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));

                String line;
                while ((line = in.readLine()) != null)
                {
                    String[] fields = line.split("\t", -1);
                    if (fields.length > 0)
                    {
                        if (localeFound == false && fields[0].equalsIgnoreCase("Locale"))
                        {
                            for (int i = 1; i < fields.length; i++)
                            {
                                if (fields[i].equals("") == false)
                                {
                                    String[] locales = fields[i].split(":");

                                    for (String currentLocale : locales)
                                    {
                                        String localeFileName;
                                        if (currentLocale.equalsIgnoreCase("default"))
                                        {
                                            localeFileName = path + File.separator + filename + ".properties";
                                            defaultLanguageColumn = i;
                                        } else
                                        {
                                            localeFileName = path + File.separator + filename + "_" + currentLocale + ".properties";
                                        }

                                        localeFound = createFile(localeFileName, localeFiles, i, localeFound);
                                    }
                                }
                            }
                        } else if (localeFound == true)
                        {
                            if (fields[0].startsWith("#") == false)
                            {
                                String propertyName = fields[0];
                                String defaultLanguageValue = fields[defaultLanguageColumn];

                                for (Entry<Integer, List<PrintWriter>> entry : localeFiles.entrySet())
                                {
                                    List<PrintWriter> localeFileWriters = entry.getValue();
                                    String localisedValue = fields[entry.getKey()];

                                    String valueToOutput = defaultLanguageValue;

                                    if (localisedValue.equals("") == false)
                                    {
                                        valueToOutput = localisedValue;
                                    }

                                    String lineToOutput = propertyName + "=" + valueToOutput + "\r\n";

                                    for (PrintWriter localeFileWriter : localeFileWriters)
                                    {
                                        localeFileWriter.write(lineToOutput);
                                    }
                                }
                            }
                        }
                    }
                }

                for (List<PrintWriter> fileWriters : localeFiles.values())
                {
                    for (PrintWriter fileWriter : fileWriters)
                    {
                        fileWriter.close();
                    }
                }

            } catch (FileNotFoundException ex)
            {
                steno.error("Couldn't open file " + args[0]);
            } catch (UnsupportedEncodingException ex)
            {
                steno.error("Unsupported Encoding");
            } catch (IOException ex)
            {
                steno.error("Exception opening file");
            }
        } else
        {
            steno.info(null);
        }
    }

    private static boolean createFile(String localeFileName, HashMap<Integer, List<PrintWriter>> localeFiles, int i, boolean localeFound) throws FileNotFoundException
    {
        File localeFile = new File(localeFileName);
        try
        {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(localeFileName), "UTF-8"));
            
            if (localeFiles.get(i) == null)
            {
                localeFiles.put(i, new ArrayList<>());
            }
            
            List<PrintWriter> writerList = localeFiles.get(i);
            writerList.add(out);
            
            localeFound = true;
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Unsupported encoding");
        }
        return localeFound;
    }
}
