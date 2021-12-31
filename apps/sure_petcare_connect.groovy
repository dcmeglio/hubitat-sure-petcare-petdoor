/**
 *  Sure Petcare (Connect)
 *
 *  Copyright 2020 Alex Lee Yuk Cheung
 *  Ported to Hubitat by Dominick Meglio
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
 * 	VERSION HISTORY
 *  17.04.2020 - v1.2c - Notification workaround based on change on ST platform. 
 *  08.10.2019 - v1.2b - Rename lock mode labels.
 *  13.09.2019 - v1.2 - Curfew option on PetCare doors
 *  10.09.2019 - v1.1b - Improve API call efficiency
 *  09.09.2019 - v1.1 - Added Keep Pet In option on Pet device for Dual Scan PetCare cat flaps
 *					  - Added Pet Status with photo.
 *	07.09.2019 - v1.0.1 - Added Notification Framework
 *	06.09.2019 - v1.0 - Initial Version
 */
definition(
    name: "Sure PetCare (Connect)",
    namespace: "alyc100",
    author: "Alex Lee Yuk Cheung",
    description: "Connect your Sure PetCare devices to Hubitat.",
    category: "",
    iconUrl: "https://www.surepetcare.io/assets/images/onboarding/Sure_Petcare_Logo.png",
    iconX2Url: "https://www.surepetcare.io/assets/images/onboarding/Sure_Petcare_Logo.png",
    iconX3Url: "https://www.surepetcare.io/assets/images/onboarding/Sure_Petcare_Logo.png")
    singleInstance: true


preferences {
	page(name:"firstPage", title:"Sure PetCare Device Setup", content:"firstPage", install: true)
    page(name: "loginPAGE")
    page(name: "selectDevicePAGE")
    page(name: "curfewPAGE")
    page(name: "preferencesPAGE")
    page(name: "timeIntervalPAGE")
}

def apiURL(path = '/') 			 { return "https://app.api.surehub.io${path}" }
def deviceId()			 { return (Math.abs(new Random().nextInt() % 9999999999) + 1000000000).toString() }

def firstPage() {
	if (username == null || username == '' || password == null || password == '') {
		return dynamicPage(name: "firstPage", title: "", install: true, uninstall: true) {
			section {
    			headerSECTION()
                href("loginPAGE", title: null, description: authenticated() ? "Authenticated as " +username : "Tap to enter Sure PetCare account credentials", state: authenticated())
  			}
    	}
    }
    else
    {
        return dynamicPage(name: "firstPage", title: "", install: true, uninstall: true) {
			section {
            	headerSECTION()
                href("loginPAGE", title: null, description: authenticated() ? "Authenticated as " +username : "Tap to enter Sure PetCare account credentials", state: authenticated())
            }
            if (stateTokenPresent()) {    
                section ("Choose your Sure PetCare devices and pets:") {
					href("selectDevicePAGE", title: null, description: devicesSelected() ? getDevicesSelectedString() : "Tap to select Sure PetCare devices", state: devicesSelected())
        		}
                if (devicesSelected() == "complete") {
                	section ("Curfew Configuration:") {
						if (selectedPetDoorConnect && selectedPetDoorConnect.size() > 0) {
                			selectedPetDoorConnect.each() {
                           		def curfewEnabled = curfewSelected(it)
                            	href("curfewPAGE", params: ["deviceId": it], title: "Curfew for ${state.surePetCarePetDoorConnectDevices[it]}", description: settings["curfewEnabled#$it"] ? "${getCurfewString(it)}" : "Tap to configure curfew for ${state.surePetCarePetDoorConnectDevices[it]}", state: curfewEnabled, required: false, submitOnChange: false)
        					}
                		}
                        if (selectedDualScanCatFlapConnect && selectedDualScanCatFlapConnect.size() > 0) {
                			settings.selectedDualScanCatFlapConnect.each() {
                           		def curfewEnabled = curfewSelected(it)
                            	href("curfewPAGE", params: ["deviceId": it], title: "Curfew for ${state.surePetCareDualScanCatFlapConnectDevices[it]}", description: settings["curfewEnabled#$it"] ? "${getCurfewString(it)}" : "Tap to configure curfew for ${state.surePetCareDualScanCatFlapConnectDevices[it]}", state: curfewEnabled, required: false, submitOnChange: false)
        					}
                		}
                	}
                	section ("Notifications:") {
						href("preferencesPAGE", title: null, description: preferencesSelected() ? getPreferencesString() : "Tap to configure notifications", state: preferencesSelected())
        			}
                	section("Pets:") {
                		getChildDevices().findAll { it.typeName == "Sure PetCare Pet" }.each { childDevice -> 
							try {
                            	paragraph image: "${childDevice.getPhotoURL()}", "${childDevice.displayName} is ${childDevice.currentPresence}."
							}
        					catch (e) {
           						log.trace "Error checking status."
            					log.trace e
        					}
						}
                	}
                }
            } else {
            	section {
            		paragraph "There was a problem connecting to Sure PetCare. Check your user credentials and error logs in Hubitat web console.\n\n${state.loginerrors}"
           		}
           }
		   section {
			input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false
		   }
    	}
    }
}

def loginPAGE() {
	if (username == null || username == '' || password == null || password == '') {
		return dynamicPage(name: "loginPAGE", title: "Login", uninstall: false, install: false) {
    		section { headerSECTION() }
        	section { paragraph "Enter your Sure PetCare account credentials below to enable Hubitat and Sure PetCare integration." }
    		section {
    			input("username", "text", title: "Username", description: "Your Sure PetCare username (usually an email address)", required: true)
				input("password", "password", title: "Password", description: "Your Sure PetCare password", required: true, submitOnChange: true)
  			}   	
    	}
    }
    else {
    	getSurePetCareAccessToken()
        dynamicPage(name: "loginPAGE", title: "Login", uninstall: false, install: false) {
    		section { headerSECTION() }
        	section { paragraph "Enter your Sure PetCare account credentials below to enable Hubitat and Sure PetCare integration." }
    		section("Sure PetCare Credentials:") {
				input("username", "text", title: "Username", description: "Your Sure PetCare username (usually an email address)", required: true)
				input("password", "password", title: "Password", description: "Your Sure PetCare password", required: true, submitOnChange: true)	
			}    	
    	
    		if (stateTokenPresent()) {
        		section {
                	paragraph "You have successfully connected to Sure PetCare. Click 'Done' to select your Sure PetCare devices."
  				}
        	}
        	else {
        		section {
            		paragraph "There was a problem connecting to Sure PetCare. Check your user credentials and error logs in Hubitat web console.\n\n${state.loginerrors}"
           		}
        	}
        }
    }
}

def selectDevicePAGE() {
	updateLocations()
	dynamicPage(name: "selectDevicePAGE", title: "Devices", uninstall: false, install: false) {
  	section { headerSECTION() }
    if (devicesSelected() == null) {
    	section("Select your Location:") {
			input "selectedLocation", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/warmup-location.png", required:false, title:"Select a Location \n(${state.surePetCareLocations.size() ?: 0} found)", multiple:false, options:state.surePetCareLocations, submitOnChange: true
		}
    }
    else {
    	section("Your location:") {
        	paragraph (image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/warmup-location.png",
                  "Location: ${state.surePetCareLocations[selectedLocation]}\n(Remove all devices to change)")
        }
    }
    if (selectedLocation) {
    	updateDevices()
    	section("Select your devices:") {
			input "selectedHub", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/surepetcare-hub.png", required:false, title:"Select PetCare Hub Devices \n(${state.surePetCareHubDevices.size() ?: 0} found)", multiple:true, options:state.surePetCareHubDevices
			input "selectedPetDoorConnect", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/surepetcare-petdoor.png", required:false, title:"Select Pet Door Connect Devices \n(${state.surePetCarePetDoorConnectDevices.size() ?: 0} found)", multiple:true, options:state.surePetCarePetDoorConnectDevices
            input "selectedDualScanCatFlapConnect", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/surepetcare-catflap.png", required:false, title:"Select Dual Scan Cat Flap Connect Devices \n(${state.surePetCareDualScanCatFlapConnectDevices.size() ?: 0} found)", multiple:true, options:state.surePetCareDualScanCatFlapConnectDevices
		}
        
        section("Select your pets:") {
			input "selectedPet", "enum", image: "https://raw.githubusercontent.com/alyc100/SmartThingsPublic/master/smartapps/alyc100/surepetcare-pet.png", required:false, title:"Select Your Pet \n(${state.surePetPets.size() ?: 0} found)", multiple:true, options:state.surePetPets
			}
    }
  }
}

def curfewPAGE(params) {
	logDebug "PARAMS: $params"
    if (params?.containsKey("deviceId")) state.configDeviceId = params?.deviceId
	def deviceId = state.configDeviceId
	return dynamicPage(name: "curfewPAGE", title: "Curfew Settings", install: false, uninstall: false) { 
    	section() {
        	paragraph "Configure a curfew schedule for your Pet Door / Cat Flap."
        	input "curfewEnabled#$deviceId", "bool", title: "Enable Curfew?", required: false, defaultValue: false, submitOnChange: true
        }
            if (settings["curfewEnabled#$deviceId"]) {
            	
                section("Curfew Time:") {
					//Define time of day
                	paragraph "Set the time of your curfew."
                    def greyedOutTime = greyedOutTime(settings["starting#$deviceId"], settings["ending#$deviceId"])
                    def timeLabel = getTimeLabel(settings["starting#$deviceId"], settings["ending#$deviceId"])
                	href ("timeIntervalPAGE", params: ["deviceId": deviceId], title: "Set curfew during a certain time", description: timeLabel, state: greyedOutTime, refreshAfterSelection:true)
                }
                /*section("Notifications:") {
                paragraph "Turn on SmartSchedule notifications. You can configure specific recipients via Notification settings section."
                	input "ssNotification", "bool", title: "Enable SmartSchedule notifications?", required: false, defaultValue: true
              	} */ 
            }
        }
    
}

def timeIntervalPAGE(params) {
	def deviceId = params.deviceId
	return dynamicPage(name: "timeIntervalPAGE", title: "Only during a certain time", refreshAfterSelection:true) {
		section {
			input "starting#$deviceId", "time", title: "Starting", required: false
			input "ending#$deviceId", "time", title: "Ending", required: false
		}
	}
}

def preferencesPAGE() {
	dynamicPage(name: "preferencesPAGE", title: "Preferences", uninstall: false, install: false) {
    	section {
			input "pushDevices", "capability.notification", multiple: true, title: "Devices to notify:"
        }
    	section("Sure PetCare Notifications:") {			
			input "sendPetDoorLock", "bool", title: "Notify when pet doors are locked and unlocked?", required: false, defaultValue: false
            input "sendPetPresence", "bool", title: "Notify when pets arrive and leave?", required: false, defaultValue: false
            input "sendPetIndoors", "bool", title: "Notify when pets are set to indoors only or allowed outdoors?", required: false, defaultValue: false
            input "sendPetLooked", "bool", title: "Notify when pets look through door?", required: false, defaultValue: false
            input "sendDoorConnection", "bool", title: "Notify when pet doors lose connection?", required: false, defaultValue: false
		}        
    }
}

def headerSECTION() {
	return paragraph (image: "https://www.surepetcare.io/assets/images/onboarding/Sure_Petcare_Logo.png",
                  "${textVersion()}")
}

def preferencesSelected() {
	return (pushDevices != null && pushDevices.size() > 0) && (sendPetDoorLock || sendPetPresence || sendPetLooked || sendDoorConnection) ? "complete" : null
}

def getPreferencesString() {
	def listString = ""
	if (pushDevices && pushDevices.size() > 0) {
		listString += "Send Notification to "
		pushDevices.each { it -> listString += it.name + ", "}
		listString = listString[0..-3]
		listString += " when "
	}
    if (sendPetDoorLock) listString += "Pet Door Locks/Unlocks, "
    if (sendPetPresence) listString += "Pet Arrives/Leaves, "
    if (sendPetLooked) listString += "Pet Looks, "
    if (sendDoorConnection) listString += "Pet Door is Offline, "
    if (listString != "") listString = listString.substring(0, listString.length() - 2)
    return listString
}

def stateTokenPresent() {
	return state.surePetCareAccessToken != null && state.surePetCareAccessToken != ''
}

def authenticated() {
	return (state.surePetCareAccessToken != null && state.surePetCareAccessToken != '') ? "complete" : null
}

def devicesSelected() {
	return (selectedHub || selectedPetDoorConnect || selectedDualScanCatFlapConnect || selectedPet) ? "complete" : null
}

def getDevicesSelectedString() {
	if (state.surePetCareHubDevices == null ||
    	state.surePetCarePetDoorConnectDevices == null || 
        state.surePetCareDualScanCatFlapConnectDevices == null || 
        state.surePetPets == null) {
    	updateDevices()
  	}
	def listString = ""
    
	selectedHub.each { childDevice ->    
    	if (null != state.surePetCareHubDevices)
    		listString += "\n• " + state.surePetCareHubDevices[childDevice]
    }
  
	selectedPetDoorConnect.each { childDevice ->
      if (null != state.surePetCarePetDoorConnectDevices) 
           	listString += "\n• " + state.surePetCarePetDoorConnectDevices[childDevice]
	}
    
	selectedDualScanCatFlapConnect.each { childDevice ->
        if (null != state.surePetCareDualScanCatFlapConnectDevices)
            listString += "\n• " + state.surePetCareDualScanCatFlapConnectDevices[childDevice]
	}
    
    selectedPet.each { childDevice ->
        if (null != state.surePetPets)
            listString += "\n• " + state.surePetPets[childDevice]
	}
    // Returns the completed list, and trims the last carrige return
	return listString.trim()
}

def curfewSelected(deviceId) {
	return settings["curfewEnabled#$deviceId"] ? "complete" : null
}

def getCurfewString(deviceId) {
	def listString = ""
    listString += "The following curfew applies:\n"
    if (settings["curfewEnabled#$deviceId"]) listString += "• ${getTimeLabel(settings["starting#$deviceId"], settings["ending#$deviceId"])}\n" 
    return listString
}

private hhmm(time, fmt = "HH:mm") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
    if (getTimeZone()) { f.setTimeZone(location.timeZone ?: timeZone(time)) }
	f.format(t)
}

private timeToString(time, fmt) {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
    if (getTimeZone()) { f.setTimeZone(location.timeZone ?: timeZone(time)) }
	f.format(t)
}

def getTimeLabel(starting, ending){
	def timeLabel = "Tap to set"

    if(starting && ending){
    	timeLabel = "Between" + " " + hhmm(starting) + " "  + "and" + " " +  hhmm(ending)
    }
    else if (starting) {
		timeLabel = "Start at" + " " + hhmm(starting)
    }
    else if(ending){
    timeLabel = "End at" + hhmm(ending)
    }
	timeLabel
}

def greyedOutTime(starting, ending){
	def result = ""
    if (starting || ending) {
    	result = "complete"
    }
    result
}

// App lifecycle hooks

def installed() {
	logDebug "installed"
	initialize()
	// Check for new devices and remove old ones every 3 hours
	runEvery3Hours('updateDevices')
    // execute refresh method every minute
    runEvery1Minute('refreshDevices')
}

// called after settings are changed
def updated() {
	logDebug "updated"
    unsubscribe()
	initialize()
    unschedule('refreshDevices')
    runEvery1Minute('refreshDevices')
}

def uninstalled() {
	log.info("Uninstalling, removing child devices...")
	unschedule()
	removeChildDevices(getChildDevices())
}

private removeChildDevices(devices) {
	devices.each {
		deleteChildDevice(it.deviceNetworkId) // 'it' is default
	}
}

// called after Done is hit after selecting a Location
def initialize() {
	logDebug "initialize"
	if (selectedHub) {
		addHub()
	}
	if (selectedPetDoorConnect) {
		addPetDoorConnect()
	}
	if(selectedDualScanCatFlapConnect) {
        addDualScanCatFlapConnect()
    }
    
    if (selectedPet) {
    	addPet()
    }
 	runIn(10, 'refreshDevices') // Asynchronously refresh devices so we don't block
    
    //subscribe to events for notifications if activated
    if (preferencesSelected() == "complete") {
    	getChildDevices().each { childDevice -> 
        	if (childDevice.typeName == "Sure PetCare Pet") {
  				subscribe(childDevice, "petInfo", evtHandler, [filterEvents: false])
  				subscribe(childDevice, "presence", evtHandler, [filterEvents: false])
  				subscribe(childDevice, "indoorsOnly", evtHandler, [filterEvents: false])
    		}
    		if (childDevice.typeName == "Sure PetCare Pet Door Connect") {
    			subscribe(childDevice, "lockMode", evtHandler, [filterEvents: false])
                subscribe(childDevice, "network", evtHandler, [filterEvents: false])
    			//enable/disable curfew for pet door devices
                runIn(1, syncCurfewSettings, [data: [deviceId: childDevice.deviceNetworkId]])
    		}
    	}
    }
}

//enable/disable curfew for pet door devices
def syncCurfewSettings(data) {
	def deviceId = data.deviceId
    def body
    if (settings["curfewEnabled#$deviceId"]) {
    	def curfew = [
        	enabled: true,
            lock_time: "${hhmm(settings["starting#$deviceId"])}",
            unlock_time: "${hhmm(settings["ending#$deviceId"])}"
        ]
        def curfewList = []
		curfewList.add(curfew)
        body = [
    		curfew: curfewList
    	]
    } else {
    	def curfew = [
       		enabled: false,
         	lock_time: "${hhmm(settings["starting#$deviceId"])}",
            unlock_time: "${hhmm(settings["ending#$deviceId"])}"
        ]
        def curfewList = []
		curfewList.add(curfew)
        body = [
    		curfew: curfewList
    	]
    }
	apiPUT("/api/device/" + deviceId + "/control", body)
}

//Event Handler for Connect App
def evtHandler(evt) {
	def msg
    if (evt.isStateChange == true) {
    	if (evt.name == "petInfo") {
    		msg = evt.value
        	if (settings.sendPetLooked) messageHandler(msg, false)  
    	} else if (evt.name == "presence") {
    		msg = (evt.value == "present") ? "${evt.displayName} has arrived." : "${evt.displayName} is leaving."
        	if (settings.sendPetPresence) messageHandler(msg, false)  
    	} else if (evt.name == "indoorsOnly") {
    		if (evt.value != "empty") {
    			msg = (evt.value == "true") ? "${evt.displayName} is set to indoors only." : "${evt.displayName} is allowed outdoors."
        		if (settings.sendPetIndoors) messageHandler(msg, false)  
        	}
    	} else if (evt.name == "lockMode") {
    		msg = (evt.value == "both" || evt.value == "in") ? "${evt.displayName} is locked." : "${evt.displayName} is unlocked."
        	if (settings.sendPetDoorLock) messageHandler(msg, false)  
    	} else if (evt.name == "network") {
    		msg = (evt.value == "Connected") ? "${evt.displayName} is online." : "${evt.displayName} is offline."
       		if (settings.sendDoorConnection) messageHandler(msg, false)  
    	}
    }
}

def updateDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	def devices = devicesList()
  	state.surePetCareHubDevices = [:]
  	state.surePetCarePetDoorConnectDevices = [:]
  	state.surePetCareDualScanCatFlapConnectDevices = [:]

    def selectors = []
	devices.each { device ->
        logDebug "Identified: device ${device.id}: ${device.product_id}: ${device.household_id}: ${device.name}: ${device.serial_number}: ${device.mac_address}"
        selectors.add("${device.id}")
        
        //Hub
        if (device.product_id == 1) {
        	logDebug "Identified: ${device.name} Pet Care Hub"
            def value = "${device.name} PetCare Hub"
            def key = device.id
            state.surePetCareHubDevices["${key}"] = value

            //Update names of devices with PetCare
            def childDevice = getChildDevice("${device.id}")
            if (childDevice) {
            	//Update name of device if different.
            	if(childDevice.name != device.name + " PetCare Hub") {
            		childDevice.name = device.name + " PetCare Hub"
                	logDebug "Device's name has changed."
            	}
            }
        }
        //Pet Door Connect
        else if (device.product_id == 3) {
        	logDebug "Identified: ${device.name} Pet Door Connect"
            def value = "${device.name} Pet Door Connect"
            def key = device.id
            state.surePetCarePetDoorConnectDevices["${key}"] = value

            //Update names of devices with PetCare
            def childDevice = getChildDevice("${device.id}")
            if (childDevice) {
            	//Update name of device if different.
            	if(childDevice.name != device.name + " Pet Door Connect") {
            		childDevice.name = device.name + " Pet Door Connect"
                	logDebug "Device's name has changed."
            	}
            }
        }
        //Dual Scan Cat Flap Connect
        else if (device.product_id == 6) {
        	logDebug "Identified: ${device.name} Dual Scan Cat Flap Connect"
            def value = "${device.name} Dual Scan Cat Flap Connect"
            def key = device.id
            state.surePetCareDualScanCatFlapConnectDevices["${key}"] = value

            //Update names of devices with PetCare
            def childDevice = getChildDevice("${device.id}")
            if (childDevice) {
            	//Update name of device if different.
            	if(childDevice.name != device.name + " Dual Scan Cat Flap Connect") {
            		childDevice.name = device.name + " Dual Scan Cat Flap Connect"
                	logDebug "Device's name has changed."
            	}
            }
        }
    }
    
    if (!state.pets) {
		state.pets = [:]
	}
    def pets = petsList()
    state.surePetPets = [:]
    
    pets.each {pet ->
    	def species = (pet.species_id == 2) ? "dog" : "cat"
    	logDebug "Identified: ${pet.name} the ${species}"
        selectors.add("${pet.id}")
        def value = "${pet.name} the ${species}"
            def key = pet.id
            state.surePetPets["${key}"] = value

            //Update names of pets with PetCare
            def childDevice = getChildDevice("${pet.id}")
            if (childDevice) {
            	//Update name of device if different.
            	if(childDevice.name != pet.name + " the ${species}") {
            		childDevice.name = pet.name + " the ${species}"
                	logDebug "Pet's name has changed."
            	}
            }
    }
    
    //Remove devices if does not exist on the Sure PetCare platform
    getChildDevices().findAll { !selectors.contains("${it.deviceNetworkId}") }.each {
		log.info("Deleting ${it.deviceNetworkId}")
        try {
			deleteChildDevice(it.deviceNetworkId)
        } catch (hubitat.exception.NotFoundException e) {
        	log.info("Could not find ${it.deviceNetworkId}. Assuming manually deleted.")
        } catch (hubitat.exception.ConflictException ce) {
        	log.info("Device ${it.deviceNetworkId} in use. Please manually delete.")
        }
	} 
}

def updateLocations() {
	def locations = locationsList()
	state.surePetCareLocations = [:]
    
    def selectors = []
	locations.each { location ->
        logDebug "Identified: location ${location.id}: ${location.name}"
            selectors.add("${location.id}")
            def value = "${location.name}"
			def key = location.id
			state.surePetCareLocations["${key}"] = value
    }
}

def addHub() {
	updateDevices()
    
    selectedHub.each { device ->
    	def childDevice = getChildDevice("${device}")
        if (!childDevice && state.surePetCareHubDevices[device] != null) {
    		log.info("Adding device ${device}: ${state.surePetCareHubDevices[device]}")

        	def data = [
                	name: state.surePetCareHubDevices[device],
					label: state.surePetCareHubDevices[device]
				]
            childDevice = addChildDevice("alyc100", "Sure PetCare Hub", "$device", data)

			logDebug "Created ${state.surePetCareHubDevices[device]} with id: ${device}"
		} else {
			logDebug "found ${state.surePetCareHubDevices[device]} with id ${device} already exists"
		}
    }
}

def addPetDoorConnect() {
	updateDevices()
    
    selectedPetDoorConnect.each { device ->
    	def childDevice = getChildDevice("${device}")
        if (!childDevice && state.surePetCarePetDoorConnectDevices[device] != null) {
    		log.info("Adding device ${device}: ${state.surePetCarePetDoorConnectDevices[device]}")

        	def data = [
                	name: state.surePetCarePetDoorConnectDevices[device],
					label: state.surePetCarePetDoorConnectDevices[device]
				]
            childDevice = addChildDevice("alyc100", "Sure PetCare Pet Door Connect", "$device", data)

			logDebug "Created ${state.surePetCarePetDoorConnectDevices[device]} with id: ${device}"
		} else {
			logDebug "found ${state.surePetCarePetDoorConnectDevices[device]} with id ${device} already exists"
		}
    }
}

def addDualScanCatFlapConnect() {
	updateDevices()
    
    selectedDualScanCatFlapConnect.each { device ->
    	def childDevice = getChildDevice("${device}")
        if (!childDevice && state.surePetCareDualScanCatFlapConnectDevices[device] != null) {
    		log.info("Adding device ${device}: ${state.surePetCareDualScanCatFlapConnectDevices[device]}")

        	def data = [
                	name: state.surePetCareDualScanCatFlapConnectDevices[device],
					label: state.surePetCareDualScanCatFlapConnectDevices[device]
				]
            childDevice = addChildDevice("alyc100", "Sure PetCare Pet Door Connect", "$device", data)

			logDebug "Created ${state.surePetCareDualScanCatFlapConnectDevices[device]} with id: ${device}"
		} else {
			logDebug "found ${state.surePetCareDualScanCatFlapConnectDevices[device]} with id ${device} already exists"
		}
    }
}

def addPet() {
	updateDevices()
    
    selectedPet.each { device ->
    	def childDevice = getChildDevice("${device}")
        if (!childDevice && state.surePetPets[device] != null) {
    		log.info("Adding pet ${device}: ${state.surePetPets[device]}")

        	def data = [
                	name: state.surePetPets[device],
					label: state.surePetPets[device]
				]
            childDevice = addChildDevice("alyc100", "Sure PetCare Pet", "$device", data)

			logDebug "Created ${state.surePetPets[device]} with id: ${device}"
		} else {
			logDebug "found ${state.surePetPets[device]} with id ${device} already exists"
		}
    }
}

def refreshDevices() {
	logDebug "Executing refreshDevices..."
    if (atomicState.refreshCounter == null || atomicState.refreshCounter >= 10) {
    	atomicState.refreshCounter = 0
    } else {
    	atomicState.refreshCounter = atomicState.refreshCounter + 1
    }
    def resp = apiGET("/api/me/start")
	getChildDevices().each { device ->
    	device.setStatusRespCode(resp.status)
        device.setStatusResponse(resp.data)
    	if (device.typeName == "Sure PetCare Pet") {
        	logDebug "High Freq Refreshing device ${device.typeName}..."
			device.refresh()
        } else if (device.typeName == "Sure PetCare Pet Door Connect") {
        	logDebug "High Freq Refreshing device ${device.typeName}..."
			device.refresh()
        	//Update curfew status
            def flap = resp.data.data.devices.find{device.deviceNetworkId.toInteger() == it.id}
            if (flap.control.curfew && !flap.control.curfew.isEmpty()) {
				def curfewObject = flap.control.curfew[0] ?: flap.control.curfew
            	app.updateSetting("curfewEnabled#${device.deviceNetworkId}", [type: "bool", value: true]) 
                app.updateSetting("starting#${device.deviceNetworkId}", [type: "date", value: timeToString(curfewObject.lock_time, "yyyy-MM-dd'T'HH:mm:ss.SSSXX")]) 
                app.updateSetting("ending#${device.deviceNetworkId}", [type: "date", value: timeToString(curfewObject.unlock_time, "yyyy-MM-dd'T'HH:mm:ss.SSSXX")]) 
            } else {
            	app.updateSetting("curfewEnabled#${device.deviceNetworkId}", [type: bool, value: false]) 
            }
        } else if (atomicState.refreshCounter == 10) {
        	logDebug "Low Freq Refreshing device ${device.name} ..."
            try {
    			device.refresh()
        	} catch (e) {
        		//WORKAROUND - Catch unexplained exception when refreshing devices.
        		logResponse(e.response)
        	}
        } 
	}
}

def devicesList() {
	logErrors([]) {
    	def resp = apiGET('/api/household/' + selectedLocation + '/device')
		if (resp.status == 200) {
			return resp.data.data
		} else {
			log.error("Non-200 from device list call. ${resp.status} ${resp.data}")
			return []
		}
	}
}

def petsList() {
	logErrors([]) {
    
    	def body = [:]
		def resp = apiGET('/api/household/' + selectedLocation + '/pet')
		if (resp.status == 200) {
            logDebug resp.data.data
			return resp.data.data
		} else {
			log.error("Non-200 from location list call. ${resp.status} ${resp.data}")
			return []
		}
	}
}

def locationsList() {
	logErrors([]) {
    
    	def body = [:]
		def resp = apiGET('/api/household')
		if (resp.status == 200) {
            logDebug resp.data.data
			return resp.data.data
		} else {
			log.error("Non-200 from location list call. ${resp.status} ${resp.data}")
			return []
		}
	}
}

def getSurePetCareAccessToken() {  
	try {
    	def params = [
			uri: apiURL('/api/auth/login'),
        	contentType: 'application/json',
        	headers: [
              'Content-Type': 'application/json'
        	],
        	body: [
        		email_address: settings.username,
                password: settings.password,
                device_id: deviceId()  	
    		]
        ]

		state.cookie = ''

		httpPostJson(params) {response ->
			logDebug "Request was successful, $response.status"
			logDebug response.headers

        	state.cookie = response?.headers?.'Set-Cookie'?.split(";")?.getAt(0)
			logDebug "Adding cookie to collection: $cookie"
        	logDebug "auth: $response.data"
			logDebug "cookie: $state.cookie"
        	logDebug "sessionid: ${response.data.data.token}"

        	state.surePetCareAccessToken = response.data.data.token
        	// set the expiration to 5 minutes
			state.surePetCareAccessToken_expires_at = new Date().getTime() + 300000
            state.loginerrors = null
		}
    } catch (groovyx.net.http.HttpResponseException e) {
    	state.surePetCareAccessToken = null
        state.surePetCareAccessToken_expires_at = null
   		state.loginerrors = "Error: ${e.response.status}: ${e.response.data}"
    	logResponse(e.response)
		return e.response
    }
}

def apiPOST(path, body = [:]) {
	def bodyString = new groovy.json.JsonBuilder(body).toString()
	logDebug("Beginning API POST: ${apiURL(path)}, ${bodyString}")
    try {
    	httpPost(uri: apiURL(path), body: bodyString, headers: apiRequestHeaders(), requestContentType: "application/json" ) {
    		response ->
			logResponse(response)
			return response
        }
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}
}

def apiPUT(path, body = [:]) {
	def bodyString = new groovy.json.JsonBuilder(body).toString()
	logDebug("Beginning API PUT: ${apiURL(path)}, ${bodyString}")
    try {
    	httpPut(uri: apiURL(path), body: bodyString, headers: apiRequestHeaders(), requestContentType: "application/json") {
    		response ->
			logResponse(response)
			return response
        }
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}  catch (Exception e) {
		logResponse(e.response)
		return e.response
	}
}

def apiGET(path) {
	logDebug("Beginning API GET: ${apiURL(path)}")
    try {
    	httpGet(uri: apiURL(path), headers: apiRequestHeaders(), timeout: 90 ) {
    		response ->
			logResponse(response)
			return response
        }
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}
}

//Used by Pet device to get tagID indoors only status for household
def getTagStatus(tagID) {
	logDebug "Executing 'getTagStatus'"
	def result = "empty"
    def profileList = []
	getChildDevices().findAll { it.typeName == "Sure PetCare Pet Door Connect" }.each { childDevice -> 
    	if (childDevice.currentState("product_id").getValue().toInteger() == 6) {
        	def resp = apiGET("/api/device/" + childDevice.deviceNetworkId + "/tag/" + tagID.toString())
        	profileList.add(resp.data.data.profile)
        }
    }
    logDebug "Profile List of child devices ${profileList}"
    result = (profileList.size() > 0 && profileList.contains(2)) ? "false" : "true"
    return result
}

//Used by Pet device to set tag ID to indoors only for household
def setTagToIndoorsOnly(tagID) {
	logDebug "Executing 'setTagToIndoorsOnly'"
	getChildDevices().findAll { it.typeName == "Sure PetCare Pet Door Connect" }.each { childDevice -> 
    	if (childDevice.currentState("product_id").getValue().toInteger() == 6) {
        	def body = [
    			profile: 3
    		]
			def resp = apiPUT("/api/device/" + childDevice.deviceNetworkId + "/tag/" + tagID, body)
        }
    }
}

//Used by Pet device to set tag ID to allow outdoors for household
def setTagToOutdoors(tagID) {
	logDebug "Executing 'setTagToOutdoors'"
	getChildDevices().findAll { it.typeName == "Sure PetCare Pet Door Connect" }.each { childDevice -> 
    	if (childDevice.currentState("product_id").getValue().toInteger() == 6) {
        	def body = [
    			profile: 2
    		]
			def resp = apiPUT("/api/device/" + childDevice.deviceNetworkId + "/tag/" + tagID, body)
        }
    }
}

def getHouseholdID() {
	return selectedLocation
}

def messageHandler(msg, forceFlag) {
	logDebug "Executing 'messageHandler for $msg. Forcing is $forceFlag'"
    if (pushDevices != null) {
		pushDevices*.deviceNotification(msg)
	}
}

def getTimeZone() {
	def tz = null
	if(location?.timeZone) { tz = location?.timeZone }
	if(!tz) { log.warn "No time zone has been retrieved from Hubitat. Please try to open your HE location and press Save." }
	return tz
}

Map apiRequestHeaders() {
   return [ "Authorization": "Bearer $state.surePetCareAccessToken"	]
}

def logResponse(response) {
	logDebug "Status: ${response.status}"
	logDebug "Body: ${response.data}"
}

def logErrors(options = [errorReturn: null, logObject: log], Closure c) {
	try {
		return c()
	} catch (groovyx.net.http.HttpResponseException e) {
		options.logObject.error("got error: ${e}, body: ${e.getResponse().getData()}")
		if (e.statusCode == 401) { // token is expired
			state.remove("surePetCareAccessToken")
			options.logObject.warn "Access token is not valid"
		}
		return options.errorReturn
	} catch (java.net.SocketTimeoutException e) {
		options.logObject.warn "Connection timed out, not much we can do here"
		return options.errorReturn
	}
}



private def textVersion() {
    def text = "Sure PetCare (Connect)\nVersion: 1.2c\nDate: 17042020(1200)"
}

private def textCopyright() {
    def text = "Copyright © 2020 Alex Lee Yuk Cheung"
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}