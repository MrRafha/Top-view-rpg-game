
package com.rpggame.world;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry central de templates usados no gerador procedural de mundo.
 */
public final class WorldTemplateRegistry {
  private WorldTemplateRegistry() {
  }

  public static Map<String, RoomTemplate> createDefaultTemplates() {
    // Mantemos LinkedHashMap para preservar ordem de cadastro e facilitar debug do
    // mundo gerado.
    Map<String, RoomTemplate> templates = new LinkedHashMap<>();

    // Village: saídas sul e oeste (portal para território e passagem secreta).
    RoomTemplate village = new RoomTemplate(
        "village",
        "maps/village.txt",
        "Vila da Praia",
        RoomType.SAFE,
        true,
        10,
        12,
        22)
        .addEntrance(Direction.SOUTH, 11, 24, "Portal Sul")
        .addEntrance(Direction.SOUTH, 12, 24, "Portal Sul")
        .addEntrance(Direction.WEST, 0, 15, "Passagem Secreta");
    templates.put(village.getId(), village);

    // Território hostil: retorno para a vila no norte.
    RoomTemplate goblinTerritories = new RoomTemplate(
        "goblin_territories",
        "maps/goblin_territories_25x25.txt",
        "Territórios Goblin",
        RoomType.HOSTILE,
        true,
        10,
        12,
        3)
        .addEntrance(Direction.NORTH, 11, 0, "Portal Norte")
        .addEntrance(Direction.NORTH, 12, 0, "Portal Norte");
    templates.put(goblinTerritories.getId(), goblinTerritories);

    // Área secreta: saída leste (retorna para a vila).
    RoomTemplate secretArea = new RoomTemplate(
        "secret_area",
        "maps/secret_area.txt",
        "Área Secreta",
        RoomType.SPECIAL,
        true,
        5,
        12,
        22)
        .addEntrance(Direction.EAST, 23, 15, "Saída da Área Secreta")
        .addEntrance(Direction.EAST, 24, 15, "Saída da Área Secreta");
    templates.put(secretArea.getId(), secretArea);

    // Bloco de salas neutras: são salas de travessia com entradas nos 4 lados.
    // Isso aumenta as chances de o gerador conseguir conectar o grafo sem becos sem
    // saída.
    RoomTemplate neutral1 = createNeutralTemplate(
        "neutral_1",
        "maps/neutral_1.txt",
        "Bosque Nebuloso",
        8);
    templates.put(neutral1.getId(), neutral1);

    RoomTemplate neutral2 = createNeutralTemplate(
        "neutral_2",
        "maps/neutral_2.txt",
        "Campos de Areia",
        8);
    templates.put(neutral2.getId(), neutral2);

    RoomTemplate neutral3 = createNeutralTemplate(
        "neutral_3",
        "maps/neutral_3.txt",
        "Ruínas Verdes",
        7);
    templates.put(neutral3.getId(), neutral3);

    RoomTemplate neutral4 = createNeutralTemplate(
        "neutral_4",
        "maps/neutral_4.txt",
        "Pântano Tranquilo",
        7);
    templates.put(neutral4.getId(), neutral4);

    RoomTemplate goblinVillage = new RoomTemplate(
        "goblin_village",
        "maps/goblin_village.txt",
        "Vila Goblin",
        RoomType.HOSTILE,
        true,
        11,
        12,
        12)
        .addEntrance(Direction.NORTH, 11, 0, "Saída Norte")
        .addEntrance(Direction.NORTH, 12, 0, "Saída Norte")
        .addEntrance(Direction.SOUTH, 11, 24, "Saída Sul")
        .addEntrance(Direction.SOUTH, 12, 24, "Saída Sul")
        .addEntrance(Direction.WEST, 0, 12, "Saída Oeste")
        .addEntrance(Direction.WEST, 0, 13, "Saída Oeste")
        .addEntrance(Direction.EAST, 24, 12, "Saída Leste")
        .addEntrance(Direction.EAST, 24, 13, "Saída Leste");
    templates.put(goblinVillage.getId(), goblinVillage);

    return templates;
  }

  private static RoomTemplate createNeutralTemplate(String id, String mapFile, String displayName, int weight) {
    // Convenção das salas neutras: spawn central e conectividade N/S/L/O para
    // encaixar em diferentes layouts.
    return new RoomTemplate(
        id,
        mapFile,
        displayName,
        RoomType.NEUTRAL,
        true,
        weight,
        12,
        12)
        .addEntrance(Direction.NORTH, 11, 0, "Saída Norte")
        .addEntrance(Direction.NORTH, 12, 0, "Saída Norte")
        .addEntrance(Direction.SOUTH, 11, 24, "Saída Sul")
        .addEntrance(Direction.SOUTH, 12, 24, "Saída Sul")
        .addEntrance(Direction.WEST, 0, 12, "Saída Oeste")
        .addEntrance(Direction.WEST, 0, 13, "Saída Oeste")
        .addEntrance(Direction.EAST, 24, 12, "Saída Leste")
        .addEntrance(Direction.EAST, 24, 13, "Saída Leste");
  }
}