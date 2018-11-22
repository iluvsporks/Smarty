/**
 *  Copyright 2015 SmartThings
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
 *	Alpha - Lutron Button Conroller
 */
metadata {
	definition (name: "Lutron Button Controller", namespace: "smartthings", author: "smartthings") {
		capability "Actuator"

//		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0402,0B05", outClusters: "0003,0006,0008,0019"
		fingerprint profileId: "C05E", inClusters: "0000,1000,FF00,FC44", outClusters: "1000,0003,0006,0008,0004,0005,0000,FF00"
	}

	// UI tile definitions
	tiles {
		standardTile("button", "device.button", width: 2, height: 2) {
			state "default", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
		}
		main "button"
		details(["button"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "Parse description $description"
	def descMap = zigbee.parseDescriptionAsMap(description)
	log.info "map is ${descMap}"
}