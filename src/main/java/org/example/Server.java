package org.example;

import org.apache.commons.codec.binary.Base32;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Properties;

class ClientConnection extends Thread {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private Operations_List oper;

//    Runnable task_send = new Runnable() {
//        public void run() {
//            System.out.println("Hello, World!");
//        }
//    };
//
//    Runnable task_read = new Runnable() {
//        public void run() {
//            System.out.println("Hello, World!");
//        }
//    };

    public ClientConnection(Socket socket) throws Exception {
        System.out.println("новое соединение");
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        while (true)
        {
            String connect = in.readLine();

            if(connect.isEmpty())
            {
                downService();
                break;
            }


            JSONObject json = new JSONObject(connect);
            System.out.println(json);
            this.oper = new Operations_List("localhost","2517Pass", json);
            var out_result = this.oper.processing(json.getString("OPERATION"));
            this.send(out_result);

            try {
                JSONObject json_check = new JSONObject(out_result);
                if(json_check.getString("EXIST").equals("1"))
                {
                    System.out.println("пользователь подтверждён");
                    break;
                }
            }
            catch (Exception e)
            {
                System.out.println("error");
            }


        }


//        Thread thread_send = new Thread(task_send);
//        Thread thread_read = new Thread(task_read);
//
//        thread_send.start();
//        thread_read.start();

        start();
    }

    @Override
    public void run() {
        String word;
        try {

            while (true) {
                word = in.readLine();
                if(word.isEmpty())
                {
                    downService();
                    break;
                }
                System.out.println(word);
                for (ClientConnection vr : Server.serverList) {
                    if(vr.equals(this)) continue;
                    vr.send(word);
                }
            }

        } catch (Exception e) {
            this.downService();
        }
    }

    private void send(String msg) {
        try {
            out.write(msg + "\n");
            out.flush();
        } catch (IOException ignored) {}
    }

    private void downService() {
        try {
            if(!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
                for (ClientConnection vr : Server.serverList) {
                    if(vr.equals(this)) vr.interrupt();
                    Server.serverList.remove(this);
                }
                System.out.println("соединение разорвано \n");
            }
        } catch (IOException ignored)
        {
        }
    }



}


public class Server {

    public static final int PORT = 4013;
    public static LinkedList<ClientConnection> serverList = new LinkedList<>();

    public static void main(String[] args) throws IOException {

//        Properties prop = new Properties();
//        String fileName = "./server.config";
//        try (FileInputStream fis = new FileInputStream(fileName)) {
//            prop.load(fis);
//        } catch (IOException ex) {}

        ServerSocket server = new ServerSocket(PORT);
        try {
            while (true) {
                Socket socket = server.accept();
                try {
                    serverList.add(new ClientConnection(socket));
                } catch (Exception e) {
                    socket.close();
                }
            }
        } finally {
            server.close();
        }
    }
}