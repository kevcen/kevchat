import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class server {
    ServerSocket serv = null;
    ArrayList<ObjectOutputStream> clientStreams; // change from OutputStream
    ArrayList<String> currentOnline;
    HashSet<ObjectOutputStream> streamsToRemove;
    ArrayList<String> adminslist;
    // HashMap<String, AudioStream> songsList;
    // HashMap<String, Clip> clipList;
    String ip;
    String pIp;

    public static void main(String[] args) {
        new server().go();
    }

    public ArrayList<String> setadminslist() {
        ArrayList<String> a = new ArrayList<String>();
        a.add("kev"); // me
        a.add("Higuro"); // joe
        a.add("Marley"); // adam
        a.add("Gabe"); // gabe
        return a;
    }

    public void getIps() {
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader br = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            pIp = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            ;
        }
        JFrame ipFrame = new JFrame("Server");

        JTextArea a = new JTextArea();
        a.setFont(new Font("Courier New", Font.PLAIN, 12));
        ipFrame.getContentPane().add(a, BorderLayout.CENTER);
        a.setEditable(false);
        a.append(String.format("LAN IP: %s%nPublic (WAN) IP: %s%n", ip, pIp));
        a.append(String.format("CODES: %n  LAN: %s%n  WAN: %s%n", encrypt(ip), encrypt(pIp)));
        ipFrame.setSize(400, 125);
        ipFrame.setVisible(true);
        ipFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public String encrypt(String address) {
        String encrypted = "";
        for (char digit : address.toCharArray()) {
            encrypted += (char) (digit * 2 - 5);
        }
        return encrypted;
    }

    public void go() {
        getIps();
        // for(char letters: encryptedIp.split(" ")) {
        //	System.out.print(char(letter - 3));
        // }
        // System.out.println();
        clientStreams = new ArrayList<>();
        currentOnline = new ArrayList<String>();
        streamsToRemove = new HashSet<>();
        adminslist = setadminslist();
        // songsList = new HashMap<>();
    /*clipList = new HashMap<>();
    try {
    	//songsList.put("mexican", new AudioStream(server.class.getResourceAsStream("mexican.wav")));
    	Clip mexican = makeClip(server.class.getResource("mexican.wav"));
    	//clipList.put("mexican", mexican);
    } catch (Exception e) {
    	e.printStackTrace();
    }
    	*/
        new Thread(new ServerSender()).start();
        try {
            serv = new ServerSocket(15028);
            System.out.println("server started");
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {

                // synchronized(this) {//makes sure the clientstream is added before new material is sent to
                // it
                Socket client = serv.accept();

                ObjectOutputStream o = new ObjectOutputStream(client.getOutputStream());

                clientStreams.add(o);
                System.out.println(
                        "client objectoutputstream added, length is now " + clientStreams.size());
                Thread t = new Thread(new ClientHandler(client, o));
                t.start();
                o.writeObject(currentOnline); // initial send for username checking
                o.flush();
                o.reset();
                System.out.println("received a connection");
                // }

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    /*public Clip makeClip(URL path) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        Clip clip = AudioSystem.getClip();
        AudioInputStream ais = AudioSystem.getAudioInputStream(path);
        clip.open(ais);
        return clip;
    }
    public void sendSong(Clip song) { //from audioStream
        for(ObjectOutputStream os: clientStreams) {
            try {
                os.writeObject(song);
            } catch(IOException e) {
                streamsToRemove.add(os);
                e.printStackTrace();
            }
        }
        for(ObjectOutputStream os: streamsToRemove) {
            clientStreams.remove(os);
            System.out.println("removed3");
        }
        streamsToRemove.clear();
    }*/
    public String printAll(ArrayList<String> l) {
        String s = "";
        for (int i = 0; i < l.size(); i++) {
            s += l.get(i) + (i == l.size() - 1 ? "" : ", ");
        }
        return s;
    }

    public synchronized void tellAll(String msg) {
        String time = new SimpleDateFormat("[HH:mm] ").format(new Date());
        for (ObjectOutputStream os : clientStreams) {
            try {
                os.writeObject(time + msg);
            } catch (IOException e) {
                streamsToRemove.add(os);
                e.printStackTrace();
            }
        }
        for (ObjectOutputStream os : streamsToRemove) {
            clientStreams.remove(os);
            System.out.println("removed1");
        }
        streamsToRemove.clear();
    }

    public synchronized void imageAll(ImageIcon img) {
        for (ObjectOutputStream os : clientStreams) {
            try {
                os.writeObject(img);
                os.flush();
            } catch (IOException e) {
                streamsToRemove.add(os);
                e.printStackTrace();
            }
        }
        for (ObjectOutputStream os : streamsToRemove) {
            clientStreams.remove(os);
            System.out.println("removed3");
        }
        streamsToRemove.clear();
    }

    public synchronized void sendOnlineList() {
        System.out.println("currentonline size " + currentOnline.size());

        for (ObjectOutputStream os : clientStreams) {
            try {
                os.writeObject(currentOnline);
                os.flush();
                os.reset();
            } catch (IOException e) {
                streamsToRemove.add(os);
                e.printStackTrace();
            }
        }
        for (ObjectOutputStream os : streamsToRemove) {
            clientStreams.remove(os);
            System.out.println("removed2"); // debug
        }
        streamsToRemove.clear();
    }

    public void finalize() {
        for (ObjectOutputStream os : clientStreams) {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ClientHandler implements Runnable { // one thread per client

        protected Socket socket;
        private String username;
        private ObjectOutputStream os; // for any direct messages
        private boolean nameRetrieved = false;

        public ClientHandler(Socket socket, ObjectOutputStream os) {
            this.socket = socket;
            this.os = os;
        }

        public void run() {
            System.out.println("in1");
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Object line = null;
            System.out.println("in2");
            try {
                while ((line = in.readObject()) != null) {
                    synchronized (this) {
                        if (line instanceof String) {
                            System.out.println("in4");
                            String msg = (String) line;
              /*if(Pattern.matches(".:\\s(?i)exit", line)) { //exit user
              currentOnline.remove(username);
              changedOnlineList = true;*/
                            // does not work, client side exits before sending to server
                            if (!nameRetrieved) { // initial new user check

                                username = msg;
                                System.out.println(username + " has connected");
                                tellAll(
                                        "From Server: "
                                                + (adminslist.contains(username) ? "[ADMIN] " : "")
                                                + username
                                                + " has joined the chatroom.");
                                currentOnline.add(username);
                                nameRetrieved = true;

                                System.out.println("Current online: " + printAll(currentOnline));

                                currentOnline.sort(
                                        new Comparator<String>() {
                                            public int compare(String s1, String s2) {
                                                if (s1.equals("kev")) return -1;
                                                if (s2.equals("kev")) return 1;
                                                return s1.compareTo(s2);
                                            }
                                        });
                                System.out.println("sent online list");
                                sendOnlineList(); // enter

                            } else { // normal msg

                                System.out.println(username + ": " + line);

                                if ((msg.matches("!kick .*") || msg.matches("!hq") || msg.matches("!secrets"))
                                        && !adminslist.contains(username)) {
                                    tellAll(
                                            String.format(
                                                    "%s: [%s]", username, msg)); // changes msg so doesn't actually kick
                                    tellAll("From Server: " + username + ", you don't have admin privileges!");
                                } else {
                                    tellAll(String.format("%s: %s", username, msg));
                                    switch (msg) { // can add more custom commands
                                        case "!hq":
                                            tellAll(
                                                    "From Server: [ADMIN] "
                                                            + username
                                                            + " can now post high quality images.");
                                            break;
                                        case "!secrets":
                                            tellAll(
                                                    "From Server: [ADMIN] "
                                                            + username
                                                            + " has now unlocked all secrets in their collection!");
                                            break;
                                    }
                                }
                            }
                        } else if (line instanceof ImageIcon) {
                            synchronized (this) {
                                tellAll(username + ": "); // default start for an image
                                imageAll((ImageIcon) line);
                            }
                        }
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                if (username != null) {
                    // synchronized(this) {
                    System.out.println("read fail/" + username + " has disconnected");
                    currentOnline.remove(username); // when socket closes, remove the user
                    tellAll(
                            "From Server: "
                                    + (adminslist.contains(username) ? "[ADMIN] " : "")
                                    + username
                                    + " has disconnected.");
                    sendOnlineList(); // leave
                    try {
                        in.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    // }
                }
            }
        }
    }

    class ServerSender implements Runnable {
        public void run() {
            try (BufferedReader serverIn = new BufferedReader(new InputStreamReader(System.in))) {
                String line = null;
                while ((line = serverIn.readLine()) != null) {
                    tellAll("From Server: " + line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
