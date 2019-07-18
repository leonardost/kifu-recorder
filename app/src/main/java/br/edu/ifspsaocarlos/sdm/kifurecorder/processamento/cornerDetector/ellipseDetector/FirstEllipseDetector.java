package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.ellipseDetector;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * This detector first preprocesses the image following these steps:
 * - Blur
 * - Detect borders with Canny filter
 * - Dilation
 * - Erosion
 * - Invert
 * - Erosion
 * 
 * Then, the resulting image's contours are detected using
 * Imgproc.findContours. Small contours are removed.
 * 
 * Each resulting contour is then checked to see if it can be an
 * ellipse by following these steps:
 * - Check if it is convex and has at least 5 sides
 * - An ellipse that fits that contour is found with Imgproc.fitEllipse
 * - That ellipse is checked against the original contour by counting
 *   the number of pixels that remain in a XOR operation of them
 * - If the ratio between that number of pixels and the area of the
 *   ellipse is smaller than 15%, that ellipse is stored. This means
 *   the contour is probably an ellipse
 */
public class FirstEllipseDetector implements EllipseDetectorInterface {

    private String filePrefix;
    private Mat image;
    private List<MatOfPoint> approximatedContours;

    public String getName() {
        return "first ellipse detector";
    }

    public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    // https://stackoverflow.com/questions/35121045/find-cost-of-ellipse-in-opencv
    public List<RotatedRect> detectEllipsesIn(Mat image) {
        this.image = image.clone();
        Mat imageWithEllipses = image.clone();
        approximatedContours = new ArrayList<>();

        Mat preprocessedImage = preprocessImage(image.clone());
        List<MatOfPoint> contours = detectContoursIn(preprocessedImage);
        outputImageWithContours(image, contours, filePrefix + "_all_contours.png");
        List<RotatedRect> ellipses = new ArrayList<>();
        // Ir more than 5 contours were found in the scene, there's something more than
        // stones and the board in the scene
        if (contours.size() > 5) {
            System.out.println("More than 5 contours found, disregarding this image");
            return ellipses;
        }

        EllipseChecker ellipseChecker = new EllipseChecker();
        ellipseChecker.setImage(this.image);

        for (int i = 0; i < contours.size(); i++) {
            RotatedRect ellipse = ellipseChecker.getEllipseFrom(contours.get(i));
            approximatedContours.add(ellipseChecker.getApproximatedContour());
            if (ellipse == null) continue;

            // Let's increase the ellipse size to encompass the entire stone and some more
            // The perspective should be taken into account here, but let's leave it like this for now
            ellipse.size.width *= 1.2;
            ellipse.size.height *= 1.1;
            ellipses.add(ellipse);
            Imgproc.ellipse(imageWithEllipses, ellipse, new Scalar(0, 255, 0));
        }

        // outputImageWithContours(image, approximatedContours, filePrefix + "_approximated_contours.png");
        // Imgcodecs.imwrite(filePrefix + "_ellipse_fit.png", imageWithEllipses);

        return ellipses;
    }

    private Mat preprocessImage(Mat image) {
        // Blur image to smooth noise
        Imgproc.blur(image, image, new Size(3, 3));
        // Imgcodecs.imwrite(filePrefix + "_preprocessed_image_0.png", image);
        // Detect borders with Canny filter
        image = detectBordersIn(image);
        // Imgcodecs.imwrite(filePrefix + "_preprocessed_image_1.png", image);
        Imgproc.dilate(image, image, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 3);
        Imgproc.erode(image, image, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 3);
        // Imgcodecs.imwrite(filePrefix + "_preprocessed_image_2.png", image);
        // Invert regions
        Core.bitwise_not(image, image);
        Imgproc.erode(image, image, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 1);
        // Imgcodecs.imwrite(filePrefix + "_preprocessed_image_3.png", image);
        return image;
    }

    private Mat detectBordersIn(Mat image) {
        Mat imageWithBordersDetected = new Mat();
        Imgproc.Canny(image, imageWithBordersDetected, 50, 150);
        return imageWithBordersDetected;
    }

    private List<MatOfPoint> detectContoursIn(Mat imageWithBordersDetected)
    {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        // Imgproc.findContours(imageWithBordersDetected, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        Imgproc.findContours(imageWithBordersDetected, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        removeSmallContours(contours);
        System.out.println("Number of contours found in scene: " + contours.size());
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

    private static void outputImageWithContours(Mat image, List<MatOfPoint> contours, String filename) {
        Mat imageWithContoursDetected = image.clone();
        Random random = new Random();
        for (MatOfPoint contour : contours) {
            int color1 = random.nextInt(255);
            int color2 = random.nextInt(255);
            int color3 = random.nextInt(255);
            List<MatOfPoint> c = new ArrayList<>();
            c.add(contour);
            Imgproc.drawContours(imageWithContoursDetected, c, -1, new Scalar(color1, color2, color3), 2);
        }

        Imgcodecs.imwrite(filename, imageWithContoursDetected);
    }

}