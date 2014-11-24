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
        return outputString.toString();
    }
}
