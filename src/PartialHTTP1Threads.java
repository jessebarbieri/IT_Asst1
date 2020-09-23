import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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

            //Waits 5 seconds for request from client
            long startTime = System.currentTimeMillis(); //fetch starting time
            while (connection.getInputStream().available() == 0 && System.currentTimeMillis() < startTime + 5000) {
                continue;
            }

            //If no request is received, send 408 Request Timeout and exit thread
            if(connection.getInputStream().available() == 0) {
                output.print("HTTP/1.0 408 Request Timeout\r\n");
                output.print("\r\n"); // End of headers
                killThread();
                return;
            }

            //Tokenizer to get method / other data
            String inLine = input.readLine();

            //Checks for If-Modified-Since: 'date'
            String ifModified = input.readLine();
            String ifModifiedDate = "";
            if (ifModified.indexOf("If-Modified-Since:") != -1) {
                ifModifiedDate = ifModified.substring(19 , ifModified.length());
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

                sendFile(output, fileURL, ifModifiedDate);
                killThread();
                return;

            }
            else if (method.equals("HEAD")) {
                output.print("HTTP/1.0 400 Bad Request\r\n");
                output.print("\r\n"); // End of headers
                killThread();
                return;
            }
            else if (method.equals("POST")) {
                //HANDLE POST
                output.print("HTTP/1.0 400 Bad Request\r\n");
                output.print("\r\n"); // End of headers
                killThread();
                return;

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
        //System.out.println("Thread terminating for --> " + this.connection.getRemoteSocketAddress());
        PartialHTTP1Server.thread_count--;
        return;
    }

    //Attempts to send file from server to client
    private void sendFile(PrintStream output, String filename, String ifModifiedDate) {
        File file;
        try {
            //Creates file
            file = new File(".", filename.substring(1, filename.length()));
            FileInputStream fileInput = new FileInputStream(file);

            //Handles mimeType
            Path path = file.toPath();
            String mimeType = Files.probeContentType(path);
            if (mimeType == null) { mimeType = ""; }
            mimeType = formatMimeType(mimeType);

            //byte array to store file data
            byte[] data = new byte[(int) file.length()];

            //Reads file into byte array && close FileInputStream
            fileInput.read(data);
            fileInput.close();

            //Last-Modified response line
            String lastModified = "";
            try{
                lastModified = convertDate(file.lastModified());
            }
            catch (Exception e){
                e.printStackTrace();
            }

            boolean dateAfter = false;
            if (!ifModifiedDate.equals("") && !lastModified.equals("") && ifModifiedDate.indexOf("GMT") != -1) {
                try {
                    dateAfter = dateIsAfter(lastModified, ifModifiedDate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!dateAfter) {
                    output.print("HTTP/1.0 304 Not Modified\r\n");
                    output.print("\r\n");
                    killThread();
                    return;
                }
            }

            //Sends 200 OK response headers, sends file data, closes output stream, returns
            output.print("HTTP/1.0 200 OK\r\n");
            output.print("Content-Type: " + mimeType + "\r\n"); // TODO - add mime type support
            output.print("Content-Length: " + file.length() + "\r\n");
            output.print("Last-Modified: " + lastModified + "\r\n");

            //Headers for all 200 OK responses
            output.print("Allow: GET, POST, HEAD\r\n");
            output.print("Content-Encoding: identity\r\n");
            output.print("Expires: Wed, 02 Oct 2024 01:37:39 GMT\r\n");

            output.print("\r\n"); // End of headers
            output.write(data);
            output.close();
            return;

            /*
             - Response header not found: "Content-Length: 3191"
             - Response header not found: "Content-Encoding: identity"
             - Response header not found: "Allow: GET, POST, HEAD"
             - Response header not found: "Expires: a future date"
             - Payload not found
             */

        }
        catch (IOException e) /* File not found exception */ {
            e.printStackTrace();
            output.print("HTTP/1.0 404 Not Found\r\n");
            output.print("\r\n"); // End of headers
            output.close();
            return;
        }
    }

    private String convertDate (long time) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));

        return format.format(calendar.getTime());
    }

    private boolean dateIsAfter(String lastModifiedDate, String ifModifiedAfter) throws ParseException {
        lastModifiedDate = lastModifiedDate.replace("GMT", "").trim();
        ifModifiedAfter = ifModifiedAfter.replace("GMT", "").trim();
        lastModifiedDate = lastModifiedDate.substring(5, lastModifiedDate.length());
        ifModifiedAfter = ifModifiedAfter.substring(5, ifModifiedAfter.length());

        SimpleDateFormat format = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
        long lastMod = format.parse(lastModifiedDate).getTime();
        long ifModAfter = format.parse(ifModifiedAfter).getTime();
        return (lastMod > ifModAfter);
    }

    private String formatMimeType(String mimeType) {
        String returnMime = mimeType;
        if (!(mimeType.equals("text/html")) && !(mimeType.equals("text/plain")) && !(mimeType.equals("image/gif")) &&
                        !(mimeType.equals("image/jpeg")) && !(mimeType.equals("image/png")) && !(mimeType.equals("application/pdf")) &&
                        !(mimeType.equals("application/x-gzip")) && !(mimeType.equals("application/zip"))) {
            returnMime = "application/octet-stream";
        }
        return returnMime;
    }

}