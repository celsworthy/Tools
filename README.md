# Tools
This repository contains a collection of tools that can be helpful when developing AutoMaker and Root.

## EncryptPrinterID
Occasionally the printer serial number stored in the Robox EEPROM becomes corrupted. If this happens, it can be reset from AutoMaker. To prevent the user from setting it to an arbitrary value, AutoMaker requires a reset code, which is an encrypted form of the serial number, which must be obtained from Robox support. This utility is used by Robox support to generate the reset code. It can be run as follows:

	java -jar <path to EncryptPrinterID.jar>
	
e.g.

	java -jar "C:\Dev\Tools\EncryptPrinterID\target\EncryptPrinterID.jar"

Copy and paste or type the serial number into the fields. The reset code is automatically placed in the copy buffer, ready to be pasted into an email and sent to the user.

## SVG to FXML converter
This is a Java tool to convert SVG images to FXML images. The command line is:

	java -jar <path to SVGFXML.jar> <path to svg file, or directory containing svg files>
	
e.g.

	java -jar "C:\Dev\Tools\SVGFXML\dist\SVGFXML.jar" .\Icon-Play.svg
