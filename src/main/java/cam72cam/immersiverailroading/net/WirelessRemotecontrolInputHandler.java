package cam72cam.immersiverailroading.net;

import java.util.UUID;

public class WirelessRemotecontrolInputHandler {

    private static UUID linkedUUID = null;

    // set UUID
    public static void setTarget(UUID uuid) {
        linkedUUID = uuid;

    }

  
    
    // get UUID
	public static UUID getTarget() {
		
		return linkedUUID;
	}
	
	
	
	
}
