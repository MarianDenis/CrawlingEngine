import java.net.MalformedURLException;
import java.net.URL;

public class Main {
    public static void main(String[] args) throws MalformedURLException {
        String url = "http://riweb.tibeica.com/crawl/";

        Crawler c = new Crawler(url);
        long startTime = System.nanoTime();
        c.start();
        long endTime = System.nanoTime();

        long duration = (endTime - startTime)/ 1_000_000_000; //secunde
        System.out.println("Execution time: " + duration + " seconds.");

    }
}
