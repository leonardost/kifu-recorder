package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.similarityCalculator;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

// https://stackoverflow.com/a/13517771
// This is very sensitive to brightness, as it compares pixel values directly,
// but it seems like a very good detector to see when something strange is in
// the scene. Images where hands appear are considered very different from
// images where only the board and stones are visible.
public class FingerprintMatching implements SimilarityCalculatorInterface
{
    private static final int SIMILARITY_THRESHOULD = 20;
    private int imageNumber;

    public void setImageNumber(int imageNumber)
    {
        this.imageNumber = imageNumber;
    }

    public double calculateSimilatiryBetween(Mat image1, Mat image2)
    {
        Mat smallImage1 = new Mat();
        Mat smallImage2 = new Mat();
        Imgproc.resize(image1, smallImage1, new Size(16, 16));
        Imgproc.resize(image2, smallImage2, new Size(16, 16));
        // Imgcodecs.imwrite("processing/image" + imageNumber + "_small1.png", smallImage1);

        int numberOfSimilarPixels = calculateNumberOfSimilarPixelsBetween(smallImage1, smallImage2);
        // System.out.println("            number of similar pixels = " + numberOfSimilarPixels);
        return numberOfSimilarPixels / 256.0;
    }

    private int calculateNumberOfSimilarPixelsBetween(Mat image1, Mat image2)
    {
        int numberOfSimilarPixels = 0;
        for (int i = 0; i < image1.rows(); i++) {
            for (int j = 0; j < image1.cols(); j++) {
                double pixel1 = image1.get(i, j)[0];
                double pixel2 = image2.get(i, j)[0];
                if (Math.abs(pixel1 - pixel2) < SIMILARITY_THRESHOULD) {
                    numberOfSimilarPixels++;
                }
            }
        }
        return numberOfSimilarPixels;
    }

    public boolean areImagesSimilar(Mat image1, Mat image2)
    {
        return calculateSimilatiryBetween(image1, image2) > 0.8;
    }
}