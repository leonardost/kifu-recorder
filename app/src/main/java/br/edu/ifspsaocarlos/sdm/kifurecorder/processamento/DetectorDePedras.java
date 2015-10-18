package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;

/**
 * Detecta as pedras do tabuleiro que está na imagem, retornando um objeto
 * Tabuleiro correspondente ao estado atual do jogo.
 */
public class DetectorDePedras {

    // Imagem ortogonal e quadrada do tabuleiro
    private Mat imagemDoTabuleiro;
    // Dimensões do tabuleiro (9x9, 13x13 ou 19x19)
    private int dimensaoDoTabuleiro;

    public void setDimensaoDoTabuleiro(int dimensaoDoTabuleiro) {
        this.dimensaoDoTabuleiro = dimensaoDoTabuleiro;
    }

    public void setImagemDoTabuleiro(Mat imagemDoTabuleiro) {
        this.imagemDoTabuleiro = imagemDoTabuleiro;
    }

    public Tabuleiro detectar() {

        int larguraImagemPreview = (int)imagemDoTabuleiro.size().width;
        int alturaImagemPreview = (int)imagemDoTabuleiro.size().height;

        Tabuleiro tabuleiro = new Tabuleiro(dimensaoDoTabuleiro);

        double[] corMediaDoTabuleiro = corMediaDoTabuleiro(imagemDoTabuleiro);
        Log.d("kifu-recorder", "Cor média do tabuleiro: (" + corMediaDoTabuleiro[0] + ", " +
                corMediaDoTabuleiro[1] + ", " +
                corMediaDoTabuleiro[2] + ")");

        for (int i = 0; i < dimensaoDoTabuleiro; ++i) {
            for (int j = 0; j < dimensaoDoTabuleiro; ++j) {
                double[] color = recuperarCorPredominanteNaPosicao(
                        (i * alturaImagemPreview / (dimensaoDoTabuleiro - 1)),
                        (j * larguraImagemPreview / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                );
                int hipotese = hipoteseDeCor(color, corMediaDoTabuleiro);
                if (hipotese != Tabuleiro.VAZIO) {
                    tabuleiro.colocarPedra(i, j, hipotese);
                }
            }
        }

        Desenhista.desenhaLinhasNoPreview(imagemDoTabuleiro, larguraImagemPreview, alturaImagemPreview);

        return tabuleiro;
    }

    // TODO: Transformar hipóteses de recuperação de cor em classes separadas
    private double[] recuperarCorPredominanteNaPosicao(int linha, int coluna, Mat imagem) {
        int hipotese = 2;
        double[] color = new double[4];
        switch (hipotese) {
            case 1:
                color = imagem.get(linha, coluna);
                break;
            case 2:
                color = recuperarMediaGaussianaDeCores(imagem, linha, coluna);
                break;
        }
        Log.i("kifu-recorder", "Pos(" + linha + ", " + coluna + ") = " + color[0] + ", " + color[1] + ", " + color[2] + ", " + color[3]);
        return color;
    }

    private double[] recuperarMediaGaussianaDeCores(Mat imagem, int x, int y) {
        double[] color = new double[] {0.0, 0.0, 0.0, 0.0};
        int radius = 10;
        int contador = 0;

        for (int yy = y - radius; yy <= y + radius; ++yy) {
            if (yy < 0 || yy >= imagem.height()) continue;
            for (int xx = x - radius; xx <= x + radius; ++xx) {
                if (xx < 0 || xx >= imagem.width()) continue;
                if (distance(xx, yy, x, y) < radius) {
                    double c[] = imagem.get(yy, xx);
                    color[0] += c[0];
                    color[1] += c[1];
                    color[2] += c[2];
                    color[3] += c[3];
                    ++contador;
                }
            }
        }

        color[0] /= contador;
        color[1] /= contador;
        color[2] /= contador;
        color[3] /= contador;

        return color;
    }

    private double distance(int x, int y, int x2, int y2) {
        return Math.sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));
    }

    /**
     * Verifica se a cor esta mais proxima de preto ou branco. Este metodo vai precisar de um
     * parametro de ajuste por causa da cor do tabuleiro e das condiçoes de iluminaçao do local.
     * O parametro poderia ser a cor media do tabuleiro todo.
     *
     * @param cor
     * @return
     */
    private int hipoteseDeCor(double[] cor, double[] corMediaDoTabuleiro) {
        double[] preto = {0.0, 0.0, 0.0, 255.0};
        double[] branco = {255.0, 255.0, 255.0, 255.0};
        double distanciaParaPreto = distanciaDeCor(cor, preto);
        double distanciaParaBranco = distanciaDeCor(cor, branco);
        double distanciaParaCorMedia = distanciaDeCor(cor, corMediaDoTabuleiro);

        Log.d("kifu-recorder", "distancia para preto = " + distanciaParaPreto);
        Log.d("kifu-recorder", "distancia para branco = " + distanciaParaBranco);
        Log.d("kifu-recorder", "distancia para media = " + distanciaParaCorMedia);

        // Se a distância para a média for menor que um certo threshold, muito provavelmente é uma
        // intersecção vazia
        if (distanciaParaCorMedia < 120) {
            return Tabuleiro.VAZIO;
        }

        if (distanciaParaPreto < distanciaParaBranco) {
            return Tabuleiro.PEDRA_PRETA;
        }
        else {
            return Tabuleiro.PEDRA_BRANCA;
        }

/*
        if (distanciaParaPreto < distanciaParaBranco && distanciaParaPreto < distanciaParaCorMedia) {
            return Tabuleiro.PEDRA_PRETA;
        }
        else if (distanciaParaBranco < distanciaParaPreto && distanciaParaBranco < distanciaParaCorMedia) {
            return Tabuleiro.PEDRA_BRANCA;
        }
        return Tabuleiro.VAZIO;
        */
    }

    private double distanciaDeCor(double[] cor1, double[] cor2) {
        double distancia = 0;
        for (int i = 0; i < 4; ++i) {
            distancia += Math.abs(cor1[i] - cor2[i]);
        }
        return distancia;
    }

    /**
     * Retorna a cor media do tabuleiro. Deve haver uma forma melhor de fazer isso.
     *
     * @param imagemDoTabuleiro
     * @return
     */
    private double[] corMediaDoTabuleiro(Mat imagemDoTabuleiro) {
        double media[] = new double[] {0.0, 0.0, 0.0, 0.0};

        for (int y = 0; y < imagemDoTabuleiro.height(); ++y) {
            for (int x = 0; x < imagemDoTabuleiro.width(); ++x) {
                double c[] = imagemDoTabuleiro.get(y, x);
                media[0] += c[0];
                media[1] += c[1];
                media[2] += c[2];
                media[3] += c[3];
            }
        }

        media[0] /= imagemDoTabuleiro.height() * imagemDoTabuleiro.width();
        media[1] /= imagemDoTabuleiro.height() * imagemDoTabuleiro.width();
        media[2] /= imagemDoTabuleiro.height() * imagemDoTabuleiro.width();
        media[3] /= imagemDoTabuleiro.height() * imagemDoTabuleiro.width();

        return media;
    }

}
