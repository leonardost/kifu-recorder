/**
 * - Guardar todas as jogadas que foram detectadas juntamente com a confiança dela -> fazer um log na partida
 */
package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Jogada;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Partida;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Desenhista;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDePedras;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.TransformadorDeTabuleiro;

public class RegistrarPartidaActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    DetectorDePedras detectorDePedras = new DetectorDePedras();
    Partida partida;
    Tabuleiro ultimoTabuleiro;
    List<String> log;                          // Guarda todo o histórico de jogadas e valores de confiança da partida

    int contadorDeJogadas = 0;                 // A cada 5 jogadas feitas a partida é salva automaticamente
    long tempoLimite = 2000;                   // Tempo que um tabuleiro detectado deve se manter inalterado para que seja considerado pelo detector
    long momentoDaUltimaDeteccaoDeTabuleiro;
    long tempoDesdeUltimaMudancaDeTabuleiro;
    String snapshotAtual;
    boolean pausado = false;

    int dimensaoDoTabuleiro;                   // 9x9, 13x13 ou 19x19
    int[] cantosDoTabuleiro;                   // Pontos dos cantos do tabuleiro
    Mat posicaoDoTabuleiroNaImagem;            // Matriz que contem os cantos do tabuleiro
    Mat tabuleiroOrtogonal;                    // Imagem do tabuleiro transformado em visão ortogonal
	MatOfPoint contornoDoTabuleiro;

    private File pastaDeRegistro;              // Local onde os registros e arquivos de log serão salvos
    private File arquivoDeRegistro;            // Representa o arquivo SGF da partida

    private ImageButton btnSalvar;
    private ImageButton btnVoltarUltimaJogada;
    private ImageButton btnRotacionarEsquerda;
    private ImageButton btnRotacionarDireita;
    private ImageButton btnPausar;
    private ImageButton btnSnapshot;
    private ImageButton btnAdicionarJogada;
    private Button btnFinalizar;
    private SoundPool soundPool;
    private int beepId;
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TestesActivity.TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_partida);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_registro);
        mOpenCvCameraView.setCvCameraViewListener(this);

        Intent i = getIntent();
        String jogadorDePretas  = i.getStringExtra("jogadorDePretas");
        String jogadorDeBrancas = i.getStringExtra("jogadorDeBrancas");
        String komi             = i.getStringExtra("komi");
        dimensaoDoTabuleiro     = i.getIntExtra("dimensaoDoTabuleiro", -1);
        cantosDoTabuleiro       = i.getIntArrayExtra("posicaoDoTabuleiroNaImagem");
		detectorDePedras.setDimensaoDoTabuleiro(dimensaoDoTabuleiro);

        processarCantosDoTabuleiro();

        partida = new Partida(dimensaoDoTabuleiro, jogadorDePretas, jogadorDeBrancas, komi);
        ultimoTabuleiro = new Tabuleiro(dimensaoDoTabuleiro);
        momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
        tempoDesdeUltimaMudancaDeTabuleiro = 0;

        btnSalvar = (ImageButton) findViewById(R.id.btnSalvar);
        btnSalvar.setOnClickListener(this);
        btnSalvar.setEnabled(true);
        btnVoltarUltimaJogada = (ImageButton) findViewById(R.id.btnVoltarUltimaJogada);
        btnVoltarUltimaJogada.setOnClickListener(this);
        btnVoltarUltimaJogada.setEnabled(false);
        btnRotacionarEsquerda = (ImageButton) findViewById(R.id.btnRotacionarEsquerda);
        btnRotacionarEsquerda.setOnClickListener(this);
        btnRotacionarDireita = (ImageButton) findViewById(R.id.btnRotacionarDireita);
        btnRotacionarDireita.setOnClickListener(this);
        btnPausar = (ImageButton) findViewById(R.id.btnPausar);
        btnPausar.setOnClickListener(this);
        btnFinalizar = (Button) findViewById(R.id.btnFinalizar);
        btnFinalizar.setOnClickListener(this);
        btnSnapshot = (ImageButton) findViewById(R.id.btnSnapshot);
        btnSnapshot.setOnClickListener(this);
        btnAdicionarJogada = (ImageButton) findViewById(R.id.btnAdicionarPedra);
        btnAdicionarJogada.setOnClickListener(this);

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        beepId = soundPool.load(this, R.raw.beep, 1);

        arquivoDeRegistro = getFile("sgf");
		pastaDeRegistro = new File(Environment.getExternalStorageDirectory(), "sgfs_salvos");
		if (!criarPastaDeRegistroSeNaoExistir()) {
			finish();
			return;
		}

        Log.i(TestesActivity.TAG, "onCreate() finalizado");
    }

    private void processarCantosDoTabuleiro() {
        posicaoDoTabuleiroNaImagem = new Mat(4, 1, CvType.CV_32FC2);
        posicaoDoTabuleiroNaImagem.put(0, 0,
                cantosDoTabuleiro[0], cantosDoTabuleiro[1],
                cantosDoTabuleiro[2], cantosDoTabuleiro[3],
                cantosDoTabuleiro[4], cantosDoTabuleiro[5],
                cantosDoTabuleiro[6], cantosDoTabuleiro[7]);

        cantos = new Point[4];
        cantos[0] = new Point(cantosDoTabuleiro[0], cantosDoTabuleiro[1]);
        cantos[1] = new Point(cantosDoTabuleiro[2], cantosDoTabuleiro[3]);
        cantos[2] = new Point(cantosDoTabuleiro[4], cantosDoTabuleiro[5]);
        cantos[3] = new Point(cantosDoTabuleiro[6], cantosDoTabuleiro[7]);
        contornoDoTabuleiro = new MatOfPoint(cantos);
    }

	private boolean criarPastaDeRegistroSeNaoExistir() {
        if (!pastaDeRegistro.exists()) {
            if (!pastaDeRegistro.mkdirs()) {
                Toast.makeText(RegistrarPartidaActivity.this, "ERRO: Diretório " + pastaDeRegistro.toString() + " não criado, verifique as configurações de armazenamento de seu dispositivo.", Toast.LENGTH_LONG).show();
                Log.e(TestesActivity.TAG, "Diretório " + pastaDeRegistro.toString() + " não criado, verifique as configurações de armazenamento de seu dispositivo.");
                return false;
            }
        }
        return true;
	}

    @Override
    public void onRestoreInstanceState(Bundle savedInstaceState) {
        super.onRestoreInstanceState(savedInstaceState);
        restaurarPartidaSalvaTemporariamente();
        processarCantosDoTabuleiro();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        guardaPartidaTemporariamente();  // TODO: Isto estava em cima, ver se não quebrou nada
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    private void guardaPartidaTemporariamente() {
        File arquivo = getTempFile();
        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(arquivo, false);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(partida);
                oos.writeObject(arquivoDeRegistro);
                oos.writeObject(cantosDoTabuleiro);
                oos.close();
                fos.close();
                Log.i(TestesActivity.TAG, "Partida salva temporariamente no arquivo " + arquivo.getName());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.e(TestesActivity.TAG, "Armazenamento externo não disponível para guardar registro temporário da partida.");
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    private void restaurarPartidaSalvaTemporariamente() {
        File arquivo = getTempFile();
        // TODO: Precisa fazer esta checagem aqui? Porque aqui só é feita a leitura de arquivos
        if (isExternalStorageWritable()) {
            try {
                FileInputStream fis = new FileInputStream(arquivo);
                ObjectInputStream ois = new ObjectInputStream(fis);
                partida = (Partida) ois.readObject();
                arquivoDeRegistro = (File) ois.readObject();
                cantosDoTabuleiro = (int[]) ois.readObject();
                ois.close();
                fis.close();
                Log.i(TestesActivity.TAG, "Partida recuperada.");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.e(TestesActivity.TAG, "Armazenamento externo não disponível para restaurar registro temporário da partida.");
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {

    }

    /**
     * Método chamado sempre que há um frame da câmera pronto para ser processado
     */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        long tempoEntrou = System.currentTimeMillis();
        Mat imagemFonte = inputFrame.rgba();

        tabuleiroOrtogonal = TransformadorDeTabuleiro.transformar(imagemFonte, posicaoDoTabuleiroNaImagem, null);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Verificar qual o tamanho da imagem do tabuleiro ortogonal aqui!!!
        //       Isso traz implicações para o raio calculado no método de calcular a cor média ao redor de uma posição
        // int larguraImagem = (int)tabuleiroOrtogonal.size().width;
        // int alturaImagem = (int)tabuleiroOrtogonal.size().height;
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        detectorDePedras.setImagemDoTabuleiro(tabuleiroOrtogonal);
        // Desenha o tabuleiro ortogonal na tela
        tabuleiroOrtogonal.copyTo(imagemFonte.rowRange(0, 500).colRange(0, 500));
  
        Tabuleiro tabuleiro = detectorDePedras.detectar(
                partida.ultimoTabuleiro(),
                partida.proximaJogadaPodeSer(Tabuleiro.PEDRA_PRETA),
                partida.proximaJogadaPodeSer(Tabuleiro.PEDRA_BRANCA)
        );

        snapshotAtual = detectorDePedras.snapshot.toString();
        snapshotAtual += partida.ultimoTabuleiro();

        if (!pausado) {

            if (ultimoTabuleiro.equals(tabuleiro)) {
                tempoDesdeUltimaMudancaDeTabuleiro += SystemClock.elapsedRealtime() - momentoDaUltimaDeteccaoDeTabuleiro;
                momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
                if (tempoDesdeUltimaMudancaDeTabuleiro > tempoLimite && partida.adicionarJogadaSeForValida(tabuleiro)) {
					novaJogadaFoiAdicionada();
                }
            } else {
                tempoDesdeUltimaMudancaDeTabuleiro = 0;
                momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
            }

        }

        ultimoTabuleiro = tabuleiro;

        Desenhista.desenharContornoDoTabuleiro(imagemFonte, contornoDoTabuleiro);
        if (pausado) {
            // Quando está pausado, desenha a saída atual do detector de pedras (útil para debugar)
            Desenhista.desenharTabuleiro(imagemFonte, tabuleiro, 0, 500, 400, null);
        }
        else {
            Desenhista.desenharTabuleiro(imagemFonte, partida.ultimoTabuleiro(), 0, 500, 400, partida.ultimaJogada());
        }

        Log.d(TestesActivity.TAG, "TEMPO (onCameraFrame()): " + (System.currentTimeMillis() - tempoEntrou));
        return imagemFonte;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSalvar:
                salvarArquivoNoDiscoESair(false);
                break;
            case R.id.btnVoltarUltimaJogada:
                temCertezaQueDesejaVoltarAUltimaJogada(getString(R.string.btn_voltar_ultima_jogada));
                break;
            case R.id.btnRotacionarEsquerda:
                rotacionar(-1);
                break;
            case R.id.btnRotacionarDireita:
                rotacionar(1);
                break;
            case R.id.btnPausar:
                pausado = !pausado;
                atualizarBotaoDePausa();
                break;
            case R.id.btnFinalizar:
                temCertezaQueDesejaFinalizarORegisro();
                break;
            case R.id.btnSnapshot:
                tirarSnapshot();
                break;
            case R.id.btnAdicionarPedra:
                adicionarJogadaAoRegistro();
                break;
        }
    }

	private void atualizarBotaoDePausa() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				btnPausar.setImageResource(pausado ? R.drawable.play : R.drawable.pause);
			}
		});
	}

    private void temCertezaQueDesejaVoltarAUltimaJogada(String mensagem) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_tem_certeza)
                .setMessage(mensagem)
                .setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        partida.voltarUltimaJogada();
                        tempoDesdeUltimaMudancaDeTabuleiro = 0;
                        momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
                        atualizarBotaoDeVoltar();
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .show();
    }

    private void tirarSnapshot() {
        try {
            Mat imagemFormatoDeCorCerto = new Mat();
            Imgproc.cvtColor(tabuleiroOrtogonal, imagemFormatoDeCorCerto, Imgproc.COLOR_RGBA2BGR);
            Highgui.imwrite(getFile("jpg").getAbsolutePath(), imagemFormatoDeCorCerto);

            FileOutputStream fos = new FileOutputStream(getFile("txt"), false);
            fos.write(snapshotAtual.getBytes());
            fos.flush();
            fos.close();

            Toast.makeText(RegistrarPartidaActivity.this, "Snapshot salva no arquivo: " + arquivoSnapshot.getName() + ".", Toast.LENGTH_LONG).show();
            Log.i(TestesActivity.TAG, "Snapshot salva: " + arquivoSnapshot.getName() + " com conteúdo " + snapshotAtual);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rotacionar(int direcao) {
        int[] cantosDoTabuleiroRotacionados = new int[8];

		// Anti-horário
        if (direcao == -1) {
            cantosDoTabuleiroRotacionados[0] = cantosDoTabuleiro[2];
            cantosDoTabuleiroRotacionados[1] = cantosDoTabuleiro[3];
            cantosDoTabuleiroRotacionados[2] = cantosDoTabuleiro[4];
            cantosDoTabuleiroRotacionados[3] = cantosDoTabuleiro[5];
            cantosDoTabuleiroRotacionados[4] = cantosDoTabuleiro[6];
            cantosDoTabuleiroRotacionados[5] = cantosDoTabuleiro[7];
            cantosDoTabuleiroRotacionados[6] = cantosDoTabuleiro[0];
            cantosDoTabuleiroRotacionados[7] = cantosDoTabuleiro[1];
        }
        // Horário
        else if (direcao == 1) {
            cantosDoTabuleiroRotacionados[0] = cantosDoTabuleiro[6];
            cantosDoTabuleiroRotacionados[1] = cantosDoTabuleiro[7];
            cantosDoTabuleiroRotacionados[2] = cantosDoTabuleiro[0];
            cantosDoTabuleiroRotacionados[3] = cantosDoTabuleiro[1];
            cantosDoTabuleiroRotacionados[4] = cantosDoTabuleiro[2];
            cantosDoTabuleiroRotacionados[5] = cantosDoTabuleiro[3];
            cantosDoTabuleiroRotacionados[6] = cantosDoTabuleiro[4];
            cantosDoTabuleiroRotacionados[7] = cantosDoTabuleiro[5];
        }

        cantosDoTabuleiro = cantosDoTabuleiroRotacionados;
        processarCantosDoTabuleiro();

        partida.rotacionar(direcao);
    }

    private void temCertezaQueDesejaFinalizarORegisro() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_tem_certeza)
                .setMessage(getString(R.string.dialog_finalizar_registro))
                .setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        salvarArquivoNoDiscoESair(true);
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .show();
    }

	/**
	 * Salva o arquivo da partida e o log no armazenamento secundário. Se o
	 * parâmetro 'sair' for verdadeiro, finaliza o registro depois de salvar.
	 */
	private void salvarArquivoNoDiscoESair(boolean sairDepoisDeSalvar) {
        String conteudoDaPartida = partida.sgf();
        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(arquivoDeRegistro, false);
                fos.write(conteudoDaPartida.getBytes());
                fos.flush();
                fos.close();
                // TODO: Verificar se isto tem que rodar na thread de UI
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(RegistrarPartidaActivity.this, "Partida salva no arquivo: " + arquivoDeRegistro.getName() + ".", Toast.LENGTH_LONG).show();
                    }
                });
                Log.i(TestesActivity.TAG, "Partida salva: " + arquivoDeRegistro.getName() + " com conteúdo " + conteudoDaPartida);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
				if (sairDepoisDeSalvar) {
					finish();
				}
			}
        }
        else {
            Toast.makeText(RegistrarPartidaActivity.this, "ERRO: Armazenamento externo nao disponivel.", Toast.LENGTH_LONG).show();
            Log.e(TestesActivity.TAG, "Armazenamento externo não disponível.");
        }
	}

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private File getFile(String extensao) {
        File arquivo = new File(pastaDeRegistro, gerarNomeDeArquivo(0, extensao));
        int contador = 1;

        while (arquivo.exists()) {
            String newFilename = gerarNomeDeArquivo(contador, extensao);
            arquivo = new File(pastaDeRegistro, newFilename);
            contador++;
        }

        return arquivo;
    }

    private File getTempFile() {
        return new File(pastaDeRegistro, "arquivo_temporario");
    }

    private String gerarNomeDeArquivo(int contadorDeNomeRepetido, String extensao) {
        // http://stackoverflow.com/questions/10203924/displaying-date-in-a-double-digit-format
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd");
        String data = sdf.format(new Date(Calendar.getInstance().getTimeInMillis()));
        String contador = "";
        if (contadorDeNomeRepetido > 0) {
            contador = "(" + contadorDeNomeRepetido + ")";
        }

        StringBuilder string = new StringBuilder();
        string.append(partida.getJogadorDeBrancas())
                .append("-")
                .append(partida.getJogadorDePretas())
                .append("_")
                .append(data)
                .append(contador)
                .append(".")
                .append(extensao);
        return string.toString();
    }

    // TODO: Verificar se isto está sendo chamado
    @Override
    public void onBackPressed() {
        temCertezaQueDesejaFinalizarORegisro();
    }

    private void adicionarJogadaAoRegistro() {
        AlertDialog.Builder dialogo = new AlertDialog.Builder(this);

        final EditText input = new EditText(RegistrarPartidaActivity.this);

        dialogo.setTitle(R.string.dialog_adicionar_jogada)
                .setMessage(getString(R.string.dialog_adicionar_jogada))
                .setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Jogada jogadaAdicionadaManualmente = processarJogadaManual(input.getText().toString());
                        Tabuleiro novoTabuleiro = partida.ultimoTabuleiro().gerarNovoTabuleiroComAJogada(jogadaAdicionadaManualmente);
                        if (partida.adicionarJogadaSeForValida(novoTabuleiro)) {
                            novaJogadaFoiAdicionada();
                            partida.adicionouJogadaManualmente();
                        }
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .setView(input)
                .show();
    }
    
    private Jogada processarJogadaManuL(String textoJogada) {
		textoJogada = textoJogada.trim();
		if (textoJogada.length() != 3) return null;
		textoJogada = textoJogada.toLowerCase();
		int cor = textoJogada.charAt(0) == 'b' ? Tabuleiro.PEDRA_PRETA : Tabuleiro.PEDRA_BRANCA;
		int linha = textoJogada.charAt(1) - 'a';
		int coluna = textoJogada.charAt(2) - 'a';
		return new Jogada(linha, coluna, cor);
	}

    private void novaJogadaFoiAdicionada() {
        contadorDeJogadas++;
        soundPool.play(beepId, 1, 1, 0, 0, 1);
        if (contadorDeJogadas % 5 == 0) salvarArquivoNoDiscoESair(false);
        atualizarBotaoDeVoltar();
    }

	private void atualizarBotaoDeVoltar() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				btnVoltarUltimaJogada.setEnabled(partida.numeroDeJogadasFeitas() > 0);
			}
		});
	}

}
