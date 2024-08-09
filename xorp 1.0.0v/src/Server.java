import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    private static Map<String, PrintWriter> clients = new HashMap<>();
    private static Map<String, String> userTags = new HashMap<>();
    private static String lastSender = "";
    private static final char PREFIX = '/';
    private static final int PORT = 23456;
    public static String userIdentity;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is active! \t Port: " + PORT);
            logExceptions("Sunucu Başlatıldı!", 1);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println(e);
            logExceptions("ServerSocket hatası. Sunucu Kapatılıyor.", 3);
        } finally {
            System.out.println("\n\nServer shutdown.");
        }
    }

    public static void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            userIdentity = in.readLine();
            if (!isValidUsername(userIdentity)) {
                out.println("SERVER: Geçersiz kullanıcı adı. Sadece harf ve sayılardan oluşmalıdır.");
                logExceptions("Geçersiz kullanıcı adı (" + userIdentity +")", 0);

                return;
            } else if (clients.containsValue(userIdentity)) {
                out.println("SERVER: Kullanıcı adı zaten alınmış.");
                return;
            } else {
                out.println(userIdentity);
                clients.put(userIdentity, out);
            }
            System.out.println("Kullanıcı katıldı. Kullanıcı Adı: " + userIdentity);

            while (true) {
                String message = in.readLine();
                if (message == null) {
                    break;
                }
                processMessage(userIdentity, message);
            }
        } catch (IOException e) {
            System.err.println(e);
            logExceptions("Mesaj alınamadı. Bağlantı Kapatıldı (" + clientSocket.getInetAddress()+")", 2);
        } finally {
            try {
                clientSocket.close();

            } catch (IOException e) {
                System.err.println(e);
                logExceptions("Kullanıcı Ayrıldı (" + clientSocket.getInetAddress()+")", 0);
            }
            System.out.println("Kullanıcı ayrıldı. IP Adresi: " + clientSocket.getInetAddress());
        }
    }

    private static String getUserID(Socket clientSocket) {
        for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
            if (entry.getValue().equals(clientSocket)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void changeUserID(String userID, String userIdentity) {
        int _index = userIdentity.indexOf('#');
        String userTag = userIdentity.substring(_index);
        System.out.println(userTag);
        String unifiedUserID = userID + userTag;
        System.out.println(unifiedUserID); // TODO: BUNU SİLMEYİ UNUTMA
        System.out.println(unifiedUserID.matches("^[a-zA-Z0-9]+#[0-9]{4}$"));
        if (unifiedUserID.matches("^[a-zA-Z0-9]+#[0-9]{4}$")) {
            System.out.println(userIdentity + " username will change to " + userID + " !!");
            broadcastMessage("SERVER", userIdentity + " named user is changed to " + userID);
            Server.userIdentity = unifiedUserID;
            userTags.replace(userIdentity, unifiedUserID);
        } else {
            clients.get(userIdentity).println("Kullanıcı etiketi değiştirilemez !!");
        }
    }

    private static void processMessage(String userIdentity, String message) {
        if (message.startsWith(String.valueOf(PREFIX))) {
            String[] parts = message.split(" ", 2);
            String command = parts[0];
            String argument = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "/ping":
                    handlePingCommand(userIdentity, argument);
                    break;
                case "/help":
                    handleHelpCommand(userIdentity);
                    break;
                case "/msg":
                    handlePrivateMessage(userIdentity, argument);
                    break;
                case "/rmsg":
                    handleReplyMessage(userIdentity, argument);
                    break;
                case "/quit":
                    handleClientQuit(userIdentity);
                    break;
                case "/reconnect":
                    handleClientReconnect(userIdentity);
                    break;
                case "/list":
                    handleClientList(userIdentity);
                    break;
                case "/username":
                    changeUserID(argument, userIdentity);

                default:
                    clients.get(userIdentity).println("SERVER: Geçersiz komut.");
                    break;
            }
        } else {
            broadcastMessage(userIdentity, message);
        }
    }

    private static void handlePingCommand(String userIdentity, String argument) {
        if (argument.isEmpty()) {
            clients.get(userIdentity).println("SERVER: Ping = " + calculatePing() + " ms");
        } else {
            String[] parts = argument.split("@");
            if (parts.length == 2 && isValidUsername(parts[1])) {
                String targetUserIdentity = parts[1];
                long ping = calculatePing();
                clients.get(userIdentity).println("SERVER: Ping to " + targetUserIdentity + " = " + ping + " ms");
            } else {
                clients.get(userIdentity).println("SERVER: Geçersiz kullanıcı adı.");
            }
        }
    }

    private static long calculatePing() {
        long startTime = System.currentTimeMillis();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    private static void handleHelpCommand(String userIdentity) {
        clients.get(userIdentity).println("SERVER:\n\t /ping veya /ping @UserIdentity = Ping ölçer.");
        clients.get(userIdentity).println("\t /help = Bütün komutların listesini gösterir.");
        clients.get(userIdentity).println("\t /msg @UserIdentity <message> = Özel mesaj gönderir.");
        clients.get(userIdentity).println("\t /rmsg <message> = En son özel mesaj gönderen kişiye cevap gönderir.");
        clients.get(userIdentity).println("\t /quit = Sunucudan ayrılır.");
        clients.get(userIdentity).println("\t /reconnect = Yeniden bağlanır.");
        clients.get(userIdentity).println("\t /list = Bağlı kullanıcıları listeler.");
        clients.get(userIdentity).println("\t /nick <username> = Kullanıcı adınızı değiştirmenizi sağlar.");

    }

    private static void handlePrivateMessage(String senderIdentity, String argument) {
        String[] parts = argument.split(" ", 2);
        if (parts.length == 2) {
            String targetUserIdentity = parts[0];
            String privateMessage = parts[1];
            PrintWriter targetUserOut = clients.get(targetUserIdentity);
            if (targetUserOut != null) {
                String formattedMessage = senderIdentity + " > " + privateMessage;
                targetUserOut.println(formattedMessage);
            } else {
                clients.get(senderIdentity).println("SERVER: Mesaj gönderilemedi. Hedef kullanıcı çevrimdışı.");
            }
        } else {
            logExceptions("Geçersiz PMsg formatı.", 1);
            clients.get(senderIdentity).println("SERVER: Geçersiz /msg komut formatı. Doğru format: /msg @UserIdentity <message>");
        }
    }

    private static void handleReplyMessage(String senderIdentity, String message) {
        if (!lastSender.isEmpty()) {
            PrintWriter targetUserOut = clients.get(lastSender);
            if (targetUserOut != null) {
                String formattedMessage = senderIdentity + " > " + message;
                targetUserOut.println(formattedMessage);
            }
        }
    }

    private static void handleClientQuit(String userIdentity) {
        clients.remove(userIdentity);
        broadcastMessage("SERVER", userIdentity + " sunucudan ayrıldı.");
    }

    private static void handleClientReconnect(String userIdentity) {
        handleClientQuit(userIdentity); // == handleClientQuit()
    }

    private static void handleClientList(String userIdentity) {
        StringBuilder userList = new StringBuilder("SERVER: Bağlı Kullanıcılar:\n");
        for (String user : clients.keySet()) {
            userList.append(user).append("\n");
        }
        clients.get(userIdentity).println(userList.toString());
    }

    private static boolean isValidUsername(String userID) {
        return userID.matches("^[a-zA-Z0-9]+#[a-zA-Z0-9]+$");
    }

    private static void broadcastMessage(String senderIdentity, String message) {
        for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
            String userID = entry.getKey();
            PrintWriter out = entry.getValue();

            out.println(message);
        }
        System.out.println(message);
        logMessage(message);
    }

    private static void logMessage(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("ChatLog.log", true))) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            writer.println("[" + timestamp + "] " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void logExceptions(String status, int Criticality) {
        String  level = null;
        File file = new File("StatusLog.log");
        try (PrintWriter writer = new PrintWriter(new FileWriter("StatusLog.log", true))) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            switch (Criticality) {
                case 0:
                    level = "Basic";
                    break;
                case 1:
                    level = "General";
                    break;
                case 2:
                    level = "Important";
                    break;
                case 3:
                    level = "Critical";
                    break;
            }
            writer.println("[" + timestamp + "] " + status + " {" + level + "}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
