package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.ellipseDetector;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * This class receives a contour and checks if it can be an ellipse.
 */
public class EllipseChecker
{
    private Mat image;
    private MatOfPoint approximatedContour = null;

    public void setImage(Mat image)
    {
        this.image = image;
    }

    public MatOfPoint getApproximatedContour()
    {
        return approximatedContour;
    }

    public RotatedRect getEllipseFrom(MatOfPoint contour)
    {
        approximatedContour = approximateContour(contour);
        if (!canContourBeAnEllipse(approximatedContour)) return null;
        RotatedRect ellipse = fitEllipseInContour(contour); 
        if (!isEllipseAGoodFitAgainstContour(ellipse, contour)) return null;
        return ellipse;
    }

    // A contour that can be an ellipse must be convex and have at least 5 sides
    private boolean canContourBeAnEllipse(MatOfPoint contour)
    {
        return Imgproc.isContourConvex(contour) && contour.rows() >= 5;
    }

    private MatOfPoint approximateContour(MatOfPoint contour)
    {
        MatOfPoint2f contour2f = new MatOfPoint2f();
        MatOfPoint2f approx2f = new MatOfPoint2f();
        contour.convertTo(contour2f, CvType.CV_32FC2);
        // The lower epsilon is, the more exact the approximation has to be
        Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.03, true);
        MatOfPoint approx = new MatOfPoint();
        approx2f.convertTo(approx, CvType.CV_32S);
        return approx;
    }

    private RotatedRect fitEllipseInContour(MatOfPoint contour)
    {
        MatOfPoint2f contour2f = new MatOfPoint2f();
        contour.convertTo(contour2f, CvType.CV_32FC2);
        return Imgproc.fitEllipse(contour2f);
    }

    private boolean isEllipseAGoodFitAgainstContour(RotatedRect ellipse, MatOfPoint contour)
    {
        // We plot a mask of the contour we are checking
        Mat maskContour = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
        List<MatOfPoint> contours = new ArrayList<>();
        contours.add(contour);
        Imgproc.drawContours(maskContour, contours, 0, new Scalar(255), -1);
        // We then plot the found ellipse
        Mat maskEllipse = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
        Imgproc.ellipse(maskEllipse, ellipse, new Scalar(255), -1);
        // we check the pixels that are only in one or the other image
        Mat leftover = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
        // The leftover is the difference between the contour found and the ellipse we're trying to fit.
        // The less leftover there is, the more the ellipse fits the contour.
        Core.bitwise_xor(maskContour, maskEllipse, leftover);

        int leftoverCount = Core.countNonZero(leftover);
        int maskEllipseCount = Core.countNonZero(maskEllipse);
        double leftoverRatio = (double)leftoverCount / (double)maskEllipseCount;

        return leftoverRatio < 0.15;
    }

}