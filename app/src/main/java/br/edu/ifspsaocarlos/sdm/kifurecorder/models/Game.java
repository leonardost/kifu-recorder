package br.edu.ifspsaocarlos.sdm.kifurecorder.models;

import android.util.Log;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.BuildConfig;

/**
 * Represents a complete game, with the sequence of boards and moves that were made.
 */
public class Game implements Serializable {

    private String blackPlayer;
    private String whitePlayer;
    private String komi;
    private int boardDimension;
    private List<Move> moves;
    private List<Board> boards;

    // Attribute to measure the system's precision
    private int numberOfUndoes;
    private int numberOfManualAdditions;

    public Game(int boardDimension, String blackPlayer, String whitePlayer, String komi) {
        this.boardDimension = boardDimension;
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
        this.komi = komi;
        moves = new ArrayList<>();
        boards = new ArrayList<>();

        Board emptyBoard = new Board(boardDimension);
        boards.add(emptyBoard);
        numberOfUndoes = 0;
        numberOfManualAdditions = 0;
    }

    public int getBoardDimension() {
        return boardDimension;
    }

    public String getBlackPlayer() {
        return blackPlayer;
    }

    public String getWhitePlayer() {
        return whitePlayer;
    }

    public boolean addMoveIfItIsValid(Board board) {
        Move playedMove = board.getDifferenceTo(getLastBoard());

        if (playedMove == null || repeatsPreviousState(board) || !canNextMoveBe(playedMove.color)) {
            return false;
        }

        boards.add(board);
        moves.add(playedMove);
        Log.i("KifuRecorder", "Adding board " + board + " (move " + playedMove.sgf() + ") to the game.");
        return true;
    }

    /**
     * Returns true if newBoard repeats any previous boards of the game (superko rule).
     */
    private boolean repeatsPreviousState(Board newBoard) {
        for (Board board : boards) {
            if (board.equals(newBoard)) return true;
        }
        return false;
    }

    public boolean canNextMoveBe(int color) {
        if (color == Board.BLACK_STONE)
            return isFirstMove() || wereOnlyBlackStonesPlayed() || wasLastMoveWhite();
        else if (color == Board.WHITE_STONE)
            return wasLastMoveBlack();
        return false;
    }

    private boolean isFirstMove() {
        return moves.isEmpty();
    }

    /**
     * This method is used to check if handicap stones are being placed.
     */
    private boolean wereOnlyBlackStonesPlayed() {
        for (Move move : moves) {
            if (move.color == Board.WHITE_STONE) return false;
        }
        return true;
    }

    private boolean wasLastMoveWhite() {
        return !isFirstMove() && getLastMove().color == Board.WHITE_STONE;
    }

    private boolean wasLastMoveBlack() {
        return !isFirstMove() && getLastMove().color == Board.BLACK_STONE;
    }

    public Move getLastMove() {
        if (moves.isEmpty()) return null;
        return moves.get(moves.size() - 1);
    }

    public Board getLastBoard() {
        return boards.get(boards.size() - 1);
    }

    /**
     * Disconsider the last move and return it.
     */
    public Move undoLastMove() {
        if (moves.isEmpty()) return null;
        boards.remove(boards.size() - 1);
        Move lastMove = moves.remove(moves.size() - 1);
        numberOfUndoes++;
        return lastMove;
    }

    public int getNumberOfMoves() {
        return moves.size();
    }

    public void updateNumberOfManualAdditions() {
        numberOfManualAdditions++;
    }

    /**
     * Rotates all boards of this game clockwise (direction = 1) or counter-clockwise
     * (direction = -1).
     */
    public void rotate(int direction) {
        if (direction != -1 && direction != 1) return;

        List<Board> rotatedBoards = new ArrayList<>();
        for (Board board : boards) {
            rotatedBoards.add(board.rotate(direction));
        }

        List<Move> rotatedMoves = new ArrayList<>();
        for (int i = 1; i < rotatedBoards.size(); ++i) {
            Board last = rotatedBoards.get(i);
            Board secondToLast = rotatedBoards.get(i - 1);
            rotatedMoves.add(last.getDifferenceTo(secondToLast));
        }

        boards = rotatedBoards;
        moves = rotatedMoves;
    }

    // SGF methods should be extracted to a SgfBuilder class that receives a Game as parameter
    /**
     * Exports the game in SGF format.
     * Reference: http://www.red-bean.com/sgf/
     */
    public String sgf() {
        StringBuilder sgf = new StringBuilder();
        writeHeader(sgf);
        for (Move move : moves) {
            Log.i("KifuRecorder", "Building SGF - move " + move.sgf());
            sgf.append(move.sgf());
        }
        sgf.append(")");
        return sgf.toString();
    }

    private void writeHeader(StringBuilder sgf) {
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        String date = sdf.format(new Date(c.getTimeInMillis()));

        sgf.append("(;");
        writeProperty(sgf, "FF", "4");     // SGF version
        writeProperty(sgf, "GM", "1");     // Type of game (1 = Go)
        writeProperty(sgf, "CA", "UTF-8");
        writeProperty(sgf, "SZ", "" + getLastBoard().getDimension());
        writeProperty(sgf, "DT", date);
        writeProperty(sgf, "AP", "Kifu Recorder v" + BuildConfig.VERSION_NAME);
        writeProperty(sgf, "KM", komi);
        writeProperty(sgf, "PW", whitePlayer);
        writeProperty(sgf, "PB", blackPlayer);
        writeProperty(sgf, "Z1", "" + getNumberOfMoves());
        writeProperty(sgf, "Z2", "" + numberOfUndoes);
        writeProperty(sgf, "Z3", "" + numberOfManualAdditions);
    }

    private void writeProperty(StringBuilder sgf, String property, String value) {
        sgf.append(property);
        sgf.append("[");
        sgf.append(value);
        sgf.append("]");
    }

}
