import java.io.*;
import java.net.*;

public class RequestThread extends Thread {
    private int name;
    private Socket clientSocket;
    private WebServer server;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    boolean workerServiceRequested = true;
    File rootDir;

    public RequestThread(int num, Socket sock, WebServer server) {
        this.name = num;
        this.clientSocket = sock;
        this.server = server;
        rootDir = new File("webspace/");
    }

    public void run(){
        try{
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();

        String request = in.readLine();
        if (request == null) {
            return;
        }
        System.out.println("Anfrage: " + request + "\n");

        String[] requestParts = request.split(" ");
        String method = requestParts[0];
        String resource = requestParts[1];

        if (method.equals("GET")) {
            String userAgent = "";
            String line;
            System.out.println("Client Header:");
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                System.out.println(line);
                if (line.startsWith("User-Agent:")) {
                    userAgent = line.substring("User-Agent:".length()).trim();
                }
            }
            System.out.println("\n");

            if (!userAgent.contains("Firefox")) {
                // Wenn der User-Agent nicht Firefox ist, senden Sie den Fehlercode 406 zurück
                sendErrorResponse(out, 406, "Not Acceptable");
            } else {
                if (resource.equals("/")) {
                    resource = "index.html";
                }
                // Prüfen Sie, ob der Ressourcenpfad gültig ist
                serveFile(out, rootDir, resource);
            }
        } else {
            // Wenn die Anfrage nicht GET ist, senden Sie den Fehlercode 400 zurück
            sendErrorResponse(out, 400, "Bad Request");
        }

        out.flush();
        clientSocket.close();}
        catch (Exception e) {
                System.out.println(e.toString());
            }finally {
            System.err.println("Request: " + name + " finished!");
            server.workerThreadsSem.release();
        }
    }
    private static void serveFile(OutputStream out, File rootDir, String resource) throws IOException {
        File file = new File(rootDir, resource);
        String mimeType = getMimeType(file);
        if (mimeType != null && file.exists()) {
            byte[] response = new byte[4096];
            if (mimeType != null && file.exists()) {
                if (mimeType.startsWith("image/")) {
                    // Wenn der MimeType ein Bild ist, lesen Sie den Dateiinhalt in ein Byte-Array
                    FileInputStream fileInputStream = new FileInputStream(file);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                    fileInputStream.close();
                    byteArrayOutputStream.close();
                    response = byteArrayOutputStream.toByteArray();
                } else if (mimeType.equals("application/pdf")) {
                    // Wenn der MimeType ein PDF ist, lesen Sie den Dateiinhalt in ein Byte-Array
                    FileInputStream fileInputStream = new FileInputStream(file);
                    response = new byte[(int) file.length()];
                    fileInputStream.read(response);
                    fileInputStream.close();
                } else {
                    // Andernfalls lesen Sie den Textinhalt der Datei
                    BufferedReader fileReader = new BufferedReader(new FileReader(file));
                    StringBuilder fileContent = new StringBuilder();
                    String line;
                    while ((line = fileReader.readLine()) != null) {
                        fileContent.append(line);
                    }
                    fileReader.close();
                    response = fileContent.toString().getBytes();
                }
            }

            // Senden Sie die Antwort inklusive des korrekten MimeType und der Länge des Inhalts
            out.write(("HTTP/1.0 200 OK\r\n" +
                    "Content-Type: " + mimeType + "\r\n" +
                    "Content-Length: " + response.length + "\r\n\r\n").getBytes());
            out.write(response);

            System.out.println("Gesendete Headerzeilen:");
            System.out.println("HTTP/1.0 200 OK");
            System.out.println("Content-Type: " + mimeType);
            System.out.println("Content-Length: " + response.length + "\n");
        } else {
            // Wenn die Datei nicht gefunden wird, senden Sie den Fehlercode 404 zurück
            sendErrorResponse(out, 404, "Not Found");
        }
    }

    private static void sendErrorResponse(OutputStream out, int errorCode, String errorMessage) throws IOException {
        String response = "HTTP/1.0 " + errorCode + " " + errorMessage + "\r\n\r\n" + errorMessage;
        out.write(response.getBytes());
    }

    private static String getMimeType(File file) {
        String fileName = file.getName();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        switch (extension) {
            case "html":
                return "text/html";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            default:
                return null;
        }
    }

}
