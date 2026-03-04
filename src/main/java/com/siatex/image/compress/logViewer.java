package com.siatex.image.compress;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import javax.swing.Timer;

public class logViewer extends javax.swing.JFrame {

    private javax.swing.JTextArea logArea;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JButton clearBtn;
    private javax.swing.JButton copyBtn;
    private javax.swing.JPanel topBar;
    private Timer liveTailTimer;
    private long lastFilePosition = 0;
    private final String logFileName = "compress_log.txt";

    public logViewer() {
        initComponents();
        startLiveTail();
    }

    private void initComponents() {
        setTitle("Compression Log Viewer");
        setSize(700, 500);
        setDefaultCloseOperation(javax.swing.JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());

        // ── Top bar ───────────────────────────────────────────────
        topBar = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));

        clearBtn = new javax.swing.JButton("Clear");
        clearBtn.setForeground(new Color(180, 50, 50));
        clearBtn.addActionListener(e -> clearLog());

        copyBtn = new javax.swing.JButton("Copy All");
        copyBtn.addActionListener(e -> copyAll());

        topBar.add(copyBtn);
        topBar.add(clearBtn);
        add(topBar, BorderLayout.NORTH);

        // ── Log text area ─────────────────────────────────────────
        logArea = new javax.swing.JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 200, 200));
        logArea.setCaretColor(Color.WHITE);
        logArea.setMargin(new Insets(8, 10, 8, 10));
        logArea.setLineWrap(false);

        scrollPane = new javax.swing.JScrollPane(logArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // ── Status bar ────────────────────────────────────────────
        javax.swing.JLabel statusLbl = new javax.swing.JLabel(" Live log: " + logFileName);
        statusLbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        statusLbl.setForeground(Color.GRAY);
        statusLbl.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        add(statusLbl, BorderLayout.SOUTH);
    }

    // ── Live tail: polls log file every 300ms for new lines ───────
    private void startLiveTail() {
        liveTailTimer = new Timer(300, e -> tailLogFile());
        liveTailTimer.start();
    }

    private void tailLogFile() {
        File logFile = new File(logFileName);
        if (!logFile.exists()) return;

        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            long fileLength = raf.length();
            if (fileLength <= lastFilePosition) return; // No new content

            raf.seek(lastFilePosition);
            StringBuilder newContent = new StringBuilder();
            String line;
            while ((line = raf.readLine()) != null) {
                newContent.append(colorLine(line)).append("\n");
            }
            lastFilePosition = raf.getFilePointer();

            if (newContent.length() > 0) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(newContent.toString());
                    // Auto-scroll to bottom
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // ── Simple text prefix coloring (readable in dark background) ─
    private String colorLine(String line) {
        // Since JTextArea doesn't support HTML colors,
        // we keep plain text but add visual markers
        if (line.startsWith("[ERROR]"))   return "✖ " + line;
        if (line.startsWith("[WARN]"))    return "⚠ " + line;
        if (line.startsWith("[OK]"))      return "✔ " + line;
        if (line.startsWith("[DONE]"))    return "● " + line;
        if (line.startsWith("[START]"))   return "\n▶ " + line;
        if (line.startsWith("[Phase 1]")) return "  ⟳ " + line;
        if (line.startsWith("[Phase 2]")) return "  ⟳ " + line;
        if (line.startsWith("[Phase 3]")) return "  ⟳ " + line;
        return line;
    }

    private void clearLog() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Clear the log file and viewer?", "Confirm Clear",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            // Clear the file
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFileName, false))) {
                pw.print("");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // Clear the display and reset position
            logArea.setText("");
            lastFilePosition = 0;
        }
    }

    private void copyAll() {
        String text = logArea.getText();
        if (!text.isEmpty()) {
            java.awt.datatransfer.StringSelection sel =
                    new java.awt.datatransfer.StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            JOptionPane.showMessageDialog(this, "Log copied to clipboard.", "Copied",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Called when window is closed — stop polling
    @Override
    public void dispose() {
        if (liveTailTimer != null) liveTailTimer.stop();
        super.dispose();
    }
}