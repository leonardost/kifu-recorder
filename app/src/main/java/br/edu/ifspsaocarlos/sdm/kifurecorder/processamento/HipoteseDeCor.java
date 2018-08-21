package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import org.opencv.core.Mat;

public class HipoteseDeCor {

    private Mat imagemOrtogonalDoTabuleiro;

    public HipoteseDeCor(Mat imagemOrtogonalDoTabuleiro) {
        this.imagemOrtogonalDoTabuleiro = imagemOrtogonalDoTabuleiro;
        // TODO: Processa informações gerais da imagem, como médias de cores de cada tipo de posição, etc.
    }

    public HipoteseDeJogada processar(int linha, int coluna) {
        // TODO: implementar
        return null;
    }

}
