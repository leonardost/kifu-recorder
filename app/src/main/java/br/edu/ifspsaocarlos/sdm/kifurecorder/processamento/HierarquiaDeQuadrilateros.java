package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Define uma hierarquia de quadriláteros, indicando quais estão dentro de quais.
 */
public class HierarquiaDeQuadrilateros {

    public Map<MatOfPoint, List<MatOfPoint>> hierarquia = new HashMap<>();
    // quadriláteros que não têm filhos
    public List<MatOfPoint> folhas = new ArrayList<>();
    // quadriláteros que têm mais de um filho
    public List<MatOfPoint> externos = new ArrayList<>();

    /**
     * Constrói a hierarquia dos quadrados passados como parâmetro.
     * @param quadrados
     */
    public HierarquiaDeQuadrilateros(List<MatOfPoint> quadrados) {

        for (MatOfPoint quadrado : quadrados) {
            hierarquia.put(quadrado, new ArrayList<MatOfPoint>());
            for (MatOfPoint outroQuadrado : quadrados) {
                if (quadrado == outroQuadrado) continue;
                if (estaDentro(quadrado, outroQuadrado)) {
                    hierarquia.get(quadrado).add(outroQuadrado);
                }
            }
        }

        // Separa a "árvore" de quadrados em quadrados folha e quadrados
        // externos
        Iterator it = hierarquia.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry par = (Map.Entry) it.next();
            List<MatOfPoint> valor = (List<MatOfPoint>)par.getValue();
            // Log.d("ASDF", "Quadrilátero tem " + valor.size() + " quadriláteros internos.");
            if (valor.size() == 0) {
                this.folhas.add((MatOfPoint) par.getKey());
            }
            else {
                this.externos.add((MatOfPoint) par.getKey());
            }
        }

    }

    /**
     * Verifica se um quadrilátero está dentro de outro.
     *
     * @param quadrilateroExterno
     * @param quadrilateroInterno
     * @return
     */
    private boolean estaDentro(MatOfPoint quadrilateroExterno, MatOfPoint quadrilateroInterno) {
        final double ESTA_DENTRO_DO_CONTORNO = 1;
        double result;
        MatOfPoint2f quadrilateroExterno2f = new MatOfPoint2f();
        quadrilateroExterno.convertTo(quadrilateroExterno2f, CvType.CV_32FC2);
        for (Point point : quadrilateroInterno.toList()) {
            result = Imgproc.pointPolygonTest(quadrilateroExterno2f, point, false);
            if (result != ESTA_DENTRO_DO_CONTORNO) {
                return false;
            }
        }
        return true;
    }

}
