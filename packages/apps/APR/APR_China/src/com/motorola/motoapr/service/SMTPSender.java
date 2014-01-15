// ********************************************************** //
// PROJECT:     APR (Automatic Panic Recording)
// DESCRIPTION: 
//   The purpose of APR is to gather the panics in the device
//   and record statics about those panics to a centralized
//   server, for automated tracking of the quality of a program.
//   To achieve this, several types of messages are required to
//   be sent at particular intervals.  This package is responsible
//   for sending the data in the correct format, at the right 
//   intervals.
// ********************************************************** //
// Change History
// ********************************************************** //
// Author          Date      Tracking  Description
// ************** ********** ********  ********************** //
// Heegoo Kang     08/24/2009  1.0      Initial Version Supporting
//   W21677                              Email.
//
// ********************************************************** //
package com.motorola.motoapr.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import android.util.Log;

public class SMTPSender {
    static String TAG = "SMTPSender";
    public static void sendMail(String smtpServer, String sender, String recipient, String subject, String content) throws Exception {

        Socket socket = new Socket(smtpServer, 25);

        BufferedReader br = new BufferedReader(new InputStreamReader( socket.getInputStream() ), 1024 );
        PrintWriter pw = new PrintWriter( socket.getOutputStream(), true );
        APRDebug.APRLog ( TAG, "server connected." );

        if(readServerResponse(socket, br, pw, "220")==false){
            throw new Exception("SMTP server not found");
        }

        APRDebug.APRLog ( TAG, "send HELO." );
        pw.println("HELO mydomain.name");

        if(readServerResponse(socket, br, pw, "250")==false){
            throw new Exception("HELO failed");
        }

        APRDebug.APRLog ( TAG, "send MAIL FROM." );
        pw.println("MAIL FROM:<"+sender+">");

        if(readServerResponse(socket, br, pw, "250")==false){
            throw new Exception("MAIL FROM failed");
        }

        APRDebug.APRLog ( TAG, "send RCPT TO." );
        pw.println("RCPT TO:<"+recipient+">");

        if(readServerResponse(socket, br, pw, "250")==false){
            throw new Exception("RCPT TO failed");
        }

        APRDebug.APRLog ( TAG, "send DATA." );
        pw.println("DATA");

        if(readServerResponse(socket, br, pw, "354")==false){
            throw new Exception("DATA failed");
        }

        pw.println("From:<" + sender +">");
        pw.println("To:<" + recipient +">");
        pw.println("Subject:" + subject);
        pw.print("\r\n");
        pw.println(content);
        pw.print("\r\n.\r\n");
        pw.flush();
        APRDebug.APRLog ( TAG, "send content + '.'" );

        if(readServerResponse(socket, br, pw, "250")==false){
            throw new Exception("failed");
        }

        pw.println("quit");
        APRDebug.APRLog ( TAG, "server connection close." );

        pw.close();
        br.close();
        socket.close();
    }
    private static boolean readServerResponse(Socket socket, BufferedReader br,
            PrintWriter pw, String response) throws Exception {
        String line=br.readLine();
        if (line == null || !line.startsWith(response)) {
            pw.close();
            br.close();
            socket.close();
            return false;
        }
        APRDebug.APRLog ( TAG, "S:"+line );
        return true;
    }

}
