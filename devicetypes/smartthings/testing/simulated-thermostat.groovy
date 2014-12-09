/**
 *  Copyright 2014 SmartThings
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
	// Automatically generated. Make future change here.
	definition (name: "Simulated Thermostat", namespace: "smartthings/testing", author: "SmartThings") {
		capability "Thermostat"

		command "tempUp"
		command "tempDown"
		command "heatUp"
		command "heatDown"
		command "coolUp"
		command "coolDown"
	}

	tiles {
		valueTile("temperature", "device.temperature", width: 1, height: 1) {
			state("temperature", label:'${currentValue}', unit:"dF",
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
			)
		}
		standardTile("tempDown", "device.temperature", inactiveLabel: false, decoration: "flat") {
			state "default", label:'down', action:"tempDown"
		}
		standardTile("tempUp", "device.temperature", inactiveLabel: false, decoration: "flat") {
			state "default", label:'up', action:"tempUp"
		}        

		valueTile("heatingSetpoint", "device.heatingSetpoint", inactiveLabel: false, decoration: "flat") {
			state "heat", label:'${currentValue} heat', unit: "F", backgroundColor:"#ffffff"
		}
		standardTile("heatDown", "device.temperature", inactiveLabel: false, decoration: "flat") {
			state "default", label:'down', action:"heatDown"
		}        
		standardTile("heatUp", "device.temperature", inactiveLabel: false, decoration: "flat") {
			state "default", label:'up', action:"heatUp"
		}
        
		valueTile("coolingSetpoint", "device.coolingSetpoint", inactiveLabel: false, decoration: "flat") {
			state "cool", label:'${currentValue} cool', unit:"F", backgroundColor:"#ffffff"
		}
		standardTile("coolDown", "device.temperature", inactiveLabel: false, decoration: "flat") {
			state "default", label:'down', action:"coolDown"
		}        
		standardTile("coolUp", "device.temperature", inactiveLabel: false, decoration: "flat") {
			state "default", label:'up', action:"coolUp"
		}
        
        standardTile("mode", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
			state "off", label:'${name}', action:"thermostat.heat", backgroundColor:"#ffffff"
			state "heat", label:'${name}', action:"thermostat.cool", backgroundColor:"#ffa81e"
			state "cool", label:'${name}', action:"thermostat.auto", backgroundColor:"#269bd2"
			state "auto", label:'${name}', action:"thermostat.off", backgroundColor:"#79b821"
		}
		standardTile("fanMode", "device.thermostatFanMode", inactiveLabel: false, decoration: "flat") {
			state "fanAuto", label:'${name}', action:"thermostat.fanOn", backgroundColor:"#ffffff"
			state "fanOn", label:'${name}', action:"thermostat.fanCirculate", backgroundColor:"#ffffff"
			state "fanCirculate", label:'${name}', action:"thermostat.fanAuto", backgroundColor:"#ffffff"
		}
		standardTile("operatingState", "device.thermostatOperatingState") {
			state "idle", label:'${name}', backgroundColor:"#ffffff"
			state "heating", label:'${name}', backgroundColor:"#ffa81e"
			state "cooling", label:'${name}', backgroundColor:"#269bd2"
		}

		main("temperature","operatingState")
		details([
        	"temperature","tempDown","tempUp", 
            "mode", "fanMode", "operatingState",
        	"heatingSetpoint", "heatDown", "heatUp",
            "coolingSetpoint", "coolDown", "coolUp",
            ])
	}
}

def parse(String description)
{
	def pair = description.split(":")
	def map = createEvent(name: pair[0].trim(), value: pair[1].trim())
	def result = [map]

	if (map.isStateChange && map.name in ["heatingSetpoint","coolingSetpoint","thermostatMode"]) {
		def map2 = [
			name: "thermostatSetpoint",
			unit: "F"
		]
		if (map.name == "thermostatMode") {
			if (map.value == "cool") {
				map2.value = device.latestValue("coolingSetpoint")
				log.info "THERMOSTAT, latest cooling setpoint = ${map2.value}"
			}
			else {
				map2.value = device.latestValue("heatingSetpoint")
				log.info "THERMOSTAT, latest heating setpoint = ${map2.value}"
			}
		}
		else {
			def mode = device.latestValue("thermostatMode")
			log.info "THERMOSTAT, latest mode = ${mode}"
			if ((map.name == "heatingSetpoint" && mode == "heat") || (map.name == "coolingSetpoint" && mode == "cool")) {
				map2.value = map.value
				map2.unit = map.unit
			}
		}
		if (map2.value != null) {
			log.debug "THERMOSTAT, adding setpoint event: $map"
			result << createEvent(map2)
		}
	}
	log.debug "Parse returned ${result?.descriptionText}"
	result
}

def evaluate(temp, heatingSetpoint, coolingSetpoint) {
	log.debug "evaluate($temp, $heatingSetpoint, $coolingSetpoint"
	def threshold = 1.0
	def current = device.currentValue("thermostatOperatingState")
    def mode = device.currentValue("thermostatMode")
    
    def heating = false
    def cooling = false
    def idle = false
    if (mode in ["heat","emergency heat","auto"]) {
    	if (heatingSetpoint - temp >= threshold) {
        	heating = true
        	sendEvent(name: "thermostatOperatingState", value: "heating")
        }
        else if (temp - heatingSetpoint >= threshold) {
            idle = true
        }
    }
    if (mode in ["cool","auto"]) {
    	if (temp - coolingSetpoint >= threshold) {
        	cooling = true
        	sendEvent(name: "thermostatOperatingState", value: "cooling")
        }
        else if (coolingSetpoint - temp >= threshold && !heating) {
        	idle = true
        }
    }
    if (idle && !heating && !cooling) {
    	sendEvent(name: "thermostatOperatingState", value: "idle")
    }
}

def setHeatingSetpoint(Double degreesF) {
	sendEvent(name: "heatingSetpoint", value: degreesF)
    evaluate(device.currentValue("temperature"), degreesF, device.currentValue("coolingSetpoint"))
}

def setCoolingSetpoint(Double degreesF) {
	sendEvent(name: "coolingSetpoint", value: degreesF)
    evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), degreesF)
}

def setThermostatMode(String value) {
	sendEvent(name: "thermostatMode", value: value)
    evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def setThermostatFanMode(String value) {
	sendEvent(name: "thermostatFanMode", value: value)
}

def off() {
	sendEvent(name: "thermostatMode", value: "off")
    evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def heat() {
	sendEvent(name: "thermostatMode", value: "heat")
    evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def auto() {
	sendEvent(name: "thermostatMode", value: "auto")
    evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def emergencyHeat() {
	sendEvent(name: "thermostatMode", value: "emergency heat")
    evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def cool() {
	sendEvent(name: "thermostatMode", value: "cool")
    evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def fanOn() {
	sendEvent(name: "thermostatFanMode", value: "fanOn")
}

def fanAuto() {
	sendEvent(name: "thermostatFanMode", value: "fanAuto")
}

def fanCirculate() {
	sendEvent(name: "thermostatFanMode", value: "fanCirculate")
}

def poll() {
	null
}

def tempUp() {
	def ts = device.currentState("temperature")
	def value = ts ? ts.integerValue + 1 : 72 
	sendEvent(name:"temperature", value: value)
    evaluate(value, device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}

def tempDown() {
	def ts = device.currentState("temperature")
	def value = ts ? ts.integerValue - 1 : 72 
	sendEvent(name:"temperature", value: value)
    evaluate(value, device.currentValue("heatingSetpoint"), device.currentValue("coolingSetpoint"))
}


def heatUp() {
	def ts = device.currentState("heatingSetpoint")
	def value = ts ? ts.integerValue + 1 : 68 
	sendEvent(name:"heatingSetpoint", value: value)
    evaluate(device.currentValue("temperature"), value, device.currentValue("coolingSetpoint"))
}

def heatDown() {
	def ts = device.currentState("heatingSetpoint")
	def value = ts ? ts.integerValue - 1 : 68 
	sendEvent(name:"heatingSetpoint", value: value)
    evaluate(device.currentValue("temperature"), value, device.currentValue("coolingSetpoint"))
}


def coolUp() {
	def ts = device.currentState("coolingSetpoint")
	def value = ts ? ts.integerValue + 1 : 76 
	sendEvent(name:"coolingSetpoint", value: value)
    evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), value)
}

def coolDown() {
	def ts = device.currentState("coolingSetpoint")
	def value = ts ? ts.integerValue - 1 : 76
	sendEvent(name:"coolingSetpoint", value: value)
    evaluate(device.currentValue("temperature"), device.currentValue("heatingSetpoint"), value)
}