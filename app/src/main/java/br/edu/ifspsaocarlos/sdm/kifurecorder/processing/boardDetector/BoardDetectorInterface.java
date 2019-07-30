package br.edu.ifspsaocarlos.sdm.kifurecorder.processing.boardDetector;

import org.opencv.core.Mat;

public interface BoardDetectorInterface {

    public static final int STATE_BOARD_IS_INSIDE = 1;
    public static final int STATE_LOOKING_FOR_BOARD = 2;

    // This is the only method that should be public
    public boolean isBoardContainedIn(Mat ortogonalBoardImage);

    public void setState(int state);
    public void setImageIndex(int imageIndex);

}
