import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Crawler {
    private static final String DISALLOW = "Disallow:";
    private static final String USER_AGENT = "User-agent:";

    private static final String CRAWLER_USER_AGENT = "RIWEB_CRAWLER";

    private Queue<String> urlQ;
    private List<String> visitedUrls;
    private List<String> visitedDomains;
    private static final int MAXIMUM_PAGES_LIMIT = 100;

    private Map<String, Integer> revisitedUrls;
    private static final int Max_COME_BACKS = 5;

    private HttpClient httpClient = new HttpClient();

    public Crawler(String url) {
        urlQ = new LinkedList<>();
        urlQ.add(url);

        visitedUrls = new LinkedList<>();
        visitedDomains = new LinkedList<>();
        revisitedUrls = new HashMap<>();

    }

    private String currentDirectory = System.getProperty("user.dir");
    private String savedDocumentsDirectory = currentDirectory + "\\" + "SavedDocuments";

    public List<String> start() {

        deleteDirectory(new File(savedDocumentsDirectory));
        new File(savedDocumentsDirectory).mkdir();

        while (urlQ.size() > 0 && (visitedUrls.size() < MAXIMUM_PAGES_LIMIT)) {
            String url = urlQ.remove();
            if (!visitedUrls.contains(url)) {
                visitedUrls.add(url);

                boolean isNewDomain = checkIfNewDomain(url);
                if (isNewDomain) {
                    if (checkAllowedFromRobotsInTxt(url)) {
                        getLinksFromUrlByMetaTag(url);
                    }
                } else {
                    getLinksFromUrlByMetaTag(url);
                }
            }
        }
        System.out.println("Queue size: " + urlQ.size() + "\nNr. of visited links: " + visitedUrls.size());
        return null;
    }

    public boolean checkAllowedFromRobotsInTxt(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            String robot = "http://" + host + "/robots.txt";
            URL urlRobot;

            try {
                urlRobot = new URL(robot);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return true;
            }
            StringBuilder commands;
            try {
                InputStream urlRobotStream = urlRobot.openStream();
                byte b[] = new byte[1000];
                int numRead = urlRobotStream.read(b);
                commands = new StringBuilder(new String(b, 0, numRead));

                while (numRead != -1) {
                    numRead = urlRobotStream.read(b);
                    if (numRead != -1) {
                        String newCommands = new String(b, 0, numRead);
                        commands.append(newCommands);
                    }
                }
                urlRobotStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return true;
            }
            String strURL = url.getFile();


            Map<String, String> agentPathMap = new HashMap<>();
            StringTokenizer st = new StringTokenizer(commands.toString());

            String agent = "";
            String token;
            if (st.hasMoreTokens()) {
                token = st.nextToken();
                while (st.hasMoreTokens()) {
                    if (token.equals(USER_AGENT)) {
                        agent = st.nextToken();
                        token = st.nextToken();

                    } else if (token.equals(DISALLOW)) {
                        token = st.nextToken();
                        if (!token.equals(USER_AGENT)) {
                            agentPathMap.put(agent, token);
                            if (st.hasMoreTokens()) {
                                token = st.nextToken();
                            }
                        } else {
                            agentPathMap.put(agent, null);
                        }
                    }
                }
            }

            if (agentPathMap.containsKey(CRAWLER_USER_AGENT)) {
                if (agentPathMap.get(CRAWLER_USER_AGENT) == null) {
                    return true;
                } else {
                    return strURL.indexOf(agentPathMap.get(CRAWLER_USER_AGENT)) != 0;
                }
            } else if (agentPathMap.containsKey("*")) {
                if (agentPathMap.get(CRAWLER_USER_AGENT) == null) {
                    return true;
                } else {
                    return strURL.indexOf(agentPathMap.get(CRAWLER_USER_AGENT)) != 0;
                }
            }
            return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void getLinksFromUrlByMetaTag(String url) {
        HttpResponse<String> response = httpClient.getResourceFromURI(url);
        String content = response.body();
        if (content != null) {
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                manageOkStatusCodes(url, content);
            } else {
                manageErrorStatusCodes(url, response);
            }
        }
    }

    public void manageErrorStatusCodes(String url, HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String content = response.body();
        if (statusCode >= 300 && statusCode < 400) {
            manageRevisitedUrl(url);
            HttpResponse<String> newResponse = httpClient.getResourceFromURI(response.uri().toString());
            while (newResponse.statusCode() >= 300 & newResponse.statusCode() < 200) {
                manageRevisitedUrl(url);
                if (revisitedUrls.get(url) >= Max_COME_BACKS)
                    break;
                newResponse = httpClient.getResourceFromURI(newResponse.uri().toString());
            }

            if (!(revisitedUrls.get(url) >= Max_COME_BACKS)) {
                String newUrl = newResponse.uri().toString();
                manageOkStatusCodes(newUrl, content);
                if (statusCode == 301) {
                    updateDirectories(url);
                }
            }
        }
    }

    public void updateDirectories(String oldUrl) {
        if (oldUrl.startsWith("https://")) {
            oldUrl = oldUrl.substring("https://".length());
        } else if (oldUrl.startsWith("http://")) {
            oldUrl = oldUrl.substring("http://".length());
        }

        String[] path = oldUrl.split("/");
        deleteCertainPath(new File(savedDocumentsDirectory), path, 0);

    }

    public void manageOkStatusCodes(String url, String content) {
        String robots = getMetaRobots(content);
        if (robots != null && (robots.equals("all") || robots.equals("index"))) {
            saveLink(url, content);
        }
        if (robots != null && (robots.equals("all") || robots.equals("follow"))) {
            List<String> newLinks = getLinks(url, content);
            urlQ.addAll(newLinks);
        }
        if (robots == null || robots.equals("")) {
            saveLink(url, content);
            List<String> newLinks = getLinks(url, content);
            urlQ.addAll(newLinks);
        }
    }


    public void manageRevisitedUrl(String url) {
        if (revisitedUrls.containsKey(url)) {
            revisitedUrls.put(url, revisitedUrls.get(url) + 1);
        } else {
            revisitedUrls.put(url, 0);
        }
    }

    public boolean checkIfNewDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (!visitedDomains.contains(host)) {
                visitedDomains.add(host);
                return true;
            }
            return false;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getMetaRobots(String html) {
        Document doc = Jsoup.parse(html);
        try {
            return doc.select("meta[name = robots]").first().attr("content");
        } catch (Exception e) {
            return null;
        }
    }

    public boolean saveLink(String url, String content) {
        try {
            String host = new URI(url).getHost();

            String mainDir = savedDocumentsDirectory + "\\" + host;
            new File(mainDir).mkdir();

            String[] paths = new URI(url).getPath().split("/");
            String query = new URI(url).getQuery();
            Queue<String> q = new LinkedList<>(Arrays.asList(paths));

            while (q.size() > 0) {
                String newDir = q.remove();
                new File(mainDir + "\\" + newDir).mkdir();
                mainDir = mainDir + "\\" + newDir;
            }

            if (query != null) {
                String newDir = query;
                new File(mainDir + "\\" + newDir).mkdir();
                mainDir = mainDir + "\\" + newDir;
            }
            saveContentInFile(mainDir, content);
            return true;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveContentInFile(String path, String content) {
        try {
            FileWriter myWriter = new FileWriter(path + "\\" + "index.html", false);
            myWriter.write(content);
            myWriter.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public List<String> getLinks(String url, String html) {
        System.out.println("Get Links From Url: " + url);
        List<String> links = new LinkedList<>();
        Document doc = Jsoup.parse(html, url);
        Element head = doc.select("head").first();
        Elements linkElements = head.select("link");

        for (Element element : linkElements) {
            String newUrl = element.absUrl("href");
            links.add(newUrl);
        }
        return links;
    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    boolean deleteCertainPath(File directory, String[] path, int index) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (path[index].equals(file.getName())) {
                    deleteCertainPath(file, path, index + 1);
                }
            }
        }

        File[] parentDirectoryFiles = directory.getParentFile().listFiles();
        if (parentDirectoryFiles != null) {
            if (parentDirectoryFiles.length == 1 && directory.listFiles() == null) {
                return directory.delete();
            }
        } else {
            return false;
        }
        return true;
    }
}
