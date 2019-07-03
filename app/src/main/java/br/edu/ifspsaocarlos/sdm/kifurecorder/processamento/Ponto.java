package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

public class Ponto {
    public int x;
    public int y;

    public Ponto() {}
    public Ponto(int x, int y) { this.x = x; this.y = y; }

    public void set(Ponto point) {
        x = point.x;
        y = point.y;
    }

    public double distanceTo(Ponto point) {
        return (y - point.y) * (y - point.y) + (x - point.x) * (x - point.x);
    }

    public int manhattanDistanceTo(Ponto point) {
        return Math.abs(y - point.y) + Math.abs(x - point.x);
    }

    public Ponto add(Ponto point) {
        Ponto newPoint = new Ponto(x, y);
        newPoint.x += point.x;
        newPoint.y += point.y;
        if (newPoint.x < 0) newPoint.x = 0;
        if (newPoint.y < 0) newPoint.y = 0;
        return newPoint;
    }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
