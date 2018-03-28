package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um agrupamento de vértices que estão próximos uns dos oturos.
 */
public class ClusterDeVertices {

    public List<Point> vertices;

    public ClusterDeVertices() {
        vertices = new ArrayList<Point>();
    }

    public void combinarPontoSeEstaProximoOSuficiente(Point p, double distanciaMaxima) {
        if (vertices.isEmpty()) {
            vertices.add(p);
        }
        else if (this.distanceTo(p) < distanciaMaxima) {
            vertices.add(p);
        }
    }

    public Point verticeMedio() {
        Point p = new Point();
        p.x = 0;
        p.y = 0;
        if (vertices.isEmpty()) {
            return p;
        }
        for (Point pp : vertices) {
            p.x += pp.x;
            p.y += pp.y;
        }
        p.x /= vertices.size();
        p.y /= vertices.size();
        return p;
    }

    public double distanceTo(Point p) {
        Point verticeMedio = verticeMedio();
        return Math.sqrt(
            (p.x - verticeMedio.x) * (p.x - verticeMedio.x) +
            (p.y - verticeMedio.y) * (p.y - verticeMedio.y)
        );
    }

}
