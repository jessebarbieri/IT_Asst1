/**
 @authors Jason Wrobel, Jesse Barbieri, Ethan Wang
 Internet Technology - 352
 Assignment 1 - HTTP/1.0 + MIME Type Support
 **/

import com.sun.source.tree.WhileLoopTree;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.Buffer;
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
            BufferedInputStream input = new BufferedInputStream(connection.getInputStream());

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


            StringBuilder sb = new StringBuilder();
            while(input.available() > 0) {
                char c = (char) input.read();
                sb.append(c);
            }

            String[] inputLines = sb.toString().split("\r\n");
            for (int i = 0; i < inputLines.length; i++) {
                inputLines[i] = decodeString(inputLines[i]);
            }


            //Reads first input line into a string
            String inLine = inputLines[0];

            //Tokenizer for first input line
            StringTokenizer tokenizer = new StringTokenizer(inLine);

            String ifModifiedDate = "";
            String from = "";

            //Checks for If-Modified-Since: 'date' / From: 'source'
            if (inputLines.length >= 2) {
                String inLine2 = inputLines[1];

                if (inLine2.indexOf("If-Modified-Since:") != -1) {
                    ifModifiedDate = inLine2.substring(19, inLine2.length());
                } else if (inLine2.indexOf("From:") != -1) {
                    from = inLine2.substring(6, inLine2.length());
                }
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

            //Checks for GET HEAD, implemented HTTP methods
            else if (method.equals("GET")|| method.equals("HEAD")) {
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
                if (fileURL.equals("/doc_root/top_secret.txt")) {
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

            else if (method.equals("POST")) {
                int currentIndex = 2;
                String userAgent, contentType, contentLength, scriptInput, inputLine;
                userAgent = "";
                contentType = "";
                contentLength = "";
                scriptInput = "";


                for (int i = currentIndex; i < inputLines.length; i++) {
                    inputLine = inputLines[i];
                    if (inputLine.indexOf("User-Agent:") != -1) {
                        userAgent = inputLine.substring(12, inputLine.length());
                    } else if (inputLine.indexOf("Content-Type") != -1) {
                        contentType = inputLine.substring(13, inputLine.length());
                    } else if (inputLine.indexOf("Content-Length") != -1) {
                        contentLength = inputLine.substring(15, inputLine.length());
                    } else if (!inputLine.equals("")) {
                        scriptInput = inputLines[i];
                    }
                }

                //Checks for missing Content Length field, sends HTTP/1.0 411 Length Required
                if (contentLength.equals("")) {
                    output.print("HTTP/1.0 411 Length Required\r\n");
                    output.print("\r\n"); // End of headers
                    killThread();
                    output.close();
                    input.close();
                    connection.close();
                    return;
                }

                //Checks for missing Content Type field, sends HTTP/1.0 500 Internal Server Error
                if (contentType.equals("")) {
                    output.print("HTTP/1.0 500 Internal Server Error\r\n");
                    output.print("\r\n"); // End of headers
                    killThread();
                    output.close();
                    input.close();
                    connection.close();
                    return;
                }

                //Checks for non-cgi file type, sends HTTP/1.0 405 Method Not Allowed
                if (!fileURL.substring(fileURL.length() - 3).equals("cgi")) {
                    output.print("HTTP/1.0 405 Method Not Allowed\r\n");
                    output.print("\r\n"); // End of headers
                    killThread();
                    output.close();
                    input.close();
                    connection.close();
                    return;
                }

                File cgiFile = new File(".", fileURL.substring(1, fileURL.length()));

                if (!cgiFile.canExecute()) {
                    output.print("HTTP/1.0 403 Forbidden\r\n");
                    output.print("\r\n"); // End of headers
                    killThread();
                    output.close();
                    input.close();
                    connection.close();
                    return;
                }

                if (Integer.parseInt(contentLength.trim()) == 0 && (fileURL.indexOf("env.cgi") == -1 && fileURL.indexOf("basic.cgi") == -1)) {
                    output.print("HTTP/1.0 204 No Content\r\n");
                    output.print("\r\n"); // End of headers
                    killThread();
                    output.close();
                    input.close();
                    connection.close();
                    return;
                }

                //URL of cgi script to be run by the ProcessBuilder
                String cmd = "." + fileURL;

                //Process builder to run the CGI scripts
                ProcessBuilder proc = new ProcessBuilder(cmd);
                proc.environment().put("CONTENT_LENGTH", contentLength.trim());
                proc.environment().put("HTTP_FROM", from);
                proc.environment().put("HTTP_USER_AGENT", userAgent);
                proc.environment().put("SCRIPT_NAME", fileURL);

                Process process = proc.start();

                //BufferedReader and StringBuilder to take output from the ProcessBuilder
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

                if (Integer.parseInt(contentLength.trim()) != 0) {
                    writer.write(scriptInput);
                    writer.flush();
                    writer.close();
                }

                System.out.println("OUTPUT FOR--->    URL: " + cmd + "    SCRIPT INPUT: " + scriptInput + "\n");
                String s = null;
                String out = "";
                while ((s = reader.readLine()) != null) {
                    System.out.println(s);
                    out += s;
                }
                System.out.println("\n\n");
                output.print("HTTP/1.0 200 OK\r\n");
                output.print("Content-Type: text/html" + "\r\n");
                output.print("Content-Length: " + out.length() + "\r\n");
                output.print("Allow: GET, POST, HEAD\r\n");
                output.print("Expires: Wed, 02 Oct 2024 01:37:39 GMT\r\n");
                output.print("\r\n"); // End of headers

                output.write(out.getBytes());

                killThread();
                output.close();
                input.close();
                connection.close();
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
            this.sleep(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

    /**
     * Decodes a string by removing the encoding characters
     * @param input string to be decoded
     * @return decoded string
     */
    private String decodeString(String input) {
        // Characters that require decoding ---> "!*'();:@$+,/?#[] "
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '!') {
                input = removeChar(i, input);
            }
        }
        return input;
    }

    /**
     * Removes a char from a string at a given index
     * @param index index to remove char from
     * @param input string to modify
     * @return input with char removed
     */
    private String removeChar(int index, String input) {
        String ret = "";
        for (int i = 0; i < input.length(); i++) {
            if (i != index) {
                ret += input.charAt(i);
            }
        }
        return ret;
    }
}