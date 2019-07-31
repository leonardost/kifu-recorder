package br.edu.ifspsaocarlos.sdm.kifurecorder.models;

public class Position {
    public int row;
    public int column;

    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }

    @Override
    public String toString() {
        int l = 'a' + add1ToJumpLetterI(row);
        int c = 'a' + add1ToJumpLetterI(column);
        return "[" + (char)l + (char)c + "]";
    }

    private char add1ToJumpLetterI(int index) {
        final int I_INDEX = 8;
        return (char)(index + (index >= I_INDEX ? 1 : 0));
    }

    @Override
    public boolean equals(Object position) {
        if (!(position instanceof Position)) return false;
        return row == ((Position)position).row
            && column == ((Position)position).column;
    }

    @Override
    public int hashCode() {
        return row * 39 + column;
    }
}
