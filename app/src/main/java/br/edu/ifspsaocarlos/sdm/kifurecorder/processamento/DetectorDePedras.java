package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import br.edu.ifspsaocarlos.sdm.kifurecorder.TestesActivity;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Jogada;
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

//        Log.d(TestesActivity.TAG, "Cor média do tabuleiro: " + printColor(corMediaDoTabuleiro));

        for (int i = 0; i < dimensaoDoTabuleiro; ++i) {
            for (int j = 0; j < dimensaoDoTabuleiro; ++j) {
                double[] color = recuperarCorPredominanteNaPosicao(
                        (i * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        (j * larguraImagem / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                );

//                Log.i(TestesActivity.TAG, "Pos(" + i + ", " + j + ") = ");

                int hipotese = hipoteseDeCor(color, corMediaDoTabuleiro);
                if (hipotese != Tabuleiro.VAZIO) {
                    tabuleiro.colocarPedra(i, j, hipotese);
                }
            }
        }

//        Desenhista.desenhaLinhasNoPreview(imagemDoTabuleiro, larguraImagem, alturaImagem);

        return tabuleiro;
    }

	/**
	 * Utiliza a informação do último estado do jogo para melhorar a detecção da
	 * última jogada feita.
	 */
	public Jogada detectar(Tabuleiro ultimoTabuleiro) {
        long tempoEntrou = System.currentTimeMillis();

        int larguraImagem = (int)imagemDoTabuleiro.size().width;
        int alturaImagem = (int)imagemDoTabuleiro.size().height;

//        Tabuleiro tabuleiro = new Tabuleiro(dimensaoDoTabuleiro);

        double[] corMediaDoTabuleiro = corMediaDoTabuleiro(imagemDoTabuleiro);
//        Log.d(TestesActivity.TAG, "Cor média do tabuleiro: " + printColor(corMediaDoTabuleiro));

        double[][] coresMedias = new double[3][imagemDoTabuleiro.channels()];
        int[] contadores = new int[3];

        encontrarCoresMedias(ultimoTabuleiro, coresMedias, contadores);

        Jogada jogada = null;

        for (int i = 0; i < dimensaoDoTabuleiro; ++i) {
            for (int j = 0; j < dimensaoDoTabuleiro; ++j) {
//                Log.i(TestesActivity.TAG, "Pos(" + i + ", " + j + ") = ");

				// Ignora as interseções das jogadas que já foram feitas
				if (ultimoTabuleiro.getPosicao(i, j) != Tabuleiro.VAZIO) continue;

                double[] corAoRedorDaPosicao = recuperarCorPredominanteNaPosicao(
                        (i * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        (j * larguraImagem / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                );

                Log.d(TestesActivity.TAG, "Cor média ao redor de (" + i + ", " + j + ") = " + printColor(corAoRedorDaPosicao));
                Log.d(TestesActivity.TAG, "Luminancia ao redor de (" + i + ", " + j + ") = " + luminancia(corAoRedorDaPosicao));

//                int hipotese = hipoteseDeCor(color, corMediaDoTabuleiro);
//                int hipotese = hipoteseDeCor2(corAoRedorDaPosicao, corMediaDoTabuleiro, coresMedias, contadores);
                int hipotese = hipoteseDeCor3(corAoRedorDaPosicao, corMediaDoTabuleiro, coresMedias, contadores);
                if (hipotese != Tabuleiro.VAZIO) {
					// Já havia detectado outra jogada, há algo errado
					if (jogada != null) return null;
					jogada = new Jogada(i, j, hipotese);
                }
            }
        }

//        Desenhista.desenhaLinhasNoPreview(imagemDoTabuleiro, larguraImagem, alturaImagem);

        Log.d(TestesActivity.TAG, "TEMPO (detectar()): " + (System.currentTimeMillis() - tempoEntrou));
		return jogada;
	}

    private void encontrarCoresMedias(Tabuleiro ultimoTabuleiro, double[][] coresMedias, int[] contadores) {
        long tempoEntrou = System.currentTimeMillis();
        contadores[Tabuleiro.VAZIO] = 0;
        contadores[Tabuleiro.PEDRA_PRETA] = 0;
        contadores[Tabuleiro.PEDRA_BRANCA] = 0;

        int larguraImagem = (int)imagemDoTabuleiro.size().width;
        int alturaImagem = (int)imagemDoTabuleiro.size().height;

        for (int i = 0; i < dimensaoDoTabuleiro; ++i) {
            for (int j = 0; j < dimensaoDoTabuleiro; ++j) {
                int cor = ultimoTabuleiro.getPosicao(i, j);
                contadores[cor]++;

                double[] mediaDeCorNaPosicao = recuperarMediaDeCores(
                        imagemDoTabuleiro,
                        (i * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        (j * larguraImagem / (dimensaoDoTabuleiro - 1))
                );

                for (int k = 0; k < imagemDoTabuleiro.channels(); ++k) {
                    coresMedias[cor][k] += mediaDeCorNaPosicao[k];
                }
            }
        }
        Log.d(TestesActivity.TAG, "TEMPO (detectar() (1)): " + (System.currentTimeMillis() - tempoEntrou));

        for (int i = 0; i < 3; ++i) {
            if (contadores[i] > 0) {
                for (int j = 0; j < imagemDoTabuleiro.channels(); ++j) {
                    coresMedias[i][j] /= contadores[i];
                }
                Log.d(TestesActivity.TAG, "Cor média[" + i + "] = " + printColor(coresMedias[i]));
                Log.d(TestesActivity.TAG, "Luminancia[" + i + "] = " + luminancia(coresMedias[i]));
            }
        }
    }

    private int hipoteseDeCor2(double[] cor, double[] corMediaDoTabuleiro, double[][] coresMedias, int[] contadores) {
        double[] preto = {10.0, 10.0, 10.0, 255.0};

        double distanciaParaPreto = distanciaDeCor(cor, preto);

        double distanciaParaMediaIntersecoes = 999;
        if (contadores[Tabuleiro.VAZIO] > 0) {
            distanciaParaMediaIntersecoes = distanciaDeCor(cor, coresMedias[Tabuleiro.VAZIO]);
        }
        double distanciaParaMediaPecasPretas = 999;
        if (contadores[Tabuleiro.PEDRA_PRETA] > 0) {
            distanciaParaMediaPecasPretas = distanciaDeCor(cor, coresMedias[Tabuleiro.PEDRA_PRETA]);
        }
        double distanciaParaMediaPecasBrancas = 999;
        if (contadores[Tabuleiro.PEDRA_BRANCA] > 0) {
            distanciaParaMediaPecasBrancas = distanciaDeCor(cor, coresMedias[Tabuleiro.PEDRA_BRANCA]);
        }

        if (contadores[Tabuleiro.PEDRA_PRETA] == 0 && contadores[Tabuleiro.PEDRA_BRANCA] == 0) {
            if (distanciaParaPreto < 60 || distanciaParaPreto < distanciaParaMediaIntersecoes) {
                return Tabuleiro.PEDRA_PRETA;
            }
            else {
                return Tabuleiro.VAZIO;
            }
        }
        if (contadores[Tabuleiro.PEDRA_BRANCA] == 0) {
            if (distanciaParaPreto < 60 || distanciaParaMediaPecasPretas < 30) {
                return Tabuleiro.PEDRA_PRETA;
            }
            else if (cor[2] >= 150 || cor[2] >= corMediaDoTabuleiro[2] * 1.35) {
                return Tabuleiro.PEDRA_BRANCA;
            }
            else {
                return Tabuleiro.VAZIO;
            }
        }

        if (distanciaParaMediaPecasPretas < distanciaParaMediaIntersecoes &&
                distanciaParaMediaPecasPretas < distanciaParaMediaPecasBrancas) {
            return Tabuleiro.PEDRA_PRETA;
        }
        else if (distanciaParaMediaPecasBrancas < distanciaParaMediaIntersecoes &&
                distanciaParaMediaPecasBrancas < distanciaParaMediaPecasPretas) {
            return Tabuleiro.PEDRA_BRANCA;
        }
        return Tabuleiro.VAZIO;
    }

    private int hipoteseDeCor3(double[] cor, double[] corMediaDoTabuleiro, double[][] coresMedias, int[] contadores) {
        double[] preto = {10.0, 10.0, 10.0, 255.0};

        double luminanciaSendoVerificada = luminancia(cor);
        double distanciaParaPreto = distanciaDeCor(cor, preto);

        double distanciaParaMediaIntersecoes = 999;
        if (contadores[Tabuleiro.VAZIO] > 0) {
            distanciaParaMediaIntersecoes = Math.abs(luminanciaSendoVerificada - luminancia(coresMedias[Tabuleiro.VAZIO])) ;
//                    distanciaDeCor(cor, coresMedias[Tabuleiro.VAZIO]);
        }
        double distanciaParaMediaPecasPretas = 999;
        if (contadores[Tabuleiro.PEDRA_PRETA] > 0) {
            distanciaParaMediaPecasPretas = Math.abs(luminanciaSendoVerificada - luminancia(coresMedias[Tabuleiro.PEDRA_PRETA]));
//                    distanciaDeCor(cor, coresMedias[Tabuleiro.PEDRA_PRETA]);
        }
        double distanciaParaMediaPecasBrancas = 999;
        if (contadores[Tabuleiro.PEDRA_BRANCA] > 0) {
            distanciaParaMediaPecasBrancas = Math.abs(luminanciaSendoVerificada - luminancia(coresMedias[Tabuleiro.PEDRA_BRANCA]));
//                    distanciaDeCor(cor, coresMedias[Tabuleiro.PEDRA_BRANCA]);
        }

        if (contadores[Tabuleiro.PEDRA_PRETA] == 0 && contadores[Tabuleiro.PEDRA_BRANCA] == 0) {
            if (distanciaParaPreto < 50 || distanciaParaPreto < distanciaParaMediaIntersecoes) {
                return Tabuleiro.PEDRA_PRETA;
            }
            else {
                return Tabuleiro.VAZIO;
            }
        }
        if (contadores[Tabuleiro.PEDRA_BRANCA] == 0) {
            if (distanciaParaPreto < 50 || distanciaParaMediaPecasPretas < 30) {
                return Tabuleiro.PEDRA_PRETA;
            }
            else if (cor[2] >= 150 || cor[2] >= corMediaDoTabuleiro[2] * 1.35) {
                return Tabuleiro.PEDRA_BRANCA;
            }
            else {
                return Tabuleiro.VAZIO;
            }
        }

        // Vamos deixar alguns hard limits aqui, acho que pode ser últil
        if (distanciaParaPreto < 30 || distanciaParaMediaPecasPretas < 30) {
            return Tabuleiro.PEDRA_PRETA;
        }
        if (cor[2] >= 170 || distanciaParaMediaPecasBrancas < 30) {
            return Tabuleiro.PEDRA_BRANCA;
        }
        if (distanciaParaMediaIntersecoes < 30) {
            return Tabuleiro.VAZIO;
        }

        if (distanciaParaMediaPecasPretas < distanciaParaMediaIntersecoes &&
                distanciaParaMediaPecasPretas < distanciaParaMediaPecasBrancas) {
            return Tabuleiro.PEDRA_PRETA;
        }
        else if (distanciaParaMediaPecasBrancas < distanciaParaMediaIntersecoes &&
                distanciaParaMediaPecasBrancas < distanciaParaMediaPecasPretas) {
            return Tabuleiro.PEDRA_BRANCA;
        }
        return Tabuleiro.VAZIO;
    }

    private double luminancia(double cor[]) {
        return 0.299 * cor[0] + 0.587 * cor[1] + 0.114 * cor[2];
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
            case 1:   // Cor pontual exatamente sobre o ponto de intersecção
                color = imagem.get(linha, coluna);
                break;
            case 2:
                color = recuperarMediaDeCores(imagem, linha, coluna);
                break;
            case 3:
                color = recuperarMediaGaussianaDeCores(imagem, linha, coluna);
                break;
        }
        return color;
    }


    /**
     * Recupera a cor media ao redor de uma posiçao na imagem
     *
     * @param imagem
     * @param y
     * @param x
     * @return
     */
    private double[] recuperarMediaDeCores(Mat imagem, int y, int x) {
        long tempoEntrou = System.currentTimeMillis();

        // Este raio deve variar de acordo com o tamanho do tabuleiro
        // As pedras tem tamanho mais ou menos padrão, então não sei o quanto este parâmetro afetaria
        int radius = 8;

        // Não é um círculo, mas pelo speedup, acho que compensa
        Mat roi = imagem.submat(
                Math.max(y - radius, 0),
                Math.min(y + radius, imagem.height()),
                Math.max(x - radius, 0),
                Math.min(x + radius, imagem.width())
        );
        Scalar mediaScalar = Core.mean(roi);

        double[] corMedia = new double[imagem.channels()];
        for (int i = 0; i < mediaScalar.val.length; ++i) {
            corMedia[i] = mediaScalar.val[i];
        }

//        Log.i(TestesActivity.TAG, "Cor média ao redor de (" + x + ", " + y + ") = " + printColor(corMedia));

        //Log.d(TestesActivity.TAG, "TEMPO (recuperarMediaDeCores()): " + (System.currentTimeMillis() - tempoEntrou));
        return corMedia;
    }

    private double[] recuperarMediaGaussianaDeCores(Mat imagem, int y, int x) {
        double[] color = new double[imagem.channels()];
        for (int i = 0; i < color.length; ++i) {
            color[i] = 0;
        }
        int radius = 10;
        double contador = 0;

        for (int yy = y - radius; yy <= y + radius; ++yy) {
            if (yy < 0 || yy >= imagem.height()) continue;
            for (int xx = x - radius; xx <= x + radius; ++xx) {
                if (xx < 0 || xx >= imagem.width()) continue;
                double distancia = distance(xx, yy, x, y);
                if (distancia < radius) {
                    double c[] = imagem.get(yy, xx);
                    double peso = 1 / (distancia / 2 + 0.1);
                    for (int i = 0; i < c.length; ++i) {
                        color[i] += c[i] * peso;
                    }
                    contador += peso;
                }
            }
        }

        for (int i = 0; i < color.length; ++i) {
            color[i] /= contador;
        }

//        Log.i(TestesActivity.TAG, printColor(color));

        return color;
    }


    private double distance(int x, int y, int x2, int y2) {
        return Math.sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));
    }

    /**
     * Verifica se uma determinada cor está mais próxima de uma pedra preta ou branca.
     *
     * @param cor Cor a ser verificada
     * @param corMediaDoTabuleiro Cor média da imagem do tabuleiro
     * @return Pedra preta, branca, ou vazio
     */
    private int hipoteseDeCor(double[] cor, double[] corMediaDoTabuleiro) {
        double[] preto = {0.0, 0.0, 0.0, 255.0};
        double[] branco = {255.0, 255.0, 255.0, 255.0};
        double distanciaParaPreto = distanciaDeCor(cor, preto);
        double distanciaParaBranco = distanciaDeCor(cor, branco);
        double distanciaParaCorMedia = distanciaDeCor(cor, corMediaDoTabuleiro);

//        Log.d(TestesActivity.TAG, "distancia para preto = " + distanciaParaPreto);
//        Log.d(TestesActivity.TAG, "distancia para branco = " + distanciaParaBranco);
//        Log.d(TestesActivity.TAG, "distancia para media = " + distanciaParaCorMedia);

        // Testando outras hipóteses
        if (distanciaParaPreto < 80 || distanciaParaPreto < distanciaParaCorMedia) {
            return Tabuleiro.PEDRA_PRETA;
        }
//        else if (cor[2] >= 150) {
        else if (cor[2] >= corMediaDoTabuleiro[2] * 1.35) {
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
     * Retorna a cor media do tabuleiro.
     * 
     * ESTA COR MUDA CONFORME O JOGO PROGRIDE E CONFORME A ILUMINAÇÃO MUDA.
     *
     * @param imagemDoTabuleiro
     * @return
     */
    private double[] corMediaDoTabuleiro(Mat imagemDoTabuleiro) {
        Scalar mediaScalar = Core.mean(imagemDoTabuleiro);
        double[] media = new double[imagemDoTabuleiro.channels()];

        for (int i = 0; i < mediaScalar.val.length; ++i) {
            media[i] = mediaScalar.val[i];
        }

        return media;
    }

}
