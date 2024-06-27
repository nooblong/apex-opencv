package org.example;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Demo2 {
    private ScheduledExecutorService timer;
    private VideoCapture capture = new VideoCapture();
    private static int cameraId = 0;
    private JLabel imageLabel = new JLabel();
    private JLabel imageLabelTotal = new JLabel();

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Demo2 demo = new Demo2();
        demo.run();
    }

    public void run() {
        // 创建一个 JFrame
        JFrame frame = new JFrame("Swing Image Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 400);
        frame.setLayout(new BorderLayout());

        // 创建一个 JLabel 用于显示图片
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVisible(false);
        imageLabelTotal.setHorizontalAlignment(JLabel.CENTER);
        imageLabelTotal.setVisible(false);


        // 创建一个按钮
        JButton button = new JButton("Click Me!");
        button.addActionListener(e -> startCamera());

        // 将 JLabel 添加到 JFrame 中部
        frame.add(imageLabel, BorderLayout.CENTER);
        frame.add(imageLabelTotal, BorderLayout.WEST);

        // 将按钮添加到 JFrame 底部
        frame.add(button, BorderLayout.SOUTH);

        // 显示 JFrame
        frame.setVisible(true);
    }

    void startCamera() {
        if (imageLabel.isVisible()) {
            imageLabel.setVisible(false);
            stopAcquisition();
        } else {
            imageLabel.setVisible(true);
            this.capture.open("/Users/lyl/Downloads/mp4-tmp/testGun.mp4");
            Runnable frameGrabber = new Runnable() {
                @Override
                public void run() {
                    // effectively grab and process a single frame
                    Mat frame = grabFrame();
                    // convert and show the frame
                    BufferedImage bufferedImage = matToBufferedImage(frame);
                    int systemWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
                    int systemHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
                    int x = bufferedImage.getWidth() - bufferedImage.getWidth() / 5 - 40;
                    int y = bufferedImage.getHeight() - bufferedImage.getHeight() / 6;
                    int width = bufferedImage.getWidth() / 5;
                    int height = bufferedImage.getHeight() / 6;
                    BufferedImage image = cropImage(bufferedImage, x, y, width, height);
                    imageLabel.setIcon(new ImageIcon(image));
                    imageLabel.validate();
                    imageLabel.repaint();

                    imageLabelTotal.setVisible(true);
                    imageLabelTotal.setIcon(new ImageIcon(scaleImage(bufferedImage, 1000, 350)));
                    imageLabelTotal.validate();
                    imageLabelTotal.repaint();
                }
            };
            this.timer = Executors.newSingleThreadScheduledExecutor();
            this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
        }
    }

    private Mat grabFrame() {
        // init everything
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened()) {
            try {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty()) {
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                }

            } catch (Exception e) {
                // log the error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }

        return frame;
    }

    private void stopAcquisition() {
        if (this.timer != null && !this.timer.isShutdown()) {
            try {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (this.capture.isOpened()) {
            // release the camera
            this.capture.release();
        }
    }

    private static BufferedImage matToBufferedImage(Mat original) {
        // init
        BufferedImage image = null;
        int width = original.width(), height = original.height(), channels = original.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        if (original.channels() > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    private static BufferedImage scaleImage(BufferedImage image, int maxWidth, int maxHeight) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        // 计算缩放比例
        double scale = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);

        // 计算缩放后的宽度和高度
        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        // 创建缩放后的图像
        Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        BufferedImage bufferedImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();
        return bufferedImage;
    }

    private static BufferedImage cropImage(BufferedImage originalImage, int x, int y, int width, int height) {
        return originalImage.getSubimage(x, y, width, height);
    }
}