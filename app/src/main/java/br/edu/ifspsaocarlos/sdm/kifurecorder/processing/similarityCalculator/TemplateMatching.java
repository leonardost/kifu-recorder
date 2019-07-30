package br.edu.ifspsaocarlos.sdm.kifurecorder.processing.similarityCalculator;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class TemplateMatching implements SimilarityCalculatorInterface
{
    private int imageNumber;

    public void setImageNumber(int imageNumber)
    {
        this.imageNumber = imageNumber;
    }

    public double calculateSimilatiryBetween(Mat image1, Mat image2)
    {
        Mat result = new Mat();
        Imgproc.matchTemplate(image2, image1, result, Imgproc.TM_SQDIFF_NORMED);
        // https://docs.opencv.org/3.4.2/d2/de8/group__core__array.html#ga87eef7ee3970f86906d69a92cbf064bd
        // It doesn't make sense to normalize here because all the images have the same size,
        // and matchTemplate will generate a 1x1 matrix, which contains how similar the
        // region is to the other region (of the same size).
        // Core.normalize(result, result, 0.0, 255.0, Core.NORM_INF);
        // The smaller maxVal is, the more similar the images are
        Core.MinMaxLocResult minMaxLoc = Core.minMaxLoc(result);
        // System.out.println("        " + minMaxLoc.maxLoc);
        // System.out.println("            " + minMaxLoc.maxVal);
        return minMaxLoc.maxVal;
    }

    public boolean areImagesSimilar(Mat image1, Mat image2)
    {
        return calculateSimilatiryBetween(image1, image2) < 0.01;
    }
}