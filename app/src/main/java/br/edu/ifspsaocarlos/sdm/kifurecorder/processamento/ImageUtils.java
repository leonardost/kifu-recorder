package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
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

    public static Mat transformarOrtogonalmente(Mat originalImage, Mat boardPositionInImage) {
        Mat ortogonalBoard = new Mat(ORTOGONAL_BOARD_IMAGE_SIZE, ORTOGONAL_BOARD_IMAGE_SIZE, originalImage.type());

        Mat ortogonalBoardCorners = new Mat(4, 1, CvType.CV_32FC2);
        ortogonalBoardCorners.put(0, 0,
                0, 0,
                ORTOGONAL_BOARD_IMAGE_SIZE, 0,
                ORTOGONAL_BOARD_IMAGE_SIZE, ORTOGONAL_BOARD_IMAGE_SIZE,
                0, ORTOGONAL_BOARD_IMAGE_SIZE);

        Mat transformationMatrix = Imgproc.getPerspectiveTransform(boardPositionInImage, ortogonalBoardCorners);
        Imgproc.warpPerspective(originalImage, ortogonalBoard, transformationMatrix, ortogonalBoard.size());

        return ortogonalBoard;
    }

    // direction = -1 counter-clockwise, 1 clockwise
    public static Mat rotateImage(Mat image, int direction) {
        Point center = new Point(image.cols() / 2, image.rows() / 2);
        // Positive values mean counter-clockwise rotation
        direction *= -1;
        Mat transformationMatrix = Imgproc.getRotationMatrix2D(center, 90 * direction, 1);
        Mat rotatedImage = new Mat();
        Imgproc.warpAffine(image, rotatedImage, transformationMatrix, new Size(image.cols(), image.rows()));
        return rotatedImage;
    }

}
