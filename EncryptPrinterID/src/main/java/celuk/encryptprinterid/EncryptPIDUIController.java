/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celuk.encryptprinterid;

import java.net.URL;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author micro
 */
public class EncryptPIDUIController implements Initializable {
    
    private static final String KEY_TO_THE_CRYPT = "4304504C02D05504B05204F04204F058";

    @FXML
    private TextField printerIDCodeField;
        
    @FXML
    private RestrictedTextField printerTypeCodeField;

    @FXML
    private RestrictedTextField printerEditionField;

    @FXML
    private RestrictedTextField printerWeekField;

    @FXML
    private RestrictedTextField printerYearField;

    @FXML
    private RestrictedTextField printerPONumberField;

    @FXML
    private RestrictedTextField printerSerialNumberField;

    @FXML
    private RestrictedTextField printerChecksumField;

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        printerTypeCodeField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerEditionField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerWeekField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerYearField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerPONumberField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerSerialNumberField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerChecksumField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
    }

    public static char generateUPSModulo10Checksum(String inputString) throws InvalidChecksumException
    {
        int sum = 0;

        for (int i = 0; i < inputString.length(); i++)
        {
            int a = 0;
            switch (inputString.charAt(i))
            {
                case '0':
                    a = 0;
                    break;
                case '1':
                    a = 1;
                    break;
                case '2':
                    a = 2;
                    break;
                case '3':
                    a = 3;
                    break;
                case '4':
                    a = 4;
                    break;
                case '5':
                    a = 5;
                    break;
                case '6':
                    a = 6;
                    break;
                case '7':
                    a = 7;
                    break;
                case '8':
                    a = 8;
                    break;
                case '9':
                    a = 9;
                    break;
                case 'a':
                case 'A':
                    a = 2;
                    break;
                case 'b':
                case 'B':
                    a = 3;
                    break;
                case 'c':
                case 'C':
                    a = 4;
                    break;
                case 'd':
                case 'D':
                    a = 5;
                    break;
                case 'e':
                case 'E':
                    a = 6;
                    break;
                case 'f':
                case 'F':
                    a = 7;
                    break;
                case 'g':
                case 'G':
                    a = 8;
                    break;
                case 'h':
                case 'H':
                    a = 9;
                    break;
                case 'i':
                case 'I':
                    a = 0;
                    break;
                case 'j':
                case 'J':
                    a = 1;
                    break;
                case 'k':
                case 'K':
                    a = 2;
                    break;
                case 'l':
                case 'L':
                    a = 3;
                    break;
                case 'm':
                case 'M':
                    a = 4;
                    break;
                case 'n':
                case 'N':
                    a = 5;
                    break;
                case 'o':
                case 'O':
                    a = 6;
                    break;
                case 'p':
                case 'P':
                    a = 7;
                    break;
                case 'q':
                case 'Q':
                    a = 8;
                    break;
                case 'r':
                case 'R':
                    a = 9;
                    break;
                case 's':
                case 'S':
                    a = 0;
                    break;
                case 't':
                case 'T':
                    a = 1;
                    break;
                case 'u':
                case 'U':
                    a = 2;
                    break;
                case 'v':
                case 'V':
                    a = 3;
                    break;
                case 'w':
                case 'W':
                    a = 4;
                    break;
                case 'x':
                case 'X':
                    a = 5;
                    break;
                case 'y':
                case 'Y':
                    a = 6;
                    break;
                case 'z':
                case 'Z':
                    a = 7;
                    break;
            }

            if (i % 2 == 0)
            {
                sum += a;
            } else
            {
                sum += a * 2;
            }
        }

        sum = sum % 10;
        sum = 10 - sum;
        if (sum == 10)
        {
            sum = 0;
        }

        return Character.forDigit(sum, 10);
    }
    
    private boolean isValid(String idToCheck, String checkChar)
    {
        boolean result = false;
        
        if (checkChar.length() == 1)
        try
        {
            result = (checkChar.charAt(0) == generateUPSModulo10Checksum(idToCheck));
    
        }
        catch(InvalidChecksumException ex)
        {
        }
        return result;
    }
            
    public void encryptPrinterIdentity()
    {
        StringBuilder checkSB = new StringBuilder();
        checkSB.append(printerTypeCodeField.getText().trim().toUpperCase());
        checkSB.append(printerEditionField.getText().trim().toUpperCase());
        checkSB.append(printerWeekField.getText().trim().toUpperCase());
        checkSB.append(printerYearField.getText().trim().toUpperCase());
        checkSB.append(printerPONumberField.getText().trim().toUpperCase());
        checkSB.append(printerSerialNumberField.getText().trim().toUpperCase());
        
        if (isValid(checkSB.toString(), printerChecksumField.getText().trim().toUpperCase()))
        {
            StringBuilder plainSB = new StringBuilder();
            plainSB.append(printerTypeCodeField.getText().trim().toUpperCase());
            plainSB.append("-");
            plainSB.append(printerEditionField.getText().trim().toUpperCase());
            plainSB.append("-");
            plainSB.append(printerWeekField.getText().trim().toUpperCase());
            plainSB.append("-");
            plainSB.append(printerYearField.getText().trim().toUpperCase());
            plainSB.append("-");
            plainSB.append(printerPONumberField.getText().trim().toUpperCase());
            plainSB.append("-");
            plainSB.append(printerSerialNumberField.getText().trim().toUpperCase());
            plainSB.append("-");
            plainSB.append(printerChecksumField.getText().trim().toUpperCase());

            String encryptedPrinterID = encrypt(plainSB.toString(), KEY_TO_THE_CRYPT);
            printerIDCodeField.setText(encryptedPrinterID);
            printerIDCodeField.selectAll();
            printerIDCodeField.copy();
            printerIDCodeField.requestFocus();
        }
        else
            printerIDCodeField.clear();
    }
    
    public static String encrypt(final String plainMessage,
                                 final String symKeyHex)
    {
        final byte[] symKeyData = DatatypeConverter.parseHexBinary(symKeyHex);

        final byte[] encodedMessage = plainMessage.getBytes(Charset.forName("UTF-8"));
        try
        {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final int blockSize = cipher.getBlockSize();

            // create the key
            final SecretKeySpec symKey = new SecretKeySpec(symKeyData, "AES");

            // generate random IV using block size (possibly create a method for
            // this)
            final byte[] ivData = new byte[blockSize];
            final SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
            rnd.nextBytes(ivData);
            final IvParameterSpec iv = new IvParameterSpec(ivData);

            cipher.init(Cipher.ENCRYPT_MODE, symKey, iv);

            final byte[] encryptedMessage = cipher.doFinal(encodedMessage);

            // concatenate IV and encrypted message
            final byte[] ivAndEncryptedMessage = new byte[ivData.length
                    + encryptedMessage.length];
            System.arraycopy(ivData, 0, ivAndEncryptedMessage, 0, blockSize);
            System.arraycopy(encryptedMessage, 0, ivAndEncryptedMessage,
                    blockSize, encryptedMessage.length);

            final String ivAndEncryptedMessageBase64 = DatatypeConverter
                    .printBase64Binary(ivAndEncryptedMessage);

            return ivAndEncryptedMessageBase64;
        }
        catch (InvalidKeyException e)
        {
            throw new IllegalArgumentException("key argument does not contain a valid AES key");
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("Unexpected exception during encryption", e);
        }
    }
}
