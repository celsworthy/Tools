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
import javafx.scene.control.ComboBox;
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

    @FXML
    private ComboBox printerElectronicsVersion;
    
    @FXML
    private void clearFields()
    {
        printerTypeCodeField.clear();
        printerEditionField.clear();
        printerWeekField.clear();
        printerYearField.clear();
        printerPONumberField.clear();
        printerSerialNumberField.clear();
        printerChecksumField.clear();
        printerElectronicsVersion.setValue("-");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        printerTypeCodeField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerTypeCodeField.setPasteHandler(s -> pasteHandler(s));
        printerEditionField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerTypeCodeField.setPasteHandler(s -> pasteHandler(s));
        printerWeekField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerTypeCodeField.setPasteHandler(s -> pasteHandler(s));
        printerYearField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerTypeCodeField.setPasteHandler(s -> pasteHandler(s));
        printerPONumberField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerTypeCodeField.setPasteHandler(s -> pasteHandler(s));
        printerSerialNumberField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerTypeCodeField.setPasteHandler(s -> pasteHandler(s));
        printerChecksumField.textProperty().addListener((observable, oldValue, newValue) -> encryptPrinterIdentity());
        printerTypeCodeField.setPasteHandler(s -> pasteHandler(s));
        printerElectronicsVersion.getItems().add("");
        printerElectronicsVersion.getItems().add("1");
        printerElectronicsVersion.getItems().add("2");
        printerElectronicsVersion.setValue("");
        printerElectronicsVersion.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            encryptPrinterIdentity();
        });
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
        {
            try
            {
                result = (checkChar.charAt(0) == generateUPSModulo10Checksum(idToCheck));

            }
            catch(InvalidChecksumException ex)
            {
            }
        }
        return result;
    }
            
    private boolean pasteHandler(String content)
    {  
        String[] components = content.split("-");
        if (components.length == 6)
        {
            String typeCode = components[0].trim().toUpperCase();
            String edition = components[1].trim().toUpperCase();
            String week = components[2].trim();
            String year = "00";
            if (week.length()> 2)
            {
                year = week.substring(2);
                week = week.substring(0, 2);
            }
            String poNumber = components[3].trim();
            String serialNumber = components[4].trim();
            String checkByte = components[5].trim();
            String electronicsVersion = "";
            if (checkByte.length() == 3 && (checkByte.charAt(1) == 'E' || checkByte.charAt(1) == 'e')) {
                electronicsVersion = checkByte.substring(2, 3);
                checkByte = checkByte.substring(0, 1);
            }

            if (isValid(typeCode + edition + week + year + poNumber + serialNumber, checkByte) && 
                (electronicsVersion.equals("") || electronicsVersion.equals("1") || electronicsVersion.equals("2")))
            {
                printerTypeCodeField.setText(typeCode);
                printerEditionField.setText(edition);
                printerWeekField.setText(week);
                printerYearField.setText(year);
                printerPONumberField.setText(poNumber);
                printerSerialNumberField.setText(serialNumber);
                printerChecksumField.setText(checkByte);
                printerElectronicsVersion.setValue(electronicsVersion);
                return true;
            }
        }
        return false;
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
            if (!printerElectronicsVersion.getValue().equals("")) {
                plainSB.append("E");
                plainSB.append(printerElectronicsVersion.getValue());
            }

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
