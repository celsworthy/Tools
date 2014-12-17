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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String testLocale = "en_GB_faux";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        boolean localeFound = false;

        Map<Integer, List<PrintWriter>> localeFiles = new HashMap<>();
        Map<Integer, String> locales = new HashMap<>();
        Map<Integer, Map<String, String>> templateValues = new HashMap<>();

        if (args.length > 0)
        {
            File sourceFile = new File(args[0]);

            String sourcePath = FilenameUtils.getFullPath(sourceFile.getAbsolutePath());

            String filename = sourceFile.getName().replaceFirst("\\..*", "");

            int defaultLanguageColumn = -1;

            try
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));

                String line;
                while ((line = in.readLine()) != null)
                {
                    ArrayList<String> fields = new ArrayList<>(Arrays.asList(line.split("\t", -1)));
                    fields.add(testLocale);
                    
                    if (fields.size() > 0)
                    {
                        if (localeFound == false && fields.get(0).equalsIgnoreCase("Locale"))
                        {
                            for (int i = 1; i < fields.size(); i++)
                            {
                                if (fields.get(i).equals("") == false)
                                {
                                    locales.put(i, fields.get(i));
                                    for (String currentLocale : fields.get(i).split(":"))
                                    {
                                        String localeFileName;
                                        if (currentLocale.equalsIgnoreCase("default"))
                                        {
                                            localeFileName = sourcePath + File.separator + filename + ".properties";
                                            defaultLanguageColumn = i;
                                        } else
                                        {
                                            localeFileName = sourcePath + File.separator + filename + "_" + currentLocale + ".properties";
                                        }

                                        localeFound = createFile(localeFileName, localeFiles, i, localeFound);
                                    }
                                }
                            }
                        } else if (localeFound == true)
                        {
                            if (fields.get(0).startsWith("#") == false)
                            {
                                String propertyName = fields.get(0);
                                String defaultLanguageValue = fields.get(defaultLanguageColumn);

                                for (Entry<Integer, List<PrintWriter>> filesforLocales : localeFiles.entrySet())
                                {
                                    writePropertyLineForLocales(filesforLocales, fields, locales, defaultLanguageValue,
                                                        propertyName, templateValues);
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

    /**
     * 
     */
    private static void writePropertyLineForLocales(Entry<Integer, List<PrintWriter>> filesforLocales,
        ArrayList<String> fields, Map<Integer, String> locales, String defaultLanguageValue,
        String propertyName, Map<Integer, Map<String, String>> templateValues)
    {
        int localeIndex = filesforLocales.getKey();
        List<PrintWriter> localeFileWriters = filesforLocales.getValue();
        String localisedValue = fields.get(localeIndex);
        if (! templateValues.containsKey(localeIndex)) {
            templateValues.put(localeIndex, new HashMap<>());
        }
        Map<String, String> templateValuesForLocale = templateValues.get(localeIndex);
        
        if (locales.get(localeIndex).equalsIgnoreCase(testLocale))
        {
            localisedValue = FauxCyrillicMapper.parseString(defaultLanguageValue);
        }
        
        String valueToOutput = defaultLanguageValue;
        
        if (localisedValue.equals("") == false)
        {
            valueToOutput = localisedValue;
        }
        
        // NB substitutions MUST come before usages in tsv file. Keys must have exactly two digits (UGH)
        // e.g. *T08
        if (propertyName.startsWith("*T")) {
            templateValuesForLocale.put(propertyName, valueToOutput);
        } else {
            for (Entry<String, String> templateSubstitutions: templateValuesForLocale.entrySet())
            {
                valueToOutput = valueToOutput.replace(templateSubstitutions.getKey(),
                                                         templateSubstitutions.getValue());
            }
        }
        
        String lineToOutput = propertyName + "=" + valueToOutput + "\r\n";
        
        for (PrintWriter localeFileWriter : localeFileWriters)
        {
            localeFileWriter.write(lineToOutput);
        }
    }

    private static boolean createFile(String localeFileName, Map<Integer, List<PrintWriter>> localeFiles, int i, boolean localeFound) throws FileNotFoundException
    {
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
