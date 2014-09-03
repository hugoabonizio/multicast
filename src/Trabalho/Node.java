package Trabalho;

import Trabalho.Message.Action;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Node {

    private String me;
    private ServerSocket server;
    private int port;
    public List<Connection> connections;
    private final Map<String, Long> pings = new HashMap<>();
    private final Map<String, Long> pingResults = new HashMap<>();

    private Tela tela;

    public Node(Tela tela, String fullAddress, int port) {
        try {
            connections = new ArrayList<>();
            this.tela = tela;
            this.port = port;
            System.out.println("porta: " + port);
            server = new ServerSocket(port);

            me = Inet4Address.getLocalHost().getHostAddress() + ":" + port;

            tela.setYouAre(me);
            if (fullAddress != null) {
                String parentAddress = fullAddress.split(":")[0];
                int parentPort = new Integer(fullAddress.split(":")[1]);
                ask(parentAddress, parentPort);
            } else {
                tela.setConnectedTo("root");
            }

            new Thread(new WaitConnection()).start();
        } catch (IOException ex) {
            System.out.println("deu pobrema");
        }
    }
    
    private class WaitConnection implements Runnable {

        private Socket socket;

        @Override
        public void run() {
            Connection connection;
            while (true) {
                try {
                    socket = server.accept();

                    connection = new Connection();
                    connection.setInput(new ObjectInputStream(socket.getInputStream()));
                    connection.setOutput(new ObjectOutputStream(socket.getOutputStream()));
                    connection.setPort(socket.getPort());
                    connection.setSocket(socket);

                    new Thread(new ListenerSocket(tela, connections, connection, port)).start();
                } catch (IOException ex) {
                    Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

    }

    private void ask(String parentAddress, int parentPort) {

        Message message = new Message();
        message.setAction(Action.ASK);
        message.setSrc(me);
        message.setText(me);

        Socket socket;
        try {
            socket = new Socket(parentAddress, parentPort);
            Connection connection = new Connection();
            connection.setOutput(new ObjectOutputStream(socket.getOutputStream()));
            connection.getOutput().writeObject(message);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //while (true) {
                            Thread.sleep(2000);
                            //if (pingResults.size() > 0) {
                                connectToSomeone();
                                return;
                            //}
                        //}
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }).start();
        } catch (IOException ex) {
            Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public void initPing(String address) {
        long startTime;
        startTime = System.nanoTime();
        
        pings.put(address, startTime);

        Message message = new Message();
        message.setAction(Action.PING);
        message.setSrc(me);
        message.setText("ma oe");
        Connection.send(address, message);
        
        /*String parentAddress = address.split(":")[0];
        int parentPort = new Integer(address.split(":")[1]);
        connect(parentAddress, parentPort);*/
    }
    
    public void endPing(String address) {
        tela.receive("entrou no endping");
        long totalTime = System.nanoTime() - pings.get(address);
        pingResults.put(address, totalTime);
    }
    
    public void connectToSomeone() {
        tela.receive("connectar to someone");
        
        long lowest = -1;
        long startTime, totalTime;
        String result = "";
        
        String resultAddress = "";
        
        for (String candidate: tela.getCandidates()) {
            startTime = System.nanoTime();
            Connection.ping(me, candidate);
            totalTime = System.nanoTime() - startTime;
            if (lowest < 0 || lowest > totalTime) {
                lowest = totalTime;
                result = candidate;
            }
        }
        
        tela.receive("conectar a " + result);
        String parentAddress = result.split(":")[0];
        int parentPort = new Integer(result.split(":")[1]);
        connect(parentAddress, parentPort);
    }

    private void connect(String parentAddress, int parentPort) {
        try {
            System.out.println("Porta:" + port);
            Socket socket = new Socket(parentAddress, parentPort);

            Connection connection = new Connection();
            connection.setOutput(new ObjectOutputStream(socket.getOutputStream()));
            connection.setInput(new ObjectInputStream(socket.getInputStream()));
            connection.setPort(socket.getPort());
            connection.setSocket(socket);
            connection.setIp(parentAddress + ":" + parentPort);
            connections.add(connection);

            Message message = new Message();
            message.setAction(Action.CONNECT);
            message.setSrc(Inet4Address.getLocalHost().getHostAddress() + ":" + port);
            connection.getOutput().writeObject(message);

            tela.setConnectedTo(parentAddress + ":" + parentPort);
            tela.addElement(parentAddress + ":" + parentPort);

            new Thread(new ListenerSocket(tela, connections, connection, port)).start();
        } catch (IOException ex) {
            Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void send(Message message) {
        for (Connection c : connections) {
            //System.out.println(c);
            try {
                c.getOutput().writeObject(message);
            } catch (IOException ex) {
                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
