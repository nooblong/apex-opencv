package org.example;


import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Main {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("Hello world!");
        Mat mat = Mat.eye(3, 3, CvType.CV_8UC1);
        System.out.println("mat: " + mat.dump());
    }
}