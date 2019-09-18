package sfsu;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

class WebServerTest {

    /**
     * Test a very simple GET. Expected: pom.xml in the working directory.
     *
     * @throws Exception
     */
    @Test
    public void get() throws Exception {
        WebServer ws = new WebServer(0); // port 0 == autoselect port

        // Open a sample file that is known to exist.
        String targetFile = "pom.xml";
        String expected = new String(Files.readAllBytes(Paths.get(targetFile)));

        // Start listening on a separate thread, or the tester would just hang.
        Thread webServerThread = new Thread(ws::listen);
        webServerThread.start();

        // Send the GET request
        Socket connection = new Socket("localhost", ws.port());
        PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
        out.println("GET /pom.xml HTTP/1.0");
        out.println("");

        // Read the whole response.
        byte[] inputBytes = connection.getInputStream().readAllBytes();
        String responseContent = new String(inputBytes);
        Scanner scanner = new Scanner(responseContent);

        // Skip over the header.
        // TODO: test the header contents.
        while (scanner.hasNext()) {
            String nextLine = scanner.nextLine();
            // All the way until the first blank line is found.
            if (nextLine.equals("")) {
                break;
            }
        }

        // Read the payload.
        String fileContents = "";
        while (scanner.hasNext()) {
            fileContents = fileContents + scanner.nextLine();
            fileContents = fileContents + "\n";
        }

        assertThat(fileContents).isEqualTo(expected + "\n");

        ws.stop();
        webServerThread.interrupt();
    }

    /**
     * Test a very simple GET. Expected: pom.xml in the working directory.
     *
     * @throws Exception
     */
    @Test
    public void head() throws Exception {
        WebServer ws = new WebServer(0); // port 0 == autoselect port

        // Start listening on a separate thread, or the tester would just hang.
        Thread webServerThread = new Thread(ws::listen);
        webServerThread.start();

        // Send the GET request
        Socket connection = new Socket("localhost", ws.port());
        PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
        out.println("GET /pom.xml HTTP/1.0");
        out.println("");

        // Read the whole response.
        byte[] inputBytes = connection.getInputStream().readAllBytes();
        String responseContent = new String(inputBytes);
        Scanner scanner = new Scanner(responseContent);

        // HTTP code
        String httpCode = scanner.nextLine();
        assertThat(httpCode).isEqualTo("HTTP/1.0 200 OK");

        // Server identification
        String server = scanner.nextLine();
        assertThat(server).isNotNull().isNotEmpty();

        // Last Modified
        String lastModified = scanner.nextLine();
        assertThat(lastModified).startsWith("Last-Modified:");

        // Content Length
        String contentLength = scanner.nextLine();
        assertThat(contentLength).startsWith("Content-Length:");

        ws.stop();
        webServerThread.interrupt();
    }

}