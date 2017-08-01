// see if you can capture a serial mouse into Arduino and send it over BT

#include <SoftwareSerial.h>

// Software serial port
const int softTx = 3;  // TX pin for Arduino; connect the Rx of BT shield to this pin
const int softRx = 2;  // RX pin for Arduino; Tx of the BT shield
SoftwareSerial softSerial(softRx, softTx); // The order is RX, TX

void setup() {
  Serial.begin(9600);
  //Serial.println("Mouse capture"); 
  //Serial.begin(1200);
 
  softSerial.begin (38400); // (9600); programming mode:38400
  softSerial.println("Mouse capture.."); 
}

char rxbyte;
void loop() {
  if (Serial.available())  {
      rxbyte = Serial.read();
      softSerial.print(rxbyte);   
  }
}
