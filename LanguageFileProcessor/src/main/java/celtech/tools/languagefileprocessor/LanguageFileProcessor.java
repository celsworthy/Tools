/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.tools.languagefileprocessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import java.nio.charset.Charset;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;

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
        HashMap<Integer, String> localeNames = new HashMap<>();
        HashMap<Integer, PrintWriter> localeFiles = new HashMap<>();

        if (args.length > 0)
        {
            File sourceFile = new File(args[0]);
            String path = sourceFile.getParent();
            String filename = sourceFile.getName().replaceFirst("\\..*", "");
            Charset charset = Charset.forName("US-ASCII");

            try
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));

                String line;
                while ((line = in.readLine()) != null)
                {
                    String[] fields = line.split("\t");
                    if (fields.length > 0)
                    {
                        if (localeFound == false && fields[0].equalsIgnoreCase("Locale"))
                        {
                            for (int i = 1; i < fields.length; i++)
                            {
                                if (fields[i] != null)
                                {
                                    localeNames.put(i, fields[i]);
                                    String localeFileName;
                                    if (fields[i].equalsIgnoreCase("default"))
                                    {
                                        localeFileName = path + File.separator + filename + ".properties";
                                    } else
                                    {
                                        localeFileName = path + File.separator + filename + "_" + fields[i] + ".properties";
                                    }

                                    File localeFile = new File(localeFileName);
                                    try
                                    {
                                        PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(localeFileName), "UTF-8"));

                                        localeFiles.put(i, out);
                                        localeFound = true;
                                    } catch (UnsupportedEncodingException ex)
                                    {
                                        steno.error("Unsupport encoding");
                                    }

                                }
                            }
                        } else if (localeFound == true)
                        {
                            if (fields[0].startsWith("#") == false)
                            {
                                for (int i = 1; i < fields.length; i++)
                                {
                                    PrintWriter localeFileWriter = localeFiles.get(i);
                                    if (localeFiles.get(i) != null)
                                    {
                                        String lineToOutput = fields[0] + "=" + fields[i] + "\r\n";
                                        localeFileWriter.write(lineToOutput);
                                    }
                                }
                            }
                        }
                    }
                }

                for (PrintWriter fileWriter : localeFiles.values())
                {
                    fileWriter.close();
                }

                steno.info("Got " + localeNames.size() + " locales");
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
}
