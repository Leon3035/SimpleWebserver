import java.net.*;
import java.util.concurrent.*;

public class WebServer {
    public Semaphore workerThreadsSem;
    public final int serverPort;
    public boolean serviceRequested = true;

    public WebServer(int serverPort, int maxThreads) {
        this.serverPort = serverPort;
        this.workerThreadsSem = new Semaphore(maxThreads);
    }
    public void startServer() {
        Socket clientSocket;

        int nextThreadNumber = 0;

        try {

            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Webserver lauscht auf Port " + serverPort + "...\n");

            while (serviceRequested) {
                workerThreadsSem.acquire();

                clientSocket = serverSocket.accept();
                System.out.println("Neue Anfrage von " + clientSocket.getInetAddress());


                (new RequestThread(nextThreadNumber++, clientSocket, this)).start();
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    public static void main(String[] args) {
        WebServer myServer = new WebServer(3000, 3);
        myServer.startServer();
    }

}
