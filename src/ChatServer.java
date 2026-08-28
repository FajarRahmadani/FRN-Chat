import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    
    private int port;
    private List<ClientHandler> clients = new ArrayList<>();

    public ChatServer(int port) {
        this.port = port;
    }

  
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server berjalan di port " + port + "...");
            
            while (true) {
                // Menerima koneksi client baru
                Socket socket = serverSocket.accept();
                System.out.println("Client baru terhubung dari: " + socket.getRemoteSocketAddress());
                
                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Terjadi kesalahan pada server: " + e.getMessage());
        }
    }

    public synchronized void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            // Pesan dikirim ke semua orang
            client.sendMessage(message);
        }
    }

    // METHOD: Menghapus client yang terputus
    public synchronized void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public static void main(String[] args) {
        
        ChatServer server = new ChatServer(2106);
        server.start();
    }
}


class ClientHandler implements Runnable {
   
    private Socket socket;
    private ChatServer server;
    private PrintWriter writer;
    private BufferedReader reader;
    private String clientName;

  
    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    
    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            
            clientName = reader.readLine();
            server.broadcast("[SYSTEM]: " + clientName + " bergabung ke dalam obrolan.", this);

            String message;
          
            while ((message = reader.readLine()) != null) {
                server.broadcast("[" + clientName + "]: " + message, this);
            }
        } catch (IOException e) {
            System.out.println(clientName + " terputus secara tiba-tiba.");
        } finally {
          
            server.removeClient(this);
            server.broadcast("[SYSTEM]: " + clientName + " telah meninggalkan obrolan.", this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void sendMessage(String message) {
        writer.println(message);
    }
}