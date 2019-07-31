package br.edu.ifspsaocarlos.sdm.kifurecorder.models;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a board state
 */
public class Board implements Serializable {

    public final static int EMPTY = 0;
    public final static int BLACK_STONE = 1;
    public final static int WHITE_STONE = 2;

    private int dimension;
    private Integer[][] board;

    public Board(int dimension) {
        this.dimension = dimension;
        this.board = new Integer[dimension][dimension];
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                board[i][j] = EMPTY;
            }
        }
    }

    public Board(Board board) {
        this.dimension = board.dimension;
        this.board = new Integer[dimension][dimension];
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                this.board[i][j] = board.board[i][j];
            }
        }
    }

    public int getDimension() {
        return dimension;
    }

    public void putStone(int linha, int coluna, int pedra) {
        if (pedra != BLACK_STONE && pedra != WHITE_STONE) {
            throw new RuntimeException("Invalid stone!");
        }
        if (isAValidPosition(linha, coluna)) {
            throw new RuntimeException("Invalid position!");
        }
        if (board[linha][coluna] != EMPTY) {
			throw new RuntimeException("There is already a stone in that position!");
		}
        board[linha][coluna] = pedra;
    }

    private boolean isAValidPosition(int row, int column) {
        return row < 0 || column < 0 || row >= dimension || column >= dimension;
    }

    public int getPosition(int row, int column) {
        return board[row][column];
    }

    public String toString() {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                if (board[i][j] == EMPTY) output.append('.');
                else if (board[i][j] == BLACK_STONE) output.append('P');
                else if (board[i][j] == WHITE_STONE) output.append('B');
            }
            output.append(System.getProperty("line.separator"));
        }

        return output.toString();
    }

    /**
     * Returns a new board that corresponds to the current board rotated clockwise (direction = 1)
     * or counter-clockwise (direction = -1)
     */
    public Board rotate(int direction) {
        if (direction == -1) return rotateCounterClockwise();
        else if (direction == 1) return rotateClockwise();
        throw new RuntimeException("Invalid rotation direction!");
    }

    private Board rotateClockwise() {
        Board rotatedBoard = new Board(dimension);
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                if (board[dimension - 1 - j][i] != Board.EMPTY) {
                    rotatedBoard.putStone(i, j, board[dimension - 1 - j][i]);
                }
            }
        }
        return rotatedBoard;
    }

    private Board rotateCounterClockwise() {
        Board rotatedBoard = new Board(dimension);
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                if (board[j][dimension - 1 - i] != Board.EMPTY) {
                    rotatedBoard.putStone(i, j, board[j][dimension - 1 - i]);
                }
            }
        }
        return rotatedBoard;
    }

    public boolean isIdenticalTo(Board otherBoard) {
        if (dimension != otherBoard.dimension) return false;
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                if (getPosition(i, j) != otherBoard.getPosition(i, j)) return false;
            }
        }
        return true;
    }

    /**
     * Checks if two board are equal, including if one is a rotation of the other.
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Board)) return false;
        Board otherBoard = (Board)object;
        if (dimension != otherBoard.dimension) return false;

        Board rotation1 = otherBoard.rotateClockwise();
        Board rotation2 = rotation1.rotateClockwise();
        Board rotation3 = rotation2.rotateClockwise();

        return this.isIdenticalTo(otherBoard) || this.isIdenticalTo(rotation1) || this.isIdenticalTo(rotation2) || this.isIdenticalTo(rotation3);
    }

    /**
     * Returns the different move between this board and the previous one. If it's not possible to
     * reach the current board from the previous one, returns null.
     */
    public Move getDifferenceTo(Board previousBoard) {
        int numberOfBlackStonesBefore = previousBoard.getNumberOfStonesOfColor(Board.BLACK_STONE);
        int numberOfWhiteStonesBefore = previousBoard.getNumberOfStonesOfColor(Board.WHITE_STONE);
        int numberOfBlackStonesAfter = getNumberOfStonesOfColor(Board.BLACK_STONE);
        int numberOfWhiteStonesAfter = getNumberOfStonesOfColor(Board.WHITE_STONE);
        int blackStonesDifference = numberOfBlackStonesAfter - numberOfBlackStonesBefore;
        int whiteStonesDifference = numberOfWhiteStonesAfter - numberOfWhiteStonesBefore;
        int moveColor;

        if (blackStonesDifference == 1) moveColor = Board.BLACK_STONE;
        else if (whiteStonesDifference == 1) moveColor = Board.WHITE_STONE;
        else return null;
        Move movePlayed = getDifferentMoveBetweenCurrentBoardAnd(previousBoard, moveColor);

        if (previousBoard.generateNewBoardWith(movePlayed).isIdenticalTo(this)) {
            return movePlayed;
        }
        return null;
    }

    private int getNumberOfStonesOfColor(int cor) {
        int numberOfStones = 0;
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                if (board[i][j] == cor) ++numberOfStones;
            }
        }
        return numberOfStones;
    }

    /**
     * Returns the first different stone found between the two boards of the specified color.
     */
    private Move getDifferentMoveBetweenCurrentBoardAnd(Board previousBoard, int color) {
        for (int i = 0; i < previousBoard.getDimension(); ++i) {
            for (int j = 0; j < previousBoard.getDimension(); ++j) {
                if (board[i][j] == color && previousBoard.getPosition(i, j) != board[i][j]) {
                    return new Move(i, j, color);
                }
            }
        }
        return null;
    }

    /**
     * Returns a new board that corresponds to the current board added with the move passed as
     * parameter. If the move is not valid, returns the current board.
     */
	public Board generateNewBoardWith(Move move) {
        if (move == null || board[move.row][move.column] != EMPTY) return this;

        Board newBoard = new Board(this);

        for (Group group : getGroupsAdjacentTo(move)) {
            if (group == null) continue;
            if (group.isCapturedBy(move)) newBoard.remove(group);
        }

        newBoard.board[move.row][move.column] = move.color;

        Group groupOfMove = newBoard.getGroupAt(move.row, move.column);
        if (groupOfMove.hasNoLiberties()) return this;

        return newBoard;
	}

    private Set<Group> getGroupsAdjacentTo(Move move) {
        Set<Group> groupsAdjacentToMove = new HashSet<>();
        groupsAdjacentToMove.add(getGroupAt(move.row - 1, move.column));
        groupsAdjacentToMove.add(getGroupAt(move.row + 1, move.column));
        groupsAdjacentToMove.add(getGroupAt(move.row, move.column - 1));
        groupsAdjacentToMove.add(getGroupAt(move.row, move.column + 1));
        return groupsAdjacentToMove;
    }

    private void remove(Group group) {
        for (Position position : group.getPositions()) {
            board[position.linha][position.coluna] = EMPTY;
        }
    }

    /**
     * Returns the group that is at a certain position on the board or null if there is no group.
     */
    public Group getGroupAt(int row, int column) {
        if (isAValidPosition(row, column) || board[row][column] == Board.EMPTY) return null;

        boolean[][] visitedPositions = new boolean[dimension][dimension];
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                visitedPositions[i][j] = false;
            }
        }

        int color = board[row][column];
        Group group = new Group(color);
        delimitGroup(row, column, visitedPositions, group);
        return group;
    }

    /**
     * Does a depth-first search to find all stones that are part of this group and their liberties.
     */
    private void delimitGroup(int row, int column, boolean[][] visitedPositions, Group group) {
        if (isAValidPosition(row, column) || visitedPositions[row][column]) return;

        visitedPositions[row][column] = true;

        if (board[row][column] == Board.EMPTY) {
            group.addLiberty(new Position(row, column));
        }
        else if (board[row][column] == group.getColor()) {
            group.addPosition(new Position(row, column));

            delimitGroup(row - 1, column, visitedPositions, group);
            delimitGroup(row + 1, column, visitedPositions, group);
            delimitGroup(row, column - 1, visitedPositions, group);
            delimitGroup(row, column + 1, visitedPositions, group);
        }
    }

}
