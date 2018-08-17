package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class CornerDetector {

    public final static int RADIUS_OF_REGION_OF_INTEREST = 50;

    public Ponto updateCorner(Mat image, Ponto corner, int cornerIndex) {

        Mat regionImage = getRegionOfInterestAround(corner, image);
//        DebugHelper.writeImage(regionImage, Imgproc.COLOR_RGBA2BGR, "debug_1_regiao_ao_redor_do_canto_" + cornerIndex + ".jpg");
        Mat correctColorFormatImage = convertImageToCorrectColorFormat(regionImage);
        Mat grayscaleImage = convertToGrayscale(correctColorFormatImage);
        Mat resultOfCornerHarris = applyCornerHarrisTo(grayscaleImage);
        resultOfCornerHarris = dilateImage(resultOfCornerHarris);
        double harrisThreshold = calculateHarrisCornerThreshold(resultOfCornerHarris);

        List<PointCluster> cornerPointsClusters = findPossibleCornerPointsAndClusterizeThem(regionImage, resultOfCornerHarris, harrisThreshold);
        List<Ponto> possibleCenters = findPossibleCenters(cornerPointsClusters);
//        plotPossibleCenters(possibleCenters);
        Ponto pointClosestToCenterOfRegionOfInterest = getNearestPointToCenterOfRegionOfInterest(possibleCenters);

        if (pointClosestToCenterOfRegionOfInterest != null) {
            Ponto upperLeftCornerOfRegionOfInterest = corner.add(new Ponto(-50, -50));
            Ponto newCornerPosition = pointClosestToCenterOfRegionOfInterest.add(upperLeftCornerOfRegionOfInterest);
            return newCornerPosition;
        }

        return null;
    }

    private Mat getRegionOfInterestAround(Ponto point, Mat image) {
        int x = point.x - RADIUS_OF_REGION_OF_INTEREST > 0 ? point.x - RADIUS_OF_REGION_OF_INTEREST : 0;
        int y = point.y - RADIUS_OF_REGION_OF_INTEREST > 0 ? point.y - RADIUS_OF_REGION_OF_INTEREST : 0;
        int w = x + 2 * RADIUS_OF_REGION_OF_INTEREST < image.cols() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.cols() - x;
        int h = y + 2 * RADIUS_OF_REGION_OF_INTEREST < image.rows() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.rows() - y;

        Rect regionOfInterest = new Rect(x, y, w, h);
        return new Mat(image, regionOfInterest);
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
        double max = resultOfCornerHarris.get(0, 0)[0];
        for (int i = 0; i < resultOfCornerHarris.height(); i++) {
            for (int j = 0; j < resultOfCornerHarris.width(); j++) {
                if (resultOfCornerHarris.get(i, j)[0] > max) {
                    max = resultOfCornerHarris.get(i, j)[0];
                }
            }
        }
        return 0.01 * max;
    }

    private void markCornerPointsInImageInRed(Mat image, Mat harrisImage, double threshold) {
        double[] red = {0, 0, 255};
        for (int i = 0; i < harrisImage.height(); i++) {
            for (int j = 0; j < harrisImage.width(); j++) {
                if (harrisImage.get(i, j)[0] > threshold) {
                    image.put(i, j, red);
                }
            }
        }
    }

    private List<PointCluster> findPossibleCornerPointsAndClusterizeThem(Mat image, Mat harrisImage, double threshold) {
        List<PointCluster> pointClusters = new ArrayList<>();

        for (int i = 0; i < harrisImage.height(); i++) {
            for (int j = 0; j < harrisImage.width(); j++) {
                if (harrisImage.get(i, j)[0] > threshold) {
                    addPointToClosestPointClusterOrCreateANewOne(new Ponto(j, i), pointClusters);
                }
            }
        }

        return pointClusters;
    }

    private void addPointToClosestPointClusterOrCreateANewOne(Ponto point, List<PointCluster> pointClusters) {
        boolean foundCluster = false;

        for (PointCluster pointCluster : pointClusters) {
            if (pointCluster.isInsideClusterDistance(point)) {
                pointCluster.add(point);
                foundCluster = true;
            }
        }

        if (!foundCluster) {
            PointCluster pointCluster = new PointCluster();
            pointCluster.add(point);
            pointClusters.add(pointCluster);
        }
    }

    private List<Ponto> findPossibleCenters(List<PointCluster> cornerPointsClusters) {
        List<Ponto> possibleCenters = new ArrayList<>();

        for (PointCluster pointCluster : cornerPointsClusters) {
            possibleCenters.add(pointCluster.getCentroid());
            Log.d("TAG", "Possible center: " + pointCluster.getCentroid());
        }

        return possibleCenters;
    }

    private void plotPossibleCenters(List<Ponto> possibleCenters, int cornerIndex) {
        Mat possibleCentersImage = new Mat(100, 100, CvType.CV_8UC3);
        byte[] blue = {(byte)255, 0, 0};
        byte[] black = {0, 0, 0};
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                possibleCentersImage.put(i, j, black);
            }
        }
        for (Ponto point : possibleCenters) {
            possibleCentersImage.put(point.y, point.x, blue);
        }
        DebugHelper.writeImage(possibleCentersImage, Imgproc.COLOR_RGB2BGR, "debug_2_regiao_ao_redor_do_canto_" + cornerIndex + ".jpg");
    }

    private Ponto getNearestPointToCenterOfRegionOfInterest(List<Ponto> points) {
        Ponto center = new Ponto(50, 50);
        Ponto nearestPoint = null;
        double minimumDistance = 999999999;
        for (Ponto point : points) {
            if (point.distanceTo(center) < minimumDistance) {
                minimumDistance = point.distanceTo(center);
                nearestPoint = point;
            }
        }
        return nearestPoint;
    }

    private void drawBorderOnImage(Mat image, Ponto[] corners, int imageIndex) {
        org.opencv.core.Point[] cantos = new org.opencv.core.Point[4];
        cantos[0] = new org.opencv.core.Point(corners[0].x, corners[0].y);
        cantos[1] = new org.opencv.core.Point(corners[1].x, corners[1].y);
        cantos[2] = new org.opencv.core.Point(corners[2].x, corners[2].y);
        cantos[3] = new org.opencv.core.Point(corners[3].x, corners[3].y);
        MatOfPoint contornoDoTabuleiro = new MatOfPoint(cantos);

        List<MatOfPoint> listaContorno = new ArrayList<MatOfPoint>();
        listaContorno.add(contornoDoTabuleiro);
        Scalar red = new Scalar(0, 0, 255);
        Imgproc.drawContours(image, listaContorno, -1, red, 2);
        Imgcodecs.imwrite("imagem" + imageIndex + ".jpg", image);
    }

}
