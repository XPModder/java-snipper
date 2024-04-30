package com.xpmodder.javasnipper;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeListener;

import java.awt.*;

import java.awt.datatransfer.Clipboard;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.awt.Color.*;

public class SnippingTool extends JFrame {

    private JPanel buttonPanel;
    private MyButton newSnip, close, fullSnip, penBtn, markerBtn, copyBtn, saveBtn, colorBtn, eraserBtn, resetScaleBtn;

    private JScrollPane scrollPane;

    private ImageIcon toolIcon, cancelIcon, fullscreenIcon, penIcon, markerIcon, copyIcon, saveIcon, colorIcon, eraserIcon, resetScaleIcon;
    private ScreenCapture screenCapture;

    private Color selectionColor;

    private Thread thread;

    private JLabel editorPane;

    private boolean penActive = false, markerActive = false, eraserActive = false;
    private Color buttonBackgroundDefault, currentPenColor = red, activeButtonColor = new Color(171, 197, 240);

    private int currentPenSize = 1, currentMarkerSize = 2, currentDrawingImage = 0;

    private List<BufferedImage> drawingImages = new ArrayList<>();

    private Point lastDragPoint;

    private JDialog dialog = new JDialog(this);
    private JLabel DialogLabel = new JLabel();
    private JSlider DialogSlider = new JSlider();
    private JPanel DialogPanel = new JPanel();


    private void updateColorButton(){
        BufferedImage bufImg = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics g2d = bufImg.getGraphics();
        g2d.setColor(currentPenColor);
        g2d.fillRect(0, 0, 24, 24);
        g2d.dispose();
        colorIcon = new ImageIcon(bufImg);

        colorBtn.setIcon(colorIcon);
        repaint();
    }


    private void updateEditorPane(){
        int width = editorPane.getIcon().getIconWidth();
        int height = editorPane.getIcon().getIconHeight();

        updateEditorPane(width, height);
    }

    private void updateEditorPane(int width, int height){

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); //Make new blank image
        Graphics g2d = image.getGraphics();
        g2d.drawImage(screenCapture.getImage(), 0, 0, width, height, null); //draw the screenshot to the new image
        for(BufferedImage img : drawingImages){
            g2d.drawImage(img, 0, 0, width, height, null);  //draw all user additions (everything drawn with pen or marker) over the new image
        }
        g2d.dispose();  //now the image contains the screenshot and overlayed on top of it, all the things the user has drawn
        editorPane.setIcon(new ImageIcon(image));   //display the new image and repaint everything to update the display
        repaint();

    }


    private void disableAllTools(){
        if(penActive){
            penActive = false;
            penBtn.setBackground(buttonBackgroundDefault);
            editorPane.removeMouseMotionListener(mouseMotionListener);
            editorPane.removeMouseListener(editorPaneMouseListener);
        }
        else if(markerActive){
            markerActive = false;
            markerBtn.setBackground(buttonBackgroundDefault);
            editorPane.removeMouseMotionListener(mouseMotionListener);
            editorPane.removeMouseListener(editorPaneMouseListener);
        }
        else if(eraserActive){
            eraserActive = false;
            eraserBtn.setBackground(buttonBackgroundDefault);
            editorPane.removeMouseListener(editorPaneMouseListener);
        }
    }


    private void enableButtons(){
        penBtn.setEnabled(true);
        markerBtn.setEnabled(true);
        copyBtn.setEnabled(true);
        saveBtn.setEnabled(true);
        colorBtn.setEnabled(true);
        eraserBtn.setEnabled(true);
        resetScaleBtn.setEnabled(true);
    }


    public SnippingTool() {


        DialogSlider.setBackground(activeButtonColor);
        DialogPanel.setBackground(activeButtonColor);
        DialogPanel.add(DialogLabel);
        DialogPanel.add(DialogSlider);
        dialog.add(DialogPanel);
        dialog.setUndecorated(true);
        dialog.setSize(200, 50);

        // icon used on buttons;
        toolIcon = new ImageIcon(getClass().getResource("images/tool.png"));
        cancelIcon = new ImageIcon(getClass().getResource("images/close.png"));
        fullscreenIcon = new ImageIcon(getClass().getResource("images/fullscreen.png"));
        penIcon = new ImageIcon(getClass().getResource("images/pen.png"));
        markerIcon = new ImageIcon(getClass().getResource("images/marker.png"));
        copyIcon = new ImageIcon(getClass().getResource("images/copy.png"));
        saveIcon = new ImageIcon(getClass().getResource("images/save.png"));
        eraserIcon = new ImageIcon(getClass().getResource("images/eraser.png"));
        resetScaleIcon = new ImageIcon(getClass().getResource("images/zoom.png"));

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

        penBtn = new MyButton("Pen " + currentPenSize, penIcon, this::penAction);
        penBtn.setEnabled(false);
        penBtn.addMouseListener(buttonMouseListener);
        buttonPanel.add(penBtn);

        markerBtn = new MyButton("Marker " + currentMarkerSize, markerIcon, this::markerAction);
        markerBtn.setEnabled(false);
        markerBtn.addMouseListener(buttonMouseListener);
        buttonPanel.add(markerBtn);

        colorBtn = new MyButton("Color", colorIcon, this::colorAction);
        colorBtn.setEnabled(false);
        updateColorButton();
        buttonPanel.add(colorBtn);

        eraserBtn = new MyButton("Eraser", eraserIcon, e -> {
            if(eraserActive){
                disableAllTools();
            }
            else{
                disableAllTools();
                eraserActive = true;
                eraserBtn.setBackground(activeButtonColor);
                editorPane.addMouseListener(editorPaneMouseListener);
            }
        });
        eraserBtn.setEnabled(false);
        buttonPanel.add(eraserBtn);

        copyBtn = new MyButton("Copy", copyIcon, this::copyAction);
        copyBtn.setEnabled(false);
        buttonPanel.add(copyBtn);

        saveBtn = new MyButton("Save", saveIcon, this::saveAction);
        saveBtn.setEnabled(false);
        buttonPanel.add(saveBtn);

        resetScaleBtn = new MyButton("Reset Zoom", resetScaleIcon, e -> updateEditorPane(screenCapture.getImage().getWidth(), screenCapture.getImage().getHeight()));
        resetScaleBtn.setEnabled(false);
        buttonPanel.add(resetScaleBtn);

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


        this.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if(Keyboard.isKeyPressed(KeyEvent.VK_CONTROL) && Keyboard.isKeyPressed(KeyEvent.VK_Z)){
                    if(!drawingImages.isEmpty()) {
                        drawingImages.remove(drawingImages.size() - 1);

                        int width = screenCapture.getImage().getWidth();
                        int height = screenCapture.getImage().getHeight();
                        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                        Graphics g2d = image.getGraphics();
                        g2d.drawImage(screenCapture.getImage(), 0, 0, width, height, null);
                        for (BufferedImage img : drawingImages) {
                            g2d.drawImage(img, 0, 0, width, height, null);
                        }
                        g2d.dispose();
                        editorPane.setIcon(new ImageIcon(image));
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });

    }

    public void newSnipAction(ActionEvent e) {

        this.setVisible(false);

        // create thread and capture image throw it;
        thread = new Thread(() -> {
            screenCapture = new ScreenCapture(selectionColor);
            screenCapture.captureImage();
            this.setVisible(true);
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

                if(width < ((10 * 90) + (9 * 10))){
                    this.setMinimumSize(new Dimension((10 * 90) + (9 * 10), height));
                }
                else{
                    this.setMinimumSize(new Dimension(width, height));
                }

                drawingImages.clear();

                scrollPane.updateUI();

                enableButtons();
            }
        });

        thread.start();

    }


    public void newFullAction(ActionEvent e) {

        try {

            this.setExtendedState(JFrame.ICONIFIED);

            TimeUnit.SECONDS.sleep(1);

        }
        catch (Exception ex){
            ex.printStackTrace();
            return;
        }

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

                if(width < ((10 * 90) + (9 * 10))){
                    this.setMinimumSize(new Dimension((10 * 90) + (9 * 10), height));
                }
                else{
                    this.setMinimumSize(new Dimension(width, height));
                }

                drawingImages.clear();

                scrollPane.updateUI();

                enableButtons();
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


    MouseListener buttonMouseListener = new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {

            if(dialog.isVisible()){
                dialog.setVisible(false);
                dialog.dispose();
            }

            if(e.getButton() == MouseEvent.BUTTON3){
                if(e.getComponent() instanceof MyButton button){
                    if(button.isEnabled()) {
                        String text = button.getText();
                        dialog.dispose();
                        for(ChangeListener cl : DialogSlider.getChangeListeners()) {
                            DialogSlider.removeChangeListener(cl);
                        }

                        if(text.contains("Pen")){

                            Point location = new Point();
                            location.x = (int)Math.round(button.getLocationOnScreen().getX() + (button.getWidth() / 2.0) - 100);
                            location.y = (int)Math.round(button.getLocationOnScreen().getY() + button.getHeight());

                            DialogLabel.setText("Pen Size:");

                            DialogSlider.setMinimum(1);
                            DialogSlider.setMaximum(10);
                            DialogSlider.setValue(currentPenSize);
                            DialogSlider.addChangeListener(cl -> {
                                currentPenSize = DialogSlider.getValue();
                                penBtn.setText("Pen " + currentPenSize);
                                repaint();
                            });

                            dialog.setLocation(location);
                            dialog.setVisible(true);

                        }
                        else if(text.contains("Marker")){

                            Point location = new Point();
                            location.x = (int)Math.round(button.getLocationOnScreen().getX() + (button.getWidth() / 2.0) - 100);
                            location.y = (int)Math.round(button.getLocationOnScreen().getY() + button.getHeight());

                            DialogLabel.setText("Marker Size:");

                            DialogSlider.setMinimum(1);
                            DialogSlider.setMaximum(10);
                            DialogSlider.setValue(currentMarkerSize);
                            DialogSlider.addChangeListener(cl -> {
                                currentMarkerSize = DialogSlider.getValue();
                                markerBtn.setText("Marker " + currentMarkerSize);
                                repaint();
                            });

                            dialog.setLocation(location);
                            dialog.setVisible(true);

                        }
                    }
                }

            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    };


    MouseWheelListener mouseWheelListener = new MouseWheelListener() {
        @Override
        public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {

            if(!screenCapture.isImageCaptured()){
                return;
            }

            if(Keyboard.isKeyPressed(KeyEvent.VK_CONTROL)){

                int scrollAmount = mouseWheelEvent.getUnitsToScroll();

                scrollAmount *= -1;

                int width = editorPane.getIcon().getIconWidth();
                int height = editorPane.getIcon().getIconHeight();

                float widthPercent = screenCapture.getImage().getWidth() / 100.0f;
                float heightPercent = screenCapture.getImage().getHeight() / 100.0f;

                float widthChange = widthPercent * scrollAmount;
                float heightChange = heightPercent * scrollAmount;

                updateEditorPane(Math.round(width + widthChange), Math.round(height + heightChange));

            }
            else{
                scrollPane.dispatchEvent(mouseWheelEvent);
            }

        }
    };



    MouseMotionListener mouseMotionListener = new MouseMotionListener() {
        @Override
        public void mouseDragged(MouseEvent mouseEvent) {

            int x = (int)Math.round(mouseEvent.getPoint().getX());
            int y = (int)Math.round(mouseEvent.getPoint().getY());


            double zoomX = (double) screenCapture.getImage().getWidth() / editorPane.getIcon().getIconWidth();
            double zoomY = (double) screenCapture.getImage().getHeight() / editorPane.getIcon().getIconHeight();

            int offsetX = (editorPane.getWidth() - editorPane.getIcon().getIconWidth()) / 2; //TODO: Fix pen position in case of zoomed small image
            int offsetY = (editorPane.getHeight() - editorPane.getIcon().getIconHeight()) / 2;

            x = (int) Math.round(x * zoomX);
            y = (int) Math.round(y * zoomY);

            x -= offsetX;
            y -= offsetY;


            if((currentDrawingImage == (drawingImages.size() - 1)) && lastDragPoint != null){ //we are still drawing on the same image as last time, so same overall drag event

                int oldX = lastDragPoint.x;
                int oldY = lastDragPoint.y;

                int xDist = x - oldX;
                int yDist = y - oldY;

                double distance = Math.sqrt(((y - oldY)^2)+((x - oldX)^2));

                int dist = (int)Math.round(distance);
                if(dist != 0){
                    xDist /= dist;
                    yDist /= dist;
                }
                else{
                    xDist = 0;
                    yDist = 0;
                }

                int currX = oldX + xDist;
                int currY = oldY + yDist;

                Graphics g2d = drawingImages.get(currentDrawingImage).getGraphics();
                g2d.setColor(currentPenColor);

                if(penActive && (distance > currentPenSize * 2.5)){
                    int actualSize = currentPenSize * 5;
                    for(int i = 0; i < dist; i++){
                        g2d.fillOval(currX - (actualSize / 2), currY - (actualSize / 2), actualSize, actualSize);
                        currX += xDist;
                        currY += yDist;
                    }
                }
                else if(markerActive && (distance > currentMarkerSize * 2.5)){
                    int actualSize = currentMarkerSize * 5;
                    for(int i = 0; i < dist; i++){
                        g2d.fillOval(currX - (actualSize / 2), currY - (actualSize / 2), actualSize, 4);
                        currX += xDist;
                        currY += yDist;
                    }
                }
                g2d.dispose();

            }

            lastDragPoint = new Point(x, y);
            currentDrawingImage = drawingImages.size() - 1;
            Graphics g2d = drawingImages.get(drawingImages.size() - 1).getGraphics();
            g2d.setColor(currentPenColor);

            if(penActive) {
                int actualSize = currentPenSize * 5;
                g2d.fillOval(x - (actualSize / 2), y - (actualSize / 2), actualSize, actualSize);
            }
            if(markerActive){
                int actualSize = currentMarkerSize * 5;
                g2d.fillRect(x - (actualSize / 2), y - (actualSize / 2), actualSize, 4);
            }

            g2d.dispose();

            updateEditorPane();

        }

        @Override
        public void mouseMoved(MouseEvent mouseEvent) {

        }
    };


    MouseListener editorPaneMouseListener = new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent mouseEvent) {

        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {

            if(eraserActive){

                int x = (int)Math.round(mouseEvent.getPoint().getX());
                int y = (int)Math.round(mouseEvent.getPoint().getY());


                double zoomX = (double) screenCapture.getImage().getWidth() / editorPane.getIcon().getIconWidth();
                double zoomY = (double) screenCapture.getImage().getHeight() / editorPane.getIcon().getIconHeight();

                x = (int) Math.round(x * zoomX);
                y = (int) Math.round(y * zoomY);

                List<BufferedImage> toDelete = new ArrayList<>();

                for(BufferedImage img : drawingImages) {

                    Color color = new Color(img.getRGB(x, y), true);
                    if (color.getAlpha() > 0) {
                        toDelete.add(img);
                    }

                }
                drawingImages.removeAll(toDelete);

                updateEditorPane();

            }
            else{
                drawingImages.add( new BufferedImage(screenCapture.getImage().getWidth(), screenCapture.getImage().getHeight(), BufferedImage.TYPE_INT_ARGB));
            }

        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {

        }

        @Override
        public void mouseEntered(MouseEvent mouseEvent) {

        }

        @Override
        public void mouseExited(MouseEvent mouseEvent) {

        }
    };


    public void penAction(ActionEvent e){

        if(!penActive) {
            if (screenCapture.isImageCaptured()) {

                disableAllTools();

                penActive = true;
                penBtn.setBackground(activeButtonColor);
                editorPane.addMouseMotionListener(mouseMotionListener);
                editorPane.addMouseListener(editorPaneMouseListener);
            }
        }
        else{
            disableAllTools();
        }

    }

    public void markerAction(ActionEvent e){

        if(!markerActive){
            if(screenCapture.isImageCaptured()) {

                disableAllTools();

                markerActive = true;
                markerBtn.setBackground(activeButtonColor);
                editorPane.addMouseMotionListener(mouseMotionListener);
                editorPane.addMouseListener(editorPaneMouseListener);
            }
        }
        else{
            disableAllTools();
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

            int width = screenCapture.getImage().getWidth();
            int height = screenCapture.getImage().getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics g2d = image.getGraphics();
            g2d.drawImage(screenCapture.getImage(), 0, 0, width, height, null);
            for(BufferedImage img : drawingImages){
                g2d.drawImage(img, 0, 0, width, height, null);
            }
            g2d.dispose();

            if(filename.endsWith("png")) {
                ImageIO.write(image, "png", new File(filename));
            }
            else if(filename.endsWith("jpg")){
                ImageIO.write(image, "jpg", new File(filename));
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


    public void colorAction(ActionEvent event){
        Color newColor = JColorChooser.showDialog(null, "Choose color", currentPenColor);
        if(newColor != null){
            currentPenColor = newColor;

            updateColorButton();

        }
    }

}