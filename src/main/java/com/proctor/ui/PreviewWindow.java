package com.proctor.ui;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/** Simple Swing window that shows the live camera feed and a status line. */
public class PreviewWindow extends JFrame {

    private final JLabel imageLabel = new JLabel();
    private final JLabel statusLabel = new JLabel(" Waiting for camera...");

    public PreviewWindow(String title, Runnable onClose) {
        super(title);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose.run();
                dispose();
            }
        });
        setLayout(new BorderLayout());
        add(imageLabel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        imageLabel.setPreferredSize(new Dimension(640, 480));
        pack();
        setLocationRelativeTo(null);
    }

    /** Safe to call from any thread. */
    public void showFrame(BufferedImage image, String status) {
        SwingUtilities.invokeLater(() -> {
            imageLabel.setIcon(new ImageIcon(image));
            statusLabel.setText(" " + status);
        });
    }

    /** Safe to call from any thread. */
    public void showStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(" " + status));
    }
}
