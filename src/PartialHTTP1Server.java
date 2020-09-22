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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PartialHTTP1Server {

    public static int MAX_THREADS = 50;
    public static int thread_count = 0;



    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        ServerSocket webSocket = null;
        try {
            webSocket = new ServerSocket(port);
        }
        catch (IOException e) {
            System.err.print("Failed to open socket on port: " + port);
            e.printStackTrace();
        }

        //Starts threadPool with maximum of 50 threads
        ExecutorService threadPool = Executors.newFixedThreadPool(50);

        //Server loop, waits for socket connection
        while(true) {
            try {
                System.out.println("Waiting for connection on port: " + port);
                Socket conn = webSocket.accept();
                if (thread_count < MAX_THREADS) {
                    System.out.println("Client connection from " + conn.getRemoteSocketAddress());
                    PartialHTTP1Threads newThread = new PartialHTTP1Threads(conn);
                    thread_count++;
                    threadPool.execute(newThread);
                } else {
                    System.out.println("\nCould not start thread");
                    //503 ERROR
                }

            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }

        //threadPool.shutdown();

    }
}
