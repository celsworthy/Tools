package celtech.tools.languagefileprocessor;

/**
 *
 * @author Ian
 */
public class FauxCyrillicMapper
{

    public static String parseString(String input)
    {
        StringBuilder outputString = new StringBuilder();
        String lowerCaseInput = input.toLowerCase();

        for (int i = 0; i < input.length(); i++)
        {
            if (lowerCaseInput.charAt(i) == '*' && lowerCaseInput.charAt(i + 1) == 't')
            {
                // TODO: fix to support variable numbers of digits following *T
                outputString.append(lowerCaseInput.substring(i, i + 4).toUpperCase());
                i += 4;
            } else if (lowerCaseInput.charAt(i) == '\\' && lowerCaseInput.charAt(i + 1) == 'u') {
                 outputString.append(lowerCaseInput.substring(i, i + 6));
                i += 6;
            } else 
            {
                switch (lowerCaseInput.charAt(i))
                {
                    case 'n':
                        outputString.append('и');
                        break;
                    case 'o':
                        outputString.append('ф');
                        break;
                    case 'b':
                        outputString.append('в');
                        break;
                    case 'a':
                        outputString.append('д');
                        break;
                    case 'k':
                        outputString.append('к');
                        break;
                    case 'x':
                        outputString.append('ж');
                        break;
                    case 'h':
                        outputString.append('ж');
                        break;
                    case 'm':
                        outputString.append('ш');
                        break;
                    case 'e':
                        outputString.append('з');
                        break;
                    case 'r':
                        outputString.append('я');
                        break;
                    default:
                        outputString.append(input.charAt(i));
                        break;
                }
            }
        }
        return outputString.toString();
    }
}
