package br.edu.ifspsaocarlos.sdm.kifurecorder.processing.cornerDetector.ellipseDetector;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Steps:
 * - Blur
 * - Transform to grayscale
 * - Clusterize historgram in 3 groups, which represent the black
 *   stones, the white stones and the board
 * - Mask out white stones and black stones separately
 * - Dilate white stones because they're usually harder to detect
 * - Detect contours
 * - Check if each contour can be an ellipse
 */
public class SecondEllipseDetector implements EllipseDetectorInterface
{
    private static final int FILTER_UNDER = 0;
    private static final int FILTER_OVER = 1;
    private static final EllipseChecker ellipseChecker = new EllipseChecker();

    private String filePrefix;
    private Mat originalImage;

    public String getName()
    {
        return "second ellipse detector, uses k-means clustering on grayscale image histogram";
    }

    public void setFilePrefix(String filePrefix)
    {
        this.filePrefix = filePrefix;
    }

    public List<RotatedRect> detectEllipsesIn(Mat image)
    {
        this.originalImage = image;
        ellipseChecker.setImage(image);
        Mat preprocessedImage = preprocessImage(image);

        int numberOfBins = 16;
        Mat histogram = getHistogramFrom(preprocessedImage, numberOfBins);
        System.out.println("histogram with 16 bins = ");
        System.out.println(histogram.t().dump());

        // Centroid 0 centers around the dark pixels and centroid 1 around the light ones
        int numberOfClusters = 3;
        int[] centroids = clusterizeHistogramAndReturnCentroids(histogram, numberOfClusters);

        List<RotatedRect> darkEllipses = getPossibleEllipsesByFilteringBelow(centroids[0], preprocessedImage);
        List<RotatedRect> lightEllipses = getPossibleEllipsesByFilteringOver(centroids[1], preprocessedImage);
        List<RotatedRect> ellipses = new ArrayList<>();
        ellipses.addAll(darkEllipses);
        ellipses.addAll(lightEllipses);

        // outputEllipsesOnOriginalImage(ellipses);

        return ellipses;
    }

    private Mat preprocessImage(Mat image)
    {
        Mat processedImage = image.clone();
        processedImage = blur(processedImage);
        processedImage = convertToGrayscale(processedImage);
        processedImage = adjustBrightnessAndContrast(processedImage);
        return processedImage;
    }

    // Blur image to smooth out noise. Being "myopic" here might be
    // good to smooth out imperfections and focus on the colors
    private Mat blur(Mat image)
    {
        Mat blurredImage = image.clone();
        Imgproc.blur(blurredImage, blurredImage, new Size(5, 5));
        Imgproc.blur(blurredImage, blurredImage, new Size(3, 3));
        Imgproc.blur(blurredImage, blurredImage, new Size(3, 3));
        // Imgcodecs.imwrite(filePrefix + "_preprocessed_image_1.png", blurredImage);
        return blurredImage;
    }

    private Mat convertToGrayscale(Mat image)
    {
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(image, grayscaleImage, Imgproc.COLOR_BGR2GRAY, 1); // 1 channel
        // Imgcodecs.imwrite(filePrefix + "_preprocessed_image_2.png", grayscaleImage);
        return grayscaleImage;
    }

    // https://docs.opencv.org/3.4/d3/dc1/tutorial_basic_linear_transform.html
    private Mat adjustBrightnessAndContrast(Mat image)
    {
        Mat adjustedImage = new Mat();
        int rType = -1;
        double alpha = 1.4; // contrast
        int beta = -50; // brightness
        image.convertTo(adjustedImage, rType, alpha, beta);
        // Imgcodecs.imwrite(filePrefix + "_preprocessed_image_3.png", adjustedImage);
        // Don't know if Gamma correction is needed
        return adjustedImage;
    }

    // https://www.programcreek.com/java-api-examples/?class=org.opencv.imgproc.Imgproc&method=calcHist
    private Mat getHistogramFrom(Mat image, int numberOfBins)
    {
        Mat histogram = new Mat();
        MatOfInt channels = new MatOfInt(0);
        Mat mask = new Mat();
        MatOfInt histogramSize = new MatOfInt(numberOfBins);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);

        Imgproc.calcHist(new ArrayList<Mat>(Arrays.asList(image)), channels, mask, histogram, histogramSize, ranges);

        return histogram;
    }

    private int[] clusterizeHistogramAndReturnCentroids(Mat histogram, int numberOfClusters)
    {
        // There should be at least 2 clusters
        if (numberOfClusters < 2) return null;

        int numberOfRows = histogram.rows();
        int[] centroids = new int[numberOfClusters];
        int[] oldCentroids = new int[numberOfClusters];

        // Let's initialize the centroids at each extreme of the histogram
        centroids[0] = 0;
        oldCentroids[0] = centroids[0];
        centroids[1] = numberOfRows - 1;
        oldCentroids[1] = centroids[1];
        if (numberOfClusters > 2) {
            centroids[2] = numberOfRows / 2;
            oldCentroids[2] = centroids[2];
        }

        boolean converged = false;
        int[] labels = new int[numberOfRows];

        while (!converged) {
            int[][] distancesToCentroids = calculateDistanceOfEachRowToCentroids(histogram, centroids);
            int[] sumOfElementsOfEachCluster = new int[numberOfClusters];

            // Set label of each row
            for (int row = 0; row < numberOfRows; row++) {
                int smallestDistance = 999999999;
                int nearestCluster = -1;
                for (int cluster = 0; cluster < numberOfClusters; cluster++) {
                    if (distancesToCentroids[cluster][row] < smallestDistance) {
                        smallestDistance = distancesToCentroids[cluster][row];
                        nearestCluster = cluster;
                    }
                }
                sumOfElementsOfEachCluster[ nearestCluster ] += histogram.get(row, 0)[0];
                labels[row] = nearestCluster;
            }

            converged = true;

            for (int cluster = 0; cluster < numberOfClusters; cluster++) {
                int sum = 0;
                int medianOfCluster = sumOfElementsOfEachCluster[cluster] / 2;

                for (int row = 0; row < numberOfRows; row++) {
                    if (labels[row] != cluster) continue;

                    sum += histogram.get(row, 0)[0];

                    if (sum >= medianOfCluster) {
                        // System.out.println("Centroid " + cluster + " is now " + row);
                        if (centroids[cluster] != row) converged = false;
                        centroids[cluster] = row;
                        break;
                    }
                }
            }

        }

        System.out.println("Centroids = ");
        for (int i = 0; i < numberOfClusters; i++) {
            System.out.println(i + " - " + centroids[i]);
        }

        return centroids;
    }

    private int[][] calculateDistanceOfEachRowToCentroids(Mat histogram, int[] centroids)
    {
        int numberOfRows = histogram.rows();
        int numberOfClusters = centroids.length;
        int[][] distancesToCentroids = new int[numberOfClusters][numberOfRows];

        for (int cluster = 0; cluster < numberOfClusters; cluster++) {
            distancesToCentroids[cluster][ centroids[cluster] ] = 0;

            for (int row = centroids[cluster] - 1; row >= 0; row--) {
                distancesToCentroids[cluster][row] =
                    distancesToCentroids[cluster][row + 1]
                    + (int)histogram.get(row, 0)[0] * (centroids[cluster] - row);
            }
            for (int row = centroids[cluster] + 1; row < histogram.rows(); row++) {
                distancesToCentroids[cluster][row] =
                    distancesToCentroids[cluster][row - 1]
                    + (int)histogram.get(row, 0)[0] * (row - centroids[cluster]);
            }
        }

        return distancesToCentroids;
    }

    private List<RotatedRect> getPossibleEllipsesByFilteringBelow(int centroid, Mat image)
    {
        Mat filteredImage = getFilteredImage(centroid * 16 + 16, FILTER_UNDER, image);
        // Imgcodecs.imwrite(filePrefix + "_preprocessed_image_3_dark_filter.png", filteredImage);

        return findPossibleEllipsesIn(filteredImage, "dark");
    }

    private List<RotatedRect> getPossibleEllipsesByFilteringOver(int centroid, Mat image)
    {
        Mat filteredImage = getFilteredImage(centroid * 16, FILTER_OVER, image);
        // Imgcodecs.imwrite(filePrefix + "_preprocessed_image_3_light_filter.png", filteredImage);
        Mat dilatedImage = new Mat();
        Imgproc.dilate(filteredImage, dilatedImage, Mat.ones(5, 5, CvType.CV_8U));
        // Imgcodecs.imwrite(filePrefix + "_preprocessed_image_3_light_filter_dilated.png", dilatedImage);

        return findPossibleEllipsesIn(dilatedImage, "light");
    }

    // There are 256 possible pixel intensities and the histogram has 16 bins,
    // so each bin represents a reange of 16 pixels
    private Mat getFilteredImage(int threshold, int filter, Mat image)
    {
        Mat filteredImage = image.clone();

        for (int i = 0; i < image.rows(); i++) {
            for (int j = 0; j < image.cols(); j++) {
                if (doesPixelPassFilter(image.get(i, j)[0], threshold, filter)) {
                    filteredImage.put(i, j, new double[]{ 255, 255, 255 });
                } else {
                    filteredImage.put(i, j, new double[]{ 0, 0, 0 });
                }
            }
        }

        return filteredImage;
    }

    private boolean doesPixelPassFilter(double pixelValue, int threshold, int filter)
    {
        return filter == FILTER_UNDER ? pixelValue <= threshold : pixelValue >= threshold;
    }

    private List<RotatedRect> findPossibleEllipsesIn(Mat image, String suffix)
    {
        List<MatOfPoint> contours = findContoursIn(image);
        outputOriginalImageWith(contours, suffix);

        List<RotatedRect> ellipses = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            RotatedRect ellipse = ellipseChecker.getEllipseFrom(contour);
            if (ellipse != null) ellipses.add(ellipse);
        }
        return ellipses;
    }

    private List<MatOfPoint> findContoursIn(Mat image)
    {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(image, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        removeSmallContours(contours);
        // System.out.println("Number of contours found in scene: " + contours.size());
        return contours;
    }

    private void removeSmallContours(List<MatOfPoint> contours)
    {
        for (Iterator<MatOfPoint> it = contours.iterator(); it.hasNext();) {
            MatOfPoint contour = it.next();
            if (Imgproc.contourArea(contour) < 200) {
                it.remove();
            }
        }
    }

    private void outputOriginalImageWith(List<MatOfPoint> contours, String suffix)
    {
        Mat imageWithContoursDetected = this.originalImage.clone();
        Imgproc.drawContours(imageWithContoursDetected, contours, -1, new Scalar(255, 255, 255), 2);
        // Imgcodecs.imwrite(filePrefix + "_preprocessed_image_3_" + suffix + "_filter_contours.png", imageWithContoursDetected);
    }

    private void outputEllipsesOnOriginalImage(List<RotatedRect> ellipses)
    {
        Mat imageWithEllipses = this.originalImage.clone();
        for (RotatedRect ellipse : ellipses) {
            Imgproc.ellipse(imageWithEllipses, ellipse, new Scalar(0, 255, 0));
        }
        Imgcodecs.imwrite(filePrefix + "_ellipses.png", imageWithEllipses);
    }

}