package org.example;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Demo2 {
    private ScheduledExecutorService timer;
    private VideoCapture capture = new VideoCapture();
    private static int cameraId = 0;
    private JLabel imageLabel = new JLabel();
    private JLabel imageLabelTotal = new JLabel();
    private JLabel imageLabelMat = new JLabel();
    double deadConfidence = 0.45;
    double confidence = 0.77;
    int machMethod = Imgproc.TM_CCOEFF_NORMED;

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Demo2 demo = new Demo2();
        System.out.println(Toolkit.getDefaultToolkit().getScreenSize().getWidth());
        System.out.println(Toolkit.getDefaultToolkit().getScreenSize().getHeight());
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
        imageLabelMat.setVisible(false);


        // 创建一个按钮
        JButton button = new JButton("Click Me!");
        button.addActionListener(e -> startCamera());

        // 将 JLabel 添加到 JFrame 中部
        frame.add(imageLabel, BorderLayout.CENTER);
        frame.add(imageLabelTotal, BorderLayout.WEST);
        frame.add(imageLabelMat, BorderLayout.EAST);

        // 将按钮添加到 JFrame 底部
        frame.add(button, BorderLayout.SOUTH);

        // 显示 JFrame
        frame.setVisible(true);
    }

    void startCamera() {
        if (imageLabel.isVisible()) {
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
                    int width = bufferedImage.getWidth() / 5 ;
                    int height = bufferedImage.getHeight() / 6;
                    BufferedImage image = cropImage(bufferedImage, x, y, width, height);
                    Rect rect = new Rect(x, y, width, height);
                    Mat cropped = new Mat(frame, rect);
                    double rampageLMG = imageDetection(cropped, "rampageLMG", false);
                    System.out.println(rampageLMG);
                    imageLabel.setIcon(new ImageIcon(matToBufferedImage(cropped)));
                    imageLabel.validate();
                    imageLabel.repaint();

                    imageLabelTotal.setVisible(true);
                    imageLabelTotal.setIcon(new ImageIcon(scaleImage(bufferedImage, 1000, 350)));
                    imageLabelTotal.validate();
                    imageLabelTotal.repaint();

                    imageLabelMat.setVisible(true);
                    Mat checkItemMat = Imgcodecs.imread( "/Users/lyl/Documents/GitHub/nosync.nosync/apex-opencv/src/main/resources/1080/" + "rampageLMG" +".png");
                    imageLabelMat.setIcon(new ImageIcon(matToBufferedImage(checkItemMat)));
                    imageLabelMat.validate();
                    imageLabelMat.repaint();
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

    public double imageDetection(Mat _1weapon_2dead_3setting, String checkItem, boolean debugVerbose) {
        try {
            Mat outputImage = new Mat();
            Mat checkItemMat = Imgcodecs.imread( "/Users/lyl/Documents/GitHub/nosync.nosync/apex-opencv/src/main/resources/1080/" + checkItem +".png");
            if (!checkItemMat.empty()) {
                Imgproc.cvtColor(checkItemMat, checkItemMat, Imgproc.COLOR_BGR2GRAY);
            }
            Imgproc.matchTemplate(_1weapon_2dead_3setting, checkItemMat, outputImage, machMethod);//
            Core.MinMaxLocResult confidenceValue = Core.minMaxLoc(outputImage);//find the max value and the location of the max value

            Point matchLoc = confidenceValue.maxLoc;
            Imgproc.rectangle(_1weapon_2dead_3setting, matchLoc,
                    new Point(matchLoc.x + checkItemMat.cols(), matchLoc.y + checkItemMat.rows()), new Scalar(0, 255, 0));

            if (debugVerbose) {
                System.out.println("checkItem: " + checkItem);
                System.out.println("screenshot: " +_1weapon_2dead_3setting);
//                System.out.println("screenResolution: " + screenResolution);
                System.out.println("confidenceValue.maxVal = " + confidenceValue.maxVal);
                // out
            }
            return confidenceValue.maxVal;
        } catch (Exception e) {
            // output error message to ui
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            return 0;
        }
    }

    public Mat matify(BufferedImage sourceImg) {
        Mat mat = new Mat(sourceImg.getHeight(), sourceImg.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) sourceImg.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }
}