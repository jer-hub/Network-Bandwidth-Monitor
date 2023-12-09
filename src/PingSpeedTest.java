import java.awt.*;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PingSpeedTest extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTextField txtHostname;
    private JButton btnPing;
    private JLabel lblResult;
    private JButton btnSpeedTest;
    private JLabel lblStatus;

    private Timer pingTimer;
    private Timer speedTestTimer;
    private final ExecutorService speedTestExecutor = Executors.newSingleThreadExecutor();

    private static final int PING_INTERVAL = 3000;
    private static final int SPEED_TEST_INTERVAL = 1000;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PingSpeedTest ex = new PingSpeedTest();
            ex.setVisible(true);
        });
    }

    public PingSpeedTest() {
        setTitle("Ping Speed Test");
        setSize(400, 210);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2));

        txtHostname = new JTextField("8.8.8.8");
        btnPing = new JButton("Ping");
        lblResult = new JLabel("Ping: ");
        btnSpeedTest = new JButton("Network Speed Test");
        lblStatus = new JLabel("Status: ");

        btnPing.addActionListener(e -> pingHost());
        btnSpeedTest.addActionListener(e -> {
            lblStatus.setText("Status: Running Speed Test");
            startSpeedTestTimer();
        });

        initializeTimers();

        panel.add(txtHostname);
        panel.add(btnPing);
        panel.add(lblResult);
        panel.add(btnSpeedTest);
        panel.add(lblStatus);

        add(panel, BorderLayout.CENTER);
    }

    private void initializeTimers() {
        pingTimer = new Timer(PING_INTERVAL, e -> pingHost());
        speedTestTimer = new Timer(SPEED_TEST_INTERVAL, e -> networkSpeedTest());
        speedTestTimer.setInitialDelay(0);
        pingTimer.start();
    }

    private void startSpeedTestTimer() {
        speedTestTimer.start();
    }

    private void pingHost() {
        new Thread(() -> {
            lblResult.setText("Ping: ");
            String hostname = txtHostname.getText();
            try {
                InetAddress address = InetAddress.getByName(hostname);
                long startTime = System.currentTimeMillis();
                long responseTime = address.isReachable(5000) ? System.currentTimeMillis() - startTime : -1;
                SwingUtilities.invokeLater(() -> lblResult.setText("Ping: " + (responseTime >= 0 ? responseTime + " ms" : "Unreachable")));
            } catch (UnknownHostException ex) {
                SwingUtilities.invokeLater(() -> lblResult.setText("Ping: Unknown Host"));
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> lblResult.setText("Ping: Exception Occurred"));
            }
        }).start();
    }

    private void networkSpeedTest() {
        speedTestExecutor.submit(() -> {
            try {
                Process process = Runtime.getRuntime().exec("speedtest-cli --simple");
                InputStream inputStream = process.getInputStream();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String downloadSpeed = null;
                    String uploadSpeed = null;

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("Download")) {
                            downloadSpeed = line.substring(line.indexOf(":") + 2);
                        } else if (line.startsWith("Upload")) {
                            uploadSpeed = line.substring(line.indexOf(":") + 2);
                        }
                    }

                    process.waitFor();
                    int exitCode = process.exitValue();

                    if (exitCode == 0 && downloadSpeed != null && uploadSpeed != null) {
                        String finalDownloadSpeed = downloadSpeed;
                        String finalUploadSpeed = uploadSpeed;
                        SwingUtilities.invokeLater(() -> lblStatus.setText("<html>Status:<br>Download Speed: " + finalDownloadSpeed + "<br>Upload Speed: " + finalUploadSpeed + "</html>"));
                    } else {
                        SwingUtilities.invokeLater(() -> lblStatus.setText("Status: Speed test failed"));
                    }
                }
            } catch (IOException | InterruptedException ex) {
                SwingUtilities.invokeLater(() -> lblStatus.setText("Status: Exception occurred during speed test"));
            }
        });
    }
}
