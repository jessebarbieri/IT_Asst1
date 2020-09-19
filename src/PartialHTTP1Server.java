import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PartialHTTP1Server {

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

        while(true) {
            try {
                System.out.println("Waiting for connection on port: " + port);
                Socket conn = webSocket.accept();
                System.out.println("Client connection from " + conn.getRemoteSocketAddress());
                PartialHTTP1Threads newThread = new PartialHTTP1Threads(conn);
                newThread.start();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
