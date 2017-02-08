/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverd;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;



public class SocketThread implements Runnable {

    Socket socket;
    boolean valid=true;
    MainForm main;
    DataInputStream dis;
    StringTokenizer st;
    String client, filesharing_username;
    //Hashtable ht = new Hashtable();
    OutputStream sendFile;

    private final int BUFFER_SIZE = 100;
    Map<String,Integer> files = new HashMap<String, Integer>();

    public SocketThread(Socket socket, MainForm main) {
        this.main = main;
        this.socket = socket;   //server socket

        try {
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            main.appendMessage("[SocketThreadIOException]: " + e.getMessage());
        }
        
    }

    @Override
    public void run() {
        try {
            while (true) {
                /**
                 * Get Client Data *
                 */
                String data = dis.readUTF();
                st = new StringTokenizer(data);
                String CMD = st.nextToken();

                /**
                 * Check CMD *
                 */
                switch (CMD) {
                    case "CMD_JOIN":
                        /**
                         * CMD_JOIN [clientUsername] *
                         */
                        String clientUsername = st.nextToken();
                        //System.out.println(st.nextToken());
                        String fildir = st.nextToken();
                        String file = st.nextToken();
                        // create a file that is really a directory
                        StringTokenizer fl = new StringTokenizer(file, "/");         //take files from a specific directory
                        String[] temp = new String[fl.countTokens()];
                        int i = 0;
                        while (fl.hasMoreTokens()) {                             
                            files.put(fl.nextToken(), 0);
                        }                       
                        for (i = 0; i < files.size(); i++) {
                            System.out.println(files.get(i));
                        }
                        client = clientUsername;
                        main.setClientList(clientUsername);
                        main.setClientFileList(clientUsername, files);
                        main.setSocketList(socket);
                        main.setClientFileDir(fildir);
                        main.appendMessage("[Client]: " + clientUsername + " joins the Peer to Peer network!");
                        break;

                    case "CMD_QUERY":   //reponds to the querier with location of the file
                        /**
                         * CMD_QUERY [clientUsername] [filename] *
                         */
                        String clientname = st.nextToken();
                        String filename = st.nextToken();
                        String host = main.findFile(filename);
                        Socket tsoc1 = main.getClientList(clientname);
                        try {
                            DataOutputStream dos1 = new DataOutputStream(tsoc1.getOutputStream());
                            String content1 = host;
                            content1 = "CMD_QUERY_RES " + content1;
                            dos1.writeUTF(content1);                           
                            main.appendMessage("[Message]: File found with client " + host);
                        } catch (IOException e) {
                            main.appendMessage("[IOException]: Unable to respond to " + clientname);
                        }
                        break;

                    case "CMD_ASK_FILE":                                 
                        String from = st.nextToken();
                        String sendTo = st.nextToken();
                        String msg = st.nextToken();
                        String dir = main.getClientFileDir(sendTo);
                        Socket tsoc = main.getClientList(sendTo);
                        try {
                            DataOutputStream doos = new DataOutputStream(tsoc.getOutputStream());                         
                            String content = from + " " + msg + " " + dir;
                            doos.writeUTF("CMD_MESSAGE " + content);
                            main.appendMessage("[Ask File]: From " + from + " To " + sendTo + " : " + dir + "/" + msg);
                        } catch (IOException e) {
                            main.appendMessage("[IOException]: Unable to ask file from " + sendTo);
                        }
                        break;

                    case "CMD_SHARINGSOCKET":
                        main.appendMessage("CMD_SHARINGSOCKET : Client stablish a socket connection for file sharing...");
                        String file_sharing_username = st.nextToken();
                        filesharing_username = file_sharing_username;
                        main.setClientFileSharingUsername(file_sharing_username);
                        main.setClientFileSharingSocket(socket);
                        main.appendMessage("CMD_SHARINGSOCKET : Username: " + file_sharing_username);
                        main.appendMessage("CMD_SHARINGSOCKET : File sharing is now open");
                        break;

                    case "CMD_SENDFILE":
                        main.appendMessage("CMD_SENDFILE : Client sending a file...");
                        /*
                        Format: CMD_SENDFILE [Filename] [Size] [Recipient] [Consignee]  from: Sender Format
                        Format: CMD_SENDFILE [Filename] [Size] [Consignee] to Receiver Format
                         */
                        String file_name = st.nextToken();
                        String filesize = st.nextToken();
                        String sendto = st.nextToken();
                        String consignee = st.nextToken();
                        main.appendMessage("CMD_SENDFILE : From: " + consignee);
                        main.appendMessage("CMD_SENDFILE : To: " + sendto);  
                        /**
                         * Get the client Socket *
                         */
                        main.appendMessage("CMD_SENDFILE : preparing connections..");
                        Socket cSock = main.getClientFileSharingSocket(sendto); 
                        /* Consignee Socket  */
                        /*   Now Check if the consignee socket exists.   */
                        if (cSock != null) {
                            /* Exists   */
                            try {
                                main.appendMessage("CMD_SENDFILE : Connected..!");                               
                                main.appendMessage("CMD_SENDFILE : Sending file to client...");
                                DataOutputStream cDos = new DataOutputStream(cSock.getOutputStream());
                                cDos.writeUTF("CMD_SENDFILE " + file_name + " " + filesize + " " + consignee);                              
                                //JOptionPane.showMessageDialog(null, "Stage Check 1");
                                main.appendMessage("CHECK_POINT: Stage 1");                                
                                InputStream input = socket.getInputStream();
                                    sendFile = cSock.getOutputStream();
                                    byte[] buffer = new byte[BUFFER_SIZE];
                                    int cnt;
                                    while ((cnt = input.read(buffer)) != -1) {                                        
                                        sendFile.write(buffer, 0, cnt);
                                    }
                                input.close();
                                sendFile.flush();
                                sendFile.close();             //Output Stream Close                              
                                /**
                                 * Remove client list *
                                 */
                                //JOptionPane.showMessageDialog(null, "Stage Check 2");                               
                                main.removeClientFileSharing(sendto);
                                main.removeClientFileSharing(consignee);
                                main.appendMessage("CMD_SENDFILE : File was sent to client...");
                            } catch (IOException e) {
                                main.appendMessage("[CMD_SENDFILE]: " + e.getMessage());
                            }
                        } else {
                            /*   Not exists, return error  */
                            /*   FORMAT: CMD_SENDFILEERROR  */
                            main.removeClientFileSharing(consignee);
                            main.appendMessage("CMD_SENDFILE : Client '" + sendto + "' was not found.!");
                            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                            dos.writeUTF("CMD_SENDFILEERROR " + "Client '" + sendto + "' was not found, File Sharing will exit.");
                        }
                        break;

                    case "CMD_SENDFILERESPONSE":
                        /*
                        Format: CMD_SENDFILERESPONSE [username] [filename] [Message]
                         */
                        String sender = st.nextToken(); // get the sender username
                        String filname = st.nextToken();
                        String rMsg = st.nextToken(); // get the success message                       
                        if(Objects.equals("Success", rMsg))
                        {
                            main.incrementdownloadsuccesscount(sender, filname);
                        }
                        break;

//                    case "CMD_SEND_FILE_ERROR":  // Format:  CMD_SEND_FILE_ERROR [receiver] [Message]
//                        String eReceiver = st.nextToken();
//                        String eMsg = "";
//                        while (st.hasMoreTokens()) {
//                            eMsg = eMsg + " " + st.nextToken();
//                        }
//                        try {
//                            /*  Send Error to the File Sharing host  */
//                            Socket eSock = main.getClientFileSharingSocket(eReceiver); // get the file sharing host socket for connection
//                            DataOutputStream eDos = new DataOutputStream(eSock.getOutputStream());
//                            //  Format:  CMD_RECEIVE_FILE_ERROR [Message]
//                            eDos.writeUTF("CMD_RECEIVE_FILE_ERROR " + eMsg);
//                        } catch (IOException e) {
//                            main.appendMessage("[CMD_RECEIVE_FILE_ERROR]: " + e.getMessage());
//                        }
//                        break;

                    default:
                        main.appendMessage("[CMDException]: Unknown Command " + CMD);
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println(client);
            System.out.println("File Sharing: " +filesharing_username);
            //main.removeFromTheList(client);
            if(filesharing_username != null){
                main.removeClientFileSharing(filesharing_username);
            }
            main.appendMessage("[SocketThread]:{Error}: Client connection closed..!");
            System.out.println("\n\n");
            System.out.println(e.getCause());
        }
    }
}
