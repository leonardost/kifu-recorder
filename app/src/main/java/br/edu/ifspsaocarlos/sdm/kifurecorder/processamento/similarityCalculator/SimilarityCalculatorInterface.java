package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.similarityCalculator;

import org.opencv.core.Mat;

public interface SimilarityCalculatorInterface
{
    public void setImageNumber(int imageNumber);
    public double calculateSimilatiryBetween(Mat image1, Mat image2);
    public boolean areImagesSimilar(Mat image1, Mat image2);
}
