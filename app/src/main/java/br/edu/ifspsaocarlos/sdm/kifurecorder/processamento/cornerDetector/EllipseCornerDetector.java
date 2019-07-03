package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.ellipseDetector.EllipseDetectorInterface;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.ellipseDetector.FirstEllipseDetector;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.ellipseDetector.SecondEllipseDetector;

public class EllipseCornerDetector implements CornerDetectorInterface {

    private int imageIndex;
    private int cornerIndex;

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }

    public void setCornerIndex(int cornerIndex) {
        this.cornerIndex = cornerIndex;
    }

    public List<Corner> detectCandidateCornersIn(Mat image) {
        List<EllipseDetectorInterface> ellipseDetectors = new ArrayList<>();
        EllipseDetectorInterface firstEllipseDetector = new FirstEllipseDetector();
        EllipseDetectorInterface secondEllipseDetector = new SecondEllipseDetector();
        String prefix = "processing/corner" + cornerIndex + "_frame" + imageIndex;
        firstEllipseDetector.setFilePrefix(prefix + "_first-filter");
        secondEllipseDetector.setFilePrefix(prefix + "_second-filter");
        ellipseDetectors.add(firstEllipseDetector);
        ellipseDetectors.add(secondEllipseDetector);

        List<Corner> candidateCorners = new ArrayList<>();

        for (EllipseDetectorInterface ellipseDetector : ellipseDetectors) {
            List<RotatedRect> ellipses = ellipseDetector.detectEllipsesIn(image);
            for (RotatedRect ellipse : ellipses) {
                Corner corner = new Corner((int)ellipse.center.x, (int)ellipse.center.y, true);
                corner.stonePosition = ellipse;
                candidateCorners.add(corner);
            }
        }

        return mergeEllipsesWithCloseCenters(candidateCorners);
    }

    private List<Corner> mergeEllipsesWithCloseCenters(List<Corner> candidateCorners) {
        List<Corner> mergedCandidateCorners = new ArrayList<Corner>();
        Set<Integer> mergedCornerIndexes = new HashSet<>();
        int DISTANCE_THRESHOLD = 50;

        for (int i = 0; i < candidateCorners.size(); i++) {
            Corner corner1 = candidateCorners.get(i);

            for (int j = i + 1; j < candidateCorners.size(); j++) {
                if (mergedCornerIndexes.contains(j)) continue;

                Corner corner2 = candidateCorners.get(j);
                if (corner1.distanceTo(corner2) < DISTANCE_THRESHOLD) {
                    mergedCandidateCorners.add(corner1.mergeWith(corner2));
                    mergedCornerIndexes.add(i);
                    mergedCornerIndexes.add(j);
                }
            }

            if (!mergedCornerIndexes.contains(i)) {
                mergedCandidateCorners.add(corner1);
            }
        }

        return mergedCandidateCorners;
    }

}