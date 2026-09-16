package br.com.marcosbassetto.concursos.infrastructure.hash;

import br.com.marcosbassetto.concursos.common.exception.BusinessException;
import br.com.marcosbassetto.concursos.common.exception.ErrorCodes;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hash SHA-256 de arquivo, usado para reconhecer um edital já importado.
 *
 * É a forma determinística de evitar reprocessar o mesmo PDF, sem depender
 * de IA nem de comparação de conteúdo linha a linha.
 */
@Service
public class HashService {

    private static final String ALGORITMO = "SHA-256";

    public String calcularSha256(InputStream is) {
        try {
            return calcularSha256(is.readAllBytes());
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCodes.ARQUIVO_INVALIDO,
                    "Não foi possível ler o arquivo para calcular o hash.",
                    e);
        }
    }

    public String calcularSha256(byte[] conteudo) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO);
            return HexFormat.of().formatHex(digest.digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é obrigatório na JVM; se faltar, o ambiente está quebrado.
            throw new IllegalStateException("Algoritmo " + ALGORITMO + " indisponível", e);
        }
    }
}