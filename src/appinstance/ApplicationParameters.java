package appinstance;

import javafx.application.Application;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationParameters implements Serializable {
	private static final long serialVersionUID = -7512050182139140930L;

	private String applicationID;
	private Map<String, String> named;
	private List<String> raw;
	private List<String> unnamed;

	public ApplicationParameters(String applicationID, Application.Parameters parameters) {
		this.applicationID = applicationID;
		named = new HashMap<>(parameters.getNamed());
		raw = new ArrayList<>(parameters.getRaw());
		unnamed = new ArrayList<>(parameters.getUnnamed());
	}

	public ApplicationParameters(String applicationID, Map<String, String> named, List<String> raw,
			List<String> unnamed) {
		this.applicationID = applicationID;
		this.named = named;
		this.raw = raw;
		this.unnamed = unnamed;
	}

	public String getApplicationID() {
		return applicationID;
	}

	public Map<String, String> getNamed() {
		return named;
	}

	public List<String> getRaw() {
		return raw;
	}

	public List<String> getUnnamed() {
		return unnamed;
	}

}
