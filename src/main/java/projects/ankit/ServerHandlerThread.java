package projects.ankit;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerHandlerThread extends Thread{

     private final int port;
     private final ServerSocket serverSocket;
     private final ExecutorService threadPool;

    public ServerHandlerThread(int port){
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.threadPool = Executors.newFixedThreadPool(16);
    }

    @Override
    public void run() {

        try {
            while (!serverSocket.isClosed() && serverSocket.isBound()) {
                System.out.println("ServerHandlerThread new thread call");

                Socket socket = serverSocket.accept();

                RequestHandler handler = new RequestHandler(socket);
                threadPool.submit(handler);
            }
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (!serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {}
            }
        }
    }
}