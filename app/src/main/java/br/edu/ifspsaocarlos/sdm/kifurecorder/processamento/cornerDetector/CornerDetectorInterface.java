package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector;

import java.util.List;
import org.opencv.core.Mat;

public interface CornerDetectorInterface {
    public void setImageIndex(int imageIndex);
    public void setCornerIndex(int cornerIndex);
    public List<Corner> detectCandidateCornersIn(Mat image);
}