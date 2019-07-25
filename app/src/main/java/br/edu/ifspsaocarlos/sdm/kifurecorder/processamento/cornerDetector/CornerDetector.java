package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.FileHelper;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Logger;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Ponto;

public class CornerDetector {

    public final static int RADIUS_OF_REGION_OF_INTEREST = 40;
    private long imageIndex;
    private int cornerIndex;
    private Corner corner;
    private FileHelper fileHelper;
    private Logger logger;

    private HarrisCornerDetector harrisCornerDetector = new HarrisCornerDetector();
    private EllipseCornerDetector ellipseCornerDetector = new EllipseCornerDetector();

    public void setFileHelper(FileHelper fileHelper) {
        this.fileHelper = fileHelper;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
        harrisCornerDetector.setImageIndex(imageIndex);
        ellipseCornerDetector.setImageIndex(imageIndex);
    };

    public void setCornerIndex(int cornerIndex) {
        this.cornerIndex = cornerIndex;
        harrisCornerDetector.setCornerIndex(cornerIndex);
        ellipseCornerDetector.setCornerIndex(cornerIndex);
    }

    public void setCorner(Corner corner) {
        this.corner = corner;
    }

    public Corner detectCornerIn(Mat image) {
        System.out.println("Processing corner " + cornerIndex);
        Mat regionImage = getRegionOfInterestAround(corner, image);
//        Imgcodecs.imwrite("processing/corner" + cornerIndex + "_frame" + imageIndex + ".png", regionImage);

        List<Corner> candidateCornerHarris = harrisCornerDetector.detectCandidateCornersIn(regionImage);
        List<Corner> candidateCornerEllipsis = ellipseCornerDetector.detectCandidateCornersIn(regionImage);

        // Remove Harris corner candidates that are too close to circle corner candidates
        // This is done to try to remove corner candidates that appear on the edge of circles
        for (Iterator<Corner> it = candidateCornerHarris.iterator(); it.hasNext();) {
            Corner corner = it.next();
            for (Corner circlePoint : candidateCornerEllipsis) {
                if (circlePoint.isTooCloseToCircle(corner.position)) {
                    it.remove();
                    break;
                }
            }
        }

        // Plot corners on image
        Mat imageWithCornersPlotted = regionImage.clone();
        for (Corner point : candidateCornerEllipsis) {
            System.out.println("Candidate corner found by circle detection in image " + imageIndex + ": ");
            System.out.println(point);
            Imgproc.circle(imageWithCornersPlotted, new Point(point.getX(), point.getY()), 1, new Scalar(0, 255, 0), -1);
            Imgproc.ellipse(imageWithCornersPlotted, point.stonePosition, new Scalar(0, 255, 255));
        }
        for (Corner point : candidateCornerHarris) {
            System.out.println("Candidate corner found by corner Harris detection in frame " + imageIndex + ": ");
            System.out.println(point);
            Imgproc.circle(imageWithCornersPlotted, new Point(point.getX(), point.getY()), 1, new Scalar(0, 0, 255), -1);
        }
//        fileHelper.writePngImage(imageWithCornersPlotted, "corner" + cornerIndex + "_frame" + imageIndex + "_candidate_corners");

        List<Corner> candidateCorners = new ArrayList<>();
        candidateCorners.addAll(candidateCornerHarris);
        candidateCorners.addAll(candidateCornerEllipsis);
//        filterCandidateCornersWhichFallOutsideROI(cnadidateCorners);

        // A corner should have at most 4 candidates, be them Harris corners or ellipsis corners
        // More than that probably means something is wrong in the detection, or there's something
        // else in the scene, like a player's hand or something else
//        if (candidateCorners.size() > 4) return null;

        Corner candidateCorner = getCandidateNearestToCenterOfRegionOfInterest(candidateCorners);

        if (candidateCorner != null) {
            Ponto upperLeftCornerOfRegionOfInterest = corner.position.add(new Ponto(-RADIUS_OF_REGION_OF_INTEREST, -RADIUS_OF_REGION_OF_INTEREST));
            Ponto newCornerPosition = candidateCorner.position.add(upperLeftCornerOfRegionOfInterest);
            return new Corner(newCornerPosition.x, newCornerPosition.y, candidateCorner.isStone);
        }

        return null;
    }

    private Mat getRegionOfInterestAround(Corner corner, Mat image) {
        int x = corner.getX() - RADIUS_OF_REGION_OF_INTEREST > 0 ? corner.getX() - RADIUS_OF_REGION_OF_INTEREST : 0;
        int y = corner.getY() - RADIUS_OF_REGION_OF_INTEREST > 0 ? corner.getY() - RADIUS_OF_REGION_OF_INTEREST : 0;
        int w = x + 2 * RADIUS_OF_REGION_OF_INTEREST < image.cols() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.cols() - x;
        int h = y + 2 * RADIUS_OF_REGION_OF_INTEREST < image.rows() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.rows() - y;

        System.out.println("ROI: (x = " + x + ", y = " + y + ", w = " + w + ", h = " + h + ")");
        Rect regionOfInterest = new Rect(x, y, w, h);
        return new Mat(image, regionOfInterest);
    }

//    private void filterCandidateCornersWhichFallOutsideROI(List<Corner> candidateCorners) {
//        Iterator it = candidateCorners.iterator();
//        while (it.hasNext()) {
//            Corner candidateCorner = (Corner)it.next();
//            if (candidateCorner.getX())
//        }
//    }

    private Corner getCandidateNearestToCenterOfRegionOfInterest(List<Corner> corners) {
        Ponto center = new Ponto(RADIUS_OF_REGION_OF_INTEREST, RADIUS_OF_REGION_OF_INTEREST);
        Corner neasrestCorner = null;
        double minimumDistance = 999999999;
        for (Corner corner : corners) {
            if (corner == null) continue;
            if (corner.distanceTo(center) < minimumDistance) {
                minimumDistance = corner.distanceTo(center);
                neasrestCorner = corner;
            }
        }
        return neasrestCorner;
    }

}
