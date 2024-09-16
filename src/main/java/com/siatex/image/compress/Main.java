package com.siatex.image.compress;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;

import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.luciad.imageio.webp.WebPWriteParam;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Apon
 */
public class Main extends javax.swing.JFrame {

    // Define supported image extensions as a static final array
    private Color backgroundColor = Color.BLACK; // Class property for background color

    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp"};
    private List<ImageIcon> generateIcons;
    private Timer animationTimer;

    public Main() {
        WindowIconSetter.setIcon(this);
        initComponents();
        generateProgress.setVisible(false);
        openDirLbl.setVisible(false);
        generateIcons = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            //new javax.swing.ImageIcon(getClass().getResource("/icons/mac-folder-20.png"))
            generateIcons.add(new ImageIcon(getClass().getResource("/icons/genereate" + i + ".png")));
        }
        //path.setEditable(false);
        path.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Browse Images");
        // Add the JRadioButton components to the ButtonGroup
        compressOptionsGroup.add(compressByPercentage);
        compressOptionsGroup.add(compressBySize);
        compressOptionsGroup.add(compressByDim);

        eventInit();
        optionEnable("percentage");

        percentage.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // Update the label with the current value of the slider
                int value = percentage.getValue();
                percentageLbl.setText(value + "%");
            }
        });

        bright.setValue(100);
        bright.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // Update the label with the current value of the slider
                int value = bright.getValue();
                if (value != 100) {
                    resetBright.setEnabled(true);
                } else {
                    resetBright.setEnabled(false);
                }
                brightLbl.setText(value + "%");
            }
        });
        resetBright.setEnabled(false);

        sharp.setValue(100);
        sharp.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // Update the label with the current value of the slider
                int value = sharp.getValue();
                if (value != 100) {
                    resetSharp.setEnabled(true);
                } else {
                    resetSharp.setEnabled(false);
                }
                sharpLbl.setText(value + "%");
            }
        });
        resetSharp.setEnabled(false);

        // Add action listeners to radio buttons
        compressByPercentage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionEnable("percentage");
            }
        });

        compressBySize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionEnable("size");
            }
        });
        compressByDim.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionEnable("dim");
            }
        });

        // Add action listener for generate button
        generateImage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                compressImages();
            }
        });
    }

    private void optionEnable(String option) {
        if (option.equals("percentage")) {
            PercentageCompression.setVisible(true);
            SizeCompression.setVisible(false);
            DimCompression.setVisible(false);
        } else if (option.equals("dim")) {
            PercentageCompression.setVisible(false);
            DimCompression.setVisible(true);
            SizeCompression.setVisible(false);
        } else {
            PercentageCompression.setVisible(false);
            DimCompression.setVisible(false);
            SizeCompression.setVisible(true);
        }
    }

    private void startAnimation() {
        animationTimer = new Timer(200, new ActionListener() { // 200 ms interval
            private int iconIndex = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                iconIndex = (iconIndex + 1) % generateIcons.size(); // Cycle through icons
                generateImage.setIcon(generateIcons.get(iconIndex));
            }
        });
        animationTimer.start();
    }

    private void stopAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
            generateImage.setIcon(generateIcons.get(0)); // Reset to initial icon
        }
    }

    private void compressImages() {

        String selectedPath = path.getText();
        File directory = new File(selectedPath);
        File[] imageFiles = directory.listFiles((dir, name) -> {
            String lowerCaseName = name.toLowerCase();
            for (String extension : IMAGE_EXTENSIONS) {
                if (lowerCaseName.endsWith(extension)) {
                    return true;
                }
            }
            return false;
        });

        if (imageFiles == null || imageFiles.length == 0) {
            browseSelectionInfo.setText("No images to compress.");
            return;
        }

        File compressedDir = new File(directory, outputDir.getText());
        if (!compressedDir.exists()) {
            compressedDir.mkdir();
        }

        generateProgress.setMaximum(imageFiles.length);
        generateProgress.setValue(0);
        startAnimation(); // Start the icon animation
        generateProgress.setVisible(true);
        openDirLbl.setVisible(true);
        openDirLbl.setEnabled(false);
        // Use SwingWorker to handle the compression on a background thread
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (compressByPercentage.isSelected()) {
                    float qualityValue = percentage.getValue() / 100.0f; // Convert slider value to quality (0.0 - 1.0)
                    for (int i = 0; i < imageFiles.length; i++) {
                        File imageFile = imageFiles[i];
                        try {
                            // Read the image
                            BufferedImage image = ImageIO.read(imageFile);
                            // Get the selected format from the JComboBox
                            String selectedFormat = (String) outputFormat.getSelectedItem();

                            // Create the output file with the desired extension
                            String originalFileName = imageFile.getName();
                            String newFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.')) + "." + selectedFormat.toLowerCase();
                            File outputFile = new File(compressedDir, newFileName);

                            // Compress the image with the specified quality and format
                            compressByQuality(image, outputFile, qualityValue, selectedFormat);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        publish(i + 1); // Update progress
                    }
                } else if (compressBySize.isSelected()) {
                    long maxSizeKB = Long.parseLong(maxSize.getText());
                    for (int i = 0; i < imageFiles.length; i++) {
                        File imageFile = imageFiles[i];
                        try {
                            // Read the image
                            BufferedImage image = ImageIO.read(imageFile);
                            // Get the selected format from the JComboBox
                            String selectedFormat = (String) outputFormat.getSelectedItem();

                            // Create the output file with the desired extension
                            String originalFileName = imageFile.getName();
                            String newFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.')) + "." + selectedFormat.toLowerCase();
                            File outputFile = new File(compressedDir, newFileName);

                            // Compress the image with the specified quality and format
                            compressToMaxSize(image, outputFile, maxSizeKB, selectedFormat);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        publish(i + 1); // Update progress
                    }
                } else if (compressByDim.isSelected()) {
                    int width = Integer.parseInt(dimW.getText());
                    int height = Integer.parseInt(dimH.getText());
                    for (int i = 0; i < imageFiles.length; i++) {
                        File imageFile = imageFiles[i];
                        try {
                            // Read the image
                            BufferedImage image = ImageIO.read(imageFile);
                            // Get the selected format from the JComboBox
                            String selectedFormat = (String) outputFormat.getSelectedItem();

                            // Create the output file with the desired extension
                            String originalFileName = imageFile.getName();
                            String newFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.')) + "." + selectedFormat.toLowerCase();
                            File outputFile = new File(compressedDir, newFileName);

                            // Compress the image with the specified quality and format
                            resizeByDimension(image, outputFile, width, height, selectedFormat);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        publish(i + 1); // Update progress
                    }

                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                // Update progress bar with the most recent value
                int progress = chunks.get(chunks.size() - 1);
                generateProgress.setValue(progress);
            }

            @Override
            protected void done() {
                browseSelectionInfo.setText("Compression completed!");
                openDirLbl.setEnabled(true);
                stopAnimation(); // Stop the icon animation when done
            }
        };

        worker.execute();
    }

    // Method to apply sharpness to the image
    private BufferedImage applySharpness(BufferedImage originalImage, float sharpnessFactor) {
        // Create a sharpen kernel
        float[] sharpenKernel = {
            0f, -1f * sharpnessFactor, 0f,
            -1f * sharpnessFactor, 5f * sharpnessFactor, -1f * sharpnessFactor,
            0f, -1f * sharpnessFactor, 0f
        };
        Kernel kernel = new Kernel(3, 3, sharpenKernel);
        ConvolveOp convolveOp = new ConvolveOp(kernel);

        // Apply the sharpness
        return convolveOp.filter(originalImage, null);
    }

    private BufferedImage enhanceColor(BufferedImage originalImage, float enhancementFactor) {
        BufferedImage enhancedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());

        for (int y = 0; y < originalImage.getHeight(); y++) {
            for (int x = 0; x < originalImage.getWidth(); x++) {
                int rgb = originalImage.getRGB(x, y);
                int a = (rgb >> 24) & 0xff; // Alpha
                int r = (rgb >> 16) & 0xff; // Red
                int g = (rgb >> 8) & 0xff;  // Green
                int b = rgb & 0xff;         // Blue

                // Increase brightness
                r = Math.min(255, (int) (r * enhancementFactor));
                g = Math.min(255, (int) (g * enhancementFactor));
                b = Math.min(255, (int) (b * enhancementFactor));

                // Set the new RGB value
                enhancedImage.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return enhancedImage;
    }

    private BufferedImage preModify(BufferedImage image) {
        // Get values from sliders or input fields
        float sharpFactor = sharp.getValue();
        float enhancementFactor = bright.getValue();

        // Check and apply sharpness if within valid range
        if (sharpFactor >= 0 && sharpFactor <= 200 && sharpFactor != 100) {
            // Convert to range suitable for the sharpness method
            sharpFactor /= 100.0f; // Convert to [0.0f, 1.0f] range
            image = applySharpness(image, sharpFactor);
        }

        // Check and apply color enhancement if within valid range
        if (enhancementFactor >= 0 && enhancementFactor <= 200 && enhancementFactor != 100) {
            // Convert to range suitable for the color enhancement method
            enhancementFactor /= 100.0f; // Convert to [0.0f, 1.0f] range
            image = enhanceColor(image, enhancementFactor);
        }

        if (image.getType() == 6) {
            // Create a new BufferedImage with the same dimensions and a solid background color
            BufferedImage imageWithBackground = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

            // Fill the background
            Graphics2D g = imageWithBackground.createGraphics();
            g.setColor(backgroundColor);
            g.fillRect(0, 0, imageWithBackground.getWidth(), imageWithBackground.getHeight());
            g.drawImage(image, 0, 0, null);
            g.dispose();
            image = imageWithBackground; // Use the new image with background
        }

        return image;
    }

    public void resizeByDimension(BufferedImage image, File outputFile, int width, int height, String selectedFormat) throws IOException {
        // Create a resized version of the original image

        BufferedImage originalImage = preModify(image);

        BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D g2d = resizedImage.createGraphics();

        //System.out.println("W-"+width+" H-"+height);
        // Draw the original image into the resized image with the new dimensions
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();

        // Configure the image writer and parameters based on the format
        ImageWriter imageWriter = ImageIO.getImageWritersByFormatName(selectedFormat).next();
        ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();

        if ("jpg".equalsIgnoreCase(selectedFormat) || "jpeg".equalsIgnoreCase(selectedFormat)) {
            // Set JPEG-specific parameters if needed
            if (imageWriteParam.canWriteCompressed()) {
                imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                imageWriteParam.setCompressionQuality(1.0f); // Max quality by default, adjust as needed
            }
        } else if ("webp".equalsIgnoreCase(selectedFormat)) {
            // Set WebP-specific parameters if needed
            WebPWriteParam writeParam = new WebPWriteParam(imageWriter.getLocale());
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

            String[] compressionTypes = writeParam.getCompressionTypes();
            if (compressionTypes != null && compressionTypes.length > 0) {
                writeParam.setCompressionType(compressionTypes[WebPWriteParam.LOSSY_COMPRESSION]); // Use lossy compression
            }

            writeParam.setCompressionQuality(1.0f); // Max quality by default, adjust as needed
            imageWriteParam = writeParam;
        }

        // Write the resized image to the output file
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputFile)) {
            imageWriter.setOutput(outputStream);
            imageWriter.write(null, new IIOImage(resizedImage, null, null), imageWriteParam);
        } finally {
            imageWriter.dispose();
        }
    }

    // Updated compressByQuality method
    public void compressByQuality(BufferedImage originalImage, File outputFile, float quality, String selectedFormat) throws IOException {
        // Enhance the color of the original image
        BufferedImage enhancedImage = preModify(originalImage);

        // Check if the format is JPG or WEBP and handle the background color for PNG
        // Convert to RGB if the selected format is JPG
        
        if ("jpg".equalsIgnoreCase(selectedFormat)) {
            BufferedImage rgbImage = new BufferedImage(enhancedImage.getWidth(), enhancedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = rgbImage.getGraphics();
            g.drawImage(enhancedImage, 0, 0, null);
            g.dispose();
            enhancedImage = rgbImage; // Use the RGB image for further processing
        }
        
        // Get an ImageWriter for the desired output format (e.g., JPG, PNG, WEBP)
        ImageWriter imageWriter = null;
        ImageWriteParam imageWriteParam = null;

        // Select the appropriate writer based on the selected format
        switch (selectedFormat.toLowerCase()) {
            case "jpg":
                imageWriter = ImageIO.getImageWritersByFormatName("jpg").next();
                imageWriteParam = imageWriter.getDefaultWriteParam();
                // Set the compression quality for JPEG
                imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                imageWriteParam.setCompressionQuality(quality);
                break;
            case "png":
                imageWriter = ImageIO.getImageWritersByFormatName("png").next();
                imageWriteParam = imageWriter.getDefaultWriteParam();
                // PNG uses lossless compression; control over compression levels is limited
                if (imageWriteParam.canWriteCompressed()) {
                    imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                }
                break;
            case "webp":
                imageWriter = ImageIO.getImageWritersByMIMEType("image/webp").next();
                // Use WebPWriteParam for setting WebP-specific parameters
                WebPWriteParam writeParam = new WebPWriteParam(imageWriter.getLocale());
                // Set the compression mode and quality
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                // Choose between lossless and lossy compression
                String[] compressionTypes = writeParam.getCompressionTypes();
                if (compressionTypes != null && compressionTypes.length > 0) {
                    writeParam.setCompressionType(compressionTypes[WebPWriteParam.LOSSY_COMPRESSION]); // or WebPWriteParam.LOSSY_COMPRESSION
                }
                // Set the compression quality
                writeParam.setCompressionQuality(quality);
                // Assign the writeParam for further usage
                imageWriteParam = writeParam;
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + selectedFormat);
        }

        // Write the image with the specified quality
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputFile)) {
            imageWriter.setOutput(outputStream);
            imageWriter.write(null, new IIOImage(enhancedImage, null, null), imageWriteParam);
        } finally {
            if (imageWriter != null) {
                imageWriter.dispose();
            }
        }
    }

    public void compressToMaxSize(BufferedImage originalImage, File outputFile, float maxSizeKB, String formatName) throws IOException {
        float quality = 1.0f; // Start with the highest quality
        File tempFile = null;

        BufferedImage enhancedImage = preModify(originalImage);

        // Loop until the compressed image is below or equal to maxSizeKB
        do {
            // Create a temporary file for size calculation
            tempFile = File.createTempFile("tempCompressedImage", "." + formatName);
            tempFile.deleteOnExit(); // Ensure the temp file is deleted on exit

            // Configure the image writer and compression settings
            ImageWriter imageWriter = ImageIO.getImageWritersByFormatName(formatName).next();
            ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();

            if ("jpg".equalsIgnoreCase(formatName) || "jpeg".equalsIgnoreCase(formatName)) {
                if (imageWriteParam.canWriteCompressed()) {
                    imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    imageWriteParam.setCompressionQuality(quality);
                }
            } else if ("webp".equalsIgnoreCase(formatName)) {
                WebPWriteParam writeParam = new WebPWriteParam(imageWriter.getLocale());
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

                String[] compressionTypes = writeParam.getCompressionTypes();
                if (compressionTypes != null && compressionTypes.length > 0) {
                    writeParam.setCompressionType(compressionTypes[WebPWriteParam.LOSSY_COMPRESSION]);
                }

                writeParam.setCompressionQuality(quality);
                imageWriteParam = writeParam;
            }

            // Write the compressed image to the temporary file
            try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(tempFile)) {
                imageWriter.setOutput(outputStream);
                imageWriter.write(null, new IIOImage(enhancedImage, null, null), imageWriteParam);
            } finally {
                imageWriter.dispose();
            }

            // Check the size of the temporary file
            if (tempFile.length() / 1024 <= maxSizeKB) {
                // If the size is within the limit, copy to the final output file
                Files.copy(tempFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                break;
            }

            // Reduce quality and try again
            quality -= 0.1f; // Decrease quality
            quality = Math.max(quality, 0.1f); // Ensure quality does not drop below 0.1

        } while (tempFile.length() / 1024 > maxSizeKB); // Check the size in KB
    }

    public static void main(String args[]) {
        //FlatRobotoFont.install();
        FlatLaf.registerCustomDefaultsSource("themes");
        //UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        FlatMacLightLaf.setup();
        //FlatMacDarkLaf.setup();
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Main App = new Main();
                App.setVisible(true);
                App.setLocationRelativeTo(null);
            }
        });

    }

    private Border defaultBorder;
    private Border dashedBorder;

    private static String getFirstAndLastPartOfString(String text, int maxWidth) {
        int visibleChars = maxWidth - 3; // Reserve space for ellipsis
        if (text.length() <= visibleChars) {
            return text; // Return full text if it fits
        }

        // Determine how many characters to take from the start
        int charsFromStart = 5; // Change this value to get more or fewer characters from the start
        // Calculate how many characters to take from the end
        int charsFromEnd = visibleChars - charsFromStart;

        // Ensure charsFromEnd is not negative
        if (charsFromEnd < 0) {
            charsFromEnd = 0; // If too short, just take from the start
        }

        // Create the final string with ellipsis
        return text.substring(0, charsFromStart) + "..." + text.substring(text.length() - charsFromEnd);
    }

    private void afterSelectedFolder(String selectedDirectory) {
        if (selectedDirectory != null) {
            path.setPreferredSize(new Dimension(154, 16)); // Fixed siz

            String displayedText = getFirstAndLastPartOfString(selectedDirectory, 75); // Max width including ellipsis
            path.setText(displayedText);//
            //path.setText(selectedDirectory);

            File directory = new File(selectedDirectory);
            if (directory.isDirectory()) {
                File[] imageFiles = directory.listFiles((dir, name) -> {
                    String lowerCaseName = name.toLowerCase();
                    for (String extension : IMAGE_EXTENSIONS) {
                        if (lowerCaseName.endsWith(extension)) {
                            return true;
                        }
                    }
                    return false;
                });

                int imageCount = (imageFiles != null) ? imageFiles.length : 0;
                browseSelectionInfo.setText("You chose a directory that contains " + imageCount + " image(s).");
            } else {
                browseSelectionInfo.setText("You chose a file.");
            }
        }
    }

    private void eventInit() {
        openDirLbl.setEnabled(false);
        btnBrowse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileDialog fileDialog = new FileDialog(Main.this, "Select Directory", FileDialog.LOAD);
                fileDialog.setDirectory(System.getProperty("user.home"));
                fileDialog.setVisible(true);

                String selectedDirectory = fileDialog.getDirectory();
                afterSelectedFolder(selectedDirectory);
            }
        });

//        btnBrowse.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                JFileChooser fileChooser = new JFileChooser();
//                // Allow selection of files and directories
//                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//
//                // Show the file chooser dialog
//                int result = fileChooser.showOpenDialog(Main.this);
//
//                // If the user selects a file or directory
//                if (result == JFileChooser.APPROVE_OPTION) {
//                    File selectedFile = fileChooser.getSelectedFile();
//                    path.setText(selectedFile.getAbsolutePath());
//                    
//                    if (selectedFile.isDirectory()) {
//                        // Filter and count image files
//                        File[] imageFiles = selectedFile.listFiles((dir, name) -> {
//                            String lowerCaseName = name.toLowerCase();
//                            for (String extension : IMAGE_EXTENSIONS) {
//                                if (lowerCaseName.endsWith(extension)) {
//                                    return true;
//                                }
//                            }
//                            return false;
//                        });
//
//                        // Update the browseSelectionInfo label with the count
//                        int imageCount = (imageFiles != null) ? imageFiles.length : 0;
//                        browseSelectionInfo.setText("You chose a directory that contains " + imageCount + " image(s).");
//                    } else {
//                        browseSelectionInfo.setText("You chose a file.");
//                    }
//                }
//            }
//        });
        openDirLbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Define the directory you want to open, using the system's file separator
                File dir = new File(path.getText() + File.separator + outputDir.getText());

                // Check if the directory exists
                if (dir.exists() && dir.isDirectory()) {
                    try {
                        // Open the directory in the system's file explorer
                        Desktop.getDesktop().open(dir);
                    } catch (IOException ex) {
                        // Show a dialog if an error occurs
                        JOptionPane.showMessageDialog(null, "Failed to open the directory: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                } else {
                    // Show a dialog if the directory does not exist
                    JOptionPane.showMessageDialog(null, "Directory does not exist!", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        defaultBorder = BorderFactory.createDashedBorder(Color.LIGHT_GRAY, 5, 5);//browseArea.getBorder();
        browseArea.setBorder(defaultBorder);

        dashedBorder = BorderFactory.createDashedBorder(Color.decode("#56ACD6"), 5, 5);//#56ACD6

        // Set up the drop target
        //here in jpanel  "browseArea" should dashed border when before drop
        new DropTarget(TopPanel, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                browseArea.setBorder(dashedBorder);
                dtde.acceptDrag(DnDConstants.ACTION_COPY);
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {

            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                browseArea.setBorder(defaultBorder);
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        java.util.List<File> droppedFiles = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                        // Check if there are any dropped files
                        if (!droppedFiles.isEmpty()) {
                            // Get the first dropped file
                            File firstFile = droppedFiles.get(0);

                            // If it's a directory, use it directly
                            if (firstFile.isDirectory()) {
                                afterSelectedFolder(firstFile.getAbsolutePath());
                            } else {
                                // If it's a file, retrieve the parent directory
                                File parentDirectory = firstFile.getParentFile();
                                if (parentDirectory != null && parentDirectory.isDirectory()) {
                                    afterSelectedFolder(parentDirectory.getAbsolutePath());
                                }
                            }
                        }
                    }
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // Restore the default border after dropping
                    browseArea.setBorder(defaultBorder);
                }
            }

        });
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        compressOptionsGroup = new javax.swing.ButtonGroup();
        TopPanel = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        browseArea = new javax.swing.JPanel();
        btnBrowse = new javax.swing.JButton();
        browseSelectionInfo = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        path = new javax.swing.JLabel();
        MainCenterPanel = new javax.swing.JPanel();
        byOption = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        CompressBylbl = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        compressByPercentage = new javax.swing.JRadioButton();
        compressBySize = new javax.swing.JRadioButton();
        compressByDim = new javax.swing.JRadioButton();
        SizeCompression = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        sizeLbl = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        maxSize = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        DimCompression = new javax.swing.JPanel();
        jPanel18 = new javax.swing.JPanel();
        sizeLbl4 = new javax.swing.JLabel();
        jPanel19 = new javax.swing.JPanel();
        dimH = new javax.swing.JTextField();
        dimW = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        PercentageCompression = new javax.swing.JPanel();
        jPanel22 = new javax.swing.JPanel();
        CompressBylbl4 = new javax.swing.JLabel();
        jPanel23 = new javax.swing.JPanel();
        percentage = new javax.swing.JSlider();
        percentageLbl = new javax.swing.JLabel();
        seper = new javax.swing.JPanel();
        jPanel24 = new javax.swing.JPanel();
        jPanel25 = new javax.swing.JPanel();
        jSeparator3 = new javax.swing.JSeparator();
        bottomPanel = new javax.swing.JPanel();
        brightnessPanel = new javax.swing.JPanel();
        jPanel29 = new javax.swing.JPanel();
        sizeLbl3 = new javax.swing.JLabel();
        jPanel30 = new javax.swing.JPanel();
        bright = new javax.swing.JSlider();
        brightLbl = new javax.swing.JLabel();
        resetBright = new javax.swing.JLabel();
        SharpnessPanel = new javax.swing.JPanel();
        jPanel33 = new javax.swing.JPanel();
        sizeLbl6 = new javax.swing.JLabel();
        jPanel34 = new javax.swing.JPanel();
        sharp = new javax.swing.JSlider();
        sharpLbl = new javax.swing.JLabel();
        resetSharp = new javax.swing.JLabel();
        outputDirPanel = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        sizeLbl1 = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        outputDir = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        outputFormatPanel = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        sizeLbl2 = new javax.swing.JLabel();
        jPanel16 = new javax.swing.JPanel();
        outputFormat = new javax.swing.JComboBox<>();
        GenerateProgress = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jPanel26 = new javax.swing.JPanel();
        generateProgress = new javax.swing.JProgressBar();
        openDirLbl = new javax.swing.JLabel();
        outputFormatPanel1 = new javax.swing.JPanel();
        jPanel27 = new javax.swing.JPanel();
        jPanel28 = new javax.swing.JPanel();
        generateImage = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Bulk Image Modifier");
        setName("imageModifier"); // NOI18N
        setPreferredSize(new java.awt.Dimension(700, 520));
        setResizable(false);

        TopPanel.setPreferredSize(new java.awt.Dimension(550, 150));
        TopPanel.setRequestFocusEnabled(false);

        btnBrowse.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/mac-folder-20.png"))); // NOI18N
        btnBrowse.setText("Browse");

        browseSelectionInfo.setForeground(new java.awt.Color(153, 153, 153));
        browseSelectionInfo.setText("You can select a Directory that contain images");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        path.setForeground(new java.awt.Color(102, 102, 102));
        path.setText("Browse or Drag-Drop a folder");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(path, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(path, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout browseAreaLayout = new javax.swing.GroupLayout(browseArea);
        browseArea.setLayout(browseAreaLayout);
        browseAreaLayout.setHorizontalGroup(
            browseAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(browseAreaLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(btnBrowse)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(browseAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(browseSelectionInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 302, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(15, Short.MAX_VALUE))
        );
        browseAreaLayout.setVerticalGroup(
            browseAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(browseAreaLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(browseAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnBrowse, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(browseSelectionInfo)
                .addContainerGap(7, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout TopPanelLayout = new javax.swing.GroupLayout(TopPanel);
        TopPanel.setLayout(TopPanelLayout);
        TopPanelLayout.setHorizontalGroup(
            TopPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TopPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator1))
            .addGroup(TopPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(TopPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 616, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseArea, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        TopPanelLayout.setVerticalGroup(
            TopPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TopPanelLayout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addComponent(browseArea, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(33, 33, 33)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(TopPanel, java.awt.BorderLayout.PAGE_START);

        MainCenterPanel.setPreferredSize(new java.awt.Dimension(550, 80));

        byOption.setMaximumSize(new java.awt.Dimension(65534, 32767));
        byOption.setPreferredSize(new java.awt.Dimension(550, 30));
        byOption.setLayout(new javax.swing.BoxLayout(byOption, javax.swing.BoxLayout.LINE_AXIS));

        jPanel6.setPreferredSize(new java.awt.Dimension(150, 30));

        CompressBylbl.setText("Compress By");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(57, Short.MAX_VALUE)
                .addComponent(CompressBylbl)
                .addGap(25, 25, 25))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(CompressBylbl)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        byOption.add(jPanel6);

        compressByPercentage.setSelected(true);
        compressByPercentage.setText("Quality");

        compressBySize.setText("Size ");

        compressByDim.setText("Dimension");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(compressByPercentage, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(compressBySize)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(compressByDim)
                .addContainerGap(315, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(compressByPercentage)
                    .addComponent(compressBySize)
                    .addComponent(compressByDim))
                .addGap(0, 0, 0))
        );

        byOption.add(jPanel7);

        SizeCompression.setPreferredSize(new java.awt.Dimension(550, 30));
        SizeCompression.setLayout(new javax.swing.BoxLayout(SizeCompression, javax.swing.BoxLayout.LINE_AXIS));

        jPanel11.setPreferredSize(new java.awt.Dimension(150, 30));

        sizeLbl.setText("Max. Size");

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel11Layout.createSequentialGroup()
                .addContainerGap(77, Short.MAX_VALUE)
                .addComponent(sizeLbl)
                .addGap(25, 25, 25))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sizeLbl)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        SizeCompression.add(jPanel11);

        maxSize.setText("148");

        jLabel2.setForeground(new java.awt.Color(102, 102, 102));
        jLabel2.setText("KB");

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel12Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(maxSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addContainerGap(448, Short.MAX_VALUE))
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(maxSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(0, 0, 0))
        );

        SizeCompression.add(jPanel12);

        DimCompression.setPreferredSize(new java.awt.Dimension(550, 30));
        DimCompression.setLayout(new javax.swing.BoxLayout(DimCompression, javax.swing.BoxLayout.LINE_AXIS));

        jPanel18.setPreferredSize(new java.awt.Dimension(150, 30));

        sizeLbl4.setText("Width x Height");

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel18Layout.createSequentialGroup()
                .addContainerGap(46, Short.MAX_VALUE)
                .addComponent(sizeLbl4)
                .addGap(25, 25, 25))
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sizeLbl4)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        DimCompression.add(jPanel18);

        dimH.setText("1024");

        dimW.setText("1024");

        jLabel1.setForeground(new java.awt.Color(102, 102, 102));
        jLabel1.setText("x");

        jLabel3.setForeground(new java.awt.Color(102, 102, 102));
        jLabel3.setText("(px)");

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(dimW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(jLabel1)
                .addGap(5, 5, 5)
                .addComponent(dimH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addContainerGap(361, Short.MAX_VALUE))
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dimH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dimW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3))
                .addGap(0, 0, 0))
        );

        DimCompression.add(jPanel19);

        PercentageCompression.setPreferredSize(new java.awt.Dimension(550, 30));
        PercentageCompression.setLayout(new javax.swing.BoxLayout(PercentageCompression, javax.swing.BoxLayout.LINE_AXIS));

        jPanel22.setPreferredSize(new java.awt.Dimension(150, 30));

        CompressBylbl4.setText("Quality");

        javax.swing.GroupLayout jPanel22Layout = new javax.swing.GroupLayout(jPanel22);
        jPanel22.setLayout(jPanel22Layout);
        jPanel22Layout.setHorizontalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel22Layout.createSequentialGroup()
                .addContainerGap(91, Short.MAX_VALUE)
                .addComponent(CompressBylbl4)
                .addGap(25, 25, 25))
        );
        jPanel22Layout.setVerticalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel22Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(CompressBylbl4)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        PercentageCompression.add(jPanel22);

        percentage.setToolTipText("");
        percentage.setValue(80);

        percentageLbl.setForeground(new java.awt.Color(51, 51, 51));
        percentageLbl.setText("80%");
        percentageLbl.setToolTipText("");

        javax.swing.GroupLayout jPanel23Layout = new javax.swing.GroupLayout(jPanel23);
        jPanel23.setLayout(jPanel23Layout);
        jPanel23Layout.setHorizontalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel23Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(percentage, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(percentageLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(233, Short.MAX_VALUE))
        );
        jPanel23Layout.setVerticalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel23Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(percentageLbl)
                    .addComponent(percentage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );

        PercentageCompression.add(jPanel23);

        seper.setPreferredSize(new java.awt.Dimension(550, 30));
        seper.setLayout(new javax.swing.BoxLayout(seper, javax.swing.BoxLayout.LINE_AXIS));

        jPanel24.setPreferredSize(new java.awt.Dimension(150, 30));

        javax.swing.GroupLayout jPanel24Layout = new javax.swing.GroupLayout(jPanel24);
        jPanel24.setLayout(jPanel24Layout);
        jPanel24Layout.setHorizontalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 151, Short.MAX_VALUE)
        );
        jPanel24Layout.setVerticalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 17, Short.MAX_VALUE)
        );

        seper.add(jPanel24);

        javax.swing.GroupLayout jPanel25Layout = new javax.swing.GroupLayout(jPanel25);
        jPanel25.setLayout(jPanel25Layout);
        jPanel25Layout.setHorizontalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel25Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 335, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(192, Short.MAX_VALUE))
        );
        jPanel25Layout.setVerticalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel25Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        seper.add(jPanel25);

        javax.swing.GroupLayout MainCenterPanelLayout = new javax.swing.GroupLayout(MainCenterPanel);
        MainCenterPanel.setLayout(MainCenterPanelLayout);
        MainCenterPanelLayout.setHorizontalGroup(
            MainCenterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(byOption, javax.swing.GroupLayout.DEFAULT_SIZE, 694, Short.MAX_VALUE)
            .addComponent(SizeCompression, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(DimCompression, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(seper, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(PercentageCompression, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        MainCenterPanelLayout.setVerticalGroup(
            MainCenterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, MainCenterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(byOption, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(SizeCompression, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(DimCompression, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(PercentageCompression, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(seper, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        getContentPane().add(MainCenterPanel, java.awt.BorderLayout.CENTER);

        bottomPanel.setPreferredSize(new java.awt.Dimension(550, 250));

        brightnessPanel.setPreferredSize(new java.awt.Dimension(550, 30));
        brightnessPanel.setLayout(new javax.swing.BoxLayout(brightnessPanel, javax.swing.BoxLayout.LINE_AXIS));

        jPanel29.setPreferredSize(new java.awt.Dimension(150, 30));

        sizeLbl3.setText("Brightness");

        javax.swing.GroupLayout jPanel29Layout = new javax.swing.GroupLayout(jPanel29);
        jPanel29.setLayout(jPanel29Layout);
        jPanel29Layout.setHorizontalGroup(
            jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel29Layout.createSequentialGroup()
                .addContainerGap(72, Short.MAX_VALUE)
                .addComponent(sizeLbl3)
                .addGap(25, 25, 25))
        );
        jPanel29Layout.setVerticalGroup(
            jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel29Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sizeLbl3)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        brightnessPanel.add(jPanel29);

        bright.setMaximum(200);
        bright.setMinimum(1);
        bright.setToolTipText("");

        brightLbl.setText("100%");

        resetBright.setForeground(new java.awt.Color(0, 153, 153));
        resetBright.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        resetBright.setText("Reset");
        resetBright.setToolTipText("");
        resetBright.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                resetBrightMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel30Layout = new javax.swing.GroupLayout(jPanel30);
        jPanel30.setLayout(jPanel30Layout);
        jPanel30Layout.setHorizontalGroup(
            jPanel30Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel30Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(bright, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(brightLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(resetBright, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(234, Short.MAX_VALUE))
        );
        jPanel30Layout.setVerticalGroup(
            jPanel30Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel30Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel30Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bright, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(brightLbl)
                    .addComponent(resetBright, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        brightnessPanel.add(jPanel30);

        SharpnessPanel.setPreferredSize(new java.awt.Dimension(550, 30));
        SharpnessPanel.setLayout(new javax.swing.BoxLayout(SharpnessPanel, javax.swing.BoxLayout.LINE_AXIS));

        jPanel33.setPreferredSize(new java.awt.Dimension(150, 30));

        sizeLbl6.setText("Sharpness");

        javax.swing.GroupLayout jPanel33Layout = new javax.swing.GroupLayout(jPanel33);
        jPanel33.setLayout(jPanel33Layout);
        jPanel33Layout.setHorizontalGroup(
            jPanel33Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel33Layout.createSequentialGroup()
                .addContainerGap(73, Short.MAX_VALUE)
                .addComponent(sizeLbl6)
                .addGap(25, 25, 25))
        );
        jPanel33Layout.setVerticalGroup(
            jPanel33Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel33Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sizeLbl6)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        SharpnessPanel.add(jPanel33);

        sharp.setMaximum(200);
        sharp.setMinimum(1);
        sharp.setToolTipText("");

        sharpLbl.setText("100%");

        resetSharp.setForeground(new java.awt.Color(0, 153, 153));
        resetSharp.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        resetSharp.setText("Reset");
        resetSharp.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                resetSharpMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel34Layout = new javax.swing.GroupLayout(jPanel34);
        jPanel34.setLayout(jPanel34Layout);
        jPanel34Layout.setHorizontalGroup(
            jPanel34Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel34Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(sharp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(sharpLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(resetSharp, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(235, Short.MAX_VALUE))
        );
        jPanel34Layout.setVerticalGroup(
            jPanel34Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel34Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel34Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sharp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sharpLbl))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel34Layout.createSequentialGroup()
                .addComponent(resetSharp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        SharpnessPanel.add(jPanel34);

        outputDirPanel.setPreferredSize(new java.awt.Dimension(550, 30));
        outputDirPanel.setLayout(new javax.swing.BoxLayout(outputDirPanel, javax.swing.BoxLayout.LINE_AXIS));

        jPanel13.setPreferredSize(new java.awt.Dimension(150, 30));

        sizeLbl1.setText("Output Folder");

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel13Layout.createSequentialGroup()
                .addContainerGap(52, Short.MAX_VALUE)
                .addComponent(sizeLbl1)
                .addGap(25, 25, 25))
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sizeLbl1)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        outputDirPanel.add(jPanel13);

        outputDir.setText("Reduced");

        jLabel4.setForeground(new java.awt.Color(153, 153, 153));
        jLabel4.setText("inside the selected directory");

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(outputDir, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(jLabel4)
                .addContainerGap(265, Short.MAX_VALUE))
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel14Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(outputDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)))
        );

        outputDirPanel.add(jPanel14);

        outputFormatPanel.setPreferredSize(new java.awt.Dimension(550, 30));
        outputFormatPanel.setLayout(new javax.swing.BoxLayout(outputFormatPanel, javax.swing.BoxLayout.LINE_AXIS));

        jPanel15.setPreferredSize(new java.awt.Dimension(150, 30));

        sizeLbl2.setText("Output Format");

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel15Layout.createSequentialGroup()
                .addContainerGap(47, Short.MAX_VALUE)
                .addComponent(sizeLbl2)
                .addGap(25, 25, 25))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sizeLbl2)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        outputFormatPanel.add(jPanel15);

        outputFormat.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "JPG", "WEBP" }));
        outputFormat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputFormatActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(outputFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(453, Short.MAX_VALUE))
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel16Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(outputFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        outputFormatPanel.add(jPanel16);

        GenerateProgress.setPreferredSize(new java.awt.Dimension(550, 30));
        GenerateProgress.setLayout(new javax.swing.BoxLayout(GenerateProgress, javax.swing.BoxLayout.LINE_AXIS));

        jPanel17.setPreferredSize(new java.awt.Dimension(150, 30));

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 150, Short.MAX_VALUE)
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 21, Short.MAX_VALUE)
        );

        GenerateProgress.add(jPanel17);

        openDirLbl.setForeground(new java.awt.Color(0, 153, 153));
        openDirLbl.setText("Quick Open ");
        openDirLbl.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel26Layout = new javax.swing.GroupLayout(jPanel26);
        jPanel26.setLayout(jPanel26Layout);
        jPanel26Layout.setHorizontalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel26Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(generateProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(openDirLbl)
                .addContainerGap(124, Short.MAX_VALUE))
        );
        jPanel26Layout.setVerticalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(openDirLbl, javax.swing.GroupLayout.DEFAULT_SIZE, 21, Short.MAX_VALUE)
            .addGroup(jPanel26Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(generateProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        GenerateProgress.add(jPanel26);

        outputFormatPanel1.setPreferredSize(new java.awt.Dimension(550, 30));
        outputFormatPanel1.setLayout(new javax.swing.BoxLayout(outputFormatPanel1, javax.swing.BoxLayout.LINE_AXIS));

        jPanel27.setPreferredSize(new java.awt.Dimension(150, 30));

        javax.swing.GroupLayout jPanel27Layout = new javax.swing.GroupLayout(jPanel27);
        jPanel27.setLayout(jPanel27Layout);
        jPanel27Layout.setHorizontalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 151, Short.MAX_VALUE)
        );
        jPanel27Layout.setVerticalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );

        outputFormatPanel1.add(jPanel27);

        generateImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/genereate0.png"))); // NOI18N
        generateImage.setText("Generate");
        generateImage.setAlignmentX(0.2F);
        generateImage.setIconTextGap(10);

        javax.swing.GroupLayout jPanel28Layout = new javax.swing.GroupLayout(jPanel28);
        jPanel28.setLayout(jPanel28Layout);
        jPanel28Layout.setHorizontalGroup(
            jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel28Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(generateImage)
                .addContainerGap(423, Short.MAX_VALUE))
        );
        jPanel28Layout.setVerticalGroup(
            jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel28Layout.createSequentialGroup()
                .addComponent(generateImage, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 8, Short.MAX_VALUE))
        );

        outputFormatPanel1.add(jPanel28);

        javax.swing.GroupLayout bottomPanelLayout = new javax.swing.GroupLayout(bottomPanel);
        bottomPanel.setLayout(bottomPanelLayout);
        bottomPanelLayout.setHorizontalGroup(
            bottomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bottomPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(bottomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(brightnessPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(outputDirPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 682, Short.MAX_VALUE)
                    .addComponent(outputFormatPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(GenerateProgress, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(outputFormatPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(bottomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bottomPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(SharpnessPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 682, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        bottomPanelLayout.setVerticalGroup(
            bottomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bottomPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(brightnessPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(43, 43, 43)
                .addComponent(outputDirPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outputFormatPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(GenerateProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outputFormatPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(32, Short.MAX_VALUE))
            .addGroup(bottomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(bottomPanelLayout.createSequentialGroup()
                    .addGap(37, 37, 37)
                    .addComponent(SharpnessPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(183, Short.MAX_VALUE)))
        );

        getContentPane().add(bottomPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void outputFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outputFormatActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_outputFormatActionPerformed

    private void resetBrightMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resetBrightMouseClicked
        resetBright.setEnabled(false);
        bright.setValue(100);
        brightLbl.setText("100%");        // TODO add your handling code here:
    }//GEN-LAST:event_resetBrightMouseClicked

    private void resetSharpMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resetSharpMouseClicked
        resetSharp.setEnabled(false);
        sharp.setValue(100);
        sharpLbl.setText("100%");
    }//GEN-LAST:event_resetSharpMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel CompressBylbl;
    private javax.swing.JLabel CompressBylbl4;
    private javax.swing.JPanel DimCompression;
    private javax.swing.JPanel GenerateProgress;
    private javax.swing.JPanel MainCenterPanel;
    private javax.swing.JPanel PercentageCompression;
    private javax.swing.JPanel SharpnessPanel;
    private javax.swing.JPanel SizeCompression;
    private javax.swing.JPanel TopPanel;
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JSlider bright;
    private javax.swing.JLabel brightLbl;
    private javax.swing.JPanel brightnessPanel;
    private javax.swing.JPanel browseArea;
    private javax.swing.JLabel browseSelectionInfo;
    private javax.swing.JButton btnBrowse;
    private javax.swing.JPanel byOption;
    private javax.swing.JRadioButton compressByDim;
    private javax.swing.JRadioButton compressByPercentage;
    private javax.swing.JRadioButton compressBySize;
    private javax.swing.ButtonGroup compressOptionsGroup;
    private javax.swing.JTextField dimH;
    private javax.swing.JTextField dimW;
    private javax.swing.JButton generateImage;
    private javax.swing.JProgressBar generateProgress;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel33;
    private javax.swing.JPanel jPanel34;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTextField maxSize;
    private javax.swing.JLabel openDirLbl;
    private javax.swing.JTextField outputDir;
    private javax.swing.JPanel outputDirPanel;
    private javax.swing.JComboBox<String> outputFormat;
    private javax.swing.JPanel outputFormatPanel;
    private javax.swing.JPanel outputFormatPanel1;
    private javax.swing.JLabel path;
    private javax.swing.JSlider percentage;
    private javax.swing.JLabel percentageLbl;
    private javax.swing.JLabel resetBright;
    private javax.swing.JLabel resetSharp;
    private javax.swing.JPanel seper;
    private javax.swing.JSlider sharp;
    private javax.swing.JLabel sharpLbl;
    private javax.swing.JLabel sizeLbl;
    private javax.swing.JLabel sizeLbl1;
    private javax.swing.JLabel sizeLbl2;
    private javax.swing.JLabel sizeLbl3;
    private javax.swing.JLabel sizeLbl4;
    private javax.swing.JLabel sizeLbl6;
    // End of variables declaration//GEN-END:variables
}
