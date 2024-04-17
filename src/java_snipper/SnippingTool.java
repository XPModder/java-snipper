package java_snipper;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;

import java.awt.datatransfer.Clipboard;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class SnippingTool extends JFrame {

    private JPanel buttonPanel;
    private MyButton newSnip, close, fullSnip, penBtn, markerBtn, copyBtn, saveBtn;

    private JScrollPane scrollPane;

    private ImageIcon toolIcon, cancelIcon, fullscreenIcon, penIcon, markerIcon, copyIcon, saveIcon;
    private ScreenCapture screenCapture;

    private Color selectionColor;

    private Thread thread;

    private JLabel editorPane;

    private boolean penActive = false, markerActive = false;
    private Color buttonBackgroundDefault;

    public SnippingTool() {

        // icon used on buttons;
        toolIcon = new ImageIcon(getClass().getResource("images/tool.png"));
        cancelIcon = new ImageIcon(getClass().getResource("images/close.png"));
        fullscreenIcon = new ImageIcon(getClass().getResource("images/fullscreen.png"));
        penIcon = new ImageIcon(getClass().getResource("images/fullscreen.png"));
        markerIcon = new ImageIcon(getClass().getResource("images/fullscreen.png"));
        copyIcon = new ImageIcon(getClass().getResource("images/fullscreen.png"));
        saveIcon = new ImageIcon(getClass().getResource("images/fullscreen.png"));

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
        newSnip = new MyButton("Rectangle", toolIcon, this::newSnipAction);
        buttonPanel.add(newSnip);

        fullSnip = new MyButton("Fullscreen", fullscreenIcon, this::newFullAction);
        buttonPanel.add(fullSnip);

        penBtn = new MyButton("Pen", penIcon, this::penAction);
        penBtn.setEnabled(false);
        buttonPanel.add(penBtn);

        markerBtn = new MyButton("Marker", markerIcon, this::markerAction);
        markerBtn.setEnabled(false);
        buttonPanel.add(markerBtn);

        copyBtn = new MyButton("Copy", copyIcon, this::copyAction);
        copyBtn.setEnabled(false);
        buttonPanel.add(copyBtn);

        saveBtn = new MyButton("Save", saveIcon, this::saveAction);
        saveBtn.setEnabled(false);
        buttonPanel.add(saveBtn);

        // close button;
        close = new MyButton("Close", cancelIcon, this::closeAction);
        buttonPanel.add(close);

        buttonBackgroundDefault = close.getBackground();

        // add panel to north side;
        this.add(buttonPanel, BorderLayout.NORTH);

        editorPane = new JLabel();
        editorPane.setHorizontalAlignment(SwingConstants.CENTER);
        editorPane.addMouseWheelListener(mouseWheelListener);

        scrollPane = new JScrollPane(editorPane){
            @Override
            public Dimension getPreferredSize(){
                if(editorPane.getIcon() != null) {
                    return new Dimension(editorPane.getIcon().getIconWidth(), editorPane.getIcon().getIconHeight());
                }
                else{
                    return new Dimension(0, 0);
                }
            }
        };
        scrollPane.setMaximumSize(new Dimension(1500, 800));

        this.add(scrollPane, BorderLayout.CENTER);


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
                editorPane.repaint();
                this.repaint();

                int width = icon.getIconWidth() + 35;
                int height = icon.getIconHeight() + 170;

                if(width > 1500){
                    width = 1500;
                }
                if(height > 800){
                    height = 800;
                }

                scrollPane.setSize(scrollPane.getPreferredSize());

                this.setMinimumSize(new Dimension(width, height));
                scrollPane.updateUI();

                penBtn.setEnabled(true);
                markerBtn.setEnabled(true);
                copyBtn.setEnabled(true);
                saveBtn.setEnabled(true);
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
            if(screenCapture.isImageCaptured()){
                ImageIcon icon = new ImageIcon(screenCapture.getImage());
                editorPane.setIcon(icon);
                editorPane.repaint();
                this.repaint();

                int width = icon.getIconWidth() + 35;
                int height = icon.getIconHeight() + 170;

                if(width > 1500){
                    width = 1500;
                }
                if(height > 800){
                    height = 800;
                }

                scrollPane.setSize(scrollPane.getPreferredSize());

                this.setMinimumSize(new Dimension(width, height));
                scrollPane.updateUI();

                penBtn.setEnabled(true);
                markerBtn.setEnabled(true);
                copyBtn.setEnabled(true);
                saveBtn.setEnabled(true);
            }
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


    public static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }


    MouseWheelListener mouseWheelListener = new MouseWheelListener() {
        @Override
        public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {

            if(!screenCapture.isImageCaptured()){
                return;
            }

            if(Keyboard.isKeyPressed(KeyEvent.VK_CONTROL)){

                int scrollAmount = mouseWheelEvent.getUnitsToScroll();

                int width = editorPane.getWidth();
                int height = editorPane.getHeight();

                float widthPercent = screenCapture.getImage().getWidth() / 100.0f;
                float heightPercent = screenCapture.getImage().getHeight() / 100.0f;

                float widthChange = widthPercent * scrollAmount;
                float heightChange = heightPercent * scrollAmount;

                Image image = ((ImageIcon)editorPane.getIcon()).getImage();
                image = image.getScaledInstance(Math.round(width + widthChange), Math.round(height + heightChange), Image.SCALE_SMOOTH);

                editorPane.setIcon(new ImageIcon(image));
                repaint();

            }

        }
    };



    MouseMotionListener mouseMotionListener = new MouseMotionListener() {
        @Override
        public void mouseDragged(MouseEvent mouseEvent) {

            int widthOffset = (editorPane.getWidth() - screenCapture.getImage().getWidth()) / 2; //This is WAY off lmao
            int heightOffset = (editorPane.getHeight() - screenCapture.getImage().getHeight()) / 2;

            int x = mouseEvent.getX() - widthOffset;
            int y = mouseEvent.getY() - heightOffset;

            BufferedImage image = toBufferedImage(((ImageIcon)editorPane.getIcon()).getImage());
            Graphics g2d = image.getGraphics();
            g2d.setColor(Color.red);

            if(penActive) {
                g2d.fillOval(x - 5, y - 5, 10, 10);
            }
            if(markerActive){
                g2d.fillRect(x - 5, y - 2, 10, 4);
            }

            g2d.dispose();
            editorPane.setIcon(new ImageIcon(image));
            repaint();

        }

        @Override
        public void mouseMoved(MouseEvent mouseEvent) {

        }
    };


    public void penAction(ActionEvent e){

        if(!penActive) {
            if (screenCapture.isImageCaptured()) {

                if(markerActive){
                    markerActive = false;
                    markerBtn.setBackground(buttonBackgroundDefault);
                    editorPane.removeMouseMotionListener(mouseMotionListener);
                }

                penActive = true;
                penBtn.setBackground(Color.green);
                editorPane.addMouseMotionListener(mouseMotionListener);
            }
        }
        else{
            penActive = false;
            penBtn.setBackground(buttonBackgroundDefault);
            editorPane.removeMouseMotionListener(mouseMotionListener);
        }

    }

    public void markerAction(ActionEvent e){

        if(!markerActive){
            if(screenCapture.isImageCaptured()) {

                if(penActive){
                    penActive = false;
                    penBtn.setBackground(buttonBackgroundDefault);
                    editorPane.removeMouseMotionListener(mouseMotionListener);
                }

                markerActive = true;
                markerBtn.setBackground(Color.green);
                editorPane.addMouseMotionListener(mouseMotionListener);
            }
        }
        else{
            markerActive = false;
            markerBtn.setBackground(buttonBackgroundDefault);
            editorPane.removeMouseMotionListener(mouseMotionListener);
        }


    }

    public void copyAction(ActionEvent e){

        if(screenCapture.isImageCaptured()) {

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new TransferableImage(screenCapture.getImage()), null);

        }
    }

    public void saveAction(ActionEvent e){

        if(!screenCapture.isImageCaptured()){
            return;
        }

        FileDialog fDialog = new FileDialog(this, "Save as...", FileDialog.SAVE);
        FilenameFilter filter = (file, s) -> {
            if(file.getName().endsWith(".png")){
                return true;
            }
            else if(file.getName().endsWith(".jpg")){
                return true;
            }
            return false;
        };
        fDialog.setFilenameFilter(filter);
        fDialog.setVisible(true);

        String filename = fDialog.getFile();
        if(filename == null){
            return;
        }

        try {

            if(filename.endsWith("png")) {
                ImageIO.write(toBufferedImage(((ImageIcon)editorPane.getIcon()).getImage()), "png", new File(filename));
            }
            else if(filename.endsWith("jpg")){
                ImageIO.write(toBufferedImage(((ImageIcon)editorPane.getIcon()).getImage()), "jpg", new File(filename));
            }

        }
        catch (IOException ex){
            Dialog dialog = new Dialog(this, "Error saving file!");
            JLabel label = new JLabel();
            label.setText("Could not save the image at: " + filename + "!");
            dialog.add(label);
            dialog.setVisible(true);
        }


    }

}