/**
 *  It Moved
 *
 *  Author: SmartThings
 */
definition(
    name: "It Moved",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Send a text when movement is detected",
    category: "Fun & Social",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/text_accelerometer.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text_accelerometer@2x.png"
)

preferences {
	section("When movement is detected...") {
		input "accelerationSensor", "capability.accelerationSensor", title: "Where?"
	}
	section("Text me at...") {
		input "phone1", "phone", title: "Phone number?"
	}
}

def installed() {
	subscribe(accelerationSensor, "acceleration.active", accelerationActiveHandler)
}

def updated() {
	unsubscribe()
	subscribe(accelerationSensor, "acceleration.active", accelerationActiveHandler)
}

def accelerationActiveHandler(evt) {
	// Don't send a continuous stream of text messages
	def deltaSeconds = 5
	def timeAgo = new Date(now() - (1000 * deltaSeconds))
	def recentEvents = accelerationSensor.eventsSince(timeAgo)
	log.trace "Found ${recentEvents?.size() ?: 0} events in the last $deltaSeconds seconds"
	def alreadySentSms = recentEvents.count { it.value && it.value == "active" } > 1

	if (alreadySentSms) {
		log.debug "SMS already sent to $phone1 within the last $deltaSeconds seconds"
	} else {
		log.debug "$accelerationSensor has moved, texting $phone1"
		sendSms(phone1, "${accelerationSensor.label ?: accelerationSensor.name} moved")
	}
}