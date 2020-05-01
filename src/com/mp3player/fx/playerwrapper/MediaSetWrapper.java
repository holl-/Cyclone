package com.mp3player.fx.playerwrapper;

import com.mp3player.model.MediaInfo;
import com.mp3player.model.MediaSelectionListener;
import com.mp3player.model.MediaSet;
import com.mp3player.model.MediaSetEvent;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MediaSetWrapper {
	private MediaSet mediaSet;


	private ObservableList<MediaInfo> items;
	public ObservableList<MediaInfo> getItems() { return items; }


	private BooleanProperty working;
	public boolean isWorking() { return working.get(); }
	public ReadOnlyBooleanProperty workingProperty() { return working; }


	public MediaSetWrapper(MediaSet set) {
		mediaSet = set;

		items = FXCollections.observableArrayList();
		items.addAll(mediaSet.getItems());

		working = new SimpleBooleanProperty(mediaSet.isWorking());

		mediaSet.addMediaSelectionListener(new MediaSelectionListener() {

			@Override
			public void onWorkingChanged(MediaSetEvent e) {
				Platform.runLater(() -> working.set(mediaSet.isWorking()));
			}

			@Override
			public void onRemoved(MediaSetEvent e) {
				Platform.runLater(() -> items.removeAll(e.getSublist()));
			}

			@Override
			public void onAdded(MediaSetEvent e) {
				Platform.runLater(() -> items.addAll(e.getIndex(), e.getSublist()));
			}
		});
	}


	public MediaSet getMediaSet() {
		return mediaSet;
	}


}
