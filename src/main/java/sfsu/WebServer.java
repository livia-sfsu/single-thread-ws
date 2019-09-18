package sfsu;

import com.google.common.base.Splitter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A sample WebServer that uses only one thread.
 *
 * @author livia@sfsu.edu
 */
public class WebServer {

    private static final String SERVER_ID = "Server: Livia's awesome WebServer";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private int port;
    private ServerSocket server = null;
    // atomic since it may be used by multiple threads.
    private AtomicBoolean stopNow = new AtomicBoolean(false);

    /**
     * Create a new WebServer on the given port.
     *
     * If the port is 0 the operating system will find an open port automatically.
     * @param port
     */
    WebServer(int port) {
        checkArgument(port >= 0, "Invalid port");
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println(String.format("Cannot open port %d", port));
            e.printStackTrace();
        }
        this.port = server.getLocalPort();
    }


    /**
     * Start listening and dispatch requests.
     */
    void listen() {
        System.out.println(String.format("Listening on port %d", port));
        while(!stopNow.get()) {
            try {
                Socket client = server.accept();
                processRequest(client);
            } catch (Exception e) {
                System.err.println("Invalid request");
                e.printStackTrace();
            }
        }
    }

    /**
     * @return the port on which the server is running.
     */
    int port() {
        return port;
    }

    void stop() {
        stopNow.set(true);
    }

    protected void processRequest(Socket client) throws IOException {
        System.out.println("Request received, processing.");

        // The first word corresponds to the HTTP verb.
        BufferedReader in = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
        Iterator<String> splits = Splitter.on(' ').split(in.readLine()).iterator();
        String verb = splits.next();
        String target = splits.next();
        if (verb.equals("GET")) {
            if (target.equals("/")) {
                target = "/index.html";
            }
            processGetRequest(client, target);
        } else if (verb.equals("HEAD")) {
            if (target.equals("/")) {
                target = "/index.html";
            }
            processHeadRequest(client, target);
        } else {
            PrintWriter out =
                    new PrintWriter(client.getOutputStream(), true);
            out.println("HTTP/1.0 400 Invalid HTTP verb");
            System.out.println("GET response sent 404.");
            return;
        }
    }

    protected void processHeadRequest(Socket client, String target) throws IOException {
        PrintWriter out =
                new PrintWriter(client.getOutputStream(), true);

        target = target.substring(1); // Remove leading slash
        File file = new File(target);
        if (file.exists()) {
            out.println("HTTP/1.0 200 OK");
        } else {
            out.println("HTTP/1.0 404 Not Found");
            System.out.println("GET response sent 404.");
            return;
        }

        // Read all the contents of the file at once.
        String fileContent = new String (Files.readAllBytes( Paths.get(target)));

        // Response header
        out.println(SERVER_ID);
        out.println("Last-Modified: " + DATE_FORMAT.format(file.lastModified()));
        out.println("Content-Length: " + fileContent.length());
        out.println("");

        // Make sure the clients are not left hanging.
        out.close();
        System.out.println("GET response sent.");
    }

    protected void processGetRequest(Socket client, String target) throws IOException {
        PrintWriter out =
                new PrintWriter(client.getOutputStream(), true);

        target = target.substring(1); // Remove leading slash
        File file = new File(target);
        if (file.exists()) {
            out.println("HTTP/1.0 200 OK");
        } else {
            out.println("HTTP/1.0 404 Not Found");
            System.out.println("GET response sent 404.");
            return;
        }

        // Read all the contents of the file at once.
        String fileContent = new String (Files.readAllBytes( Paths.get(target)));
        // Response header
        out.println(SERVER_ID);
        out.println("Last-Modified: " + DATE_FORMAT.format(file.lastModified()));
        out.println("Content-Length: " + fileContent.length());
        out.println("");

        // Send the contents
        out.print(fileContent);

        // Make sure the clients are not left hanging.
        out.close();
        System.out.println("GET response sent.");
    }


    /**
     * Runs the Web Server.
     *
     * Specify a port in the command line, or get one assigned automatically.
     */
    public static void main(String[] commandLineArguments) {
        int port = 0; // In case no port is specified.
        if (commandLineArguments.length > 1) {
            port = Integer.valueOf(commandLineArguments[0]);
        }

        WebServer ws = new WebServer(port);
        try {
            ws.listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
