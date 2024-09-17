//Emir Adar
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the main class that holds the application
 * */
public class Canvas extends JFrame {
    private Paper paper;

    /**
     * This is the main method
     * @throws UnknownHostException if the host cannot be connected to
     * */
    public static void main(String[] args) throws UnknownHostException {

        // setting initial values
        String host = "localhost";
        int sendPort = 2000;
        int receivePort = 2001;

        // checking the arguments given while starting the program
        if (args.length > 0){
            host = args[0];
            if (args.length > 1){
                sendPort = Integer.parseInt(args[1]);
                if(args.length > 2){
                    receivePort = Integer.parseInt(args[2]);
                }
            }
        }
        new Canvas(host, sendPort, receivePort);
    }

    /**
     * This is the constructor for this class
     * @param host
     * @param sendPort
     * @param receivePort
     * @throws UnknownHostException if the host cannot be connected to
     * */
    public Canvas(String host, int sendPort, int receivePort) throws UnknownHostException {
        // setting up the UI and adding a paper component
        super("Host: " + host + " | Sending from: " + sendPort + " | Receiving from: " + receivePort);
        paper = new Paper(host, sendPort, receivePort);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().add(paper, BorderLayout.CENTER);
        setSize(640, 480);
        setVisible(true);
    }
}

/**
 * This is the class that handles the content of the drawing board
 * */
class Paper extends JPanel implements Runnable {

    // declaring and initializing variables
    private Thread thread = new Thread(this);
    private Set<Point> points = ConcurrentHashMap.newKeySet();
    private DatagramSocket sendSocket;
    private DatagramSocket receiveSocket;
    private InetAddress address;
    private int sendPort;
    private int diameter = 2;
    private Color color = Color.BLACK;

    /**
     * This is the constructor for this class
     * @param host
     * @param sendPort
     * @param receivePort
     * @throws UnknownHostException if the host cannot be connected to
     * */
    public Paper(String host, int sendPort, int receivePort) throws UnknownHostException {

        // setting up the UI
        setBackground(Color.white);
        Box buttons = Box.createHorizontalBox();
        add(buttons, BorderLayout.SOUTH);
        addMouseListener(new MousePressed());
        addMouseMotionListener(new MouseDragged());

        // trying to open up sockets and starting the thread
        try {
            sendSocket = new DatagramSocket();
            receiveSocket = new DatagramSocket(receivePort);
            address = InetAddress.getByName(host);
            this.sendPort = sendPort;
            new Thread(this::run).start();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        thread.start();
    }

    /**
     * This method paints the points
     * */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(color);
        for (Point p : points) {
            g.fillOval(p.x, p.y, diameter, diameter);
        }
    }

    // adds a point to the set, paints it on the panel and sends coordinates to the given socket
    private void addPoint(Point p) {
        points.add(p);
        repaint();
        sendCoordinates(p.x, p.y);
    }

    // breaks down the coordinates to a packet to send to the given socket
    private void sendCoordinates(int x, int y) {

        try {
            byte[] buf = (x + " " + y).getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, sendPort);
            sendSocket.send(packet);
        } catch (IOException e) {
            System.out.println("Could not send the data: " + e.getMessage());
        }
    }

    // deciphers the incoming packets from the given socket
    private void receiveCoordinates(DatagramSocket receiveSocket){

        byte[] buf = new byte[256];

        try{
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            receiveSocket.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());
            String[] coordinates = received.split(" ");
            if(coordinates.length == 2){
                int x = Integer.parseInt(coordinates[0]);
                int y = Integer.parseInt(coordinates[1]);
                SwingUtilities.invokeLater(()->addPoint(new Point(x, y)));
            }
        } catch (IOException e) {
            System.out.println("Could not receive the data");
        }
    }

    @Override
    public void run() {
        while(true)
            receiveCoordinates(receiveSocket);
    }

    class MousePressed extends MouseAdapter {
        public void mousePressed(MouseEvent me) {addPoint(me.getPoint());}
    }

    class MouseDragged extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent me) {addPoint(me.getPoint());}
    }
}