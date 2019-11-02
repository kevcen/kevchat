import java.awt.*;
import java.util.List;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

public class client {
  private ObjectInputStream is = null; // from ObjectInputStream
  private ObjectOutputStream out = null;
  private Socket socket = null;
  private String username = null;
  /*private Socket audioSocket = null;
  private BufferedInputStream ain = null;
  private DataOutputStream aout = null;*/
  private JFrame mainFrame;
  private JTextPane text;
  private StyledDocument textDoc;
  private JTextArea online;
  private JTextField message;
  private ArrayList<String> onlineList;
  private JTextField userField;
  private JPanel usernamePanel;
  private JLabel uLabel;
  private JPanel sendPanel;
  private JPanel sidePanel;
  // private JPanel musicPanel;
  private JScrollPane scrollChat;
  // private JTextArea musicStatus;
  // private int currentSong = -1; //default is 0 will not be able to request mex song with '!=0'
  private Clip currentClip;
  private JButton stopMusic;
  private Clip notificationSound;
  private boolean notifyOn = true;
  private List<String> badWords;
  private HashMap<String, String> adminMap;
  private boolean adminLogin;
  private JFrame customizeFrame;
  private int currentTheme; // currentTheme is default to begin with anyways
  private JLabel enterText;
  private JLabel currentTime;
  private JLabel patchLabel;
  private JLabel onLabel;
  private JScrollPane scrollOnline;
  private List<Color> textBg;
  private List<Color> textFg;
  private List<Color> onlineBg;
  private List<Color> onlineFg;
  private List<Color> onLabelFg;
  // currentTime fg and patchLabel fg is same as onLabel fg
  private List<Color> sidePanelBg;
  private List<Color> sendPanelBg;
  private List<Color> enterTextFg;
  private boolean imageHQ = false;
  private HashMap<String, Image> emoticonMap;
  private HashMap<String, String> secretsMap;
  private JFrame secretsFrame;
  private JTextArea secretsText;
  private Path secretPath;
  private Image currentImgBG;
  private JPanel mainPanel;
  private String serverIP;
  private JTextField ipField;
  private JFrame ipFrame;

  public static void main(String[] args) {
    new client().collectIP();
  }

  public void collectIP() {
    ipFrame = new JFrame("Connect to a server");

    JLabel l = new JLabel("Server code [Provided by server host]:");
    l.setFont(new Font("Courier New", Font.PLAIN, 12));
    ipField = new JTextField(20);
    JButton b = new JButton("Connect");
    b.setContentAreaFilled(false);
    ipField.addActionListener(new ipListener());
    b.addActionListener(new ipListener());
    JPanel p =
        new JPanel() {
          {
            add(l);
            add(ipField);
            add(b);
          }
        };
    ipFrame.getContentPane().add(p, BorderLayout.CENTER);
    ipFrame.setSize(500, 100);
    ipFrame.setVisible(true);
    ipFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    try {
      ipFrame.setIconImage(ImageIO.read(client.class.getResource("kevchat-icon.png")));
    } catch (IOException e) {
      e.printStackTrace();
    }
    /*System.out.print("Server code [Provided by server host]: ");
    Scanner sc = new Scanner(System.in);
    serverIP = unencrypt(sc.next());
    sc.close();
    start();*/
  }

  public class ipListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      serverIP = unencrypt(ipField.getText());
      try {
        socket = new Socket(serverIP, 15028);
        is = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream());
        ipFrame.setVisible(false);
        start();
      } catch (IOException e1) {
        ipField.setText("");
        JOptionPane.showMessageDialog(
            null, "Invalid server code.", "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  public String unencrypt(String code) {
    String unencrypted = "";
    for (char digit : code.toCharArray()) {
      unencrypted += (char) ((digit + 5) / 2);
    }
    return unencrypted;
  }

  public void start() {
    // username = name;
    badWords = setNaughtyList();
    adminMap = new HashMap<>();
    setAdmins();
    emoticonMap = new HashMap<>();
    setEmoticons();
    secretsMap = setSecrets();
    secretPath = openSecrets();
    currentImgBG = null;
    /*try {

    	 socket = new Socket(serverIP, 15028);
    	 is = new ObjectInputStream(socket.getInputStream());
    	 out = new ObjectOutputStream(socket.getOutputStream());
    	 //audioSocket = new Socket("2.31.146.54", 16024);
    	 //ain = new BufferedInputStream(audioSocket.getInputStream());
    	 //aout = new DataOutputStream(audioSocket.getOutputStream());
    	 //out.println(username); //initial name retrieval
    } catch(Exception ex) {ex.printStackTrace();}*/
    Thread inputThread = new Thread(new ServReader());

    buildGui();
    inputThread.start();
  }

  public void buildGui() {
    JFrame.setDefaultLookAndFeelDecorated(true);
    mainFrame = new JFrame("Kevchat");
    mainFrame.addComponentListener(
        new ComponentAdapter() {
          public void componentResized(ComponentEvent e) {
            if (currentImgBG != null) setEaster(); // exclusive
          }
        });
    mainPanel =
        new JPanel() {
          @Override
          public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(currentImgBG, 0, 0, null);
          }
        };
    JButton sender;
    JButton imgSender;
    JButton emotSender;
    try {
      sender = new JButton(new ImageIcon(ImageIO.read(client.class.getResource("send icon.png"))));
      imgSender =
          new JButton(new ImageIcon(ImageIO.read(client.class.getResource("img 24px icon.png"))));
      emotSender =
          new JButton(
              new ImageIcon(ImageIO.read(client.class.getResource("emoticon 24 px icon.png"))));
      sender.setContentAreaFilled(false);
      imgSender.setContentAreaFilled(false);
      emotSender.setContentAreaFilled(false);
      sender.setBorder(null);
      imgSender.setBorder(null);
      emotSender.setBorder(null);

      JPopupMenu emotMenu = new JPopupMenu();
      emotMenu.setLayout(new GridLayout(3, 4));
      String[] emojiCodes =
          new String[] {
            ":cringe:",
            ":disappointed:",
            ":shades:",
            ":scared:",
            ":in love:",
            ":laughing:",
            ":sad:",
            ":shocked:",
            ":happy:",
            ":calculated:",
            ":relieved:",
            ":salty:"
          };
      for (String code : emojiCodes) { // adds all emoticons
        JMenuItem item = new JMenuItem(new ImageIcon(emoticonMap.get(code)));
        item.addActionListener(
            e ->
                message.setText(
                    String.format(
                        "%s %s ",
                        message.getText(), code))); // spaces on either side of appended code
        emotMenu.add(item);
      }

      // emotSender.setComponentPopupMenu(emotMenu);
      emotSender.addMouseListener(
          new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
              emotMenu.show(emotSender, e.getX(), e.getY());
            }
          });

      sender.addActionListener(new SendListener());
      imgSender.addActionListener(new ImgListener());

      message = new JTextField(12);
      message.addActionListener(new SendListener());
      enterText = new JLabel("Enter message: ");

      sendPanel = new JPanel();
      sendPanel.setOpaque(false);
      sendPanel.add(enterText);
      sendPanel.add(message);
      JPanel sendButtonsPanel = new JPanel();
      sendButtonsPanel.setOpaque(false);
      sendButtonsPanel.add(sender);
      sendButtonsPanel.add(imgSender);
      sendButtonsPanel.add(emotSender);
      sendPanel.add(sendButtonsPanel);

      JButton userSetter = new JButton("Connect");
      userSetter.setContentAreaFilled(false);
      userSetter.addActionListener(new UsernameListener());
      userField = new JTextField(12);
      userField.addActionListener(new UsernameListener());
      uLabel = new JLabel("Set username: ");
      usernamePanel = new JPanel();
      usernamePanel.add(uLabel);
      usernamePanel.add(userField);
      usernamePanel.add(userSetter);
      text = new JTextPane();
      textDoc = text.getStyledDocument();
      text.setEditable(false);
      // line wrap
      text.setFont(new Font("Comic Sans MS", Font.PLAIN, 12));
      scrollChat = new JScrollPane(text);
      // scrollChat.setViewportView(text);
      scrollChat.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      scrollChat.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      currentTime = new JLabel("");
      currentTime.setFont(new Font("Century Gothic", Font.PLAIN, 20));
      Timer timeSetter =
          new Timer(1000, e -> currentTime.setText(String.format("%tr", new Date())));
      timeSetter.start();
      onLabel = new JLabel("Currently online users: ");
      online = new JTextArea(20, 5);
      online.setText("Server is not online!");
      online.setEditable(false);
      // online.setOpaque(false);
      online.setBackground(mainFrame.getBackground());
      online.setFont(new Font("Dialog", Font.ITALIC, 15));
      scrollOnline = new JScrollPane(online);
      scrollOnline.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      scrollOnline.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      // scrollOnline.setBorder(null);
      patchLabel = new JLabel("Recent updates: ");

      JTextArea patchNotes = new JTextArea(8, 5);

      patchNotes.setLineWrap(true);
      patchNotes.setFont(new Font("Consolas", Font.PLAIN, 10));
      patchNotes.setWrapStyleWord(true);
      patchNotes.setText(
          "Welcome to Kevchat 5!\n\nWe (just kev) have introduced:\n -Sending images and kevmojis\n -List of secret commands; you can add more if you happen to discover more - progress is saved    and will not be lost when closing Kevchat.\n -Exclusive !easter command\n -Restrictions to some commands (now jason can't kick everyone out).\n -Mexican rave is now on endless loop! :)\n\nnice read");
      patchNotes.setCaretPosition(0);
      JScrollPane patchScroll = new JScrollPane(patchNotes);
      patchScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      patchScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

      JButton secretButton = new JButton("Secrets Collection");
      secretButton.setContentAreaFilled(false);
      secretButton.addActionListener(e -> showSecrets());

      /*musicPanel = new JPanel();
      musicPanel.setLayout(new BoxLayout(musicPanel, BoxLayout.Y_AXIS));
      JButton musicButton = new JButton("start a tune");
      musicButton.addActionListener(e-> {
      	//if(currentClip!=null)currentClip.stop();
      	int i;!
      	do {
      		i = (int) Math.round(Math.random()*6+1);
      	} while(currentSong==i);
      	askMusic(i);
      });
      JButton replayButton = new JButton("replay");
      replayButton.addActionListener(e -> {
      	if(currentClip!=null) {
      		currentClip.setMicrosecondPosition(0);
      		currentClip.start();
      	}
      });
      JLabel currentPlaying = new JLabel("Current song: ");
      musicStatus = new JTextArea(3, 5);
      musicStatus.setEditable(false);
      musicStatus.setText("waiting for a tune");
      JPanel musicControls = new JPanel();
      musicControls.add(musicButton);
      musicControls.add(replayButton);
      musicPanel.add(musicControls);
      musicPanel.add(currentPlaying);
      musicPanel.add(musicStatus);*/

      stopMusic = new JButton("stop");
      stopMusic.addActionListener(e -> currentClip.stop());
      sidePanel = new JPanel();
      sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
      sidePanel.add(currentTime);
      // sidePanel.add(new JSeparator(JSeparator.HORIZONTAL));
      sidePanel.add(onLabel);
      sidePanel.add(scrollOnline);
      sidePanel.add(secretButton);
      sidePanel.add(patchLabel);
      sidePanel.add(patchScroll);

      /*sidePanel.add(musicButton);
      sidePanel.add(replayButton);
      sidePanel.add(currentPlaying);
      sidePanel.add(musicStatus);*/
      mainPanel.setLayout(new BorderLayout());
      mainPanel.add(BorderLayout.CENTER, sidePanel);
      mainPanel.add(BorderLayout.SOUTH, usernamePanel);

      // mainFrame.getContentPane().add(BorderLayout.CENTER, sidePanel);
      // mainFrame.getContentPane().add(BorderLayout.SOUTH, usernamePanel);
      try {
        mainFrame.setIconImage(ImageIO.read(client.class.getResource("kevchat-icon.png")));
      } catch (IOException e) {
        e.printStackTrace();
      }
      mainFrame.getContentPane().add(mainPanel);
      mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      mainFrame.setSize(600, 700);
      mainFrame.setVisible(true);

      userField.requestFocus();
    } catch (IOException e) {
      e.printStackTrace();
    }
    setAudio();
    setNaughtyList();
    buildCustomize();
    buildSecrets();
  }

  public void buildSecrets() {
    secretsFrame = new JFrame("List of Secret Commands");
    JPanel p = new JPanel();
    p.setLayout(new BorderLayout());
    JLabel s = new JLabel("Secret Commands:");
    s.setFont(new Font("Courier New", Font.PLAIN, 15));
    try {
      JButton info =
          new JButton(new ImageIcon(ImageIO.read(client.class.getResource("stuck icon.png"))));
      info.setBorder(null);
      info.setContentAreaFilled(false);
      info.addActionListener(
          e ->
              JOptionPane.showMessageDialog(
                  secretsFrame,
                  "Here is a list of the secrets you can use in Kevchat! \n\nTo use them, simply enter them into the chatroom and the effects\nwill take place. You can also add to the list as you discover more\nsecret commands - good luck!",
                  "what is going on",
                  JOptionPane.INFORMATION_MESSAGE));
      p.add(s, BorderLayout.WEST);
      p.add(info, BorderLayout.EAST);
    } catch (IOException e) {
      e.printStackTrace();
    }
    JPanel midPanel = new JPanel();
    secretsText = new JTextArea();
    secretsText.setEditable(false);
    secretsText.setFont(new Font("Courier New", Font.PLAIN, 12));
    JScrollPane secretsScroll = new JScrollPane(secretsText);
    secretsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    secretsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    midPanel.add(secretsScroll);
    secretsFrame.getContentPane().add(p, BorderLayout.NORTH);
    secretsFrame.getContentPane().add(midPanel, BorderLayout.CENTER);
    secretsFrame.setSize(700, 300);
  }

  public void buildCustomize() {

    customizeFrame = new JFrame("Customise!");
    JPanel p = new JPanel();
    JLabel t = new JLabel("Themes: ");
    String[] themes =
        new String[] {
          "Default", "Modern", "Dream", "Lightning", "female", "Toxic", "Ruby", "kids Dinosaur!"
        };
    @SuppressWarnings("unchecked")
    JComboBox cb = new JComboBox(themes);
    cb.addActionListener(
        e -> {
          currentTheme = cb.getSelectedIndex();
        });
    JButton b = new JButton("ok");
    textBg =
        Arrays.asList(
            Color.WHITE,
            Color.BLUE,
            Color.DARK_GRAY,
            Color.DARK_GRAY,
            Color.DARK_GRAY,
            new Color(255, 153, 201),
            Color.DARK_GRAY,
            Color.DARK_GRAY,
            new Color(44, 131, 174));
    textFg =
        Arrays.asList(
            new Color(22, 22, 22),
            Color.WHITE,
            Color.WHITE,
            new Color(201, 113, 233),
            Color.CYAN,
            Color.WHITE,
            Color.GREEN,
            Color.WHITE,
            Color.WHITE);
    onlineBg =
        Arrays.asList(
            mainFrame.getBackground(),
            Color.BLUE,
            new Color(22, 22, 22),
            Color.DARK_GRAY,
            Color.DARK_GRAY,
            Color.WHITE,
            new Color(22, 22, 22),
            Color.WHITE,
            new Color(44, 131, 174));
    onlineFg =
        Arrays.asList(
            new Color(22, 22, 22),
            Color.WHITE,
            Color.WHITE,
            Color.WHITE,
            Color.WHITE,
            new Color(255, 153, 201),
            Color.GREEN,
            new Color(22, 22, 22),
            new Color(255, 219, 61));
    onLabelFg =
        Arrays.asList(
            new Color(22, 22, 22),
            Color.WHITE,
            Color.WHITE,
            Color.WHITE,
            Color.CYAN,
            Color.WHITE,
            Color.GREEN,
            Color.WHITE,
            new Color(44, 131, 174));
    // currentTime fg and patchLabel fg is same as onLabel fg
    sidePanelBg =
        Arrays.asList(
            mainFrame.getBackground(),
            Color.BLUE,
            new Color(22, 22, 22),
            new Color(99, 37, 120),
            Color.DARK_GRAY,
            new Color(255, 191, 241),
            new Color(22, 22, 22),
            new Color(127, 0, 0),
            new Color(255, 219, 61));
    sendPanelBg =
        Arrays.asList(
            mainFrame.getBackground(),
            Color.BLUE,
            Color.WHITE,
            new Color(99, 37, 120),
            Color.WHITE,
            Color.WHITE,
            new Color(22, 22, 22),
            new Color(127, 0, 0),
            new Color(255, 219, 61));
    enterTextFg =
        Arrays.asList(
            new Color(22, 22, 22),
            Color.WHITE,
            Color.DARK_GRAY,
            new Color(201, 113, 233),
            Color.DARK_GRAY,
            new Color(255, 69, 168),
            Color.GREEN,
            new Color(127, 0, 0),
            new Color(44, 131, 174));
    b.addActionListener(
        e -> {
          doCustomize(currentTheme + (currentTheme != 0 ? 1 : 0)); // +1 to skip bsod theme
          // chat fg bg fonts
          // online list fg bg label fonts
          // time fg
          // sidepanel bg
          // patch notes label
          // send panel labelfont
          // customizeFrame.setVisible(false);
        });
    p.add(t);
    p.add(cb);
    p.add(b);
    customizeFrame.getContentPane().add(BorderLayout.CENTER, p);
    customizeFrame.setSize(250, 75);
    // customizeFrame.setVisible(true); //to undo
  }

  public void doCustomize(int theme) {
    unBackground();

    currentTheme = theme;
    // System.out.println(theme);
    String fontName =
        theme == 0
            ? "Dialog"
            : theme == 8 ? "Comic Sans MS" : theme == 1 ? "Courier New" : "Century Gothic";
    text.setBackground(textBg.get(theme));
    text.setForeground(textFg.get(theme));
    text.setFont(new Font(theme == 0 ? "Comic Sans MS" : fontName, Font.PLAIN, 12));
    online.setForeground(onlineFg.get(theme));
    online.setFont(new Font(fontName, Font.ITALIC, 15));
    online.setBackground(onlineBg.get(theme));
    onLabel.setForeground(onLabelFg.get(theme));
    onLabel.setFont(new Font(fontName, (theme == 0 ? Font.BOLD : Font.PLAIN), 12));
    currentTime.setForeground(onLabelFg.get(theme));
    currentTime.setFont(new Font("Century Gothic", Font.PLAIN, 20));
    sidePanel.setBackground(sidePanelBg.get(theme));
    patchLabel.setForeground(onLabelFg.get(theme));
    patchLabel.setFont(new Font(fontName, (theme == 0 ? Font.BOLD : Font.PLAIN), 12));
    sendPanel.setBackground(sendPanelBg.get(theme));
    enterText.setForeground(enterTextFg.get(theme));
    enterText.setFont(new Font(fontName, (theme == 0 ? Font.BOLD : Font.PLAIN), 12));
    mainFrame.repaint();
  }

  public void setAudio() {
    try {
      currentClip = AudioSystem.getClip();
      AudioInputStream ais =
          AudioSystem.getAudioInputStream(client.class.getResource("mexican.wav"));
      currentClip.open(ais);
      currentClip.loop(Clip.LOOP_CONTINUOUSLY);

      notificationSound = AudioSystem.getClip();
      AudioInputStream nais =
          AudioSystem.getAudioInputStream(client.class.getResource("notification.wav"));
      notificationSound.open(nais);
    } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public HashMap<String, String> setSecrets() {
    HashMap<String, String> s = new HashMap<>();
    s.put("!bsod", "!bsod - Sets a theme reminiscent of the Windows' Blue Screen of Death");
    s.put("!rape", "!rape - Alternate command to !rape [Joe's Request]");
    s.put("!rave", "!rave - Forces all members to listen to mexican rave");
    s.put("!stop this sh*t", "!stop this sh*t - Stops the mind-breaking music");
    s.put(
        "!kick <username>",
        "!kick <username> - Kicks the user associated with the username from Kevchat [ADMIN Locked]");
    s.put(
        "!customise",
        "!customise - Gives a selection of various themes to customise your Kevchat!");
    s.put("!clear", "!clear - Clears all messages from your Kevchat");
    s.put("!bites the dust", "!bites the dust - Alternate command to !clear [Adam's Request]");
    s.put(
        "!notify",
        "!notify - Turns on/off notifications depending on whether it was previously on or off");
    s.put("baibai", "baibai - Alternate to !exit [Exclusive command requested by Higuro]");
    s.put("!exit", "!exit - Exits Kevchat.");
    s.put("!hq", "!hq - Enables you to post higher quality images [ADMIN Locked]");
    s.put("!easter", "!easter - Sets an Easter theme [Exclusive Easter event command]");
    s.put("!secrets", "!secrets - Reveals all secret commands (what a loser) [ADMIN Locked]");
    return s;
  }

  public Path openSecrets() {
    Path p = Paths.get("C:\\Kevchat\\secrets.txt");
    if (!Files.exists(p)) {
      try {
        if (!Files.exists(p.getParent())) Files.createDirectories(p.getParent());
        Files.createFile(p);
        Files.write(
            p,
            Arrays.asList(
                secretsMap.get("!rave"),
                secretsMap.get("!customise"),
                secretsMap.get("!stop this sh*t"),
                secretsMap.get("!notify"),
                secretsMap.get("!kick <username>"),
                secretsMap.get("!clear"),
                secretsMap.get("!exit"),
                secretsMap.get("baibai")),
            StandardOpenOption.APPEND);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return p;
  }

  public void addSecret(String command) {
    try {
      String description = secretsMap.get(command);
      for (String line : Files.readAllLines(secretPath)) {
        if (description.equals(line)) // already unlocked secret
        return;
      }
      Files.write(
          secretPath,
          Arrays.asList(description),
          StandardCharsets.UTF_8,
          StandardOpenOption.APPEND); // use list version rather than bytearray for new lines
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void showSecrets() {
    try {
      secretsText.setText(new String(Files.readAllBytes(secretPath)));
    } catch (IOException e) {
      e.printStackTrace();
    }
    secretsFrame.repaint();
    secretsFrame.setVisible(true);
  }

  public void setEmoticons() {
    try {
      emoticonMap.put(
          ":shocked:", ImageIO.read(client.class.getResource("emoticon - shocked.png")));
      emoticonMap.put(
          ":disappointed:", ImageIO.read(client.class.getResource("emoticon - disappointed.png")));
      emoticonMap.put(":shades:", ImageIO.read(client.class.getResource("emoticon - shades.png")));
      emoticonMap.put(":scared:", ImageIO.read(client.class.getResource("emoticon - scared.png")));
      emoticonMap.put(
          ":in love:", ImageIO.read(client.class.getResource("emoticon - in-love.png")));
      emoticonMap.put(
          ":laughing:", ImageIO.read(client.class.getResource("emoticon - laughing.png")));
      emoticonMap.put(":sad:", ImageIO.read(client.class.getResource("emoticon - sad.png")));
      emoticonMap.put(":cringe:", ImageIO.read(client.class.getResource("emoticon - cringe.png")));
      emoticonMap.put(":happy:", ImageIO.read(client.class.getResource("emoticon - happy.png")));
      emoticonMap.put(
          ":calculated:", ImageIO.read(client.class.getResource("emoticon - calculated.png")));
      emoticonMap.put(
          ":relieved:", ImageIO.read(client.class.getResource("emoticon - relieved.png")));
      emoticonMap.put(":salty:", ImageIO.read(client.class.getResource("emoticon - salty.png")));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void setAdmins() {
    adminMap.put("kev", "8k5e3v5");
    adminMap.put("Higuro", "tHemrmystery");
    adminMap.put("Marley", "06.09.00");
    adminMap.put("Gabe", "Holmes58");
  }

  public List<String> setNaughtyList() {
    return Arrays.asList("fuck", "shit", "cunt", "bitch", "anal", "whore", "dick", "pussy");
  }

  /*public void askMusic(int num) {
  	try {
  		aout.writeInt(num);
  		System.out.printf("current is %d, new num is %d %n", currentSong, num);
  		currentSong = num;
  		switch(currentSong) {
  			case 0: musicStatus.setText("mexican rave !");
  				break;
  			case 1: musicStatus.setText("Feint - Vagrant");
  				break;
  			case 2: musicStatus.setText("Panda Eyes - Colorblind");
  				break;
  			case 3: musicStatus.setText("Sean&Bobo - Swing it");
  				break;
  			case 4: musicStatus.setText("Pegboard Nerds - Try This");
  				break;
  			case 5: musicStatus.setText("Snavs - Time");
  				break;
  			case 6: musicStatus.setText("Yuki Hayashi - Above");
  				break;
  			case 7: musicStatus.setText("Yuki Hayashi - Team Potential");
  				break;
  		}
  		System.out.println("asked music");

  		getMusic();
  	} catch(IOException e) {e.printStackTrace();}
  }
  public void getMusic() {
  	try {
  		Clip clip = AudioSystem.getClip();
  		AudioInputStream ais = AudioSystem.getAudioInputStream(ain);
  		//System.out.println(1.5);
  		if(ownedSongs.containsKey(ais)) {
  			System.out.println("enter 1");
  			ownedSongs.get(ais).start();
  			System.out.println(2);
  		} else {
  			clip.open(ais);

  			if(currentClip!=null)currentClip.close();
  			currentClip = clip;

  			clip.start();
  		//}
  		System.out.println("got music");
  	} catch(Exception e) {e.printStackTrace();}

  }*/
  public void enter(String username) {
    this.username = username;
    System.out.printf("Welcome to kevchat, %s. \n", username);

    // mainFrame.getContentPane().remove(usernamePanel);
    // mainFrame.getContentPane().remove(sidePanel);
    text.setText(""); // all text until you actually enter
    // mainFrame.getContentPane().add(BorderLayout.CENTER, scrollChat);
    scrollOnline.setBorder(null);
    // sidePanel.add(stopMusic); //even more previous version
    // mainFrame.getContentPane().add(BorderLayout.EAST, sidePanel);

    // mainFrame.getContentPane().add(BorderLayout.SOUTH, sendPanel);

    mainPanel.remove(usernamePanel);
    mainPanel.remove(sidePanel);
    mainPanel.add(BorderLayout.CENTER, scrollChat);
    mainPanel.add(BorderLayout.EAST, sidePanel);
    mainPanel.add(BorderLayout.SOUTH, sendPanel);
    mainFrame.revalidate();
    mainFrame.repaint();
    message.requestFocus();
    try {
      out.writeObject(username);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public class UsernameListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String line = userField.getText();
      if (adminLogin) {
        // System.out.println(line + ", " + adminMap.get(username));
        if (line.equals(adminMap.get(username))) enter(username);
        else {
          JOptionPane.showMessageDialog(
              null, "no", "Incorrect Password", JOptionPane.ERROR_MESSAGE);
          System.exit(-2); // invalid password
        }
      } else if (onlineList.contains(line)) {
        JOptionPane.showMessageDialog(
            null,
            "Username is already taken!",
            "Invalid Username",
            JOptionPane.INFORMATION_MESSAGE);
        userField.setText("");
        // userField.requestFocus();
      } else if (line.equals("")) {
        JOptionPane.showMessageDialog(
            null, "Please enter a username", "Invalid Username", JOptionPane.INFORMATION_MESSAGE);
        userField.setText("");
        // userField.requestFocus();
      } else if (adminMap.containsKey(line)) {
        username = line;
        JOptionPane.showMessageDialog(
            null, "Enter Password", "Please enter your password.", JOptionPane.WARNING_MESSAGE);
        uLabel.setForeground(Color.RED);
        uLabel.setText("PASSWORD: ");
        userField.setText("");
        adminLogin = true;
        // userField.requestFocus();
      } else {
        enter(line);
      }
    }
  }

  public class SendListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String line = message.getText();

      switch (line) {
        case "!stop this sh*t":
          currentClip.stop();
          break;
        case "!notify":
          if (notifyOn) notifyOn = false;
          else notifyOn = true;
          break;
        case "!customise":
          customizeFrame.setVisible(true);
          message.setText("");
          break;
        case "!bsod":
          doCustomize(1);
          currentTheme = 1;
          break;
        case "!kick kev":
          text.setText("");
          text.setText("DONT KICK OUR KING");
          doCustomize(1);
          JOptionPane.showMessageDialog(null, "U DIE", "DONT KICK KEV", JOptionPane.ERROR_MESSAGE);
          System.exit(0);
        case "!hq":
          if (adminMap.containsKey(username)) {
            text.setCaretPosition(textDoc.getLength());
            imageHQ = true;
          }
          break;
        case "!easter":
          if (currentImgBG == null) setEaster();
          else {
            doCustomize(currentTheme);
          }
          break;
        case "!secrets":
          if (adminMap.containsKey(username))
            for (String secret : secretsMap.keySet()) addSecret(secret);
          break;
        case "":
          return;
      }
      if (line.equals("!bites the dust") || line.equals("!clear"))
        text.setText(""); // multiple selections
      if (secretsMap.containsKey(line)) addSecret(line);

      line = clean(line);
      try {
        out.writeObject(line);
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      if (line.equalsIgnoreCase("EXIT") || line.equalsIgnoreCase("baibai")) {
        System.exit(-1);
      }

      message.setText("");
    }
  }

  public class ImgListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      JFileChooser jf = new JFileChooser();
      FileNameExtensionFilter filter =
          new FileNameExtensionFilter("Naked Panda Pics", "jpg", "jpeg", "png", "gif");
      jf.setFileFilter(filter);
      int option = jf.showOpenDialog(mainFrame);
      if (option == JFileChooser.APPROVE_OPTION) {
        File imgFile = jf.getSelectedFile();
        try {
          Image img = ImageIO.read(imgFile);
          ImageIcon iconToSend = new ImageIcon(img);
          if (img.getWidth(null) > (text.getWidth() / 2))
            iconToSend =
                new ImageIcon(
                    img.getScaledInstance(
                        imageHQ ? text.getWidth() : text.getWidth() / 2,
                        -1,
                        (imageHQ
                            ? Image.SCALE_SMOOTH
                            : Image
                                .SCALE_FAST))); // negative height for a same aspect ratio height as
          // original image

          out.writeObject(iconToSend);

        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }
  }

  public String clean(String line) {
    for (String naughty : badWords) {
      line =
          line.replaceAll(
              "(?i)" + naughty, naughty.charAt(0) + "***" + (naughty.length() == 5 ? "*" : ""));
    }
    return line;
  }

  public void setOnline(ArrayList<String> l) {
    onlineList = l;
    // System.out.println("list size " + onlineList.size()); debug
    online.setText("");
    for (String user : l) {
      online.append("  - " + user + "\n");
    }
    if (online.getText().equals("")) online.setText("\n		    No-one is currently online!");
  }

  public void printWithEmote(String msg) {
    ArrayList<List<Integer>> emoticonRanges = new ArrayList<>();

    for (String emotCode : emoticonMap.keySet()) { // find where each emoticon is in the msg
      String line = msg;
      int msgStart = 0;
      int msgEnd = line.length();
      int count = 0;
      while (line.contains(emotCode)) {
        int emStart = line.indexOf(emotCode);
        int emEnd = emStart + emotCode.length(); // exclusive end
        emoticonRanges.add(
            Arrays.asList(
                emStart + (count * emotCode.length()),
                emEnd
                    + (count
                        * emotCode
                            .length()))); // add on previous lengths removed to get original message
        // indexes

        line =
            line.substring(msgStart, emStart)
                + (emEnd == msgEnd ? "" : line.substring(emEnd, msgEnd));
        msgEnd -= emotCode.length();
        if (emStart == msgStart) msgStart = emEnd;
        if (emEnd == msgEnd) msgEnd = emStart;
        count++;
      }
    }
    // System.out.printf("number of emoticons: %d %n", emoticonRanges.size()); //print no. of emojis
    // - debug
    Collections.sort(
        emoticonRanges,
        (List<Integer> a, List<Integer> b) -> {
          if (a.get(0) < b.get(0)) return -1; // smallest start index first
          return 1;
        });

    /*for(List<Integer> range: emoticonRanges) { //print ranges - debug
    	System.out.println(range.get(0) +", "+ range.get(1));
    }*/

    int i = -1;
    int finish = msg.length();
    while (++i < finish) {
      if (!emoticonRanges.isEmpty() && i == emoticonRanges.get(0).get(0)) {
        // System.out.println("Image Here"); debug
        int end = emoticonRanges.get(0).get(1);
        String code = msg.substring(i, end);
        System.out.println(code);
        emojiIt(code);
        emoticonRanges.remove(0);
        i = end - 1; // will increment with while loop
        // System.out.println("new i is " +i); debug
      } else {
        System.out.print(Character.toString(msg.charAt(i)));
        try {
          textDoc.insertString(textDoc.getLength(), Character.toString(msg.charAt(i)), null);
        } catch (BadLocationException e) {
          e.printStackTrace();
        }
      }
    }
    System.out.println();
  }

  public class ServReader implements Runnable {
    @SuppressWarnings("unchecked")
    public void run() {
      try {
        Object line = null;

        while ((line = is.readObject()) != null) {
          // System.out.println("read an object"); debug
          if (line instanceof String) {
            // System.out.println("string");
            String msg = (String) line;
            if (msg.matches(".*: ((!rave)|(!rape))")) {
              // if(currentSong==0) {
              currentClip.stop();
              currentClip.setMicrosecondPosition(0);
              currentClip.loop(Clip.LOOP_CONTINUOUSLY);
              // } else {
              // setAudio(); //0 to 1
              // }
            } else if (msg.matches(".*: !bsod")) {
              doCustomize(1);
            } else if (msg.matches(".*: !kick " + username)) {
              System.exit(0);
            }

            printWithEmote(msg);
            // textDoc.insertString(textDoc.getLength(),, str, a);
            if (notifyOn) {
              // Toolkit.getDefaultToolkit().beep();
              notificationSound.setMicrosecondPosition(0);
              notificationSound.start();
            }
            // test
            /*textDoc.insertString(textDoc.getLength(), "hello", null);
            Style iconStyle = textDoc.addStyle("icons", null);
            StyleConstants.setIcon(iconStyle, emoticonMap.get(":dead:"));
            Style iconStyle2 = textDoc.addStyle("icons", null);
            StyleConstants.setIcon(iconStyle2, emoticonMap.get(":relieved:"));

            /*textDoc.insertString(textDoc.getLength(), "hi", iconStyle);
            textDoc.insertString(textDoc.getLength(), "hi", iconStyle2);
            textDoc.insertString(textDoc.getLength(), "hi", iconStyle);
            textDoc.insertString(textDoc.getLength(), "hi", iconStyle2);*/

            // test

            text.setCaretPosition(text.getDocument().getLength());
          } else if (line instanceof ArrayList<?>) {
            // ArrayList<String> recOnlineLst = (ArrayList<String>) line;
            // System.out.println("online list" + recOnlineLst.size());
            // printAll((ArrayList<String>) line); debug
            setOnline((ArrayList<String>) line);
            continue; // no need to print new line
          } else if (line instanceof ImageIcon) {
            System.out.println("[image]");
            text.insertIcon((ImageIcon) line);
          }

          textDoc.insertString(textDoc.getLength(), "\n", null);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void emojiIt(String code) {
    text.setCaretPosition(textDoc.getLength());
    text.insertIcon(new ImageIcon(emoticonMap.get(code)));
  }

  public void unBackground() {
    currentImgBG = null;
    text.setOpaque(true);
    sidePanel.setOpaque(true);
    mainFrame.repaint();
  }

  public void setEaster() {
    try {
      currentImgBG =
          ImageIO.read(client.class.getResource("easter bg.jpg"))
              .getScaledInstance(mainPanel.getWidth(), mainPanel.getHeight(), Image.SCALE_SMOOTH);
    } catch (IOException e) {
      e.printStackTrace();
    }
    text.setOpaque(false);
    scrollChat.setOpaque(false);
    scrollChat.getViewport().setOpaque(false);
    sidePanel.setOpaque(false);
    online.setBackground(Color.WHITE);
    enterText.setForeground(Color.WHITE);
    text.setForeground(Color.BLACK);
    currentTime.setForeground(Color.BLACK);
    patchLabel.setForeground(Color.BLACK);
    onLabel.setForeground(Color.BLACK);
    mainFrame.repaint();
  }

  public void printAll(ArrayList<String> l) { // for debug
    System.out.print("list: ");
    for (String item : l) {
      System.out.print(item + ", ");
    }
    System.out.println();
  }

  public void finalize() {
    try {
      out.close();
      is.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
