#include "HX711.h"
#include <WiFi.h>
#include <SD.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"

#define API_KEY "AIzaSyBAD4mmDpBhQ7Tsij3kpDBdfDEIfEcQRFI"
#define DATABASE_URL "https://gasdetection-4f88c-default-rtdb.firebaseio.com/"
#define USER_EMAIL "swarilpatil932@gmail.com"
#define USER_PASSWORD "Swarilpatil@123"

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

const int LOADCELL_DOUT_PIN = 19;
const int LOADCELL_SCK_PIN = 18;
const int MQ2_PIN = 35;

const char* ssid = "altamsh";
const char* password = "123321000";

const int GAS_MIN = 0;
const int GAS_MAX = 1023;
const int GAS_LEAK_THRESHOLD = 200;
const float WEIGHT_THRESHOLD = 50.0;

HX711 scale;

void calibrateScale() {
    Serial.println("Starting scale calibration...");
    scale.set_scale();
    scale.tare();
    Serial.println("Remove all weights, wait 5 seconds...");
    delay(5000);
    
    float calibration_factor = -357.28;
    scale.set_scale(calibration_factor);
    scale.tare();
    Serial.println("Scale calibration completed!");
}

void reconnectWiFi() {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("Attempting to reconnect to WiFi...");
        WiFi.disconnect();
        WiFi.begin(ssid, password);
    }
}

void setup() {
    Serial.begin(115200);
    Serial.println("\nStarting Gas Monitoring System...");
    
    scale.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);
    calibrateScale();
    
    pinMode(MQ2_PIN, INPUT);
    
    // Setup WiFi
    WiFi.begin(ssid, password);
    Serial.print("Connecting to WiFi");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nConnected to WiFi!");
    
    config.api_key = API_KEY;
    config.database_url = DATABASE_URL;
    auth.user.email = USER_EMAIL;
    auth.user.password = USER_PASSWORD;
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
}

void loop() {
    unsigned long currentMillis = millis();
    
    if (WiFi.status() != WL_CONNECTED) {
        reconnectWiFi();
        return;
    }
    
    scale.power_up();
    
    float weight = abs(scale.get_units(5));
    scale.power_down();
    
    int rawGasValue = analogRead(MQ2_PIN);
    Serial.println(rawGasValue);
    int normalizedGasValue = map(rawGasValue, GAS_MIN, GAS_MAX, 0, 100);
    normalizedGasValue = constrain(normalizedGasValue, 0, 100);
    
    FirebaseJson json;
    json.setDoubleDigits(3);
    json.add("weight", weight);
    json.add("gasRaw", rawGasValue);
    json.add("gasNormalized", normalizedGasValue);
    json.add("timestamp", currentMillis);
    
    Serial.printf("Updating json... %s\n", Firebase.RTDB.setJSON(&fbdo, "/gasData", &json) ? "ok" : fbdo.errorReason().c_str());
    
    Serial.printf("Weight: %.2f g, Raw Gas: %d, Normalized Gas: %d\n", weight, rawGasValue, normalizedGasValue);
    
    delay(5000);
}

