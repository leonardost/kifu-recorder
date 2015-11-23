package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import android.util.Log;

import org.opencv.core.Mat;

import br.edu.ifspsaocarlos.sdm.kifurecorder.MainActivity;
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

        int larguraImagem = (int)imagemDoTabuleiro.size().width;
        int alturaImagem = (int)imagemDoTabuleiro.size().height;

        Tabuleiro tabuleiro = new Tabuleiro(dimensaoDoTabuleiro);

        double[] corMediaDoTabuleiro = corMediaDoTabuleiro(imagemDoTabuleiro);

        Log.d(MainActivity.TAG, "Cor média do tabuleiro: " + printColor(corMediaDoTabuleiro));

        for (int i = 0; i < dimensaoDoTabuleiro; ++i) {
            for (int j = 0; j < dimensaoDoTabuleiro; ++j) {
                double[] color = recuperarCorPredominanteNaPosicao(
                        (i * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        (j * larguraImagem / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                );

                Log.i(MainActivity.TAG, "Pos(" + i + ", " + j + ") = ");

                int hipotese = hipoteseDeCor(color, corMediaDoTabuleiro);
                if (hipotese != Tabuleiro.VAZIO) {
                    tabuleiro.colocarPedra(i, j, hipotese);
                }
            }
        }

        Desenhista.desenhaLinhasNoPreview(imagemDoTabuleiro, larguraImagem, alturaImagem);

        return tabuleiro;
    }

    private String printColor(double color[]) {
        StringBuilder saida = new StringBuilder("(");
        for (int i = 0; i < color.length; ++i) {
            saida.append(color[i] + ", ");
        }
        return saida.toString();
    }

    // TODO: Transformar hipóteses de recuperação de cor em classes separadas
    private double[] recuperarCorPredominanteNaPosicao(int linha, int coluna, Mat imagem) {
        int hipotese = 2;
        double[] color = new double[imagem.channels()];
        switch (hipotese) {
            case 1:
                color = imagem.get(linha, coluna);
                break;
            case 2:
                color = recuperarMediaGaussianaDeCores(imagem, linha, coluna);
                break;
        }
        return color;
    }

    private double[] recuperarMediaGaussianaDeCores(Mat imagem, int y, int x) {
        double[] color = new double[imagem.channels()];
        for (int i = 0; i < color.length; ++i) {
            color[i] = 0;
        }
        int radius = 10;
        int contador = 0;

        for (int yy = y - radius; yy <= y + radius; ++yy) {
            if (yy < 0 || yy >= imagem.height()) continue;
            for (int xx = x - radius; xx <= x + radius; ++xx) {
                if (xx < 0 || xx >= imagem.width()) continue;
                if (distance(xx, yy, x, y) < radius) {
                    double c[] = imagem.get(yy, xx);
                    for (int i = 0; i < c.length; ++i) {
                        color[i] += c[i];
                    }
                    ++contador;
                }
            }
        }

        for (int i = 0; i < color.length; ++i) {
            color[i] /= contador;
        }

        Log.i(MainActivity.TAG, printColor(color));

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

        Log.d(MainActivity.TAG, "distancia para preto = " + distanciaParaPreto);
        Log.d(MainActivity.TAG, "distancia para branco = " + distanciaParaBranco);
        Log.d(MainActivity.TAG, "distancia para media = " + distanciaParaCorMedia);

        // Testando outras hipóteses
        if (distanciaParaPreto < 80 || distanciaParaPreto < distanciaParaCorMedia) {
            return Tabuleiro.PEDRA_PRETA;
        }
        else if (cor[2] >= 150) {
            return Tabuleiro.PEDRA_BRANCA;
        }
        else if (true) {
            return Tabuleiro.VAZIO;
        }

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
        for (int i = 0; i < Math.min(cor1.length, cor2.length); ++i) {
            distancia += Math.abs(cor1[i] - cor2[i]);
        }
        return distancia;
    }

    // Só considera a distância para a componente azul
    private double distanciaDeCor2(double[] cor1, double[] cor2) {
        double distancia = 0;
        for (int i = 0; i < Math.min(cor1.length, cor2.length); ++i) {
        //for (int i = 0; i < 3; ++i) {
            distancia += Math.abs(cor1[2] - cor2[2]);
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
        double media[] = new double[imagemDoTabuleiro.channels()];
        for (int i = 0; i < media.length; ++i) {
            media[i] = 0;
        }

        for (int y = 0; y < imagemDoTabuleiro.height(); ++y) {
            for (int x = 0; x < imagemDoTabuleiro.width(); ++x) {
                double c[] = imagemDoTabuleiro.get(y, x);
                for (int i = 0; i < c.length; ++i) {
                    media[i] += c[i];
                }
            }
        }

        for (int i = 0; i < media.length; ++i) {
            media[i] /= imagemDoTabuleiro.height() * imagemDoTabuleiro.width();
        }

        return media;
    }

}
