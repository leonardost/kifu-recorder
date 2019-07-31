package br.edu.ifspsaocarlos.sdm.kifurecorder.processing;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.cornerDetector.Corner;

public class ImageUtils {

    public static final int ORTHOGONAL_BOARD_IMAGE_SIZE = 500;

    public static Mat generateOrthogonalBoardImage(Mat image, Corner[] corners) {
        Mat orthogonalBoardImage = new Mat(ORTHOGONAL_BOARD_IMAGE_SIZE, ORTHOGONAL_BOARD_IMAGE_SIZE, image.type());

        Mat orthogonalImageCorners = new Mat(4, 1, CvType.CV_32FC2);
        orthogonalImageCorners.put(0, 0,
                0, 0,
                ORTHOGONAL_BOARD_IMAGE_SIZE, 0,
                ORTHOGONAL_BOARD_IMAGE_SIZE, ORTHOGONAL_BOARD_IMAGE_SIZE,
                0, ORTHOGONAL_BOARD_IMAGE_SIZE);

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

        Mat transformationMatrix = Imgproc.getPerspectiveTransform(boardPositionInImage, orthogonalImageCorners);
        Imgproc.warpPerspective(image, orthogonalBoardImage, transformationMatrix, orthogonalBoardImage.size());
        return orthogonalBoardImage;
    }

    public static Mat transformOrthogonally(Mat originalImage, Mat boardPositionInImage) {
        Mat orthogonalBoard = new Mat(ORTHOGONAL_BOARD_IMAGE_SIZE, ORTHOGONAL_BOARD_IMAGE_SIZE, originalImage.type());

        Mat orthogonalBoardCorners = new Mat(4, 1, CvType.CV_32FC2);
        orthogonalBoardCorners.put(0, 0,
                0, 0,
                ORTHOGONAL_BOARD_IMAGE_SIZE, 0,
                ORTHOGONAL_BOARD_IMAGE_SIZE, ORTHOGONAL_BOARD_IMAGE_SIZE,
                0, ORTHOGONAL_BOARD_IMAGE_SIZE);

        Mat transformationMatrix = Imgproc.getPerspectiveTransform(boardPositionInImage, orthogonalBoardCorners);
        Imgproc.warpPerspective(originalImage, orthogonalBoard, transformationMatrix, orthogonalBoard.size());

        return orthogonalBoard;
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
