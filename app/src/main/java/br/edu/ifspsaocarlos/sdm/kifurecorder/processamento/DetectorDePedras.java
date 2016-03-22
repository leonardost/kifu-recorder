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
    // Informações de debug do estado atual sendo visto pelo detector
    public StringBuilder snapshot;

    public void setDimensaoDoTabuleiro(int dimensaoDoTabuleiro) {
        this.dimensaoDoTabuleiro = dimensaoDoTabuleiro;
    }

    public void setImagemDoTabuleiro(Mat imagemDoTabuleiro) {
        this.imagemDoTabuleiro = imagemDoTabuleiro;
    }

    /**
     * Detecção de pedras que não utiliza o estado anterior da partida.
     *//*
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
    }*/

    /**
     * Utiliza a informação do último estado do jogo para melhorar a detecção da
     * última jogada feita.
     *//*
    public Jogada detectar(Tabuleiro ultimoTabuleiro) {
        long tempoEntrou             = System.currentTimeMillis();

//        Tabuleiro tabuleiro = new Tabuleiro(dimensaoDoTabuleiro);

        int larguraImagem            = (int)imagemDoTabuleiro.size().width;
        int alturaImagem             = (int)imagemDoTabuleiro.size().height;
        double[] corMediaDoTabuleiro = corMediaDoTabuleiro(imagemDoTabuleiro);
        double[][] coresMedias       = new double[3][imagemDoTabuleiro.channels()];
        int[] contadores             = new int[3];

//        Log.d(TestesActivity.TAG, "Cor média do tabuleiro: " + printColor(corMediaDoTabuleiro));

        snapshot = new StringBuilder();

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

                double[][] coresNasPosicoesLivresAdjacentes = new double[4][];
                coresNasPosicoesLivresAdjacentes[0] = (i > 0) ? ultimoTabuleiro.getPosicao(i - 1, j) == Tabuleiro.VAZIO ? recuperarCorPredominanteNaPosicao(
                        ((i - 1) * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        (j * larguraImagem / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                ) : null : null;
                coresNasPosicoesLivresAdjacentes[1] = (j < dimensaoDoTabuleiro - 1) ? ultimoTabuleiro.getPosicao(i, j + 1) == Tabuleiro.VAZIO ? recuperarCorPredominanteNaPosicao(
                        (i * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        ((j + 1) * larguraImagem / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                ) : null : null;
                coresNasPosicoesLivresAdjacentes[2] = (i < dimensaoDoTabuleiro - 1) ? ultimoTabuleiro.getPosicao(i + 1, j) == Tabuleiro.VAZIO ? recuperarCorPredominanteNaPosicao(
                        ((i + 1) * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        (j * larguraImagem / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                ) : null : null;
                coresNasPosicoesLivresAdjacentes[3] = (j > 0) ? ultimoTabuleiro.getPosicao(i, j - 1) == Tabuleiro.VAZIO ? recuperarCorPredominanteNaPosicao(
                        (i * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        ((j - 1) * larguraImagem / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                ) : null : null;

                Log.d(TestesActivity.TAG, "Cor média ao redor de (" + i + ", " + j + ") = " + printColor(corAoRedorDaPosicao));
                Log.d(TestesActivity.TAG, "Luminancia ao redor de (" + i + ", " + j + ") = " + luminancia(corAoRedorDaPosicao));
                Log.d(TestesActivity.TAG, "Variância ao redor de (" + i + ", " + j + ") = " + variancia(corAoRedorDaPosicao));
                snapshot.append("Cor média ao redor de (" + i + ", " + j + ") = " + printColor(corAoRedorDaPosicao) + "\n");
                snapshot.append("Luminancia ao redor de (" + i + ", " + j + ") = " + luminancia(corAoRedorDaPosicao) + "\n");
                snapshot.append("Variância ao redor de (" + i + ", " + j + ") = " + variancia(corAoRedorDaPosicao) + "\n");

//                int hipotese = hipoteseDeCor(color, corMediaDoTabuleiro);
//                int hipotese = hipoteseDeCor2(corAoRedorDaPosicao, corMediaDoTabuleiro, coresMedias, contadores);
//                int hipotese = hipoteseDeCor3(corAoRedorDaPosicao, corMediaDoTabuleiro, coresMedias, contadores);
//                int hipotese = hipoteseDeCor4(corAoRedorDaPosicao, corMediaDoTabuleiro, coresMedias, contadores);
                int hipotese = hipoteseDeCor5(corAoRedorDaPosicao, corMediaDoTabuleiro, coresMedias, contadores, coresNasPosicoesLivresAdjacentes);

                snapshot.append("Hipótese para (" + i + ", " + j + ") = " + hipotese + "\n");

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
    }*/

    /**
     * Utiliza a informação do último estado do jogo para melhorar a detecção da
     * última jogada feita. Os parâmetros informam se o detector deve procurar
     * uma pedra preta, branca, ou ambas, de acordo com o estado atual da
     * partida.
     */
    public Tabuleiro detectar(Tabuleiro ultimoTabuleiro, boolean podeSerPedraPreta, boolean podeSerPedraBranca) {
        long tempoEntrou             = System.currentTimeMillis();
        int larguraImagem            = (int)imagemDoTabuleiro.size().width;
        int alturaImagem             = (int)imagemDoTabuleiro.size().height;
        double[] corMediaDoTabuleiro = corMediaDoTabuleiro(imagemDoTabuleiro);
        double[][] coresMedias       = new double[3][imagemDoTabuleiro.channels()];
        int[] contadores             = new int[3];

//        Log.d(TestesActivity.TAG, "Cor média do tabuleiro: " + printColor(corMediaDoTabuleiro));

        snapshot = new StringBuilder();

        encontrarCoresMedias(ultimoTabuleiro, coresMedias, contadores);

        ArrayList<HipoteseDeJogada> hipotesesDeJogadasEncontradas = new ArrayList<HipoteseDeJogada>();

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

                double[][] coresNasPosicoesLivresAdjacentes = new double[4][];
                coresNasPosicoesLivresAdjacentes[0] = (i > 0) ? ultimoTabuleiro.getPosicao(i - 1, j) == Tabuleiro.VAZIO ? recuperarCorPredominanteNaPosicao(
                        ((i - 1) * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        (j * larguraImagem / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                ) : null : null;
                coresNasPosicoesLivresAdjacentes[1] = (j < dimensaoDoTabuleiro - 1) ? ultimoTabuleiro.getPosicao(i, j + 1) == Tabuleiro.VAZIO ? recuperarCorPredominanteNaPosicao(
                        (i * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        ((j + 1) * larguraImagem / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                ) : null : null;
                coresNasPosicoesLivresAdjacentes[2] = (i < dimensaoDoTabuleiro - 1) ? ultimoTabuleiro.getPosicao(i + 1, j) == Tabuleiro.VAZIO ? recuperarCorPredominanteNaPosicao(
                        ((i + 1) * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        (j * larguraImagem / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                ) : null : null;
                coresNasPosicoesLivresAdjacentes[3] = (j > 0) ? ultimoTabuleiro.getPosicao(i, j - 1) == Tabuleiro.VAZIO ? recuperarCorPredominanteNaPosicao(
                        (i * alturaImagem / (dimensaoDoTabuleiro - 1)),
                        ((j - 1) * larguraImagem / (dimensaoDoTabuleiro - 1)),
                        imagemDoTabuleiro
                ) : null : null;

                Log.d(TestesActivity.TAG, "Cor média ao redor de (" + i + ", " + j + ") = " + printColor(corAoRedorDaPosicao));
                Log.d(TestesActivity.TAG, "Luminancia ao redor de (" + i + ", " + j + ") = " + luminancia(corAoRedorDaPosicao));
                Log.d(TestesActivity.TAG, "Variância ao redor de (" + i + ", " + j + ") = " + variancia(corAoRedorDaPosicao));
                snapshot.append("Cor média ao redor de (" + i + ", " + j + ") = " + printColor(corAoRedorDaPosicao) + "\n");
                snapshot.append("Luminancia ao redor de (" + i + ", " + j + ") = " + luminancia(corAoRedorDaPosicao) + "\n");
                snapshot.append("Variância ao redor de (" + i + ", " + j + ") = " + variancia(corAoRedorDaPosicao) + "\n");

//                int hipotese = hipoteseDeCor(color, corMediaDoTabuleiro);
//                int hipotese = hipoteseDeCor2(corAoRedorDaPosicao, corMediaDoTabuleiro, coresMedias, contadores);
//                int hipotese = hipoteseDeCor3(corAoRedorDaPosicao, corMediaDoTabuleiro, coresMedias, contadores);
//                int hipotese = hipoteseDeCor4(corAoRedorDaPosicao, corMediaDoTabuleiro, coresMedias, contadores);
                HipoteseDeJogada hipotese = hipoteseDeCor5(corAoRedorDaPosicao, corMediaDoTabuleiro, coresMedias, contadores, coresNasPosicoesLivresAdjacentes);
                hipotese.linha = i;
                hipotese.coluna = j;

                snapshot.append("Hipótese para (" + i + ", " + j + ") = " + hipotese.cor + "\n");

                if (hipotese.cor != Tabuleiro.VAZIO) {
                    if (podeSerPedraPreta && hipotese.cor == Tabuleiro.PEDRA_PRETA ||
                            podeSerPedraBranca && hipotese.cor == Tabuleiro.PEDRA_BRANCA) {
                        hipotesesDeJogadasEncontradas.add(hipotese);
                    }
                }

            }
        }

        // Escolhe a jogada que obteve maior confiança
        // IMPORTANTE: Poderia verificar se a diferença de confiança entre as
        // duas jogadas mais prováveis for muito pequena, desconsiderar ambas,
        // porque é um sinal que o detector está confuso
        
        Jogada jogadaEscolhida = null;
        double maiorConfianca = 0;
        for (HipoteseDeJogada hipotese : hipotesesDeJogadasEncontradas) {
            if (hipotese.confianca > maiorConfianca) {
                maiorConfianca = hipotese.confianca;
                jogadaEscolhida = new Jogada(hipotese.linha, hipotese.coluna, hipotese.cor);
            }
        }

        Tabuleiro tabuleiro = new Tabuleiro(ultimoTabuleiro);
        if (jogadaEscolhida != null) {
            tabuleiro = tabuleiro.gerarNovoTabuleiroComAJogada(jogadaEscolhida);
        }

//        Desenhista.desenhaLinhasNoPreview(imagemDoTabuleiro, larguraImagem, alturaImagem);
//        Log.d(TestesActivity.TAG, "TEMPO (detectar()): " + (System.currentTimeMillis() - tempoEntrou));
        return tabuleiro;
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
                snapshot.append("Cor média (");
                if (i == Tabuleiro.VAZIO) {
                    snapshot.append("interseções livres");
                }
                else if (i == Tabuleiro.PEDRA_PRETA) {
                    snapshot.append("pedras pretas");
                }
                else if (i == Tabuleiro.PEDRA_BRANCA) {
                    snapshot.append("pedras brancas");
                }
                snapshot.append(") = " + printColor(coresMedias[i]) + "\n");
                snapshot.append("Luminancia = " + luminancia(coresMedias[i]) + "\n");
                snapshot.append("Variância = " + variancia(coresMedias[i]) + "\n");
            }
        }
    }
/*
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
*/
    private double luminancia(double cor[]) {
        return 0.299 * cor[0] + 0.587 * cor[1] + 0.114 * cor[2];
    }

    /**
     * Hipótese que leva em conta a variância das cores
     * @param cor
     * @param corMediaDoTabuleiro
     * @param coresMedias
     * @param contadores
     * @return
     *//*
    private int hipoteseDeCor4(double[] cor, double[] corMediaDoTabuleiro, double[][] coresMedias, int[] contadores) {
        double[] preto = {10.0, 10.0, 10.0, 255.0};

        double luminanciaSendoVerificada = luminancia(cor);
        double distanciaParaPreto = distanciaDeCor(cor, preto);

        // Se a variância é maior que um certo valor, considera que é uma interseção vazia
        // TODO: Este valor pode varia de acordo com a cor do tabuleiro, será que seria melhor
        //       baseá-lo na variância da cor média do tabuleiro? Mas difiiclmente um tabuleiro
        //       tem cor próxima do cinza

        // Percebi que a variância não é uma boa medida para verificar se é uma interseção vazia,
        // alguns testes mostram que uma pedra branca pode ter variãncia maior que o tabuleiro,
        // dependendo das condições de iluminação
        if (variancia(cor) > 150) return Tabuleiro.VAZIO;

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
            // Estes valores para pedras brancas precisariam ser revistos
            else if (cor[2] >= 150 || cor[2] >= corMediaDoTabuleiro[2] * 1.25) {
                return Tabuleiro.PEDRA_BRANCA;
            }
            else {
                return Tabuleiro.VAZIO;
            }
        }

        if (distanciaParaPreto < 30 || distanciaParaMediaPecasPretas < 25) {
            return Tabuleiro.PEDRA_PRETA;
        }
        if (cor[2] >= 170 || distanciaParaMediaPecasBrancas < 25) {
            return Tabuleiro.PEDRA_BRANCA;
        }
        if (variancia(cor) > 120 || distanciaParaMediaIntersecoes < 25) {
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
*/
    private HipoteseDeJogada hipoteseDeCor5(double[] cor, double[] corMediaDoTabuleiro, double[][] coresMedias, int[] contadores, double[][] coresNasPosicoesAdjacentes) {
        double[] preto = {10.0, 10.0, 10.0, 255.0};

        double luminanciaSendoVerificada = luminancia(cor);
        double distanciaParaPreto = distanciaDeCor(cor, preto);
        double diferencaDeLuminanciaParaOsVizinhos = diferencaDeLuminancia(cor, coresNasPosicoesAdjacentes);
        snapshot.append("Diferença de luminância para as posições adjacentes vazias = " + diferencaDeLuminanciaParaOsVizinhos + "\n");

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
                return new HipoteseDeJogada(Tabuleiro.PEDRA_PRETA, 1);
            }
            else {
                return new HipoteseDeJogada(Tabuleiro.VAZIO, 1);
            }
        }

        if (contadores[Tabuleiro.PEDRA_BRANCA] == 0) {
            if (distanciaParaPreto < 50 || distanciaParaMediaPecasPretas < 30) {
                return new HipoteseDeJogada(Tabuleiro.PEDRA_PRETA, 1);
            }
            else if (diferencaDeLuminanciaParaOsVizinhos > 70) {
                return new HipoteseDeJogada(Tabuleiro.PEDRA_BRANCA, 0.9);
            }
            // Estes valores para pedras brancas precisariam ser revistos
            else if (cor[2] >= 150 || cor[2] >= corMediaDoTabuleiro[2] * 1.25) {
                return new HipoteseDeJogada(Tabuleiro.PEDRA_BRANCA, 0.7);
            }
            else {
                return new HipoteseDeJogada(Tabuleiro.VAZIO, 1);
            }
        }

        double[] probabilidadeDeSer = new double[3];
        probabilidadeDeSer[Tabuleiro.VAZIO] = 0;
        probabilidadeDeSer[Tabuleiro.PEDRA_PRETA] = 0;
        probabilidadeDeSer[Tabuleiro.PEDRA_BRANCA] = 0;

        if (distanciaParaPreto < 30) {
            return new HipoteseDeJogada(Tabuleiro.PEDRA_PRETA, 1);
        }
        if (diferencaDeLuminanciaParaOsVizinhos > 70) {
            return new HipoteseDeJogada(Tabuleiro.PEDRA_BRANCA, 0.9);
        }
        probabilidadeDeSer[Tabuleiro.PEDRA_PRETA] = 1 - ((double)distanciaParaMediaPecasPretas / 255);
        probabilidadeDeSer[Tabuleiro.PEDRA_BRANCA] = 1 - ((double)distanciaParaMediaPecasBrancas / 255);
        probabilidadeDeSer[Tabuleiro.VAZIO] = 1 - ((double)distanciaParaMediaIntersecoes / 255);
        snapshot.append("Probabilidade de ser pedra preta  = " + probabilidadeDeSer[Tabuleiro.PEDRA_PRETA] + "\n");
        snapshot.append("Probabilidade de ser pedra branca = " + probabilidadeDeSer[Tabuleiro.PEDRA_BRANCA] + "\n");
        snapshot.append("Probabilidade de ser vazio        = " + probabilidadeDeSer[Tabuleiro.VAZIO] + "\n");

        if (probabilidadeDeSer[Tabuleiro.PEDRA_PRETA] > probabilidadeDeSer[Tabuleiro.PEDRA_BRANCA] &&
                probabilidadeDeSer[Tabuleiro.PEDRA_PRETA] > probabilidadeDeSer[Tabuleiro.VAZIO]) {
            double diferencas = probabilidadeDeSer[Tabuleiro.PEDRA_PRETA] - probabilidadeDeSer[Tabuleiro.PEDRA_BRANCA];
            diferencas += probabilidadeDeSer[Tabuleiro.PEDRA_PRETA] - probabilidadeDeSer[Tabuleiro.VAZIO];
            snapshot.append("Hipótese de ser pedra preta com diferenças de " + (diferencas / 2) + "\n");
            return new HipoteseDeJogada(Tabuleiro.PEDRA_PRETA, diferencas / 2);
        }

        if (probabilidadeDeSer[Tabuleiro.PEDRA_BRANCA] > probabilidadeDeSer[Tabuleiro.PEDRA_PRETA] &&
                probabilidadeDeSer[Tabuleiro.PEDRA_BRANCA] > probabilidadeDeSer[Tabuleiro.VAZIO]) {
            double diferencas = probabilidadeDeSer[Tabuleiro.PEDRA_BRANCA] - probabilidadeDeSer[Tabuleiro.PEDRA_PRETA];
            diferencas += probabilidadeDeSer[Tabuleiro.PEDRA_BRANCA] - probabilidadeDeSer[Tabuleiro.VAZIO];
            snapshot.append("Hipótese de ser pedra branca com diferenças de " + (diferencas / 2) + "\n");
            return new HipoteseDeJogada(Tabuleiro.PEDRA_BRANCA, diferencas / 2);
        }

        return new HipoteseDeJogada(Tabuleiro.VAZIO, 1);
    }

    private double diferencaDeLuminancia(double cor[], double corNasPosicoesAdjacentes[][]) {
        double diferenca = 0;
        double luminanciaDoCentro = luminancia(cor);
        for (int i = 0; i < 4; ++i) {
            if (corNasPosicoesAdjacentes[i] != null) {
                double luminancia = luminancia(corNasPosicoesAdjacentes[i]);
                diferenca += (luminanciaDoCentro - luminancia);
            }
        }
        return diferenca;
    }

    private String printColor(double color[]) {
        StringBuilder saida = new StringBuilder("(");
        for (int i = 0; i < color.length; ++i) {
            saida.append(color[i] + ", ");
        }
        saida.append(")");
        return saida.toString();
    }

    private double variancia(double cor[]) {
        double media = (cor[0] + cor[1] + cor[2]) / 3;
        double diferencas[] = {cor[0] - media, cor[1] - media, cor[2] - media};
        return (diferencas[0] * diferencas[0] +
                diferencas[1] * diferencas[1] +
                diferencas[2] * diferencas[2]) / 3;
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
        
        /**
         * A imagem do tabuleiro ortogonal tem 500x500 pixels de dimensão.
         * Este cálculo pega mais ou menos o tamanho de pouco menos de metade de uma pedra na imagem do
         * tabuleiro ortogonal.
         * 9x9 -> 25
         * 13x13 -> 17
         * 19x19 -> 11
         * 
         * Antes o raio sendo utilizado era de 8 pixels. Em um tabuleiro 9x9 em uma imagme de 500x500
         * pixels, um raio de 8 pixels, em uma interseção que tem o ponto do hoshi, o detector quase
         * achava que havia uma pedra preta ali.
         */ 
        //int radius = 500 / (partida.getDimensaoDoTabuleiro() - 1) * 0.4;
        int radius;
        if (dimensaoDoTabuleiro == 9) {
            radius = 25;
        }
        else if (dimensaoDoTabuleiro == 13) {
            radius = 17;
        }
        else if (dimensaoDoTabuleiro == 19) {
            radius = 11;
        }

        // Não é um círculo, mas pelo speedup, acho que compensa pegar a média
        // de cores assim
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
//        Log.d(TestesActivity.TAG, "TEMPO (recuperarMediaDeCores()): " + (System.currentTimeMillis() - tempoEntrou));
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
     *//*
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
*/
/*
        if (distanciaParaPreto < distanciaParaBranco && distanciaParaPreto < distanciaParaCorMedia) {
            return Tabuleiro.PEDRA_PRETA;
        }
        else if (distanciaParaBranco < distanciaParaPreto && distanciaParaBranco < distanciaParaCorMedia) {
            return Tabuleiro.PEDRA_BRANCA;
        }
        return Tabuleiro.VAZIO;
        *//*
    }*/

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
