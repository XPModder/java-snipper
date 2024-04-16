package java_snipper;

import javax.swing.*;

import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SnippingTool extends JFrame {

    private JPanel buttonPanel;
    private MyButton newSnip, close, fullSnip;

    private ImageIcon toolIcon, cancelIcon, fullscreenIcon;
    private ScreenCapture screenCapture;

    private Color selectionColor;

    private Thread thread;

    private JLabel editorPane;

    public SnippingTool() {

        // icon used on buttons;
        toolIcon = new ImageIcon(getClass().getResource("images/tool.png"));
        cancelIcon = new ImageIcon(getClass().getResource("images/close.png"));
        fullscreenIcon = new ImageIcon(getClass().getResource("images/fullscreen.png"));

        selectionColor = Color.RED;
        screenCapture = new ScreenCapture(selectionColor);

        // Create thread to capture image in new thread;
        // and to stop current thread;
        thread = new Thread(() -> {
            screenCapture.captureImage();
        });

        // set frame properties;
        this.setTitle("Snipping Tool");
        this.setIconImage(toolIcon.getImage());
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLayout(new BorderLayout());

        // Panel to set buttons on it;
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);

        // snip capture button;
        newSnip = new MyButton("Rectangle Snip", toolIcon, this::newSnipAction);
        buttonPanel.add(newSnip);

        fullSnip = new MyButton("Fullscreen Snip", fullscreenIcon, this::newFullAction);
        buttonPanel.add(fullSnip);

        // close button;
        close = new MyButton("Close", cancelIcon, this::closeAction);
        buttonPanel.add(close);

        // add panel to north side;
        this.add(buttonPanel, BorderLayout.NORTH);

        editorPane = new JLabel(){
            @Override
            public Dimension getPreferredSize(){
                if(this.getIcon() != null) {
                    return new Dimension(this.getIcon().getIconWidth(), this.getIcon().getIconHeight());
                }
                else{
                    return new Dimension(0, 0);
                }
            }
        };
        editorPane.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(editorPane, BorderLayout.SOUTH);


        this.pack();
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        // overriding close operation to stop thread and then to close;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thread = null;
                super.windowClosing(e);
            }
        });
    }

    public void newSnipAction(ActionEvent e) {

        Dimension size = this.getSize();
        Point location = this.getLocation();
        this.setSize(0, 0);
        this.setLocation(-1000, -1000);

        // create thread and capture image throw it;
        thread = new Thread(() -> {
            screenCapture = new ScreenCapture(selectionColor);
            screenCapture.captureImage();
            this.setSize(size);
            this.setLocation(location);
            if(screenCapture.isImageCaptured()){
                ImageIcon icon = new ImageIcon(screenCapture.getImage());
                editorPane.setIcon(icon);
                editorPane.setSize(editorPane.getPreferredSize());
                editorPane.repaint();
                this.repaint();
                this.setMinimumSize(new Dimension(icon.getIconWidth() + 35, 170 + icon.getIconHeight()));
            }
        });

        thread.start();

    }


    public void newFullAction(ActionEvent e) {

        this.setState(JFrame.ICONIFIED);

        // create thread and capture image throw it;
        thread = new Thread(() -> {
            screenCapture = new ScreenCapture(selectionColor);
            screenCapture.captureFullscreen();
        });

        thread.start();
        while(thread.isAlive()){}
        this.setState(JFrame.NORMAL);
    }


    public void closeAction(ActionEvent e) {
        thread = null;
        this.dispose();
    }

    public void showAndWait() {
        // show;
        this.setVisible(true);

        // wait;
        holdThread();
    }

    public ScreenCapture getScreenCapture() {
        return screenCapture;
    }

    public void setSelectionColor(Color selectionColor) {
        this.selectionColor = selectionColor;
    }

    // hold current thread here;
    // until snipped_tool is open;
    private void holdThread() {
        while(thread != null) {
            try {
                Thread.sleep(250);
            }
            catch (InterruptedException e) {}
        }
    }
}