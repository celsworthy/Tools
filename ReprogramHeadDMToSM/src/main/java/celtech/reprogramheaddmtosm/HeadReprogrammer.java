package celtech.reprogramheaddmtosm;

import celtech.Lookup;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;

/**
 *
 * @author Ian
 */
public class HeadReprogrammer implements PrinterListChangesListener
{

    boolean keepRunning = true;

    @Override
    public void whenPrinterAdded(Printer printer)
    {
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        try
        {
            HeadEEPROMDataResponse headResponse = printer.readHeadEEPROM();

            System.out.println("A head of type " + headResponse.getTypeCode() + " is attached");

            if (headResponse.getTypeCode().equals("RBX01-DM"))
            {
                System.out.println("Writing head as RBX01-SM");
                HeadFile smHeadFile = HeadContainer.getHeadByID("RBX01-SM");
                printer.writeHeadEEPROM(new Head(smHeadFile));
            }
            else
            {
                System.out.println("No action taken");
            }
        } catch (RoboxCommsException ex)
        {
            System.err.println("Couldn't write to this head");
        } finally
        {
            keepRunning = false;
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
    }

    boolean reprogram()
    {

        boolean succeeded = false;

        Lookup.getPrinterListChangesNotifier().addListener(this);

        while (keepRunning)
        {
            try
            {
                Thread.sleep(200);
            } catch (InterruptedException ex)
            {
                System.err.println("Interrupted whilst writing to the head");
            }
        }

        return succeeded;
    }

    @Override
    public void whenExtruderAdded(Printer printer, int extruderIndex)
    {
    }

    @Override
    public void whenExtruderRemoved(Printer printer, int extruderIndex)
    {
    }

}
