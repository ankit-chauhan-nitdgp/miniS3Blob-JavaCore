package projects.ankit;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MiniS3Server {

    public static void main(String[] args) {

        ServerHandlerThread serverHandlerThread = new ServerHandlerThread(9000);
        serverHandlerThread.start();

    }

//    public static void main(String[] args) throws IOException {
//        int port = 9000;
//        ServerSocket serverSocket = new ServerSocket(port);
//        System.out.println("miniS3Bucket listening on port " + port);
//
//        while (true) {
//            Socket socket = serverSocket.accept();
//            new Thread(new RequestHandler(socket)).start();
//        }
//    }

}