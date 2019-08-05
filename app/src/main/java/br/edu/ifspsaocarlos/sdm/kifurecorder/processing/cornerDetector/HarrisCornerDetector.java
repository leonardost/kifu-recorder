package br.edu.ifspsaocarlos.sdm.kifurecorder.processing.cornerDetector;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class HarrisCornerDetector implements CornerDetectorInterface {

    private int imageIndex;
    private int cornerIndex;

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }

    public void setCornerIndex(int cornerIndex) {
        this.cornerIndex = cornerIndex;
    }

    public List<Corner> detectCandidateCornersIn(Mat image) {
        Mat correctColorFormatImage = convertImageToCorrectColorFormat(image);
        Mat grayscaleImage = convertToGrayscale(correctColorFormatImage);
        Mat resultOfCornerHarris = applyCornerHarrisTo(grayscaleImage);
        resultOfCornerHarris = dilateImage(resultOfCornerHarris);
        double harrisThreshold = calculateHarrisCornerThreshold(resultOfCornerHarris);
        List<PointCluster> cornerPointsClusters = findPossibleCornerRegions(resultOfCornerHarris, harrisThreshold);
        List<Ponto> possibleCenters = getCentroidsFrom(cornerPointsClusters);
        List<Corner> candidateCorners = new ArrayList<>();
        for (Ponto center : possibleCenters) {
            candidateCorners.add(new Corner(center.x, center.y));
        }
        return candidateCorners;
    }

    private Mat convertImageToCorrectColorFormat(Mat image) {
        Mat correctColorFormatImage = new Mat();
        Imgproc.cvtColor(image, correctColorFormatImage, Imgproc.COLOR_BGR2GRAY);
        return correctColorFormatImage;
    }

    private Mat convertToGrayscale(Mat image) {
        Mat grayscaleImage = new Mat();
        image.convertTo(grayscaleImage, CvType.CV_32F);
        return grayscaleImage;
    }

    private Mat applyCornerHarrisTo(Mat image) {
        Mat resultOfCornerHarris = new Mat();
        Imgproc.cornerHarris(image, resultOfCornerHarris, 2, 3, 0.04);
        return resultOfCornerHarris;
    }

    private Mat dilateImage(Mat image) {
        Mat dilatedImage = new Mat();
        Mat emptyKernel = new Mat();
        Imgproc.dilate(image, dilatedImage, emptyKernel);
        return dilatedImage;
    }

    private double calculateHarrisCornerThreshold(Mat resultOfCornerHarris) {
        // 1% of the maximum value in the matrix
        return Core.minMaxLoc(resultOfCornerHarris).maxVal * 0.01;
    }

    // Find Harris corner points by using depth-first search in the image, because these points form regions
    private List<PointCluster> findPossibleCornerRegions(Mat harrisImage, double threshold) {
        List<PointCluster> pointClusters = new ArrayList<>();
        boolean[][] visitedPoints = new boolean[harrisImage.height()][harrisImage.width()];

        for (int i = 0; i < harrisImage.height(); i++) {
            for (int j = 0; j < harrisImage.width(); j++) {
                if (visitedPoints[i][j]) continue;
                if (harrisImage.get(i, j)[0] > threshold) {
                    PointCluster pointCluster = new PointCluster();
                    pointClusters.add(pointCluster);
                    doDepthFirstSearchToFindCornerRegions(harrisImage, i, j, threshold, visitedPoints, pointClusters);
                }
            }
        }

        return pointClusters;
    }

    private void doDepthFirstSearchToFindCornerRegions(Mat image, int line, int column, double threshold, boolean[][] visitedPoints, List<PointCluster> clusters) {
        if (line < 0 || column < 0 || line >= image.height() || column >= image.width()) return;
        if (visitedPoints[line][column]) return;
        visitedPoints[line][column] = true;

        if (image.get(line, column)[0] > threshold) {
            clusters.get(clusters.size() -1).add(new Ponto(column, line));
            doDepthFirstSearchToFindCornerRegions(image, line - 1, column, threshold, visitedPoints, clusters);
            doDepthFirstSearchToFindCornerRegions(image, line + 1, column, threshold, visitedPoints, clusters);
            doDepthFirstSearchToFindCornerRegions(image, line, column - 1, threshold, visitedPoints, clusters);
            doDepthFirstSearchToFindCornerRegions(image, line, column + 1, threshold, visitedPoints, clusters);
        }
    }

    private List<Ponto> getCentroidsFrom(List<PointCluster> cornerPointsClusters) {
        List<Ponto> possibleCenters = new ArrayList<>();
        for (PointCluster pointCluster : cornerPointsClusters) {
            possibleCenters.add(pointCluster.getCentroid());
        }
        return possibleCenters;
    }

}