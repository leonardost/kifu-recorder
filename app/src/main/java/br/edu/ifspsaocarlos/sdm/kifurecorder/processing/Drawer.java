package br.edu.ifspsaocarlos.sdm.kifurecorder.processing;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Board;
import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Move;

/**
 * Responsible for drawing images.
 */
public class Drawer {

    private static Scalar mBlack = new Scalar(  0,   0,   0);
    private static Scalar mWhite = new Scalar(255, 255, 255);
    private static Scalar mRed   = new Scalar(255,   0,   0);
    private static Scalar mGreen = new Scalar(  0, 255,   0);
    private static Scalar mBlue  = new Scalar(  0,   0, 255);
    private static Scalar mBoardBrown = new Scalar(219, 176, 105);

    private static Scalar[] colors = new Scalar[] {
        mRed, mGreen, mBlue, mWhite
    };

    public static void drawRelevantContours(Mat image, QuadrilateralHierarchy quadrilateralHierarchy, MatOfPoint contourClosestToTheBoard) {
        // Draw the leaf quadrilaterals in green
        if (quadrilateralHierarchy.leaves.size() > 0) {
            Imgproc.drawContours(image, quadrilateralHierarchy.leaves, -1, mGreen, 2);
        }

        // Draw the external quadrilaterals in blue
        if (quadrilateralHierarchy.externals.size() > 0) {
            Imgproc.drawContours(image, quadrilateralHierarchy.externals, -1, mBlue, 2);
        }

        // Draw the board contour in red
        if (contourClosestToTheBoard != null) {
            drawContour(image, contourClosestToTheBoard, mRed);
        }
    }

    private static void drawContour(Mat image, MatOfPoint contour, Scalar color) {
        List<MatOfPoint> contourList = new ArrayList<MatOfPoint>();
        contourList.add(contour);
        Imgproc.drawContours(image, contourList, -1, color, 2);
    }

    public static void drawBoardContour(Mat image, MatOfPoint boardContour) {
        List<MatOfPoint> contourList = new ArrayList<MatOfPoint>();
        contourList.add(boardContour);
        Imgproc.drawContours(image, contourList, -1, mRed, 6);
    }

    public static void drawLostBoardContour(Mat image, MatOfPoint boardContour) {
        List<MatOfPoint> contour = new ArrayList<MatOfPoint>();
        contour.add(boardContour);
        Imgproc.drawContours(image, contour, -1, mBlue, 6);
    }

    /**
     * Draws the board over matrix 'image' with origin on coordinates 'x' and 'y' passed as
     * parameters and with size 'imageSize'. The drawing is done respecting the dimension of the
     * board, that is, if the board has a smaller dimension, the preview is smaller. The last move
     * is marked in blue. If the 'lastMove' parameter is null, the last move is not marked.
     *
     * @param image
     * @param board
     * @param x
     * @param y
     * @param imageSize
     * @param lastMove
     */
    public static void drawBoard(Mat image, Board board, int x, int y, int imageSize, Move lastMove) {
        Point p1 = new Point();
        Point p2 = new Point();
        double distanceBetweenLines = imageSize / (board.getDimension() + 1);
        double endOfLines = imageSize - distanceBetweenLines;
        int stoneRadius = 29 - board.getDimension(); // was using imageSize / 20 for 9x9 board
        p1.x = x;
        p1.y = y;
        p2.x = x + imageSize;
        p2.y = y + imageSize;

        Imgproc.rectangle(image, p1, p2, mBoardBrown, -1);

        // Draw horizontal lines
        for (int i = 0; i < board.getDimension(); ++i) {
            Point start = new Point();
            Point end = new Point();
            start.x = x + distanceBetweenLines;
            start.y = y + distanceBetweenLines + distanceBetweenLines * i;
            end.x = x + endOfLines;
            end.y = start.y;
            Imgproc.line(image, start, end, mBlack);
        }

        // Draw vertical lines
        for (int i = 0; i < board.getDimension(); ++i) {
            Point start = new Point();
            Point end = new Point();
            start.x = x + distanceBetweenLines + distanceBetweenLines * i;
            start.y = y + distanceBetweenLines;
            end.x = start.x;
            end.y = y + endOfLines;
            Imgproc.line(image, start, end, mBlack);
        }

        // Draw stones
        for (int i = 0; i < board.getDimension(); ++i) {
            for (int j = 0; j < board.getDimension(); ++j) {
                Point center = new Point();
                center.x = x + distanceBetweenLines + j * distanceBetweenLines;
                center.y = y + distanceBetweenLines + i * distanceBetweenLines;
                if (board.getPosition(i, j) == Board.BLACK_STONE) {
                    Imgproc.circle(image, center, stoneRadius, mBlack, -1);
                } else if (board.getPosition(i, j) == Board.WHITE_STONE) {
                    Imgproc.circle(image, center, stoneRadius, mWhite, -1);
                    Imgproc.circle(image, center, stoneRadius, mBlack);
                }
            }
        }

        // Mark the last move
        if (lastMove != null) {
            Point center = new Point();
            center.x = x + distanceBetweenLines + lastMove.column * distanceBetweenLines;
            center.y = y + distanceBetweenLines + lastMove.row * distanceBetweenLines;
            Scalar markColor = lastMove.color == Board.BLACK_STONE ? mWhite : mBlack;
            Imgproc.circle(image, center, (int)(stoneRadius * 0.6), markColor, 1);
            Imgproc.circle(image, center, stoneRadius, mBlue, -1);
        }
    }

}
