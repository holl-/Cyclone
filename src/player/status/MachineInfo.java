package player.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import vdp.Conflict;
import vdp.Distributed;
import vdp.Peer;

public class MachineInfo extends Distributed {
	private static final long serialVersionUID = 8986346914415798967L;


	private List<Speaker> speakers;



	public static String id(Peer peer) {
		return "machineinfo-"+peer.getID();
	}

	public static Optional<String> getPeer(Distributed d) {
		if(d.getID().startsWith("machineinfo-")) {
			return Optional.of(d.getID().substring("machineinfo-".length()));
		}
		else return Optional.empty();
	}


	public MachineInfo(Peer peer, List<Speaker> speakers) {
		super(id(peer), false, true);
		this.speakers = new ArrayList<>(speakers);
	}

	@Override
	public Distributed resolveConflict(Conflict conflict) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Speaker> getSpeakers() {
		return Collections.unmodifiableList(speakers);
	}
}
