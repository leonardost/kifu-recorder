package br.edu.ifspsaocarlos.sdm.kifurecorder.processing;

import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Defines a quadrilateral spatial hierarchy, indicating which ones are inside which ones.
 */
public class QuadrilateralHierarchy {

    public Map<MatOfPoint, List<MatOfPoint>> hierarchy = new HashMap<>();
    // quadrilaterals that do not have children
    public List<MatOfPoint> leaves = new ArrayList<>();
    // quadrilaterals that have one or more children
    public List<MatOfPoint> externals = new ArrayList<>();

    /**
     * Builds the hierarchy of the provided quadrilaterals.
     * @param quadrilaterals
     */
    public QuadrilateralHierarchy(List<MatOfPoint> quadrilaterals) {

        for (MatOfPoint quadrilateral : quadrilaterals) {
            hierarchy.put(quadrilateral, new ArrayList<MatOfPoint>());
            for (MatOfPoint otherQuadrilateral : quadrilaterals) {
                if (quadrilateral == otherQuadrilateral) continue;
                if (isInside(quadrilateral, otherQuadrilateral)) {
                    hierarchy.get(quadrilateral).add(otherQuadrilateral);
                }
            }
        }

        // Separates the "tree" of quadrilaterals in leaf quadrilaterals and external quadrilaterals
        Iterator it = hierarchy.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            List<MatOfPoint> valor = (List<MatOfPoint>)pair.getValue();
            // Log.d("ASDF", "Quadrilateral has " + valor.size() + " internal quadrilaterals.");
            if (valor.size() == 0) {
                this.leaves.add((MatOfPoint) pair.getKey());
            }
            else {
                this.externals.add((MatOfPoint) pair.getKey());
            }
        }

    }

    /**
     * Checks if a quadrilateral is inside another
     *
     * @param externalQuadrilateral
     * @param internalQuadrilateral
     * @return
     */
    private boolean isInside(MatOfPoint externalQuadrilateral, MatOfPoint internalQuadrilateral) {
        final double IS_INSIDE_CONTOUR = 1;
        double result;
        MatOfPoint2f externalQuadrilateral2f = new MatOfPoint2f();
        externalQuadrilateral.convertTo(externalQuadrilateral2f, CvType.CV_32FC2);
        for (Point point : internalQuadrilateral.toList()) {
            result = Imgproc.pointPolygonTest(externalQuadrilateral2f, point, false);
            if (result != IS_INSIDE_CONTOUR) {
                return false;
            }
        }
        return true;
    }

}
