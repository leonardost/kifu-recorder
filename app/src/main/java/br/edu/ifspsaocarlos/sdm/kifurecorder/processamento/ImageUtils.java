package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
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

        Mat boardPositionInImage = new Mat(4, 1, CvType.CV_32FC2);
        boardPositionInImage.put(0, 0,
                corners[0].getX(), corners[0].getY(),
                corners[1].getX(), corners[1].getY(),
                corners[2].getX(), corners[2].getY(),
                corners[3].getX(), corners[3].getY());

        Mat transformationMatrix = Imgproc.getPerspectiveTransform(boardPositionInImage, ortogonalImageCorners);
        Imgproc.warpPerspective(image, ortogonalBoardImage, transformationMatrix, ortogonalBoardImage.size());
        return ortogonalBoardImage;
    }

}
