package com.mp3player.model;

import com.mp3player.model.MediaInfo.Property;

public class MediaInfoEvent {
	private MediaInfo info;
	private Property property;
	private String customProperty;
	private Object newValue;

	public MediaInfoEvent(MediaInfo info, Property property, String customProperty, Object newValue) {
		this.info = info;
		this.property = property;
		this.customProperty = customProperty;
		this.newValue = newValue;
	}

	public MediaInfo getInfo() {
		return info;
	}

	public Property getProperty() {
		return property;
	}

	public String getCustomProperty() {
		return customProperty;
	}

	public boolean wasCustomPropertyChanged() {
		return customProperty != null;
	}

	public boolean wasMainPropertyChanged() {
		return property != null;
	}

	public Object getNewValue() {
		return newValue;
	}

}
