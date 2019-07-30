package br.edu.ifspsaocarlos.sdm.kifurecorder.processing.cornerDetector.ellipseDetector;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

import java.util.List;

public interface EllipseDetectorInterface {
    void setFilePrefix(String filePrefix);
    String getName();
    List<RotatedRect> detectEllipsesIn(Mat image);
}
