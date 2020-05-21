/**
 *  Sure PetCare Hub
 *
 *  Copyright 2019 Alex Lee Yuk Cheung
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
 *  VERSION HISTORY
 *  10.09.2019 - v1.1b - Improve API call efficiency
 *  06.09.2019 - v1.0 - Initial Version
 */
 

metadata {
	definition (name: "Sure PetCare Hub", namespace: "alyc100", author: "Alex Lee Yuk Cheung") {
		capability "Polling"
		capability "Refresh"
        capability "Sensor"
        
        attribute "network","string"
		attribute "ledMode","string"
        
        command "toggleLedMode"
        command "setLedMode", ["string"]
	}

}

// handle commands
def installed() {
    sendEvent(name: "checkInterval", value: 48 * 60 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

def updated() {
    sendEvent(name: "checkInterval", value: 48 * 60 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

def poll() {
	parent.logDebug "Executing 'poll'"
	
    if (!state.statusRespCode || state.statusRespCode != 200) {
		log.error("Unexpected result in poll(): [${state.statusRespCode}] ${state.statusResponse}")
		return []
	}
    def response = state.statusResponse.data.devices
    def hub = response.find{device.deviceNetworkId.toInteger() == it.id}
    sendEvent(name: "hubname", value: hub.name)
    sendEvent(name: "serial_number", value: hub.serial_number)
    sendEvent(name: "mac_address", value: hub.mac_address)
    sendEvent(name: "created_at", value: hub.created_at)
    sendEvent(name: "updated_at", value: hub.updated_at)
    def ledMode
    switch (hub.status.led_mode) {
    	case 1:
            ledMode = "bright"
       		break;
        case 4:
        	ledMode = "dim"
            break;
        default:
        	ledMode = "off"
			break;
        }
    sendEvent(name: "ledMode", value: ledMode)
    if (hub.status.online) {
    	sendEvent(name: 'network', value: "Connected" as String)
    } else {
    	sendEvent(name: 'network', value: "Pending" as String)
    }
    sendEvent(name: "hardware", value: hub.status.version.device.hardware)
    sendEvent(name: "firmware", value: hub.status.version.device.firmware)
}

def toggleLedMode() {
	parent.logDebug "Executing 'toggleLedMode'"
    def ledMode
	if (device.currentState("ledMode").getValue() == "off") { 
    	ledMode = "dim"
    } else if (device.currentState("ledMode").getValue() == "dim")  {
    	ledMode = "bright"
    } else {
    	ledMode = "off"
    }
    setLedMode(ledMode)
    sendEvent(name: "ledMode", value: ledMode)
    runIn(2, "updateStatusAndRefresh")
}

def setLedMode(mode) {
	parent.logDebug "Executing 'setLedMode' with mode ${mode}"
	def modeValue
	switch (mode) {
    	case "bright":
            modeValue = 1
       		break;
        case "dim":
        	modeValue = 4
            break;
        default:
        	modeValue = 0
			break;
    }
	def body = [
    	led_mode: modeValue
    ]
	def resp = parent.apiPUT("/api/device/" + device.deviceNetworkId + "/control", body)
}

def refresh() {
	parent.logDebug "Executing 'refresh'"
	poll()    
}

def updateStatusAndRefresh() {
	parent.logDebug "Executing 'updateStatusAndRefresh'"
    def resp = parent.apiGET("/api/me/start")
    setStatusRespCode(resp.status)
    setStatusResponse(resp.data)
    refresh()
}

def setStatusRespCode(respCode) {
	state.statusRespCode = respCode
}

def setStatusResponse(respBody) {
	state.statusResponse = respBody
}