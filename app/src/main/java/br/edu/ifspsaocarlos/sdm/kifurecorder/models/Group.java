package br.edu.ifspsaocarlos.sdm.kifurecorder.models;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a group of stones on the board.
 */
public class Group {

    private int color;
    private Set<Position> positions;
    private Set<Position> liberties;

    public Group(int color) {
        this.color = color;
        positions = new HashSet<>();
        liberties = new HashSet<>();
    }

    public int getColor() {
        return color;
    }

    public Set<Position> getPositions() {
        return positions;
    }

    public void addPosition(Position position) {
        positions.add(position);
    }

    public void addLiberty(Position liberdade) {
        liberties.add(liberdade);
    }

    public boolean isInAtari() {
        return liberties.size() == 1;
    }

    public boolean isCapturedBy(Move move) {
        return move.cor != color && isInAtari() && liberties.contains(move.posicao());
    }

    public boolean hasNoLiberties() {
        return liberties.size() == 0;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Group)) return false;

        Group otherGroup = (Group)other;
        return color == otherGroup.color
            && positions.equals(otherGroup.positions)
            && liberties.equals(otherGroup.liberties);
    }

    @Override
    public int hashCode() {
        return color + positions.hashCode() + liberties.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        string.append("Group of color " + color + ": ");
        for (Position position : positions) {
            string.append(position);
        }
        return string.toString();
    }

}
