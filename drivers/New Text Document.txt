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
        capability "Health Check"
        
        attribute "network","string"
        
        command "toggleLedMode"
        command "setLedMode", ["string"]
	}

	tiles(scale: 2) {
    	standardTile("hubname", "device.hubname", width: 6, height: 4) {
        		state "default", label:"PetCare Hub", inactivelabel:true, icon:"https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/surepetcare-hub.png", backgroundColor: "#cccccc"
        }
        valueTile("serial_number", "device.serial_number", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Serial Number:\n${currentValue}'
		}
        
        valueTile("mac_address", "device.mac_address", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'MAC Address:\n${currentValue}'
		}
        valueTile("created_at", "device.created_at", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Created at:\n${currentValue}'
		}
        valueTile("updated_at", "device.updated_at", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Updated at:\n${currentValue}'
		}
        valueTile("firmware", "device.firmware", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Firmware Version:\n${currentValue}'
		}
        valueTile("hardware", "device.hardware", decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Hardware Version:\n${currentValue}'
		}
        
        standardTile("network", "device.network", width: 2, height: 2, inactiveLabel: false, canChangeIcon: false) {
			state ("default", label:'unknown', icon: "st.unknown.unknown.unknown")
			state ("Connected", label:'Online', icon: "st.Health & Wellness.health9", backgroundColor: "#79b821")
			state ("Pending", label:'Pending', icon: "st.Health & Wellness.health9", backgroundColor: "#ffa500")
			state ("Not Connected", label:'Offline', icon: "st.Health & Wellness.health9", backgroundColor: "#bc2323")
		}
        
        standardTile("ledMode", "device.ledMode", inactiveLabel: false, width: 2, height: 2, decoration: "flat") {
			state("bright", label:'LED Bright', action:"toggleLedMode", icon:"https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/devicetypes/alyc100/surepetcare-hub-bright.png", nextState: "off")
            state("dim", label:'LED Dim', action:"toggleLedMode", icon:"https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/devicetypes/alyc100/surepetcare-hub-dim.png", nextState: "bright")
            state("off", label:'LED Off', action:"toggleLedMode", icon:"https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/devicetypes/alyc100/surepetcare-hub-off.png", nextState: "dim")
            
		}
        
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        
		main (["hubname"])
		details(["hubname", "serial_number", "mac_address", "created_at", "updated_at", "hardware", "firmware", "ledMode", "network", "refresh"])
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
	log.debug "Executing 'poll'"
	
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
	log.debug "Executing 'toggleLedMode'"
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
	log.debug "Executing 'setLedMode' with mode ${mode}"
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
	log.debug "Executing 'refresh'"
	poll()    
}

def updateStatusAndRefresh() {
	log.debug "Executing 'updateStatusAndRefresh'"
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