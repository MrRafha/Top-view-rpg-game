package com.rpggame.world;

import java.io.File;
import java.io.InputStream;

/**
 * Utilitário para resolver caminhos de recursos independente da estrutura de
 * diretórios
 * Suporta tanto desenvolvimento (arquivos externos) quanto JAR (recursos
 * internos)
 */
public class ResourceResolver {

    /**
     * Resolve o caminho correto para recursos baseado na estrutura de diretórios
     */
    public static String getResourcePath(String relativePath) {
        // Primeiro tentar como recurso do classpath (funciona no JAR)
        InputStream resourceStream = ResourceResolver.class.getClassLoader().getResourceAsStream(relativePath);
        if (resourceStream != null) {
            try {
                resourceStream.close();
            } catch (Exception e) {
                // Ignorar erro de fechamento
            }
            return relativePath; // Retornar path original para uso com getResourceAsStream
        }

        // Tentar primeiro o caminho direto (desenvolvimento)
        File direct = new File(relativePath);
        if (direct.exists()) {
            return relativePath;
        }

        // Tentar dentro da pasta resources (executável)
        File inResources = new File("resources/" + relativePath);
        if (inResources.exists()) {
            return "resources/" + relativePath;
        }

        // Tentar voltar um nível e acessar (compatibilidade)
        File upOne = new File("../" + relativePath);
        if (upOne.exists()) {
            return "../" + relativePath;
        }

        // Retornar caminho direto como fallback
        return relativePath;
    }

    /**
     * Verifica se um arquivo de recurso existe usando os caminhos possíveis
     */
    public static boolean resourceExists(String relativePath) {
        // Verificar se existe como recurso do classpath primeiro
        InputStream resourceStream = ResourceResolver.class.getClassLoader().getResourceAsStream(relativePath);
        if (resourceStream != null) {
            try {
                resourceStream.close();
            } catch (Exception e) {
                // Ignorar erro de fechamento
            }
            return true;
        }

        // Fallback para verificação de arquivo
        return new File(getResourcePath(relativePath)).exists();
    }

    /**
     * Retorna um File object para o recurso resolvido
     */
    public static File getResourceFile(String relativePath) {
        return new File(getResourcePath(relativePath));
    }
}