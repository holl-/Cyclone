/**
 * This package contains the serializable data containers that fully specify the state of the player.
 * These data are can be sent over network to synchronize different instances of the application.
 *
 * Shared data objects:
 * - PlaylistPlayerData: holds playlist and related properties (not vital, could be replaced by other algorithm)
 * - Program: holds instructions for PlaybackEngine
 * - PlaybackStatus[]  PlaybackEngine feeds current information here
 * - Speaker[]
 */
package player.model.data;