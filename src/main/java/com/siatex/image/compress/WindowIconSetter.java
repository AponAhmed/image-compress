package com.siatex.image.compress;

import javax.swing.*;
import java.util.List;

import javax.swing.JFrame;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.net.URL;

public class WindowIconSetter {

    public static void setIcon(JFrame frame) {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            // loading an image from a file
            final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
            final URL imageResource = WindowIconSetter.class.getResource(IconConfig.MAC_ICON_PATH);
            final Image image = defaultToolkit.getImage(imageResource);
            // this is new since JDK 9
            final Taskbar taskbar = Taskbar.getTaskbar();
            try {
                // set icon for mac os (and other systems which do support this method)
                taskbar.setIconImage(image);
            } catch (final UnsupportedOperationException e) {
                System.out.println("The os does not support: 'taskbar.setIconImage'");
            } catch (final SecurityException e) {
                System.out.println("There was a security exception for: 'taskbar.setIconImage'");
            }
        } else {
            ImageIcon icon16x16 = new ImageIcon(WindowIconSetter.class.getResource(IconConfig.ICON_16_PATH));
            ImageIcon icon32x32 = new ImageIcon(WindowIconSetter.class.getResource(IconConfig.ICON_32_PATH));
            ImageIcon icon48x48 = new ImageIcon(WindowIconSetter.class.getResource(IconConfig.ICON_48_PATH));
            frame.setIconImage(icon16x16.getImage()); // Set the default (16x16) icon
            frame.setIconImages(List.of(icon16x16.getImage(), icon32x32.getImage(), icon48x48.getImage())); // Set multiple icons for different sizes
        }
    }

    public static void setIcon(JFrame frame, String iconPath16x16, String iconPath32x32, String iconPath48x48) {
        // Check if icon paths are provided, otherwise use paths from IconConfig
        if (iconPath16x16 == null || iconPath16x16.isEmpty()) {
            iconPath16x16 = IconConfig.ICON_16_PATH;
        }
        if (iconPath32x32 == null || iconPath32x32.isEmpty()) {
            iconPath32x32 = IconConfig.ICON_32_PATH;
        }
        if (iconPath48x48 == null || iconPath48x48.isEmpty()) {
            iconPath48x48 = IconConfig.ICON_48_PATH;
        }

        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            // loading an image from a file
            final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
            final URL imageResource = WindowIconSetter.class.getResource(iconPath48x48);
            final Image image = defaultToolkit.getImage(imageResource);
            // this is new since JDK 9
            final Taskbar taskbar = Taskbar.getTaskbar();
            try {
                // set icon for mac os (and other systems which do support this method)
                taskbar.setIconImage(image);
            } catch (final UnsupportedOperationException e) {
                System.out.println("The os does not support: 'taskbar.setIconImage'");
            } catch (final SecurityException e) {
                System.out.println("There was a security exception for: 'taskbar.setIconImage'");
            }
        } else {
            ImageIcon icon16x16 = new ImageIcon(WindowIconSetter.class.getResource(iconPath16x16));
            ImageIcon icon32x32 = new ImageIcon(WindowIconSetter.class.getResource(iconPath32x32));
            ImageIcon icon48x48 = new ImageIcon(WindowIconSetter.class.getResource(iconPath48x48));
            frame.setIconImage(icon16x16.getImage()); // Set the default (16x16) icon
            frame.setIconImages(List.of(icon16x16.getImage(), icon32x32.getImage(), icon48x48.getImage())); // Set multiple icons for different sizes
        }
    }

}

class IconConfig {

    public static final String MAC_ICON_PATH = "/icons/app-48.png";
    public static final String ICON_16_PATH = "/icons/app-16.png";
    public static final String ICON_32_PATH = "/icons/app-32.png";
    public static final String ICON_48_PATH = "/icons/app-48.png";
}
