/**
 *  Improved Mobile Presence
 *
  *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
metadata {
	definition (name: "Improved Mobile Presence", namespace: "iluvsporks", author: "iluvsporks") {
		capability "Presence Sensor"
        capability "Sensor"
        
        attribute "currentLocation", "String"
        attribute "previousLocation", "String"
        
        command "checkPresence", ["boolean"]
        command "setPresence",["boolean","string"]
	}


	simulator {
    	standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
            state "present", labelIcon:"st.presence.tile.present", backgroundColor:"#00a0dc"
            state "not present", labelIcon:"st.presence.tile.not-present", backgroundColor:"#ffffff"
        }
		status "present":  "set-present"
        status "not present": "set-away"
	}

	tiles {
        standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
            state "present", labelIcon:"st.presence.tile.present", backgroundColor:"#00a0dc"
            state "not present", labelIcon:"st.presence.tile.not-present", backgroundColor:"#ffffff"
        }
        valueTile("location", "device.currentLocation", width: 2, height: 2) {
        	state "Home", label: '${currentValue}', backgroundColor:"#00a0dc"
            state "default", label: '${currentValue}', backgroundColor:"#ffffff"
		}
        main "location"
        details(["presence"])
    }
    
    preferences {
    	input "timeout", "number", title: "Presence timeout", description: "Time before leaving a location is reported (minutes)"
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	if(description == "set-present")
    	setPresence(true);
	if(description == "set-away")
    	setPresence(false);
}

def installed() {
	configure()
}

def updated() {
	configure()
}

def configure() {
	log.debug "Running config with settings: ${settings}"
	runEvery15Minutes(checkPresence);
    checkPresence(false);
}

def checkPresence(){
	checkPresence(false);
}

// Used after timeout is complete
def forceUpdate(){
	checkPresence(true);
}

def checkPresence(boolean force){
	log.debug "Checking presence"
    def params = [
    	uri: "https://st.callahtech.com/detailedpresence?id=${device.getDeviceNetworkId()}"
	]
	try {
	    httpGet(params) { resp ->
            log.debug "Recieved ${resp.data} from server"
            log.debug "Current Location: " + device.currentValue("currentLocation");
            log.debug "Previous Location: " + device.currentValue("previousLocation");
	    	log.debug "response data: ${resp.data}"
            if(resp.data.error) log.error('Error checking presence - ' + resp.data.message);
            else setPresence(resp.data.present, resp.data.location, force);
	    }
	} catch (e) {
	    log.error "Error setting presence: $e"
		setPresence(false, "Away", force);
	}
}

def setPresence(boolean present, String location){
	setPresence(present, location, false);
}

def setPresence(boolean present, String location, boolean force){
	log.debug "setPresence(" + present + ")"
    if(location != device.currentValue("currentLocation") || (present ? "present":"not present") != device.currentValue("presence")){
    	if(timeout && timeout > 0 && location == "Away" && !force){
        	log.debug("Delaying update by ${timeout} minute(s)");
            // Schedule update for time defined in timeout
        	runIn(timeout * 60, forceUpdate);
            return;
        } else if(timeout && timeout > 0){
        	// Ensure there are no pending events if a timeout is set
            unschedule();
        }
		if(present)
	   		sendEvent(displayed: true,  isStateChange: true, name: "presence", value: "present", descriptionText: "$device.displayName has arrived at " + location)
		else {
        	if(location == "Away")
	    		sendEvent(displayed: true,  isStateChange: true, name: "presence", value: "not present", descriptionText: "$device.displayName has left " + device.currentValue("currentLocation"))
            else
                sendEvent(displayed: true,  isStateChange: true, name: "presence", value: "not present", descriptionText: "$device.displayName has arrived at " + location)
        }
        setPresenceLocation(location);
        log.debug "Presence set"
	}
}

def setPresenceLocation(String location){
	if(location != device.currentValue("currentLocation")){
		log.debug "Setting location to: " + location
		sendEvent(name: "previousLocation", value: device.currentValue("currentLocation"), isStateChange: true, displayed: false)
		sendEvent(name: "currentLocation", value: location, isStateChange: true, displayed: false)
		log.debug "Current location: " + device.currentValue("currentLoaction")
		log.debug "Previous location: " + device.currentValue("previousLocation")
    }
}