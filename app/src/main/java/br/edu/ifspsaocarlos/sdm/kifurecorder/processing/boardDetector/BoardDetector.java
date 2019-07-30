package br.edu.ifspsaocarlos.sdm.kifurecorder.processing.boardDetector;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class BoardDetector implements BoardDetectorInterface {

    private int imageIndex;
    private int state;
    private List<BoardDetectorInterface> boardDetectors;

    public BoardDetector() {
        boardDetectors = new ArrayList<>();
        boardDetectors.add(new BoardDetectorByQuadrilateralCounting());
        setState(STATE_BOARD_IS_INSIDE);
    }

    public boolean isBoardContainedIn(Mat ortogonalBoardImage) {
        if (isBoardInsideAccordingToEnsenble(ortogonalBoardImage)) {
            setState(STATE_BOARD_IS_INSIDE);
        } else {
            setState(STATE_LOOKING_FOR_BOARD);
        }
        return state == STATE_BOARD_IS_INSIDE;
    }

    private boolean isBoardInsideAccordingToEnsenble(Mat ortogonalBoardImage) {
        boolean consensus = true;
        for (BoardDetectorInterface boardDetector : boardDetectors) {
            if (!boardDetector.isBoardContainedIn(ortogonalBoardImage)) {
                consensus = false;
            }
        }
        // A full consensus is a very simple way of combining different classifiers.
        // In this case, quadrilateral counting should have more weight than the
        // image similarity verifier because it produces better results overall.
        return consensus;
    }

    public void setState(int state) {
        this.state = state;
        for (BoardDetectorInterface boardDetector : boardDetectors) {
            boardDetector.setState(state);
        }
    }

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
        for (BoardDetectorInterface boardDetector : boardDetectors) {
            boardDetector.setImageIndex(imageIndex);
        }
    }

}
