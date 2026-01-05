package com.rpggame.systems;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gerencia a reprodução de músicas de fundo no jogo
 */
public class MusicManager {
  private Clip currentClip;
  private String currentTrack;
  private Map<String, String> musicFiles;
  private float volume = 0.7f; // Volume padrão (0.0 a 1.0)

  public MusicManager() {
    this.musicFiles = new HashMap<>();
    initializeMusicTracks();
  }

  /**
   * Inicializa os caminhos das músicas
   */
  private void initializeMusicTracks() {
    musicFiles.put("village", "songs/MainOST.wav");
    musicFiles.put("goblin_territories", "songs/BossBattleOST.wav");
    musicFiles.put("secret_area", "songs/SecretAreaOST.wav");
    System.out.println("🎵 MusicManager inicializado com " + musicFiles.size() + " faixas");
  }

  /**
   * Toca uma música de fundo baseada no ID do mapa
   */
  public void playMusicForMap(String mapId) {
    String musicPath = musicFiles.get(mapId);

    if (musicPath == null) {
      System.out.println("⚠️ Nenhuma música definida para o mapa: " + mapId);
      return;
    }

    // Se já está tocando a mesma música, não fazer nada
    if (currentTrack != null && currentTrack.equals(musicPath)) {
      return;
    }

    stopMusic();
    playMusic(musicPath);
  }

  /**
   * Toca uma música diretamente pelo caminho do arquivo
   */
  public void playMusicByPath(String filePath) {
    // Se já está tocando a mesma música, não fazer nada
    if (currentTrack != null && currentTrack.equals(filePath)) {
      return;
    }

    stopMusic();
    playMusic(filePath);
  }

  /**
   * Toca uma música
   */
  private void playMusic(String filePath) {
    try {
      // Tentar carregar o arquivo de áudio
      File audioFile = new File(filePath);

      if (!audioFile.exists()) {
        System.err.println("❌ Arquivo de música não encontrado: " + filePath);
        return;
      }

      AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
      currentClip = AudioSystem.getClip();
      currentClip.open(audioStream);

      // Configurar volume
      setVolume(volume);

      // Loop infinito
      currentClip.loop(Clip.LOOP_CONTINUOUSLY);
      currentClip.start();

      currentTrack = filePath;
      System.out.println("🎵 Tocando: " + filePath);

    } catch (UnsupportedAudioFileException e) {
      System.err.println("❌ Formato de áudio não suportado: " + filePath);
      System.err.println("   Nota: Java suporta nativamente .wav, .aiff, .au");
      System.err.println("   Para MP3, é necessário converter para WAV ou usar biblioteca externa");
    } catch (IOException e) {
      System.err.println("❌ Erro ao ler arquivo de áudio: " + e.getMessage());
    } catch (LineUnavailableException e) {
      System.err.println("❌ Linha de áudio não disponível: " + e.getMessage());
    }
  }

  /**
   * Para a música atual
   */
  public void stopMusic() {
    if (currentClip != null && currentClip.isRunning()) {
      currentClip.stop();
      currentClip.close();
      System.out.println("⏹️ Música parada: " + currentTrack);
    }
    currentClip = null;
    currentTrack = null;
  }

  /**
   * Pausa a música
   */
  public void pauseMusic() {
    if (currentClip != null && currentClip.isRunning()) {
      currentClip.stop();
    }
  }

  /**
   * Resume a música
   */
  public void resumeMusic() {
    if (currentClip != null && !currentClip.isRunning()) {
      currentClip.start();
    }
  }

  /**
   * Define o volume (0.0 a 1.0)
   */
  public void setVolume(float volume) {
    this.volume = Math.max(0.0f, Math.min(1.0f, volume));

    if (currentClip != null && currentClip.isOpen()) {
      try {
        FloatControl volumeControl = (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);
        // Converter de 0.0-1.0 para decibéis
        float dB = (float) (Math.log(this.volume) / Math.log(10.0) * 20.0);
        volumeControl.setValue(dB);
      } catch (Exception e) {
        System.err.println("⚠️ Não foi possível ajustar o volume: " + e.getMessage());
      }
    }
  }

  /**
   * Obtém o volume atual
   */
  public float getVolume() {
    return volume;
  }

  /**
   * Verifica se há música tocando
   */
  public boolean isPlaying() {
    return currentClip != null && currentClip.isRunning();
  }

  /**
   * Limpa recursos ao fechar o jogo
   */
  public void cleanup() {
    stopMusic();
    System.out.println("🎵 MusicManager finalizado");
  }
}
