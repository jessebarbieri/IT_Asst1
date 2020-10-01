/**
 @authors Jason Wrobel, Jesse Barbieri, Ethan Wang
 Internet Technology - 352
 Assignment 1 - HTTP/1.0 + MIME Type Support
 **/

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PartialHTTP1Threads extends Thread{

    Socket connection;

    /**
     * Creates new thread object to be executed from Server class
     * @param conn from client
     */
    public PartialHTTP1Threads(Socket conn) {
        this.connection = conn;
    }

    /**
     * Handles a single request from a client
     */
    public void run() {
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
                output.close();
                input.close();
                connection.close();
                return;
            }

            //Reads first input line into a string
            String inLine = input.readLine();

            //Tokenizer for first input line
            StringTokenizer tokenizer = new StringTokenizer(inLine);

            //Checks for If-Modified-Since: 'date'
            String ifModified = input.readLine();
            String ifModifiedDate = "";
            if (ifModified.indexOf("If-Modified-Since:") != -1) {
                ifModifiedDate = ifModified.substring(19 , ifModified.length());
            }

            //Holds HTTPMethod and file directory from first input line
            String method = tokenizer.nextToken();
            String fileURL = tokenizer.nextToken();

            //Gets HTTP version if it is present
            String version = null;
            if (tokenizer.hasMoreTokens()) {
                version = tokenizer.nextToken();
            }

            //Sends 400 Bad Request if HTTP version is null
            if (version == null || tokenizer.hasMoreTokens()) {
                output.print("HTTP/1.0 400 Bad Request\r\n");
                output.print("\r\n"); // End of headers
                killThread();
                output.close();
                input.close();
                connection.close();
                return;
            }

            //Sends 505 HTTP Version Not Supported if version is not HTTP/1.0
            if (!version.equals("HTTP/1.0")) {
                output.print("HTTP/1.0 505 HTTP Version Not Supported\r\n");
                output.print("\r\n"); // End of headers
                killThread();
                output.close();
                input.close();
                connection.close();
                return;
            }


            //Checks for methods that are not implemented, PUT DELETE LINK UNLINK
            //Sends 501 Not Implemented header if method is not implemented
            if(method.equals("PUT") || method.equals("DELETE") || method.equals("LINK") || method.equals("UNLINK")) {
                output.print("HTTP/1.0 501 Not Implemented\r\n");
                output.print("\r\n"); // End of headers
                killThread();
                output.close();
                input.close();
                connection.close();
                return;
            }

            //Checks for GET POST HEAD, implemented HTTP methods
            else if (method.equals("GET") || method.equals("POST") || method.equals("HEAD")) {
                //Checks for invalid file directory, sends 400 Bad Request if directory is invalid format
                if (fileURL.charAt(0) != '/') {
                    output.print("HTTP/1.0 400 Bad Request\r\n");
                    output.print("\r\n"); // End of headers
                    killThread();
                    output.close();
                    input.close();
                    connection.close();
                    return;
                }
                //Checks for invalid file directory, sends 400 Bad Request if directory is invalid format
                if (fileURL.indexOf("../") != -1) {
                    output.print("HTTP/1.0 400 Bad Request\r\n");
                    output.print("\r\n"); // End of headers
                    killThread();
                    output.close();
                    input.close();
                    connection.close();
                    return;
                }

                //Checks for forbidden file top_secret.txt, sends 403 Forbidden
                if (fileURL.equals("/top_secret.txt")) {
                    output.print("HTTP/1.0 403 Forbidden\r\n");
                    output.print("\r\n"); // End of headers
                    killThread();
                    output.close();
                    input.close();
                    connection.close();
                    return;
                }

                //Passes control to the sendFile method, which handles other HTTP headers/sending byte data
                sendFile(output, fileURL, ifModifiedDate, method);
                output.close();
                input.close();
                connection.close();
                killThread();
                return;

            }

            //Upon failing all method checks, sends 400 Bad Request
            else {
                output.print("HTTP/1.0 400 Bad Request\r\n");
                output.print("\r\n"); // End of headers
                killThread();
                output.close();
                input.close();
                connection.close();
                return;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles closing down of thread
     */
    private void killThread() {
        try {
            //Sleep thread for .25 seconds, then return thread
            this.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Keeps track of number of active threads in the server class
        PartialHTTP1Server.thread_count--;
        return;
    }

    /**
     *
     * @param output stream from socket
     * @param filename directory of file from client's request
     * @param ifModifiedDate conditional date if present in client's request
     * @param method command from client's request (GET, HEAD, or POST)
     */
    private void sendFile(PrintStream output, String filename, String ifModifiedDate, String method) {
        //File for client's requested file
        File file;

        try {
            //Instantiates file and gets inputstream from file
            file = new File(".", filename.substring(1, filename.length()));
            FileInputStream fileInput = new FileInputStream(file);

            //Gets file's mimeType and stores in mimeType -- Defaults to octet-stream if mime type is not supported
            Path path = file.toPath();
            String mimeType = Files.probeContentType(path);
            if (mimeType == null) { mimeType = ""; }
            mimeType = formatMimeType(mimeType);

            //Byte array to store file data
            byte[] data = new byte[(int) file.length()];

            //Reads file into byte array and closes FileInputStream
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

            //Boolean that stores whether date is after lastModified date of file
            boolean dateAfter = false;

            //If request has "If-Modified-Since" and file has lastModified date, HEAD method ignores "If-Modified-Since"
            //indexOf("GMT") checks for properly formatted dates
            if (!ifModifiedDate.equals("") && !lastModified.equals("") && ifModifiedDate.indexOf("GMT") != -1 && !method.equals("HEAD")) {
                try {
                    //Compares dates
                    dateAfter = dateIsAfter(lastModified, ifModifiedDate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //When last modified date is before If=Modified-Since date, sends 304 Not Modified
                if (!dateAfter) {
                    output.print("HTTP/1.0 304 Not Modified\r\n");
                    output.print("Expires: Sat, 21 Jul 2021 11:00:00 GMT\r\n");
                    output.print("\r\n"); // End of headers
                    return;
                }
            }

            //Sends 200 OK response headers, sends file data, closes output stream, returns
            output.print("HTTP/1.0 200 OK\r\n");
            output.print("Content-Type: " + mimeType + "\r\n");
            output.print("Content-Length: " + file.length() + "\r\n");
            output.print("Last-Modified: " + lastModified + "\r\n");
            output.print("Allow: GET, POST, HEAD\r\n");
            output.print("Content-Encoding: identity\r\n");
            output.print("Expires: Wed, 02 Oct 2024 01:37:39 GMT\r\n");
            output.print("\r\n"); // End of headers

            //Sends file data if method is GET or POST, as HEAD does not send file data
            if (!method.equals("HEAD"))
            {
                output.write(data);
            }

            //Closes output stream and returns from sendFile method
            output.close();
            return;

        }
        //Sends 404 Not Found header if file is not found
        catch (IOException e) {
            e.printStackTrace();
            output.print("HTTP/1.0 404 Not Found\r\n");
            output.print("\r\n"); // End of headers
            output.close();
            killThread();
            return;
        }
    }

    /**
     *
     * @param time file's last modified date
     * @return Properly formatted date for HTTP headers
     * @throws ParseException
     */
    private String convertDate (long time) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));

        return format.format(calendar.getTime());
    }

    /**
     *
     * @param lastModifiedDate files last modified date
     * @param ifModifiedAfter "If=Modified-Since" date from client's request
     * @return true if lastModifiedDate is after ifModifiedAfter, false otherwise
     * @throws ParseException
     */
    private boolean dateIsAfter(String lastModifiedDate, String ifModifiedAfter) throws ParseException {
        //Removes TimeZone, extra spaces, and day of week from date strings
        lastModifiedDate = lastModifiedDate.replace("GMT", "").trim();
        ifModifiedAfter = ifModifiedAfter.replace("GMT", "").trim();
        lastModifiedDate = lastModifiedDate.substring(5, lastModifiedDate.length());
        ifModifiedAfter = ifModifiedAfter.substring(5, ifModifiedAfter.length());

        //Converts string dates back into long times
        SimpleDateFormat format = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
        long lastMod = format.parse(lastModifiedDate).getTime();
        long ifModAfter = format.parse(ifModifiedAfter).getTime();

        //Returns comparison of dates, a greater number will be a more recent date
        return (lastMod > ifModAfter);
    }

    /**
     *
     * @param mimeType MimeType of file
     * @return supported mimeType of file
     */
    private String formatMimeType(String mimeType) {
        String returnMime = mimeType;
        //If mimetype is not a supported type, defaults to an octet-stream (Raw byte data)
        if (!(mimeType.equals("text/html")) && !(mimeType.equals("text/plain")) && !(mimeType.equals("image/gif")) &&
                !(mimeType.equals("image/jpeg")) && !(mimeType.equals("image/png")) && !(mimeType.equals("application/pdf")) &&
                !(mimeType.equals("application/x-gzip")) && !(mimeType.equals("application/zip"))) {
            returnMime = "application/octet-stream";
        }
        return returnMime;
    }

}