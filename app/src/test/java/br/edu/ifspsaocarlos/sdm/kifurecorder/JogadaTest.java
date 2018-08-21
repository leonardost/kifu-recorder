package br.edu.ifspsaocarlos.sdm.kifurecorder;

import org.junit.Test;

import android.test.AndroidTestCase;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Jogada;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;

public class JogadaTest extends AndroidTestCase {

    @Test
    public void testSgf() {
        Jogada jogada = new Jogada(0, 0, Tabuleiro.PEDRA_PRETA);
        assertEquals("[aa]", jogada.sgf());
    }

}
