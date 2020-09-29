/**
@authors Jason Wrobel, Jesse Barbieri, Ethan Wang
 Internet Technology - 352
 Assignment 1 - HTTP/1.0 + MIME Type Support
**/

/*
TODO
- GET method
- HEAD method
- POST method
- MIME type support:
    text: html, plain
    image: gif, jpeg, png
    application: octet-stream, pdf, x-gzip, zip
- Response status codes:
    200     OK
    304     Not Modified
    400     Bad Request
    403     Forbidden
    404     Not Found
    408     Request Timeout (if client opens a connection and does not send a request in 5 sec.)
    500     Internal Server Error
    501     Not Implemented
    503     Service Unavailable
    505     HTTP Version Not Supported
 */


import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PartialHTTP1Server {

    //Global variables to store maximum number of threads, as well as active thread count
    public static int MAX_THREADS = 50;
    public static int thread_count = 0;

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
        ExecutorService threadPool = Executors.newFixedThreadPool(50);

        //Server loop, waits for socket connection
        while(true) {
            try {
                System.out.println("Waiting for connection on port: " + port);
                Socket conn = webSocket.accept();
                //If currently active threads do not exceed maximum thread count, executes new thread for connection
                if (thread_count < MAX_THREADS) {
                    System.out.println("Client connection from " + conn.getRemoteSocketAddress());
                    PartialHTTP1Threads newThread = new PartialHTTP1Threads(conn);
                    thread_count++;
                    threadPool.execute(newThread);
                //If maximum threads are in use, rejects connection and sends 503 Service Unavailable to output
                } else {
                    PrintStream output = new PrintStream(conn.getOutputStream());
                    output.print("HTTP/1.0 503 Service Unavailable\r\n");
                    output.print("\r\n");
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
