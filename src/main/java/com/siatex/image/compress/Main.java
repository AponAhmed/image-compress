package com.siatex.image.compress;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;

import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.luciad.imageio.webp.WebPWriteParam;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
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
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Apon
 */
public class Main extends javax.swing.JFrame {

    // Define supported image extensions as a static final array
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
    private List<ImageIcon> generateIcons;
    private Timer animationTimer;

    public Main() {
        WindowIconSetter.setIcon(this);
        initComponents();
        generateProgress.setVisible(false);
        generateIcons = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            //new javax.swing.ImageIcon(getClass().getResource("/icons/mac-folder-20.png"))
            generateIcons.add(new ImageIcon(getClass().getResource("/icons/genereate" + i + ".png")));
        }
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
                stopAnimation(); // Stop the icon animation when done
            }
        };

        worker.execute();
    }

    public void resizeByDimension(BufferedImage originalImage, File outputFile, int width, int height, String selectedFormat) throws IOException {
        // Create a resized version of the original image
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

    public void compressByQuality(BufferedImage originalImage, File outputFile, float quality, String selectedFormat) throws IOException {
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

                // PNG uses lossless compression, control over compression levels is limited
                if (imageWriteParam.canWriteCompressed()) {
                    imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

                    // PNG compression is lossless, and Java does not expose detailed compression level controls
                    // This line can be omitted as it doesn't impact PNG compression in practice:
                    // imageWriteParam.setCompressionQuality(quality);
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
            imageWriter.write(null, new IIOImage(originalImage, null, null), imageWriteParam);
        } finally {
            if (imageWriter != null) {
                imageWriter.dispose();
            }
        }
    }

    public void compressToMaxSize(BufferedImage originalImage, File outputFile, float maxSizeKB, String formatName) throws IOException {
        float quality = 1.0f; // Start with the highest quality
        File tempFile = null;

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
                imageWriter.write(null, new IIOImage(originalImage, null, null), imageWriteParam);
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

    private void eventInit() {
        btnBrowse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                // Allow selection of files and directories
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                // Show the file chooser dialog
                int result = fileChooser.showOpenDialog(Main.this);

                // If the user selects a file or directory
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    path.setText(selectedFile.getAbsolutePath());

                    if (selectedFile.isDirectory()) {
                        // Filter and count image files
                        File[] imageFiles = selectedFile.listFiles((dir, name) -> {
                            String lowerCaseName = name.toLowerCase();
                            for (String extension : IMAGE_EXTENSIONS) {
                                if (lowerCaseName.endsWith(extension)) {
                                    return true;
                                }
                            }
                            return false;
                        });

                        // Update the browseSelectionInfo label with the count
                        int imageCount = (imageFiles != null) ? imageFiles.length : 0;
                        browseSelectionInfo.setText("You chose a directory that contains " + imageCount + " image(s).");
                    } else {
                        browseSelectionInfo.setText("You chose a file.");
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        compressOptionsGroup = new javax.swing.ButtonGroup();
        jPanel2 = new javax.swing.JPanel();
        btnBrowse = new javax.swing.JButton();
        path = new javax.swing.JTextField();
        browseSelectionInfo = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jPanel5 = new javax.swing.JPanel();
        byOption = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        CompressBylbl = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        compressByPercentage = new javax.swing.JRadioButton();
        compressBySize = new javax.swing.JRadioButton();
        compressByDim = new javax.swing.JRadioButton();
        PercentageCompression = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        CompressBylbl2 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        percentage = new javax.swing.JSlider();
        percentageLbl = new javax.swing.JLabel();
        SizeCompression = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        sizeLbl = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        maxSize = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        DimCompression = new javax.swing.JPanel();
        jPanel18 = new javax.swing.JPanel();
        sizeLbl4 = new javax.swing.JLabel();
        jPanel19 = new javax.swing.JPanel();
        dimH = new javax.swing.JTextField();
        dimW = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        generateImage = new javax.swing.JButton();
        generateProgress = new javax.swing.JProgressBar();
        SizeCompression1 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        sizeLbl1 = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        outputDir = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        SizeCompression2 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        sizeLbl2 = new javax.swing.JLabel();
        jPanel16 = new javax.swing.JPanel();
        outputFormat = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Bulk Image Modifier");
        setName("imageModifier"); // NOI18N
        setResizable(false);

        jPanel2.setPreferredSize(new java.awt.Dimension(550, 120));
        jPanel2.setRequestFocusEnabled(false);

        btnBrowse.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/mac-folder-20.png"))); // NOI18N
        btnBrowse.setText("Browse");

        path.setEditable(false);

        browseSelectionInfo.setForeground(new java.awt.Color(153, 153, 153));
        browseSelectionInfo.setText("You can select a Directory that contain images");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addComponent(jSeparator1))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(44, 44, 44)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jSeparator2)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(btnBrowse)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(browseSelectionInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 302, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(path, javax.swing.GroupLayout.PREFERRED_SIZE, 348, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(0, 0, 0))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnBrowse)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(path)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(browseSelectionInfo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 4, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel2, java.awt.BorderLayout.PAGE_START);

        jPanel5.setPreferredSize(new java.awt.Dimension(550, 80));

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
                .addContainerGap(171, Short.MAX_VALUE))
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

        PercentageCompression.setPreferredSize(new java.awt.Dimension(550, 30));
        PercentageCompression.setLayout(new javax.swing.BoxLayout(PercentageCompression, javax.swing.BoxLayout.LINE_AXIS));

        jPanel9.setPreferredSize(new java.awt.Dimension(150, 30));

        CompressBylbl2.setText("Percentage");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap(67, Short.MAX_VALUE)
                .addComponent(CompressBylbl2)
                .addGap(25, 25, 25))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(CompressBylbl2)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        PercentageCompression.add(jPanel9);

        jPanel10.setPreferredSize(new java.awt.Dimension(398, 50));

        percentage.setToolTipText("");
        percentage.setValue(80);

        percentageLbl.setForeground(new java.awt.Color(51, 51, 51));
        percentageLbl.setText("80%");
        percentageLbl.setToolTipText("");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addComponent(percentage, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(percentageLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(133, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addGap(4, 6, Short.MAX_VALUE)
                .addComponent(percentageLbl)
                .addGap(8, 8, 8))
            .addComponent(percentage, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        PercentageCompression.add(jPanel10);

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
                .addContainerGap(304, Short.MAX_VALUE))
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
                .addContainerGap(217, Short.MAX_VALUE))
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

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(byOption, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addComponent(SizeCompression, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addComponent(PercentageCompression, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addComponent(DimCompression, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(75, 75, 75))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(byOption, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(PercentageCompression, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(SizeCompression, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(DimCompression, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(112, 112, 112))
        );

        getContentPane().add(jPanel5, java.awt.BorderLayout.CENTER);

        jPanel1.setPreferredSize(new java.awt.Dimension(550, 180));

        generateImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/genereate0.png"))); // NOI18N
        generateImage.setText("Generate");
        generateImage.setAlignmentX(0.2F);
        generateImage.setIconTextGap(10);

        SizeCompression1.setPreferredSize(new java.awt.Dimension(550, 30));
        SizeCompression1.setLayout(new javax.swing.BoxLayout(SizeCompression1, javax.swing.BoxLayout.LINE_AXIS));

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

        SizeCompression1.add(jPanel13);

        outputDir.setText("Reduced");

        jLabel4.setForeground(new java.awt.Color(153, 153, 153));
        jLabel4.setText("inside the selected directory");

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(outputDir, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(jLabel4)
                .addContainerGap(122, Short.MAX_VALUE))
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel14Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(outputDir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)))
        );

        SizeCompression1.add(jPanel14);

        SizeCompression2.setPreferredSize(new java.awt.Dimension(550, 30));
        SizeCompression2.setLayout(new javax.swing.BoxLayout(SizeCompression2, javax.swing.BoxLayout.LINE_AXIS));

        jPanel15.setPreferredSize(new java.awt.Dimension(150, 30));

        sizeLbl2.setText("Output Format");

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel15Layout.createSequentialGroup()
                .addContainerGap(46, Short.MAX_VALUE)
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

        SizeCompression2.add(jPanel15);

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
                .addGap(10, 10, 10)
                .addComponent(outputFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(312, Short.MAX_VALUE))
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel16Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(outputFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        SizeCompression2.add(jPanel16);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(161, 161, 161)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(generateProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(generateImage))
                        .addGap(0, 67, Short.MAX_VALUE))
                    .addComponent(SizeCompression1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(SizeCompression2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(SizeCompression1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SizeCompression2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addComponent(generateProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(generateImage, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40))
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void outputFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outputFormatActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_outputFormatActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel CompressBylbl;
    private javax.swing.JLabel CompressBylbl2;
    private javax.swing.JPanel DimCompression;
    private javax.swing.JPanel PercentageCompression;
    private javax.swing.JPanel SizeCompression;
    private javax.swing.JPanel SizeCompression1;
    private javax.swing.JPanel SizeCompression2;
    private javax.swing.JPanel SizeCompression3;
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
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTextField maxSize;
    private javax.swing.JTextField outputDir;
    private javax.swing.JComboBox<String> outputFormat;
    private javax.swing.JTextField path;
    private javax.swing.JSlider percentage;
    private javax.swing.JLabel percentageLbl;
    private javax.swing.JLabel sizeLbl;
    private javax.swing.JLabel sizeLbl1;
    private javax.swing.JLabel sizeLbl2;
    private javax.swing.JLabel sizeLbl3;
    private javax.swing.JLabel sizeLbl4;
    // End of variables declaration//GEN-END:variables
}
