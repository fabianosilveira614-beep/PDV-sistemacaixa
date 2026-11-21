package br.com.ferragem.dominio.modelo.cliente;

import br.com.ferragem.dominio.excecao.CpfInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes do value object {@link Cpf} (RN16). Os CPFs válidos usados aqui são números de teste
 * conhecidos, gerados pelo próprio algoritmo, não de pessoas reais.
 */
class CpfTest {

    @Test
    @DisplayName("Aceita CPF válido com pontuação e guarda só os dígitos")
    void aceitaCpfComPontuacao() throws Exception {
        Cpf cpf = Cpf.de("529.982.247-25");

        assertEquals("52998224725", cpf.getDigitos());
        assertEquals("529.982.247-25", cpf.formatado());
    }

    @Test
    @DisplayName("Aceita CPF válido sem pontuação")
    void aceitaCpfSemPontuacao() throws Exception {
        assertEquals("111.444.777-35", Cpf.de("11144477735").formatado());
    }

    @Test
    @DisplayName("CT-21a CPF com dígito verificador errado é recusado")
    void ct21_recusaDigitoVerificadorErrado() {
        assertThrows(CpfInvalidoException.class, () -> Cpf.de("529.982.247-26"));
    }

    @Test
    @DisplayName("CPF com todos os dígitos iguais é recusado (passa no cálculo, mas é inválido)")
    void recusaTodosOsDigitosIguais() {
        assertThrows(CpfInvalidoException.class, () -> Cpf.de("111.111.111-11"));
    }

    @Test
    @DisplayName("CPF com tamanho errado ou letras é recusado")
    void recusaTamanhoErradoOuLetras() {
        assertThrows(CpfInvalidoException.class, () -> Cpf.de("1234567890"));
        assertThrows(CpfInvalidoException.class, () -> Cpf.de("529.982.247-2X"));
        assertThrows(CpfInvalidoException.class, () -> Cpf.de(""));
        assertThrows(CpfInvalidoException.class, () -> Cpf.de(null));
    }

    @Test
    @DisplayName("Dois CPFs com os mesmos números são iguais (value object)")
    void cpfsComMesmosNumerosSaoIguais() throws Exception {
        Cpf comPontuacao = Cpf.de("529.982.247-25");
        Cpf semPontuacao = Cpf.de("52998224725");

        assertEquals(comPontuacao, semPontuacao);
        assertEquals(comPontuacao.hashCode(), semPontuacao.hashCode());
        assertNotEquals(comPontuacao, Cpf.de("111.444.777-35"));
    }
}
