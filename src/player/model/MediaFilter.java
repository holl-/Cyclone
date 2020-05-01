package player.model;

import java.io.Serializable;
import java.util.function.ToDoubleFunction;

public interface MediaFilter extends ToDoubleFunction<MediaInfo>, Serializable {

}
