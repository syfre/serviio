import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JavaScan {

    public static void main(String[] args) {

        final File folder = new File("/mnt/DataF/Archive/Porn");

        search(".*\\.mp4", folder);
    }

    public static void search(final String pattern, final File folder) {
        for (final File f : folder.listFiles()) {

            if (f.isDirectory()) {
                search(pattern, f);
            }
            if (f.isFile()) {
                System.out.print(".");
                if (f.getName().matches(pattern)) {
		    String[] parts = f.getName().split("-");
            	    System.out.println(parts[1]);
                }
            }

        }
    }
    
}

