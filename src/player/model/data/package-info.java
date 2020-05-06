/**
 * This package contains the serializable data containers that fully specify the state of the player.
 * These data are can be sent over network to synchronize different instances of the application.
 *
 * Shared data objects:
 * - PlayerData.X: holds playlist and related properties (specific to PlaylistPlayer)
 * - Task[]: holds instructions for PlaybackEngine
 * - TaskStatus[]  PlaybackEngine feeds current information here
 * - Speaker[]
 */
package player.model.data;