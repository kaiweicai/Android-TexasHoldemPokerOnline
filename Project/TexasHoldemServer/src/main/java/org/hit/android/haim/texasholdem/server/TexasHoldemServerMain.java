package org.hit.android.haim.texasholdem.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hit.android.haim.texasholdem.server.model.service.GameService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author Haim Adrian
 * @since 14-Apr-21
 * @see org.hit.android.haim.texasholdem.server.controller.UserController
 * @see org.hit.android.haim.texasholdem.server.controller.MessageController
 */
@SpringBootApplication
public class TexasHoldemServerMain {
    private static final String STDOUT_LOGGER_NAME = "stdout";
    private static final String STDERR_LOGGER_NAME = "stderr";

    private static final Logger log;
    private static TrayIcon trayIcon;
    private static boolean wasShutDown = false;
    private static ConfigurableApplicationContext applicationContext;

    static {
        configureLog4j2();
        redirectStreamsToLog4j();
        log = LogManager.getLogger(TexasHoldemServerMain.class);
        log.info("Log was initialized");
    }

    public static void main(String[] args) {
        log.info("Enter Main");
        SpringApplicationBuilder builder = new SpringApplicationBuilder(TexasHoldemServerMain.class);
        // builder.headless(false); // So we will be able to use AWT (TrayIcon)
        applicationContext = builder.run(args);

        // showTrayIcon();
        Runtime.getRuntime().addShutdownHook(new Thread(TexasHoldemServerMain::shutdown, "ServerShutdownThread"));
        // log.info("Exit Main");
    }

    private static void configureLog4j2() {
        // Use asynchronous loggers by default for better performance
        System.setProperty("log4j2.contextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

        // When the async queue is full, discard all DEBUG and TRACE messages that can not be ingested to the queue. INFO and more descriptive will block the caller
        // thread until there is a space for the log event to be kept.
        System.setProperty("log4j2.AsyncQueueFullPolicy", "Discard");
        System.setProperty("log4j2.DiscardThreshold", "DEBUG");

        // Redirect logs from Java Util Logging to our log4j2, cause there are third parties using JUL.
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        // Set default log directory in case it was not specified outside the application
        if (System.getProperty("org.hit.android.haim.texasholdem.logdir") == null) {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                System.setProperty("org.hit.android.haim.texasholdem.logdir", "C:/temp/AndroidProjects/TexasHoldem/log");
            } else {
                System.setProperty("org.hit.android.haim.texasholdem.logdir", "/home/cloudwei/TexasHoldem/log");
            }
        }
    }

    private static void redirectStreamsToLog4j() {
        System.setOut(new PrintStream(new LoggingStream(STDOUT_LOGGER_NAME), true));
        System.setErr(new PrintStream(new LoggingStream(STDERR_LOGGER_NAME), true));

        System.out.println(getJavaVersionString());
    }

    private static String getJavaVersionString() {
        return "java version \"" + System.getProperty("java.version") + "\"" + System.lineSeparator() + System.getProperty("java.runtime.name") +
            " (build " + System.getProperty("java.runtime.version") + ")" + System.lineSeparator() + System.getProperty("java.vm.name") +
            " (build " + System.getProperty("java.vm.version") + ", " + System.getProperty("java.vm.info") + ")";
    }

    /**
     * Display tray icon.
     */
    private static void showTrayIcon() {
        try {
            String className = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
            try {
                UIManager.setLookAndFeel(className);
            } catch (Exception e) {
                log.error("Failed setting NimbusLookAndFeel. Defaulting to system L&F", e);

                className = UIManager.getSystemLookAndFeelClassName();
                try {
                    UIManager.setLookAndFeel(className);
                } catch (Exception classNotFoundException) {
                    log.error("Failed setting SystemLookAndFeel.. FML", classNotFoundException);
                }
            }

            PopupMenu popup = new PopupMenu();
            MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(e -> shutdown());
            popup.add(exit);

            InputStream icon = TexasHoldemServerMain.class.getClassLoader().getResourceAsStream("icon.png");
            if (icon != null) {
                trayIcon = new TrayIcon(new ImageIcon(ImageIO.read(icon)).getImage());
                trayIcon.setImageAutoSize(true);
                trayIcon.setPopupMenu(popup);
                SystemTray.getSystemTray().add(trayIcon);

                trayIcon.displayMessage("Texas Holdem Server", "Game server is running", TrayIcon.MessageType.INFO);
                trayIcon.setToolTip("Texas Holdem Server");
            } else {
                log.warn("Icon could not be found. As a result, there will be no tray icon");
            }
        } catch (Exception e) {
            log.error("Failed to add system tray icon: " + e.getMessage(), e);
        }
    }

    private static void shutdown() {
        if (!wasShutDown) {
            wasShutDown = true;
            log.info("Shutting down Texas Holdem Server");
            try { applicationContext.getBean(GameService.class).shutdown(); } catch (Exception ignore) {}
            SpringApplication.exit(applicationContext, () -> 0);
            // SystemTray.getSystemTray().remove(trayIcon);
            LogManager.shutdown();
        }
    }

    /**
     * This output stream holds all streamed data in an internal buffer. On flush() it will send buffer data to the relevant logger.
     */
    private static class LoggingStream extends OutputStream {
        /**
         * Internal stream buffer
         */
        private final StringBuilder sb;

        /**
         * The logger where we flush internal buffer to
         */
        private final Logger logger;

        /**
         * Constructs a new {@link LoggingStream}
         *
         * @param loggerName Name of the logger to log messages to
         */
        public LoggingStream(String loggerName) {
            sb = new StringBuilder(128);
            logger = LogManager.getLogger(loggerName);
        }

        /**
         * Writes the specified byte to this output stream. The general contract for <code>write</code> is
         * that one byte is written to the output stream. The byte to be written is the eight low-order bits of
         * the argument <code>b</code>. The 24 high-order bits of <code>b</code> are ignored.
         * <p>
         * Subclasses of <code>OutputStream</code> must provide an implementation for this method.
         *
         * @param b the <code>byte</code>.
         */
        @Override
        public void write(int b) {
            sb.append((char) b);
        }

        /**
         * Flushes this output stream and forces any buffered output bytes to be written out. The general
         * contract of <code>flush</code> is that calling it is an indication that, if any bytes previously
         * written have been buffered by the implementation of the output stream, such bytes should immediately
         * be written to their intended destination.
         * <p>
         * The <code>flush</code> method of <code>OutputStream</code> does nothing.
         */
        @Override
        public void flush() {
            if (sb.length() > 0) {
                String message = sb.toString();

                // When calling System.out.println, there is the print of the message which is flushed
                // and we print it with a new line as part of the logger implementation, and then there is additional
                // newLine() call of the println, which we would like to ignore cause the logger already prints a new line.
                if (!System.lineSeparator().equals(message)) {
                    logger.info(message);
                }

                sb.setLength(0);
            }
        }

        /**
         * Closes this output stream and releases any system resources associated with this stream. The general
         * contract of <code>close</code> is that it closes the output stream. A closed stream cannot perform
         * output operations and cannot be reopened.
         * <p>
         * The <code>close</code> method of <code>OutputStream</code> does nothing.
         */
        @Override
        public void close() {
            flush();
        }
    }
}

