import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

public class PartialHTTP1Threads extends Thread{

    Socket connection;

    public PartialHTTP1Threads(Socket conn) {
        this.connection = conn;
    }

    public void run() {
        //Output Stream Initialization
        try {
            //Gets outputstream and inputstream from socket (connection)
            PrintStream output = new PrintStream(this.connection.getOutputStream());
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            //Tokenizer to get method / other data
            String inLine = input.readLine();
            StringTokenizer test = new StringTokenizer(inLine);
            while (test.hasMoreTokens()) {
                System.err.println(test.nextToken());
            }
            StringTokenizer tokenizer = new StringTokenizer(inLine);

            String method = tokenizer.nextToken();
            String fileURL = tokenizer.nextToken();

            String version = null;
            if (tokenizer.hasMoreTokens()) {
                version = tokenizer.nextToken();
            }

            if (version == null || tokenizer.hasMoreTokens()) {
                output.print("HTTP/1.0 400 Bad Request\r\n");
                output.print("\r\n"); // End of headers
                killThread();
                return;
            }

            if (!version.equals("HTTP/1.0")) {
                output.print("HTTP/1.0 505 HTTP Version Not Supported\r\n");
                output.print("\r\n"); // End of headers
                killThread();
                return;
            }


            //Check method type
            if(method.equals("PUT") || method.equals("DELETE") || method.equals("LINK") || method.equals("UNLINK")) {
                //Send 501 Header, methods not implemented
                output.print("HTTP/1.0 501 Not Implemented\r\n");
                output.print("\r\n"); // End of headers
                killThread();
                return;
            }
            else if (method.equals("GET")) {
                //HANDLE GET
                while ((inLine = input.readLine()) != null) {
                    if (inLine.trim().equals("")) break;
                }

                if (fileURL.charAt(0) != '/') {
                    output.print("HTTP/1.0 400 Bad Request\r\n");
                    output.print("\r\n"); // End of headers
                    killThread();
                    return;
                }
                if (fileURL.indexOf("../") != -1) {
                    output.print("HTTP/1.0 400 Bad Request\r\n");
                    output.print("\r\n"); // End of headers
                    killThread();
                    return;
                }

                sendFile(output, fileURL);
                killThread();
                return;

            }
            else if (method.equals("HEAD")) {
                //HANDLE HEAD
            }
            else if (method.equals("POST")) {
                //HANDLE POST
            }
            else {
                output.print("HTTP/1.0 400 Bad Request\r\n");
                output.print("\r\n"); // End of headers
                killThread();
                return;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void killThread() {
        try {
            //Sleep thread for .25 seconds, then return thread
            this.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Thread terminating for --> " + this.connection.getRemoteSocketAddress());
        PartialHTTP1Server.thread_count--;
        return;
    }

    //Attempts to send file from server to client
    private void sendFile(PrintStream output, String filename) {
        File file;
        try {
            //Creates file
            file = new File(".", filename.substring(1, filename.length()));
            FileInputStream fileInput = new FileInputStream(file);

            //byte array to store file data
            byte[] data = new byte[(int) file.length()];

            //Reads file into byte array && close FileInputStream
            fileInput.read(data);
            fileInput.close();

            //Sends 200 OK response header, sends file data, closes output stream, returns
            output.print("HTTP/1.0 200 OK\r\n");
            output.print("Content-Type: text/html\r\n");
            output.print("Content-Length: 3191\r\n");
            // output.print("Last-Modified: " + convertDate(file.lastModified()) + "\r\n");
            output.print("\r\n"); // End of headers
            output.write(data);
            output.close();
            return;

            /*
             - Response header not found: "Content-Type: text/html"
             - Response header not found: "Content-Length: 3191"
             - Response header not found: "Last-Modified: Wed, 15 Jul 2015 04:14:40 GMT"
             - Response header not found: "Content-Encoding: identity"
             - Response header not found: "Allow: GET, POST, HEAD"
             - Response header not found: "Expires: a future date"
             - Payload not found
             */

        }
        catch (IOException e) /* File not found exception */ {
            output.print("HTTP/1.0 404 Not Found\r\n");
            output.print("\r\n"); // End of headers
            output.close();
            return;
        }
    }

//    private Date convertDate (long time) throws ParseException {
//        Date returnDate = new Date();
//        SimpleDateFormat format = new SimpleDateFormat("dd M yyyy hh:mm:ss");
//        String temp = Long.toString(time);
//        format.parse(temp);
//
//
//        return returnDate;
//    }
//
}
