package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.Corner;

public class ImageUtils {

    public static final int ORTOGONAL_BOARD_IMAGE_SIZE = 500;

    public static Mat generateOrtogonalBoardImage(Mat image, Corner[] corners) {
        Mat ortogonalBoardImage = new Mat(ORTOGONAL_BOARD_IMAGE_SIZE, ORTOGONAL_BOARD_IMAGE_SIZE, image.type());

        Mat ortogonalImageCorners = new Mat(4, 1, CvType.CV_32FC2);
        ortogonalImageCorners.put(0, 0,
                0, 0,
                ORTOGONAL_BOARD_IMAGE_SIZE, 0,
                ORTOGONAL_BOARD_IMAGE_SIZE, ORTOGONAL_BOARD_IMAGE_SIZE,
                0, ORTOGONAL_BOARD_IMAGE_SIZE);

        Point[] realCornerPositions = new Point[4];
        for (int i = 0; i < 4; i++) {
            Ponto realCornerPosition = corners[i].getRealCornerPosition();
            realCornerPositions[i] = new Point(realCornerPosition.x, realCornerPosition.y);
        }

        Mat boardPositionInImage = new Mat(4, 1, CvType.CV_32FC2);
        boardPositionInImage.put(0, 0,
                realCornerPositions[0].x, realCornerPositions[0].y,
                realCornerPositions[1].x, realCornerPositions[1].y,
                realCornerPositions[2].x, realCornerPositions[2].y,
                realCornerPositions[3].x, realCornerPositions[3].y);

        Mat transformationMatrix = Imgproc.getPerspectiveTransform(boardPositionInImage, ortogonalImageCorners);
        Imgproc.warpPerspective(image, ortogonalBoardImage, transformationMatrix, ortogonalBoardImage.size());
        return ortogonalBoardImage;
    }

}
