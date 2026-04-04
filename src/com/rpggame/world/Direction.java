package com.rpggame.world;

/**
 * Direções cardeais usadas para conectar salas no layout procedural.
 */
public enum Direction {
  NORTH,
  SOUTH,
  EAST,
  WEST;

  public Direction opposite() {
    switch (this) {
      case NORTH:
        return SOUTH;
      case SOUTH:
        return NORTH;
      case EAST:
        return WEST;
      case WEST:
      default:
        return EAST;
    }
  }
}