import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;
import java.util.regex.*;

public class test {

    public static void main(String args[]) throws IOException {

        //String[] parts = args[0].split("-");
        //String S = normalizeName(parts[1]);
        //System.out.println(String.format("input:'%s' parts:'%s' output:'%s'", args[0], parts[1], S));
        //if (S.equals("E")) System.out.println("ok"); else System.out.println("nok");

        if (isProcessRunning("org.serviio.MediaServer")) {
          System.out.println("serviio running");
        } else{
         System.out.println("serviio not running");
      }
     }

     static public String normalizeName(String name) {
        String s = name.trim();
        int index;
        for (index = s.length() - 1; index > 0; index--) {
           if ("0123456789".indexOf(s.charAt(index)) == -1) {
              break;
           }
        }
        return s.substring(0, index + 1).trim();
     }
  
     static boolean isProcessRunning(String processName) throws IOException {

      // Running command that will get all the working processes.
      String line;
      Process proc = Runtime.getRuntime().exec("ps -ef");
      InputStream stream = proc.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      //Parsing the input stream.
      while ((line = reader.readLine()) != null) {
          Pattern pattern = Pattern.compile(processName);
          Matcher matcher = pattern.matcher(line);
          if (matcher.find()) {
              return true;
          }
      }
      return false;
   }
}