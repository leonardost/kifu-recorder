package br.edu.ifspsaocarlos.sdm.kifurecorder.processing;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.TestsActivity;

/**
 * Detects the position of a Go board in an image and its dimension (9x9, 13x13 or 19x19).
 */
public class InitialBoardDetector {

    // Camera image
    private Mat image;
    private Mat previewImage;

    // Attributes calculated by the class
    private boolean processedWithSuccess = false;
    private int boardDimension;
    private Mat positionOfBoardInImage;
    private boolean shouldDrawPreview = false;

    public InitialBoardDetector(boolean shouldDrawPreview) {
        this.shouldDrawPreview = shouldDrawPreview;
    }

    public void setImage(Mat image) {
        this.image = image;
    }

    public void setPreviewImage(Mat previewImage) {
        this.previewImage = previewImage;
    }

    /**
     * Processes the provided image. Returns true if the complete processing ran with success, i.e.,
     * if a Go board was detected in the image. Returns false otherwise.
     *
     * @return boolean
     */
    public boolean process() {
        if (image == null) {
            // throw error
            return false;
        }

        Mat imageWithBordersInEvidence = detectBorders();

        // Se quiser ver a saída do detector de bordas Cammy
        //Imgproc.cvtColor(imageWithBordersInEvidence, previewImage, Imgproc.COLOR_GRAY2BGR, 4); if (true) return false;
//        previewImage = imageWithBordersInEvidence; if (true) return false;

        List<MatOfPoint> contours = detectContours(imageWithBordersInEvidence);

        if (contours.isEmpty()) {
            Log.i(TestsActivity.TAG, "> Image processing: contours were not found.");
            return false;
        }

        // Se quiser ver a saída do detector de contours
        //Imgproc.drawContours(previewImage, contours, -1, new Scalar(0, 0, 255), 2); if (true) return false;

        List<MatOfPoint> quadrilaterals = detectQuadrilaterals(contours);

        if (quadrilaterals.isEmpty()) {
            Log.i(TestsActivity.TAG, "> Image processing: quadrilaterals were not found.");
            return false;
        }

        //Se quiser ver a saída do detector de quadriláteros
        //for (MatOfPoint quadrilatero : quadrilaterals) { List<MatOfPoint> listaContorno = new ArrayList<MatOfPoint>(); listaContorno.add(quadrilatero); Imgproc.drawContours(previewImage, listaContorno, -1, new Scalar(255, 0, 0), 3); } if (true) return false;

        MatOfPoint boardQuadrilateral = detectBoard(quadrilaterals);

        if (boardQuadrilateral == null) {
            Log.i(TestsActivity.TAG, "> Image processing: board quadrilateral was not found.");
            return false;
        }

        QuadrilateralHierarchy quadrilateralHierarchy = new QuadrilateralHierarchy(quadrilaterals);
        double averageArea = 0;
        for (MatOfPoint quadrilateral : quadrilateralHierarchy.hierarchy.get(boardQuadrilateral)) {
            averageArea += Imgproc.contourArea(quadrilateral);
        }
        averageArea /= quadrilateralHierarchy.hierarchy.get(boardQuadrilateral).size();
        double boardArea = Imgproc.contourArea(boardQuadrilateral);
        double ratio = averageArea / boardArea;
//        Log.d(TestsActivity.TAG, "Razão entre a área dos quadrados internos e a área do tabuleiro = " + ratio);

        // Determines the dimension of the board according to the ratio between the area of the
        // internal quadrilaterals and the area of the board quadrilateral
        if (ratio <= 1.0 / 324.0) {     // 18 quadrados por 18
            boardDimension = 19;
        }
        else if (ratio <= 1.0 / 144.0) {
            boardDimension = 13;   // 12 quadrados por 12
        }
        else {
            boardDimension = 9;
        }

        List<Point> boardCorners = orderCorners(boardQuadrilateral);

        if (shouldDrawPreview) {
//            Drawer.desenhaInterseccoesECantosDoTabuleiro(previewImage, intersecoes, boardCorners);
            Drawer.drawBoardContour(previewImage, boardQuadrilateral);
        }

        positionOfBoardInImage = new Mat(4, 1, CvType.CV_32FC2);
        positionOfBoardInImage.put(0, 0,
                (int) boardCorners.get(0).x, (int) boardCorners.get(0).y,
                (int) boardCorners.get(1).x, (int) boardCorners.get(1).y,
                (int) boardCorners.get(2).x, (int) boardCorners.get(2).y,
                (int) boardCorners.get(3).x, (int) boardCorners.get(3).y);

        /*
        for (int i = 0; i < positionOfBoardInImage.rows(); ++i) {
            for (int j = 0; j < positionOfBoardInImage.cols(); ++j) {
                double[] valor = positionOfBoardInImage.get(i, j);
                Log.d(TestsActivity.TAG, "(" + i + ", " + j + ") = " + valor[0] + ", " + valor[1]);
            }
        }
        */

        processedWithSuccess = true;

        return true;
    }

    private Mat detectBorders() {
        Mat intermediaryImage = new Mat();
        //Imgproc.Canny(image, mIntermediateMat, 80, 90);
        //Imgproc.Canny(image, mIntermediateMat, 35, 70);
        //Imgproc.Canny(image, mIntermediateMat, 30, 90);

        // It doesn't seem the gaussian filter helped much to lessen the image noises
//        Size size = new Size(5, 5);
//        Imgproc.GaussianBlur(image, mIntermediateMat, size, 2);

        Imgproc.Canny(image, intermediaryImage, 30, 100);
//        Imgproc.Canny(image, mIntermediateMat, 30, 100);
        //Imgproc.Canny(image, mIntermediateMat, 45, 100);
        //Imgproc.Canny(image, mIntermediateMat, 50, 100);   // Melhores resultados até agora
        //Imgproc.Canny(image, mIntermediateMat, 100, 200);    // Fica bem limpo, mas perde alguns contornos válidos
        //Imgproc.Canny(image, mIntermediateMat, 75, 150); // Ainda perde alguns contornos
        //Imgproc.Canny(image, mIntermediateMat, 65, 130);

//        Imgproc.Canny(image, intermediaryImage, 40, 110);

        Imgproc.dilate(intermediaryImage, intermediaryImage, Mat.ones(3, 3, CvType.CV_32F));
//        Imgproc.dilate(intermediaryImage, intermediaryImage, new Mat());
        return intermediaryImage;
    }

    private List<MatOfPoint> detectContours(Mat imageWithBordersInEvidence) {
        // The contours delimited by lines are found
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(imageWithBordersInEvidence, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        Log.d("kifu-recorder", "Number of contours found: " + contours.size());

        // Remove very small contours which are probably noise
        for (Iterator<MatOfPoint> it = contours.iterator(); it.hasNext();) {
            MatOfPoint contour = it.next();
            // With 1000 already loses the smaller quadrilaterals in a 19x19 board
            // The ideal would be to do this as a ratio on the area of the image
            if (Imgproc.contourArea(contour) < 700) {
                it.remove();
            }
        }

        // Image is converted to a color format again
        Imgproc.cvtColor(imageWithBordersInEvidence, image, Imgproc.COLOR_GRAY2BGR, 4);
        imageWithBordersInEvidence.release();
        return contours;
    }

    private List<MatOfPoint> detectQuadrilaterals(List<MatOfPoint> contours) {
        List<MatOfPoint> quadrilaterals = new ArrayList<>();

        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f approx2f = new MatOfPoint2f();
            contour.convertTo(contour2f, CvType.CV_32FC2);
            // * 0.02 and * 0.03 also show interesting results
            // Apparently, the bigger epsilon is, the more curves that don't fit perfectly on contours
            // are considered. However, this parameter seems very sensitive.
            //Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.04, true);
//            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.008, true);  // perde muitos quadrados
//            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.01, true);  // borda do tabuleiro encontrada bem, mas quadrados internos sofrem. Talvez seja melhor usar este por detectar melhor o quadrado externo
//            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.015, true);  // melhores resultados até agora
            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.012, true);
//            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.008, true);

            MatOfPoint approx = new MatOfPoint();
            approx2f.convertTo(approx, CvType.CV_32S);
            double contourArea = Math.abs(Imgproc.contourArea(approx2f));

            // If it has 4 sides, it's convex and not too small, it's a valid quadrilateral
            if (approx2f.toList().size() == 4 &&
                    contourArea > 400 &&
                    Imgproc.isContourConvex(approx)) {
                quadrilaterals.add(approx);
            }
        }

        Log.d("kifu-recorder", "Number of quadrilaterals found: " + quadrilaterals.size());
        return quadrilaterals;
    }

    private MatOfPoint detectBoard(List<MatOfPoint> quadrilaterals) {
        QuadrilateralHierarchy quadrilateralHierarchy = new QuadrilateralHierarchy(quadrilaterals);

        MatOfPoint contourClosestToTheBoard = null;
        int numberOfChildren = 9999;
        // Must have at least this number of leaf quadrilaterals inside
        int threshold = 10;

        for (MatOfPoint contour : quadrilateralHierarchy.externals) {
            if (quadrilateralHierarchy.hierarchy.get(contour).size() < numberOfChildren &&
                    quadrilateralHierarchy.hierarchy.get(contour).size() > threshold) {
                contourClosestToTheBoard = contour;
                numberOfChildren = quadrilateralHierarchy.hierarchy.get(contour).size();
            }
        }

//        if (shouldDrawPreview) {
//            Drawer.drawRelevantContours(previewImage, quadrilateralHierarchy, contourClosestToTheBoard);
//        }

        return contourClosestToTheBoard;
    }

    private List<Point> orderCorners(MatOfPoint boardQuadrilateral) {
        List<Point> corners = new ArrayList<>();
        corners.add(boardQuadrilateral.toArray()[0]);
        corners.add(boardQuadrilateral.toArray()[3]);
        corners.add(boardQuadrilateral.toArray()[2]);
        corners.add(boardQuadrilateral.toArray()[1]);
        return corners;
    }

    public int getBoardDimension() {
        if (!processedWithSuccess) {
            // throw error
        }
        return boardDimension;
    }

    public Mat getPositionOfBoardInImage() {
        if (!processedWithSuccess) {
            // throw error
        }
        return positionOfBoardInImage;
    }

}
