import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

// the server that can be run as a console
public class Server {
    private static int uniqueId;
    private ArrayList<ClientThread> al;
    private SimpleDateFormat sdf;
    private int port;
    private boolean keepGoing;
    private String notif = " *** ";

    // constructor that receives the port to listen to for connection
    public Server(int port) {
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        al = new ArrayList<>();
    }

    public void start() {
        keepGoing = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (keepGoing) {
                display("Server waiting for Clients on port " + port + ".");
                Socket socket = serverSocket.accept();
                if (!keepGoing) break;
                ClientThread t = new ClientThread(socket);
                al.add(t);
                t.start();
            }

            // stop server
            try {
                serverSocket.close();
                for (ClientThread tc : al) {
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                        // ignore
                    }
                }
            } catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }

        } catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    protected void stop() {
        keepGoing = false;
        try {
            new Socket("localhost", port);
        } catch (Exception e) {
            // ignore
        }
    }

    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    private synchronized boolean broadcast(String message) {
        String time = sdf.format(new Date());
        String[] w = message.split(" ", 3);
        boolean isPrivate = w.length > 1 && w[1].charAt(0) == '@';

        if (isPrivate) {
            String toCheck = w[1].substring(1);
            message = w[0] + w[2];
            String messageLf = time + " " + message + "\n";
            boolean found = false;

            for (int y = al.size() - 1; y >= 0; y--) {
                ClientThread ct1 = al.get(y);
                if (ct1.getUsername().equals(toCheck)) {
                    if (!ct1.writeMsg(messageLf)) {
                        al.remove(y);
                        display("Disconnected Client " + ct1.username + " removed from list.");
                    }
                    found = true;
                    break;
                }
            }

            if (!found) return false;

        } else {
            String messageLf = time + " " + message + "\n";
            System.out.print(messageLf);
            for (int i = al.size() - 1; i >= 0; i--) {
                ClientThread ct = al.get(i);
                if (!ct.writeMsg(messageLf)) {
                    al.remove(i);
                    display("Disconnected Client " + ct.username + " removed from list.");
                }
            }
        }

        return true;
    }

    synchronized void remove(int id) {
        String disconnectedClient = "";
        for (int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            if (ct.id == id) {
                disconnectedClient = ct.getUsername();
                al.remove(i);
                break;
            }
        }
        broadcast(notif + disconnectedClient + " has left the chat room." + notif);
    }

    public static void main(String[] args) {
        int portNumber = 1500;

        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
                break;
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;
        }

        Server server = new Server(portNumber);
        server.start();
    }

    class ClientThread extends Thread {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;
        String date;

        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            System.out.println("Thread trying to create Object Input/Output Streams");

            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                broadcast(notif + username + " has joined the chat room." + notif);
            } catch (IOException | ClassNotFoundException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }

            date = new Date().toString() + "\n";
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void run() {
            boolean keepGoing = true;
            while (keepGoing) {
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                }

                String message = cm.getMessage();

                switch (cm.getType()) {
                    case ChatMessage.MESSAGE:
                        boolean confirmation = broadcast(username + ": " + message);
                        if (!confirmation) {
                            writeMsg(notif + "Sorry. No such user exists." + notif);
                        }
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        for (int i = 0; i < al.size(); ++i) {
                            ClientThread ct = al.get(i);
                            writeMsg((i + 1) + ") " + ct.username + " since " + ct.date);
                        }
                        break;
                }
            }
            remove(id);
            close();
        }

        private void close() {
            try {
                if (sOutput != null) sOutput.close();
            } catch (Exception e) {}
            try {
                if (sInput != null) sInput.close();
            } catch (Exception e) {}
            try {
                if (socket != null) socket.close();
            } catch (Exception e) {}
        }

        private boolean writeMsg(String msg) {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                sOutput.writeObject(msg);
            } catch (IOException e) {
                display(notif + "Error sending message to " + username + notif);
                display(e.toString());
            }
            return true;
        }
    }
}
