import java.net.Socket;

public class PartialHTTP1Threads extends Thread{

    Socket connection;

    public PartialHTTP1Threads(Socket conn) {
        this.connection = conn;
    }

    public void run() {
        System.out.println("Thread started for --> " + this.connection.getRemoteSocketAddress());
        try {
            this.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PartialHTTP1Server.thread_count--;
        return;
    }
}
