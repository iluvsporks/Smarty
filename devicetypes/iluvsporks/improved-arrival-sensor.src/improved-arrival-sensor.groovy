import groovy.json.JsonOutput

/**
 *  Improved Arrivial Sensor
 *
 *  Arrival Sensor HA with added functionality to Activate and Inactivate
 *  Originally designed for using an Arrival Sensor as a Guest Key
 *
 */
metadata {
    definition (name: "Improved Arrival Sensor", namespace: "iluvsporks", author: "iluvsporks") {
        capability "Tone"
        capability "Actuator"
        capability "Presence Sensor"
        capability "Sensor"
        capability "Battery"
        capability "Configuration"
        capability "Health Check"
        capability "Switch"
        
        command "enable"
        command "disable"

        fingerprint inClusters: "0000,0001,0003,000F,0020", outClusters: "0003,0019",
                        manufacturer: "SmartThings", model: "tagv4", deviceJoinName: "Arrival Sensor"
    }

    preferences {

        section {
            input "checkInterval", "enum", title: "Presence timeout (minutes)", description: "Tap to set",
                    defaultValue:"2", options: ["2", "3", "5"], displayDuringSetup: false
        }
    }

    tiles {
        standardTile("presence", "device.presence", width: 2, height: 3, canChangeBackground: true) {
            state "present", labelIcon:"st.presence.tile.present", backgroundColor:"#00a0dc"
            state "not present", labelIcon:"st.presence.tile.not-present", backgroundColor:"#ffffff"
        }
        standardTile("beep", "device.beep", decoration: "flat") {
            state "beep", label:'', action:"tone.beep", icon:"st.secondary.beep", backgroundColor:"#ffffff"
        }
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false) {
            state "battery", label:'${currentValue}% battery', unit:""
        }
		standardTile("guestKey", "device.switch", decoration: "flat") {
        	state "on", label:'ACTIVE', action:"switch.off", icon: "st.presence.tile.presence-default", backgroundColor:"#00a0dc"
        	state "off", label:'INACTIVE', action:"switch.on", icon: "st.presence.tile.presence-default", backgroundColor:"#ffffff"
        }
        
        main "presence"
        details(["presence", "battery", "beep","guestKey"])
    }
}

def updated() {
    startTimer()
}

def installed() {
    // Arrival sensors only goes OFFLINE when Hub is off
    sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "zigbee", scheme:"untracked"]), displayed: false)
}

def configure() {
    def cmds = zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020) + zigbee.batteryConfig(20, 20, 0x01)
    log.debug "configure -- cmds: ${cmds}"
    return cmds
}

def beep() {
    log.debug "Sending Identify command to beep the sensor for 5 seconds"
    return zigbee.command(0x0003, 0x00, "0500")
}

def parse(String description) {    
	state.lastCheckin = now()

	//Only check for arrival sensor presence if indicated as active
    if (device.currentValue("switch") == "on") {
    	handlePresenceEvent(true)
    }

    if (description?.startsWith('read attr -')) {
        handleReportAttributeMessage(description)
    }

    return []
    
}

private handleReportAttributeMessage(String description) {
    def descMap = zigbee.parseDescriptionAsMap(description)
    if (descMap.clusterInt == 0x0001 && descMap.attrInt == 0x0020) {
        handleBatteryEvent(Integer.parseInt(descMap.value, 16))
    }
}

/**
 * Create battery event from reported battery voltage.
 *
 * @param volts Battery voltage in .1V increments
 */
private handleBatteryEvent(volts) {
	def descriptionText
    if (volts == 0 || volts == 255) {
        log.debug "Ignoring invalid value for voltage (${volts/10}V)"
    }
    else {
        def batteryMap = [28:100, 27:100, 26:100, 25:90, 24:90, 23:70,
                          22:70, 21:50, 20:50, 19:30, 18:30, 17:15, 16:1, 15:0]
        def minVolts = 15
        def maxVolts = 28

        if (volts < minVolts)
            volts = minVolts
        else if (volts > maxVolts)
            volts = maxVolts
        def value = batteryMap[volts]
        if (value != null) {
            def linkText = getLinkText(device)
            descriptionText = '{{ linkText }} battery was {{ value }}'
            def eventMap = [
                name: 'battery',
                value: value,
                descriptionText: descriptionText,
                translatable: true
            ]
            log.debug "Creating battery event for voltage=${volts/10}V: ${linkText} ${eventMap.name} is ${eventMap.value}%"
            sendEvent(eventMap)
        }
    }
}

private handlePresenceEvent(present) { 
    def wasPresent = device.currentState("presence")?.value == "present"
    if (!wasPresent && present) {
        log.debug "Sensor is present"
        startTimer()
    } else if (!present) {
        log.debug "Sensor is not present"
        stopTimer()
    }
    def linkText = getLinkText(device)
    def descriptionText
    if ( present )
    	descriptionText = "{{ linkText }} has arrived"
    else
    	descriptionText = "{{ linkText }} has left"
    def eventMap = [
        name: "presence",
        value: present ? "present" : "not present",
        linkText: linkText,
        descriptionText: descriptionText,
        translatable: true
    ]
    log.debug "Creating presence event: ${device.displayName} ${eventMap.name} is ${eventMap.value}"
    sendEvent(eventMap)

}

private startTimer() {
    log.debug "Scheduling periodic timer"
    runEvery1Minute("checkPresenceCallback")
}

private stopTimer() {
    log.debug "Stopping periodic timer"
    unschedule()
}

def checkPresenceCallback() {
    def timeSinceLastCheckin = (now() - state.lastCheckin) / 1000
    def theCheckInterval = (checkInterval ? checkInterval as int : 2) * 60
    log.debug "Sensor checked in ${timeSinceLastCheckin} seconds ago"
    if (timeSinceLastCheckin >= theCheckInterval) {
        handlePresenceEvent(false)
    }
}

def enable() {
	on()
}

def disable() {
	off()
}

def on() {
    sendEvent(name: "switch", value: "on", descriptionText: "{{ linkText }} is Active")
    handlePresenceEvent(true)
    log.debug
}

def off() {
    sendEvent(name: "switch", value: "off", descriptionText: "{{ linkText }} is Inactive")
	sendEvent(name: "presence", value: "not present", displayed: false)
    log.debug
}