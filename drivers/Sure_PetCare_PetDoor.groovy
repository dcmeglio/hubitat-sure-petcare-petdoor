/**
 *  Sure PetCare Pet Door Connect
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
 *  08.10.2019 - v1.2.1b - Change lock behaviour to 'Pet In' rather than lock both ways
 *  13.09.2019 - v1.2.1 - Add curfew status tile
 *  10.09.2019 - v1.2 - Add button controls to change lock status
 *  10.09.2019 - v1.1b - Improve API call efficiency
 *  09.09.2019 - v1.1 - Added Keep Pet In option for Dual Scan devices
 *  06.09.2019 - v1.0 - Initial Version
 */
 

metadata {
	definition (name: "Sure PetCare Pet Door Connect", namespace: "alyc100", author: "Alex Lee Yuk Cheung") {
		capability "Polling"
		capability "Refresh"
        capability "Actuator"
        capability "Battery"
		capability "Lock"
		capability "Sensor"
        
        attribute "network","string"
        
        command "toggleLockMode"
        command "setLockMode", ["string"]
        command "setLockModeToBoth"
        command "setLockModeToIn"
        command "setLockModeToOut"
        command "setLockModeToNone"
	}
}

// handle commands
def installed() {
    sendEvent(name: "checkInterval", value: 1 * 60 * 60, data: [protocol: "cloud"], displayed: false)
}

def updated() {
    sendEvent(name: "checkInterval", value: 1 * 60 * 60, data: [protocol: "cloud"], displayed: false)
}

def poll() {
	log.debug "Executing 'poll'"
	
    if (!state.statusRespCode || state.statusRespCode != 200) {
		log.error("Unexpected result in poll(): [${state.statusRespCode}] ${state.statusResponse}")
		return []
	}
    def response = state.statusResponse.data.devices
    def flap = response.find{device.deviceNetworkId.toInteger() == it.id}
    sendEvent(name: 'product_id', value: flap.product_id)
    sendEvent(name: "serial_number", value: flap.serial_number)
    sendEvent(name: "mac_address", value: flap.mac_address)
    sendEvent(name: "created_at", value: flap.created_at)
    sendEvent(name: "updated_at", value: flap.updated_at)
    def curfewStatus
    if (flap.control.curfew && !flap.control.curfew.isEmpty()) {
    	curfewStatus = "A curfew is activated on this device between ${flap.control.curfew[0].lock_time} and ${flap.control.curfew[0].unlock_time}."
    } else {
    	curfewStatus = "A curfew is not enabled on this device."
    }
    sendEvent(name: "curfewStatus", value: curfewStatus)
    def lockMode
    switch (flap.status.locking.mode) {
    	case 0:
            lockMode = "none"
       		break;
        case 1:
        	lockMode = "in"
            break;
        case 2:
        	lockMode = "out"
            break;
        default:
        	lockMode = "both"
			break;
    }
    if (lockMode == "none" || lockMode == "out") {
    	 sendEvent(name: "lock", value: "unlocked")
    } else {
    	 sendEvent(name: "lock", value: "locked")
    }
    sendEvent(name: "lockMode", value: lockMode)
    if (flap.status.online) {
    	sendEvent(name: 'network', value: "Connected" as String)
    } else {
    	sendEvent(name: 'network', value: "Pending" as String)
    }
    def batteryPercent = getBatteryPercent(flap.status.battery)
    sendEvent(name: 'battery', value: batteryPercent)
    sendEvent(name: 'device_rssi', value: flap.status.signal.device_rssi)
    sendEvent(name: 'hub_rssi', value: flap.status.signal.hub_rssi)
}

def toggleLockMode() {
	log.debug "Executing 'toggleLockMode'"
    def lockMode
	if (device.currentState("lockMode").getValue() == "both") { 
    	lockMode = "in"
    } else if (device.currentState("lockMode").getValue() == "in")  {
    	lockMode = "out"
    } else if (device.currentState("lockMode").getValue() == "out")  {
    	lockMode = "none"
    } else {
    	lockMode = "both"
    }
    setLockMode(lockMode)
}

def setLockMode(mode) {
	log.debug "Executing 'setLockMode' with mode ${mode}"
	def modeValue
	switch (mode) {
    	case "none":
            modeValue = 0
       		break;
        case "in":
        	modeValue = 1
            break;
        case "out":
        	modeValue = 2
            break;
        case "both":
        	modeValue = 3
			break;
        default:
        	log.error("Unsupported lock mode: [${mode}]")
        	return []
    }
	def body = [
    	locking: modeValue
    ]
	def resp = parent.apiPUT("/api/device/" + device.deviceNetworkId + "/control", body)
    sendEvent(name: "lockMode", value: mode)
    runIn(2, "updateStatusAndRefresh")
}

def lock() {
	log.debug "Executing 'lock'"
	setLockMode("in")
}

def unlock() {
	log.debug "Executing 'unlock'"
	setLockMode("none")
}

def getBatteryPercent(voltage) {
	log.debug "Executing 'getBatteryPercent'"
	def percentage
	switch (voltage) {
    	case voltage < 4:
            percentage = 1
       		break;
        case voltage < 4.5:
            percentage = 25
       		break;
        case voltage < 5:
            percentage = 50
       		break;
        case voltage < 5.5:
            percentage = 75
       		break;
        default:
            percentage = 100
       		break;
    }
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

def setLockModeToBoth() {
	setLockMode("both")
}

def setLockModeToIn() {
	setLockMode("in")
}

def setLockModeToOut() {
	setLockMode("out")
}

def setLockModeToNone() {
	setLockMode("none")
}