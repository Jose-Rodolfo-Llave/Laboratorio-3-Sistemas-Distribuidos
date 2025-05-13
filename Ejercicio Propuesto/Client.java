import java.net.*;
import java.io.*;
import java.util.*;

// The Client that can be run as a console
public class Client {
    // notification
    private String notif = " *** ";

    // for I/O
    private ObjectInputStream sInput; // to read from the socket
    private ObjectOutputStream sOutput; // to write on the socket
    private Socket socket; // socket object
    private String server, username; // server and username
    private int port; // port

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /*
     * Constructor to set below things
     * server: the server address
     * port: the port number
     * username: the username
     */
    Client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    /*
     * To start the chat
     */
    public boolean start() {
        // try to connect to the server
        try {
            socket = new Socket(server, port);
        } catch (Exception ec) {
            display("Error connecting to server: " + ec);
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        // Creating both Data Stream
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server
        new ListenFromServer().start();

        // Send our username to the server
        try {
            sOutput.writeObject(username);
        } catch (IOException eIO) {
            display("Exception doing login: " + eIO);
            disconnect();
            return false;
        }

        // success
        return true;
    }

    /*
     * To send a message to the console
     */
    private void display(String msg) {
        System.out.println(msg);
    }

    /*
     * To send a message to the server
     */
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    /*
     * When something goes wrong
     * Close the Input/Output streams and disconnect
     */
    private void disconnect() {
        try {
            if (sInput != null) sInput.close();
        } catch (Exception e) {}

        try {
            if (sOutput != null) sOutput.close();
        } catch (Exception e) {}

        try {
            if (socket != null) socket.close();
        } catch (Exception e) {}
    }

    /*
     * To start the Client in console mode use one of the following commands:
     * > java Client
     * > java Client username
     * > java Client username portNumber
     * > java Client username portNumber serverAddress
     */
    public static void main(String[] args) {
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Anonymous";

        Scanner scan = new Scanner(System.in);
        System.out.println("Enter the username: ");
        userName = scan.nextLine();

        // different cases according to the length of the arguments
        switch (args.length) {
            case 3:
                serverAddress = args[2];
            case 2:
                try {
                    portNumber = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
            case 1:
                userName = args[0];
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                return;
        }

        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName);

        // try to connect to the server and return if not connected
        if (!client.start()) return;

        System.out.println("\nHello! Welcome to the chatroom.");
        System.out.println("Instructions:");
        System.out.println("1. Type a message to broadcast to all active clients.");
        System.out.println("2. Type '@username <your message>' to send a private message.");
        System.out.println("3. Type 'WHOISIN' to see the list of active clients.");
        System.out.println("4. Type 'LOGOUT' to log off from the server.");

        // infinite loop to get the input from the user
        while (true) {
            System.out.print("> ");
            String msg = scan.nextLine();

            if (msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                break;
            } else if (msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            } else {
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }

        // close resources
        scan.close();
        client.disconnect();
    }

    /*
     * A class that waits for the message from the server
     */
    class ListenFromServer extends Thread {
        public void run() {
            while (true) {
                try {
                    String msg = (String) sInput.readObject();
                    System.out.println(msg);
                    System.out.print("> ");
                } catch (IOException e) {
                    display(notif + "Server has closed the connection: " + e + notif);
                    break;
                } catch (ClassNotFoundException e2) {
                    // do nothing
                }
            }
        }
    }
}
