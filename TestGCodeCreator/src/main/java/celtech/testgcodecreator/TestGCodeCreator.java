/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.testgcodecreator;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileContainer;
import celtech.services.slicer.RoboxProfile;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import libertysystems.configuration.ConfigNotLoadedException;
import libertysystems.configuration.Configuration;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public class TestGCodeCreator
{

    private static Vector2D lastPosition = new Vector2D(0, 0);
    private static float currentLayerHeight = 0.2f;
    private static final float layerHeight = 0.4f;
    private static int currentLayerNumber = 0;
    private static final float distanceToVolume = 0.3f;
    private static final float gapsize = 10;

    public static void main(String[] args)
    {
        int nozzleArg = 0;
        int profileArg = 1;
        int fileArg = 2;

        float lineLength = 125;
        float farleft = 50;
        float farright = farleft + lineLength;
        float linespacing = 10;
        float startY = 20;
        float currentY = startY;
        float endOfFirstExtrusion = farleft + 30;
        float endOfFirstGap = endOfFirstExtrusion + gapsize;
        float endOfSecondExtrusion = 0;
        float endOfSecondGap = 0;
        float endOfThirdExtrusion = 0;
        float endOfThirdGap = 0;
        float endOfFourthExtrusion = 0;
        float endOfFourthGap = 0;

        float normalFeedrate = 2100.0f;
        float currentFeedrate = normalFeedrate / 2;

        float retractAmount = -0.05f;
        float unretractAmount = 0.05f;

        Configuration configuration = null;

        String installDir = ApplicationConfiguration.getApplicationInstallDirectory(TestGCodeCreator.class);
        Lookup.initialise();

        try
        {
            configuration = Configuration.getInstance();

            int nozzleToUse = Integer.valueOf(args[nozzleArg]);
            RoboxProfile profileToUse = PrintProfileContainer.getSettingsByProfileName(args[profileArg]);

            float nozzle_open_over_volume = profileToUse.getNozzle_open_over_volume().get(nozzleToUse).floatValue();
            float nozzle_preejection_volume = profileToUse.getNozzle_preejection_volume().get(nozzleToUse).floatValue();
            float nozzle_ejection_volume = profileToUse.getNozzle_ejection_volume().get(nozzleToUse).floatValue();
            float nozzle_wipe_volume = profileToUse.getNozzle_wipe_volume().get(nozzleToUse).floatValue();
            float nozzle_midpoint_bValue = profileToUse.getNozzle_close_at_midpoint().get(nozzleToUse).floatValue();
            float nozzle_midpoint_percent = profileToUse.getNozzle_close_midpoint_percent().get(nozzleToUse).floatValue();

            float pathLength_normal = nozzle_open_over_volume
                + nozzle_preejection_volume
                + nozzle_ejection_volume
                + nozzle_wipe_volume
                + 10;

            float pathVolume_eject_wipe_only = nozzle_open_over_volume
                + nozzle_preejection_volume
                + nozzle_ejection_volume
                + nozzle_wipe_volume - 0.1f;

            float pathVolume_partialB = nozzle_ejection_volume + nozzle_wipe_volume - 0.1f;

            float pathVolume_minimumB = (nozzle_ejection_volume * profileToUse.getNozzle_partial_b_minimum().get(nozzleToUse).floatValue()) - 0.05f;

            endOfSecondExtrusion = endOfFirstGap + (pathVolume_eject_wipe_only / distanceToVolume);

            endOfSecondGap = endOfSecondExtrusion + gapsize;

            endOfThirdExtrusion = endOfSecondGap + (pathVolume_partialB / distanceToVolume);

            endOfThirdGap = endOfThirdExtrusion + gapsize;

            endOfFourthExtrusion = endOfThirdGap + (pathVolume_minimumB / distanceToVolume);

            endOfFourthGap = endOfFourthExtrusion + gapsize;

            try
            {
                File outputFile = new File(args[fileArg]);
                BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outputFile));

                writeHeader(fileWriter);

                for (int baseLayerCount = 0; baseLayerCount < 3; baseLayerCount++)
                {
                    currentY = startY;

                    writeLayerChange(fileWriter);

                    int numberOfPairsOfRows = 2;

                    // Travel to the start
                    writeTravel(fileWriter, farleft, currentY);

                    writeExtrusionCommand(fileWriter, -retractAmount);

                    // First lay down a continuous line along the zigzag
                    //Right
                    writeTripleLine(fileWriter, farright, currentY, farleft, currentY, normalFeedrate, 2.0f);

                    currentY += linespacing;
                    //Back
                    writeOutputLine(fileWriter, farright, currentY, normalFeedrate, 2.0f);
                    //Left
                    writeTripleLine(fileWriter, farleft, currentY, farright, currentY, normalFeedrate, 2.0f);

                    currentY += linespacing;
                    //Back
                    writeOutputLine(fileWriter, farleft, currentY, normalFeedrate, 2.0f);
                    //Right
                    writeTripleLine(fileWriter, farright, currentY, farleft, currentY, normalFeedrate, 2.0f);

                    writeExtrusionCommand(fileWriter, retractAmount);
                }

                writeExtrusionCommand(fileWriter, retractAmount);

                for (int layerCount = 0; layerCount < 10; layerCount++)
                {

                    float nozzleYOffset = 0;

                    writeLayerChange(fileWriter);

                    float extrusionMultiplier = 0;

                    extrusionMultiplier = 1;

                    currentFeedrate = normalFeedrate / 2;

                    currentY = startY;

                    for (int i = 0; i < 3; i++)
                    {
                        // Travel to the start
                        writeTravel(fileWriter, farleft, currentY);

                        writeExtrusionCommand(fileWriter, -retractAmount);

                        // First part of the line
                        writeOutputLine(fileWriter, endOfFirstExtrusion, currentY, currentFeedrate,
                                        extrusionMultiplier);

                        // Retract (to cause close)
                        writeExtrusionCommand(fileWriter, retractAmount);

                        // Move to point B
                        writeTravel(fileWriter, endOfFirstGap, currentY);

                        writeExtrusionCommand(fileWriter, -retractAmount);

                        // Second part of the line
                        writeOutputLine(fileWriter, endOfSecondExtrusion, currentY, currentFeedrate,
                                        extrusionMultiplier);

                        // Retract (to cause close)
                        writeExtrusionCommand(fileWriter, retractAmount);

                        writeTravel(fileWriter, endOfSecondGap, currentY);

                        writeExtrusionCommand(fileWriter, -retractAmount);

                        writeOutputLine(fileWriter, endOfThirdExtrusion, currentY, currentFeedrate,
                                        extrusionMultiplier);

                        // Retract (to cause close)
                        writeExtrusionCommand(fileWriter, retractAmount);

                        writeTravel(fileWriter, endOfThirdGap, currentY);

                        writeExtrusionCommand(fileWriter, -retractAmount);
                        
                        writeOutputLine(fileWriter, endOfFourthExtrusion, currentY, currentFeedrate,
                                        extrusionMultiplier);

                        // Retract (to cause close)
                        writeExtrusionCommand(fileWriter, retractAmount);

                        writeTravel(fileWriter, endOfFourthGap, currentY);

                        writeExtrusionCommand(fileWriter, -retractAmount);

                        // Third part of the line
                        writeOutputLine(fileWriter, farright, currentY, currentFeedrate,
                                        extrusionMultiplier);

                        // Retract (to cause close)
                        writeExtrusionCommand(fileWriter, retractAmount);

                        currentY += linespacing;
                        currentFeedrate += normalFeedrate / 2;
                    }
                }
                fileWriter.close();

            } catch (IOException ex)
            {
                System.out.println("Error - " + ex.getMessage());
            }
        } catch (ConfigNotLoadedException ex)
        {
            System.err.println("Couldn't load application configuration");
        }
    }

    private static void writeTripleLine(BufferedWriter fileWriter, float destinationX,
        float destinationY, float originX, float originY,
        float feedrate, float extrusionWidth)
    {
        float yOffset = 0.5f;
        writeOutputLine(fileWriter, destinationX, destinationY, feedrate, extrusionWidth);
        writeOutputLine(fileWriter, destinationX, destinationY - yOffset, feedrate, extrusionWidth);
        writeOutputLine(fileWriter, originX, originY - yOffset, feedrate, extrusionWidth);
        writeOutputLine(fileWriter, originX, originY + yOffset, feedrate, extrusionWidth);
        writeOutputLine(fileWriter, destinationX, destinationY + yOffset, feedrate, extrusionWidth);
        writeOutputLine(fileWriter, destinationX, destinationY, feedrate, extrusionWidth);
    }

    private static void writeHeader(BufferedWriter fileWriter)
    {
        try
        {
            fileWriter.write("G21 ; set units to millimeters\n"
                + "M104 S200 T1 ; set temperature\n"
                + "M109 S200 T1 ; wait for temperature to be reached\n"
                + "G90 ; use absolute coordinates\n"
                + "M83 ; use relative distances for extrusion\n"
                + "T1 ; change extruder\n"
                + "M106 S153 ; enable fan\n");
        } catch (IOException ex)
        {
            System.out.println("Error - " + ex.getMessage());
        }
    }

    private static void writeLayerChange(BufferedWriter fileWriter)
    {
        try
        {
            currentLayerNumber++;
            fileWriter.write(String.format("\n\nG1 Z%.3f F12000.0 ; Layer %d\n", currentLayerHeight,
                                           currentLayerNumber));
            currentLayerHeight += layerHeight;
        } catch (IOException ex)
        {
            System.out.println("Error - " + ex.getMessage());
        }
    }

    private static void writeTravel(BufferedWriter fileWriter, float destinationX,
        float destinationY)
    {
        try
        {
            fileWriter.write(String.format("G1 X%.3f Y%.3f F12000.0\n", destinationX, destinationY));
            lastPosition = new Vector2D(destinationX, destinationY);
        } catch (IOException ex)
        {
            System.out.println("Error - " + ex.getMessage());
        }
    }

    private static void writeOutputLine(BufferedWriter fileWriter, float destinationX,
        float destinationY, float feedrate)
    {
        writeOutputLine(fileWriter, destinationX, destinationY, feedrate, 1.0f);
    }

    private static void writeOutputLine(BufferedWriter fileWriter, float destinationX,
        float destinationY, float feedrate, float extrusionMultiplier)
    {
        Vector2D destination = new Vector2D(destinationX, destinationY);

        double distance = destination.distance(lastPosition);
        double extrusion = distanceToVolume * distance;

        try
        {
            fileWriter.write(
                String.format("G1 X%.3f Y%.3f E%.3f F%.3f\n", destinationX, destinationY, extrusion,
                              feedrate));
            lastPosition = destination;
        } catch (IOException ex)
        {
            System.out.println("Error - " + ex.getMessage());
        }
    }

    private static void writeExtrusionCommand(BufferedWriter fileWriter, float extruderMovement)
    {
        try
        {
            fileWriter.write(String.format("G1 E%.3f F12000.0\n", extruderMovement));
        } catch (IOException ex)
        {
            System.out.println("Error - " + ex.getMessage());
        }
    }
}
