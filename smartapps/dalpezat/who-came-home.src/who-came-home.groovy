/**
 *  Who Came Home
 *
 *  Copyright 2016 Tony DalPezzo
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
definition(
    name: "Who Came Home",
    namespace: "dalpezat",
    author: "Tony DalPezzo",
    description: "Who came home and what door did they enter through.",
    category: "Family",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Which doors would you like to watch?") {
        input "door", "capability.contactSensor", required: true, multiple: true
	}
    section("Who would you like to know about?"){
    	input "person", "capability.presenceSensor", required: true
    }
      section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)",
                description: "Phone Number", required: false
        }
    }
    section("Notification offset"){
    	input "presenceOffset", "number", title: "Delay between presence and door event,1-5 minutes", required: true, range: "1..5", defaultValue: 1
    }
    section("Send Push Notification?") {
        input "sendPush", "bool", required: false,
              title: "Send Push Notification when person enters the location?"
    }
    
    //TODO: add text notifications only between a date time range
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(door, "contact.open", doorOpenHandler)
}

def doorOpenHandler(evt){
	log.debug "recipients configured: $recipients"
    def message = "${person.label} is at ${location.name} and entered through the ${evt.displayName}"
    
 	log.info(message)
	def recentNotification = false
    
   if(presenceOffset){
    def timeAgo = new Date(now() - presenceOffset * 60 * 1000)
    log.info(now())
    log.info(timeAgo.getTime())
    def recentEvents = person.eventsSince(timeAgo)
    log.info( recentEvents.count{ it.value && it.value == "present" })
    recentNotification = recentEvents.count{ it.value && it.value == "present" } > 0
    log.debug(recentNotification)
    }
    
    if(recentNotification)
    {
        if (sendPush) {
            sendPush(message)
        }
        if (location.contactBookEnabled && recipients) {
            log.debug "contact book enabled!"
            sendNotificationToContacts(message, recipients)
        } else {
            log.debug "contact book not enabled"
            if (phone) {
                sendSms(phone, message)
            }
        }
    }
}