package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.boardDetector;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BoardDetectorByQuadrilateralCounting implements BoardDetectorInterface
{
    // This is the threshould of vanished quadrilaterals to consider the board was lost
    public static final int THRESHOULD = 10;
    // This is the threshould of quadrilaterals to consider that the board was found again
    public static final int RECOVERY_THRESHOULD = 4;
    public static final Scalar RED = new Scalar(0, 0, 255);
    public static final Scalar BLUE = new Scalar(255, 0, 0);

    private int imageIndex;
    private int state;

    private int numberOfQuadrilateralsFound;
    private int lastNumberOfQuadrilateralsFound;
    private int lastNumberOfQuadrilateralsFoundWhileBoardWasInsideContour;

    public BoardDetectorByQuadrilateralCounting() {
        lastNumberOfQuadrilateralsFound = -1;
    }

    public boolean isBoardContainedIn(Mat ortogonalBoardImage) {
        numberOfQuadrilateralsFound = calculateNumberOfQuadrilateralsInside(ortogonalBoardImage);
        boolean isBoardContainedIn = isBoardInsideContourAccordingToQuadrilateralsDetection();

        if (isBoardContainedIn) {
            lastNumberOfQuadrilateralsFoundWhileBoardWasInsideContour = numberOfQuadrilateralsFound;
        }
        lastNumberOfQuadrilateralsFound = numberOfQuadrilateralsFound;

        return isBoardContainedIn;
    }

    private int calculateNumberOfQuadrilateralsInside(Mat ortogonalBoardImage) {
        Mat imageWithBordersDetected = detectBordersIn(addBlackBorderAround(ortogonalBoardImage));
        outputImageWithBorders(imageWithBordersDetected);

        List<MatOfPoint> contours = detectContoursIn(imageWithBordersDetected);
        outputImageWithContours(ortogonalBoardImage, contours);

        List<MatOfPoint> quadrilaterals = detectQuadrilateralsAmong(contours);
        outputImageWithQuadrilaterals(ortogonalBoardImage, quadrilaterals);

//        System.out.println("Number of quadrilaterals found: " + quadrilaterals.size());

        return quadrilaterals.size();
    }

    private Mat addBlackBorderAround(Mat image) {
        Mat imageWithBlackBorder = image.clone();
        Imgproc.rectangle(imageWithBlackBorder, new Point(0, 0), new Point(499, 499), new Scalar(0, 0, 0), 1);
        return imageWithBlackBorder;
    }

    private Mat detectBordersIn(Mat image) {
        Mat imageWithBordersDetected = new Mat();
        Imgproc.Canny(image, imageWithBordersDetected, 50, 100);
        Imgproc.dilate(imageWithBordersDetected, imageWithBordersDetected, Mat.ones(3, 3, CvType.CV_32F));
        return imageWithBordersDetected;
    }

    private void outputImageWithBorders(Mat imageWithBordersDetected) {
//        Imgcodecs.imwrite("processing/ortogonal_with_borders_detected_" + imageIndex + ".jpg", imageWithBordersDetected);
    }

    private List<MatOfPoint> detectContoursIn(Mat imageWithBordersDetected) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(imageWithBordersDetected, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        removeSmallContours(contours);
        return contours;
    }

    private void removeSmallContours(List<MatOfPoint> contours) {
        for (Iterator<MatOfPoint> it = contours.iterator(); it.hasNext();) {
            MatOfPoint contour = it.next();
            if (Imgproc.contourArea(contour) < 400) {
                it.remove();
            }
        }
    }

    private void outputImageWithContours(Mat ortogonalBoardImage, List<MatOfPoint> contours) {
        Mat imageWithContoursDetected = ortogonalBoardImage.clone();
        Imgproc.drawContours(imageWithContoursDetected, contours, -1, RED, 2);
//        Imgcodecs.imwrite("processing/ortogonal_with_contours_detected_" + imageIndex + ".jpg", imageWithContoursDetected);
    }

    private List<MatOfPoint> detectQuadrilateralsAmong(List<MatOfPoint> contours) {
        List<MatOfPoint> quadrilaterals = new ArrayList<>();

        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f approx2f = new MatOfPoint2f();
            contour.convertTo(contour2f, CvType.CV_32FC2);
            // The 0.1 means this detection is very lenient, as the goal here
            // is to find as most squares as possible inside the image
            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.1, true);

            MatOfPoint approx = new MatOfPoint();
            approx2f.convertTo(approx, CvType.CV_32S);
            double contourArea = Math.abs(Imgproc.contourArea(approx2f));

            if (isQuadrilateral(approx2f, approx)) {
                quadrilaterals.add(approx);
            }
        }

        return quadrilaterals;
    }

    private boolean isQuadrilateral(MatOfPoint2f approx2f, MatOfPoint approx) {
        return approx2f.toList().size() == 4 && Imgproc.isContourConvex(approx);
    }

    private void outputImageWithQuadrilaterals(Mat ortogonalBoardImage, List<MatOfPoint> quadrilaterals) {
        Mat imageWithQuadrilateralsDetected = ortogonalBoardImage.clone();
        for (MatOfPoint quadrilateral : quadrilaterals) {
            List<MatOfPoint> contoursList = new ArrayList<MatOfPoint>();
            contoursList.add(quadrilateral);
            Imgproc.drawContours(imageWithQuadrilateralsDetected, contoursList, -1, BLUE, 2);
        }
//        Imgcodecs.imwrite("processing/ortogonal_with_quadrilaterals_detected_" + imageIndex + ".jpg", imageWithQuadrilateralsDetected);
    }

    private boolean isBoardInsideContourAccordingToQuadrilateralsDetection() {
        if (state == STATE_BOARD_IS_INSIDE) {
            return isFirstDetection() || calculateDifferenceOfDetectedQuadrilaterals() < THRESHOULD;
        } else {
            return lastNumberOfQuadrilateralsFoundWhileBoardWasInsideContour - numberOfQuadrilateralsFound
                <= RECOVERY_THRESHOULD;
        }
    }

    private boolean isFirstDetection() {
        return lastNumberOfQuadrilateralsFound == -1;
    }

    private int calculateDifferenceOfDetectedQuadrilaterals() {
        return lastNumberOfQuadrilateralsFound - numberOfQuadrilateralsFound;
    }

    public int getNumberOfQuadrilateralsFound() {
        return numberOfQuadrilateralsFound;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }

}
