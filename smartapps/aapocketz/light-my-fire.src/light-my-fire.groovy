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
        name: "Light My Fire",
        namespace: "aapocketz",
        author: "Aaron Paquette",
        description: "turn on a heater when its cold and there is motion at night",
        category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)


preferences {
    page(name: "configurations")
    page(name: "options")

    page(name: "timeIntervalInput", title: "Only during a certain time...") {
        section {
            input "starting", "time", title: "Starting", required: false
            input "ending", "time", title: "Ending", required: false
        }
    }
}

def configurations() {
    dynamicPage(name: "configurations", title: "Configurations...", uninstall: true, nextPage: "options") {
        section(title: "Turn ON heaters on movement when...") {
            input "sun", "bool", title: "Between sunset and sunrise?", required: true
        }
        section(title: "More options...", hidden: hideOptionsSection(), hideable: true) {
            def timeLabel = timeIntervalLabel()
            href "timeIntervalInput", title: "Only during a certain time:", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
            input "days", "enum", title: "Only on certain days of the week:", multiple: true, required: false, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            input "modes", "mode", title: "Only when mode is:", multiple: true, required: false
        }
        section("Assign a name") {
            label title: "Assign a name", required: false
        }
    }
}

def options() {
    if (sun == true) {
        dynamicPage(name: "options", title: "Heaters will turn ON on movement when it is cold and between sunset and sunrise...", install: true, uninstall: true) {
            section("Control these heater(s)...") {
                input "heaters", "capability.switch", title: "Heater(s)?", multiple: true, required: false
            }
            section("Control these dimmer(s)...") {
                input "dimmers", "capability.switchLevel", title: "Dimmer(s)?", multiple: true, required: false
                input "level", "number", title: "How bright?", required: false, description: "0% to 100%"
            }
            section("Turning ON when it's cold and there's movement...") {
                input "motionSensor", "capability.motionSensor", title: "Where?", multiple: true, required: true
            }
            section("And then OFF when it's warm or there's been no movement for...") {
                input "delayMinutes", "number", title: "Minutes?", required: false
            }
            section("Using this temp sensor...") {
                input "temperatureSensor", "capability.temperatureMeasurement", title: "Temp Sensor?", multiple: false, required: true
                input "temperatureLevel", "number", title: "Temperature threshold? (default 70 degrees)", defaultValue: "70", required: false
            }
            section("And between sunset and sunrise...") {
                input "sunriseOffsetValue", "text", title: "Sunrise offset", required: false, description: "00:00"
                input "sunriseOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before", "After"]]
                input "sunsetOffsetValue", "text", title: "Sunset offset", required: false, description: "00:00"
                input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before", "After"]]
            }
            section("Zip code (optional, defaults to location coordinates when location services are enabled)...") {
                input "zipCode", "text", title: "Zip Code?", required: false, description: "Local Zip Code"
            }
        }
    } else {
        dynamicPage(name: "options", title: "Heaters will turn ON on movement when it is cold...", install: true, uninstall: true) {
            section("Control these heater(s)...") {
                input "heaters", "capability.switch", title: "Heater(s)?", multiple: true, required: false
            }
            section("Control these dimmer(s)...") {
                input "dimmers", "capability.switchLevel", title: "Dimmer(s)?", multiple: true, required: false
                input "level", "number", title: "How bright?", required: false, description: "0% to 100%"
            }
            section("Turning ON when it's cold and there's movement...") {
                input "motionSensor", "capability.motionSensor", title: "Where?", multiple: true, required: true
            }
            section("And then OFF when it's heater or there's been no movement for...") {
                input "delayMinutes", "number", title: "Minutes?", required: false
            }
            section("Using this temperature sensor...") {
                input "temperatureSensor", "capability.temperatureMeasurement", title: "Temperature Sensor?", multiple: false, required: true
                input "temperatureLevel", "number", title: "Temperature threshold? (default 70)", defaultValue: "70", required: false
            }
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}."
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}."
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    subscribe(motionSensor, "motion", motionHandler)
    if (heaters != null && heaters != "" && dimmers != null && dimmers != "") {
        log.debug "$heaters subscribing..."
        subscribe(heaters, "switch", heatersHandler)
        log.debug "$dimmers subscribing..."
        subscribe(dimmers, "switch", dimmersHandler)
        if (temperatureSensor != null && temperatureSensor != "") {
            log.debug "$heaters and $dimmers will turn ON when movement detected and when it is cold..."
            subscribe(temperatureSensor, "temperature", temperatureHandler, [filterEvents: false])
        }
        if (sun == true) {
            log.debug "$heaters and $dimmers will turn ON when movement detected between sunset and sunrise..."
            astroCheck()
            subscribe(location, "position", locationPositionChange)
            subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
            subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
        } else {
            log.debug "$heaters and $dimmers will turn ON when movement detected..."
        }
    } else if (heaters != null && heaters != "") {
        log.debug "$heaters subscribing..."
        subscribe(heaters, "switch", heatersHandler)
        if (temperatureSensor != null && temperatureSensor != "") {
            log.debug "$heaters will turn ON when movement detected and when it is cold..."
            subscribe(temperatureSensor, "temperature", temperatureHandler, [filterEvents: false])
        }
        if (sun == true) {
            log.debug "$heaters will turn ON when movement detected between sunset and sunrise..."
            astroCheck()
            subscribe(location, "position", locationPositionChange)
            subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
            subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
        } else {
            log.debug "$heaters will turn ON when movement detected..."
        }
    } else if (dimmers != null && dimmers != "") {
        log.debug "$dimmers subscribing..."
        subscribe(dimmers, "switch", dimmersHandler)
        if (temperatureSensor != null && temperatureSensor != "") {
            log.debug "$dimmers will turn ON when movement detected and when it is cold..."
            subscribe(temperatureSensor, "temperature", temperatureHandler, [filterEvents: false])
        }
        if (sun == true) {
            log.debug "$dimmers will turn ON when movement detected between sunset and sunrise..."
            astroCheck()
            subscribe(location, "position", locationPositionChange)
            subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
            subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
        } else {
            log.debug "$dimmers will turn ON when movement detected..."
        }
    }
    log.debug "Determinating heaters and dimmers current value..."
    if (heaters != null && heaters != "") {
        if (heaters.currentValue("switch").toString().contains("on")) {
            state.heatersState = "on"
            log.debug "Heaters $state.heatersState."
        } else if (heaters.currentValue("switch").toString().contains("off")) {
            state.heatersState = "off"
            log.debug "Heaters $state.heatersState."
        } else {
            log.debug "ERROR!"
        }
    }
    if (dimmers != null && dimmers != "") {
        if (dimmers.currentValue("switch").toString().contains("on")) {
            state.dimmersState = "on"
            log.debug "Dimmers $state.dimmersState."
        } else if (dimmers.currentValue("switch").toString().contains("off")) {
            state.dimmersState = "off"
            log.debug "Dimmers $state.dimmersState."
        } else {
            log.debug "ERROR!"
        }
    }
}

def locationPositionChange(evt) {
    log.trace "locationChange()"
    astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
    state.lastSunriseSunsetEvent = now()
    log.debug "SmartNightheater.sunriseSunsetTimeHandler($app.id)"
    astroCheck()
}

def motionHandler(evt) {
    log.debug "$evt.name: $evt.value"
    if (evt.value == "active") {
        unschedule(turnOffHeaters)
        unschedule(turnOffDimmers)
        if (sun == true) {
            if (coldOk == true && sunOk == true) {
                log.debug "Heaters and Dimmers will turn ON because $motionSensor detected motion and $temperatureSensor was cold or because $motionSensor detected motion between sunset and sunrise..."
                if (heaters != null && heaters != "") {
                    log.debug "Heaters: $heaters will turn ON..."
                    turnOnHeaters()
                }
                if (dimmers != null && dimmers != "") {
                    log.debug "Dimmers: $dimmers will turn ON..."
                    turnOnDimmers()
                }
            } else if (coldOk == true && sunOk != true) {
                log.debug "Heaters and Dimmers will turn ON because $motionSensor detected motion and $temperatureSensor was cold..."
                if (heaters != null && heaters != "") {
                    log.debug "Heaters: $heaters will turn ON..."
                    turnOnHeaters()
                }
                if (dimmers != null && dimmers != "") {
                    log.debug "Dimmers: $dimmers will turn ON..."
                    turnOnDimmers()
                }
            } else if (coldOk != true && sunOk == true) {
                log.debug "dimmers will turn ON because $motionSensor detected motion between sunset and sunrise. "  
                if (dimmers != null && dimmers != "") {
                    log.debug "Dimmers: $dimmers will turn ON..."
                    turnOnDimmers()
                }
            } else {
                log.debug "Heaters and dimmers will not turn ON because $temperatureSensor is too warm or because time not between sunset and surise."
            }
        } else {
            if (coldOk == true) {
                log.debug "Heaters and dimmers will turn ON because $motionSensor detected motion and $temperatureSensor was cold..."
                if (heaters != null && heaters != "") {
                    log.debug "Heaters: $heaters will turn ON..."
                    turnOnHeaters()
                }
                if (dimmers != null && dimmers != "") {
                    log.debug "Dimmers: $dimmers will turn ON..."
                    turnOnDimmers()
                }
            } else {
                log.debug "Heaters and dimmers will not turn ON because $temperatureSensor is too warm"
            }
        }
    } else if (evt.value == "inactive") {
        unschedule(turnOffHeaters)
        unschedule(turnOffDimmers)
        if (state.heatersState != "off" || state.dimmersState != "off") {
            log.debug "Heaters and/or dimmers are not OFF."
            if (delayMinutes) {
                def delay = delayMinutes * 60
                if (sun == true) {
                    log.debug "Heaters and dimmers will turn OFF in $delayMinutes minute(s) after turning ON when cold or between sunset and sunrise..."
                    if (heaters != null && heaters != "") {
                        log.debug "Heaters: $heaters will turn OFF in $delayMinutes minute(s)..."
                        runIn(delay, turnOffHeaters)
                    }
                    if (dimmers != null && dimmers != "") {
                        log.debug "Dimmers: $dimmers will turn OFF in $delayMinutes minute(s)..."
                        runIn(delay, turnOffDimmers)
                    }
                } else {
                    log.debug "Heaters and dimmers will turn OFF in $delayMinutes minute(s) after turning ON when cold..."
                    if (heaters != null && heaters != "") {
                        log.debug "Heaters: $heaters will turn OFF in $delayMinutes minute(s)..."
                        runIn(delay, turnOffHeaters)
                    }
                    if (dimmers != null && dimmers != "") {
                        log.debug "Dimmers: $dimmers will turn OFF in $delayMinutes minute(s)..."
                        runIn(delay, turnOffDimmers)
                    }
                }
            } else {
                log.debug "Heaters and dimmers will stay ON because no turn OFF delay was set..."
            }
        } else if (state.heatersState == "off" && state.dimmersState == "off") {
            log.debug "Heaters and dimmers are already OFF and will not turn OFF in $delayMinutes minute(s)."
        }
    }
}

def heatersHandler(evt) {
    log.debug "Heaters Handler $evt.name: $evt.value"
    if (evt.value == "on") {
        log.debug "Heaters: $heaters now ON."
        unschedule(turnOffHeaters)
        state.heatersState = "on"
    } else if (evt.value == "off") {
        log.debug "Heaters: $heaters now OFF."
        unschedule(turnOffHeaters)
        state.heatersState = "off"
    }
}

def dimmersHandler(evt) {
    log.debug "Dimmer Handler $evt.name: $evt.value"
    if (evt.value == "on") {
        log.debug "Dimmers: $dimmers now ON."
        unschedule(turnOffDimmers)
        state.dimmersState = "on"
    } else if (evt.value == "off") {
        log.debug "Dimmers: $dimmers now OFF."
        unschedule(turnOffDimmers)
        state.dimmersState = "off"
    }
}

def temperatureHandler(evt) {
    log.debug "$evt.name: $evt.value, lastStatus heaters: $state.heatersState, lastStatus dimmers: $state.dimmersState"
    if (evt.integerValue > ((temperatureLevel != null && temperatureLevel != "") ? temperatureLevel : 70)) {
        log.debug "Heaters will turn OFF because temperature is above $temperatureLevel degrees..."
        if (heaters != null && heaters != "") {
            log.debug "Heaters: $heaters will turn OFF..."
            turnOffHeaters()
        }
    }
}

def turnOnHeaters() {
    if (allOk) {
        if (state.heatersState != "on") {
            log.debug "Turning ON heaters: $heaters..."
            heaters?.on()
            state.heatersState = "on"
        } else {
            log.debug "Heaters: $heaters already ON."
        }
    } else {
        log.debug "Time, days of the week or mode out of range! $heaters will not turn ON."
    }
}

def turnOnDimmers() {
    if (allOk) {
        if (state.dimmersState != "on") {
            log.debug "Turning ON dimmers: $dimmers..."
            settings.dimmers?.setLevel(level)
            state.dimmersState = "on"
        } else {
            log.debug "Dimmers: $dimmers already ON."
        }
    } else {
        log.debug "Time, days of the week or mode out of range! $dimmers will not turn ON."
    }
}


def turnOffHeaters() {
    if (allOk) {
        if (state.heatersState != "off") {
            log.debug "Turning OFF heaters: $heaters..."
            heaters?.off()
            state.heatersState = "on"
        } else {
            log.debug "Heaters: $heaters already OFF."
        }
    } else {
        log.debug "Time, day of the week or mode out of range! $heaters will not turn OFF."
    }
}

def turnOffDimmers() {
    if (allOk) {
        if (state.dimmersState != "off") {
            log.debug "Turning OFF dimmers: $dimmers..."
            dimmers?.off()
            state.dimmersState = "off"
        } else {
            log.debug "Dimmers: $dimmers already OFF."
        }
    } else {
        log.debug "Time, day of the week or mode out of range! $dimmers will not turn OFF."
    }
}

def astroCheck() {
    def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
    state.riseTime = s.sunrise.time
    state.setTime = s.sunset.time
    log.debug "Sunrise: ${new Date(state.riseTime)}($state.riseTime), Sunset: ${new Date(state.setTime)}($state.setTime)"
}

private getColdOk() {
    def result
    if (temperatureSensor != null && temperatureSensor != "") {
    	log.debug "temperature was read $temperatureSensor.temperatureState.integerValue degrees"
        result = temperatureSensor.temperatureState.integerValue < ((temperatureLevel != null && temperatureLevel != "") ? temperatureLevel : 70)
    }
    log.trace "coldOk = $result"
    result
}

private getSunOk() {
    def result
    if (sun == true) {
        def t = now()
        result = t < state.riseTime || t > state.setTime
    }
    log.trace "sunOk = $result"
    result
}

private getSunriseOffset() {
    sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
    sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}

private getAllOk() {
    modeOk && daysOk && timeOk
}

private getModeOk() {
    def result = !modes || modes.contains(location.mode)
    log.trace "modeOk = $result"
    result
}

private getDaysOk() {
    def result = true
    if (days) {
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        } else {
            df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
        def day = df.format(new Date())
        result = days.contains(day)
    }
    log.trace "daysOk = $result"
    result
}

private getTimeOk() {
    def result = true
    if (starting && ending) {
        def currTime = now()
        def start = timeToday(starting).time
        def stop = timeToday(ending).time
        result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
    }
    log.trace "timeOk = $result"
    result
}

private hhmm(time, fmt = "h:mm a") {
    def t = timeToday(time, location.timeZone)
    def f = new java.text.SimpleDateFormat(fmt)
    f.setTimeZone(location.timeZone ?: timeZone(time))
    f.format(t)
}

private hideOptionsSection() {
    (starting || ending || days || modes) ? false : true
}

private timeIntervalLabel() {
    (starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
