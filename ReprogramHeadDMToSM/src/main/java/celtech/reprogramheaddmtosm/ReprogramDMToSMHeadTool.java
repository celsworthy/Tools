package celtech.reprogramheaddmtosm;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.printerControl.comms.RoboxCommsManager;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author Ian
 */
public class ReprogramDMToSMHeadTool
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        int exitStatus = 0;

        ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader) cl).getURLs();

        for (URL url : urls)
        {
            System.out.println(url.getFile());
        }

        System.out.println("Starting " + ReprogramDMToSMHeadTool.class.getSimpleName());

        String installDir = ApplicationConfiguration.getApplicationInstallDirectory(
            ReprogramDMToSMHeadTool.class);
        Lookup.initialise();

        Lookup.setTaskExecutor(new celtech.dmtosmheadtool.NoGUITaskExecutor());
        Lookup.setSystemNotificationHandler(new NoGUISystemNotificationManager());

        RoboxCommsManager commsManager = RoboxCommsManager.
            getInstance(ApplicationConfiguration.getBinariesDirectory());

        HeadReprogrammer headReprogrammer = new HeadReprogrammer();

        commsManager.start();

        headReprogrammer.reprogram();

        System.out.println(ReprogramDMToSMHeadTool.class.getSimpleName() + " shutting down");

        System.exit(exitStatus);
    }

}
