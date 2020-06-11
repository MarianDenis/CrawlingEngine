import java.io.*;
import java.net.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpClient {


    private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .build();

    public HttpResponse<String> getResourceFromURI(String uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .header("User-Agent", "RIWEB_CRAWLER")
                    .version(java.net.http.HttpClient.Version.HTTP_1_1)
                    .GET()
                    .build();


            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            try {
                FileWriter myWriter = new FileWriter("Errors2.txt", false);
                myWriter.write(String.valueOf(request));
                myWriter.write(response.statusCode());
                myWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return response;

        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getHostFromURI(String uri) {
        int index1 = uri.indexOf('/') + 2;
        int index2 = uri.indexOf('/', index1);
        return uri.substring(index1, index2);
    }

    public String getPathFromURI(String uri) {
        int index1 = uri.indexOf('/') + 2;
        int index2 = uri.indexOf('/', index1);
        int index3 = uri.indexOf('/', index2);
        int index4 = uri.indexOf('?');
        if (index4 != -1) {
            return uri.substring(index3, index4);
        } else {
            return uri.substring(index3);
        }
    }


    public String getResource(String uri) {
        String host = getHostFromURI(uri);
        String path = getPathFromURI(uri);
        int timeoutMs = 30000;

        SocketAddress socketAdress = new InetSocketAddress(host, 80);
        Socket socket = new Socket();

        try {
            socket.connect(socketAdress, timeoutMs);


            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            pw.print("GET " + path + " HTTP/1.1\r\n");
            pw.print("Host: " + host + "\r\n");
            pw.print("User-Agent: CLIENT RIW\r\n\r\n");
            pw.flush();

            StringBuilder sb = new StringBuilder(8096);
            String temp;
            String firstLine = br.readLine();
            boolean found = false;
            while ((temp = br.readLine()) != null) {
                if (temp.startsWith("<!DOCTYPE")) {
                    found = true;
                }
                if (found) {
                    sb.append(temp);
                    sb.append("\n");
                }
            }

            int index1 = firstLine.indexOf(' ');
            int index2 = firstLine.indexOf(' ', index1 + 1);
            String statusCode = firstLine.substring(index1 + 1, index2);

            if (statusCode.startsWith("4") || statusCode.startsWith("5")) {
                try {
                    FileWriter myWriter = new FileWriter("Errors.txt", false);

                    String temp2;
                    StringBuilder sb2 = new StringBuilder(8096);
                    while (!((temp2 = br.readLine()).startsWith("<!DOCTYPE"))) {
                        sb2.append(temp2);
                    }
                    myWriter.write(pw.toString());
                    myWriter.write(String.valueOf(sb2));
                    myWriter.close();
                    System.out.println("Wrote to Errors.html file.");
                    return null;
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                    return null;
                } finally {
                    pw.close();
                    br.close();
                    socket.close();
                }
            } else {
                try {
                    FileWriter myWriter = new FileWriter("index.html", false);
                    myWriter.write(String.valueOf(sb));
                    myWriter.close();
                    System.out.println("Wrote to index.html file.");
                    System.out.println(String.valueOf(sb));
                    return String.valueOf(sb);
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                    return null;
                } finally {
                    pw.close();
                    br.close();
                    socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
