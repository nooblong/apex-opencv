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

public class Demo1 {
    private ScheduledExecutorService timer;
    private VideoCapture capture = new VideoCapture();
    private static int cameraId = 0;
    private JLabel imageLabel = new JLabel();

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Demo1 demo = new Demo1();
        demo.run();
    }

    public void run() {
        // 创建一个 JFrame
        JFrame frame = new JFrame("Swing Image Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLayout(new BorderLayout());

        // 创建一个 JLabel 用于显示图片
        ImageIcon imageIcon = new ImageIcon("/Users/lyl/Downloads/img-tmp/8f87eefdb4f4c6e5c510ff9b8c9c1bffd2877997.jpg"); // 图片路径
        imageLabel.setIcon(imageIcon);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);


        // 创建一个按钮
        JButton button = new JButton("Click Me!");
        button.addActionListener(e -> startCamera());

        // 将 JLabel 添加到 JFrame 中部
        frame.add(imageLabel, BorderLayout.CENTER);

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
            this.capture.open(cameraId);
            Runnable frameGrabber = new Runnable() {
                @Override
                public void run() {
                    // effectively grab and process a single frame
                    Mat frame = grabFrame();
                    // convert and show the frame
                    BufferedImage bufferedImage = matToBufferedImage(frame);
                    imageLabel.setIcon(new ImageIcon(bufferedImage));
                    imageLabel.validate();
                    imageLabel.repaint();
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
}