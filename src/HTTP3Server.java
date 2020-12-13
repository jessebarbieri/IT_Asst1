/**
@authors Jason Wrobel, Jesse Barbieri, Ethan Wang
 Internet Technology - 352
 Assignment 1 - HTTP/1.0 + MIME Type Support
**/

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class HTTP3Server {


    /**
     * Initializes socket server and manages threads for request execution
     * @param args port number from command line input
     */
    public static void main(String[] args) {
        //Gets port from command line input
        int port = Integer.parseInt(args[0]);

        //Attempts to start socket server on provided port
        ServerSocket webSocket = null;
        try {
            webSocket = new ServerSocket(port);
        }
        catch (IOException e) {
            //Handles failed initialization of server
            System.err.print("Failed to open socket on port: " + port);
            e.printStackTrace();
        }

        //Creates threadPool with maximum of 50 threads
        ThreadPoolExecutor threadPool =  new ThreadPoolExecutor(5, 50, 100, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());

        //Server loop, waits for socket connection
        Socket conn = null;

        while(true) {
            try {
                System.out.println("Waiting for connection on port: " + port);
                conn = webSocket.accept();
                System.out.println("Client connection from " + conn.getRemoteSocketAddress());
                HTTP3Threads newThread = new HTTP3Threads(conn);
                threadPool.execute(newThread);
                System.out.println(threadPool.getActiveCount());
//                System.out.println("THREAD ACCEPTED");
            }
            catch(Exception e) {
                try {
//                    System.out.println("THREAD REJECTED");
                    PrintStream output = new PrintStream(conn.getOutputStream());
                    output.print("HTTP/1.0 503 Service Unavailable\r\n");
                    output.print("\r\n");
                    output.close();
                    conn.close();
                } catch (Exception exception){
                    e.printStackTrace();
                }
            }
        }
    }
}
