/**
 *  Copyright 2015 Aaron Paquette
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
 *  Light My Fire
 *
 *  Author: Aaron Paquette
 */
definition(
    name:"Light My Fire",
    namespace: "aapocketz",
    author: "Aaron Paquette",
    description: "Monitor the temperature and when it drops below your setting turn on a fireplace",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	section("When the temperature drops below...") {
		input "temperature1", "number", title: "Temperature?"
	}
	section("Turn on a heater...") {
		input "switch1", "capability.switch", required: false
	}
}

def installed() {
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def updated() {
	unsubscribe()
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def tooCold = temperature1
	def mySwitch = settings.switch1
    
	if (evt.doubleValue <= tooCold) {
		log.debug "Temperature dropped below $tooCold: activating $mySwitch"
		switch1?.on()
	}
    
}