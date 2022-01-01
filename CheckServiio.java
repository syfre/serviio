import syfre.serviio.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class CheckServiio {

   public static void main(String args[])
         throws ClassNotFoundException, IOException, InterruptedException, SQLException, Exception {

      Serviio serviio = new Serviio("/opt/serviio/library");
      serviio.setVerbose(true);

      serviio.start();
      try {
         
         //  Block of code to try
         serviio.clean();
         serviio.load();
   
         File folder = new File("/mnt/DataF/Archive/Porn");
         serviio.scan(".*\\.mp4", folder);
   
         folder = new File("/mnt/DataG/Archive/Porn");
         serviio.scan(".*\\.mp4", folder);
   
         serviio.printStats();
         
      }
      finally {
      serviio.stop();
      }
   }
}