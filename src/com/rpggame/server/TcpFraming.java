package com.rpggame.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utilitarios de framing para protocolo TCP do jogo.
 *
 * Protocolo:
 *   [4 bytes big-endian: comprimento do payload em bytes][payload UTF-8]
 *
 * Isso evita fragmentacao de pacotes TCP — cada mensagem e lida/escrita
 * de forma atomica independente do tamanho.
 */
public final class TcpFraming {

  private TcpFraming() {}

  /**
   * Escreve uma mensagem JSON no stream com prefixo de 4 bytes de tamanho.
   * Thread-safe desde que apenas uma thread escreva por vez no mesmo stream.
   */
  public static void writeMessage(OutputStream out, String json) throws IOException {
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    DataOutputStream dos = new DataOutputStream(out);
    dos.writeInt(bytes.length);
    dos.write(bytes);
    dos.flush();
  }

  /**
   * Le uma mensagem do stream. Bloqueia ate receber o payload completo.
   * Lanca EOFException se a conexao for encerrada antes de completar a leitura.
   * Thread-safe desde que apenas uma thread leia por vez no mesmo stream.
   */
  public static String readMessage(InputStream in) throws IOException {
    DataInputStream dis = new DataInputStream(in);
    int length = dis.readInt(); // bloqueia ate 4 bytes ou EOF
    if (length < 0 || length > 4 * 1024 * 1024) {
      throw new IOException("Tamanho de mensagem invalido: " + length);
    }
    byte[] bytes = new byte[length];
    dis.readFully(bytes); // bloqueia ate ler todos os bytes
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
