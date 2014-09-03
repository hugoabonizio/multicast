package Trabalho;

import Trabalho.Message.Action;
import java.io.EOFException;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ListenerSocket implements Runnable {

    private final Tela tela;
    private final List<Connection> connections;
    private final Connection connection;
    private final int port;

    public ListenerSocket(Tela tela, List<Connection> connections, Connection connection, int port) {
        this.tela = tela;
        this.connections = connections;
        this.connection = connection;
        this.port = port;
    }

    @Override
    public void run() {
        Message message, answer;
        try {
            String me = Inet4Address.getLocalHost().getHostAddress() + ":" + port;
            String source;

            while ((message = (Message) connection.getInput().readObject()) != null) {
                Action action = message.getAction();

                if (action.equals(Action.CONNECT)) {
                    // CONNECT
                    System.out.println(message.getSrc());
                    connection.setIp(message.getSrc());
                    connections.add(connection);
                    
                    tela.addElement(message.getSrc());
                } else if (action.equals(Action.MESSAGE)) {
                    // MESSAGE
                    System.out.println(message.getAction() + ", " + message.getSrc());
                    tela.receive(message.getText());

                    source = message.getSrc();
                    message.setSrc(me);
                    for (Connection c : connections) {
                        //System.out.println(me + ".equals " + c.getIp());
                        if (!source.equals(c.getIp())) {
                            c.getOutput().writeObject(message);
                        }
                    }
                } else if (action.equals(Action.PING)) {
                    // PING
                    
                    Random threadRandom = new Random();
                    Integer timeRandom = threadRandom.nextInt(1500) + 1;
                    tela.receive("random: " + timeRandom.toString());
                    
                    
                    Thread.sleep(timeRandom);
                    
                    message.setAction(Action.PING_BACK);
                    message.setSrc(me);
                    tela.receive("envia ping_back" + message.getSrc());
                    connection.getOutput().writeObject(message);
                    
                } else if (action.equals(Action.PING_BACK)) {
                    // PING_BACK
                    
                    tela.receive("PING_BACK source: " + message.getSrc());
                    //tela.getNode().endPing(message.getSrc());
                    
                } else if (action.equals(Action.ASK)) {
                    // ASK
                    
                    tela.receive("recebi um ask.fm de " + message.getText());
                    
                    answer = new Message();
                    answer.setSrc(me);
                    answer.setAction(Action.IP);
                    Connection.send(message.getText(), answer);
                    
                    source = message.getSrc();
                    message.setSrc(me);
                    for (Connection c : connections) {
                        tela.receive("passando para o " + c.getIp());
                        if (!source.equals(c.getIp())) {
                            c.getOutput().writeObject(message);
                        }
                    }
                    
                    //connection.getOutput().writeObject(message);
                    
                } else if (action.equals(Action.IP)) {
                    // IP
                    
                    //tela.getNode().initPing(message.getSrc());
                    
                    tela.receive("recebi esse doido: " + message.getSrc());
                    tela.getCandidates().add(message.getSrc());
                }
            }
        } catch (Exception ex) {
            //Logger.getLogger(ListenerSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
