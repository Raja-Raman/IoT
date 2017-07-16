// remote controller for hydroponics;  BT protocol; auto status response; auto initial state

#define ON   1  // active HIGH
#define OFF  0  // active HIGH

#include <SoftwareSerial.h>
#include "Timer.h"

const int redLed = 9;
const int greenLed = 10;
const int blueLed = 11;
const int relay1 = 4;
const int relay2 = 6;
const int relay3 = 5;
const int ldr = A0;
Timer T;

// Software serial port
const int softTx = 3;  // TX pin for Arduino; connect the Rx of BT shield to this pin
const int softRx = 2;  // RX pin for Arduino; Tx of the BT shield
SoftwareSerial softSerial(softRx, softTx); // The order is RX, TX

void setup() {
  pinMode(ldr, INPUT);
  pinMode(redLed, OUTPUT);  
  pinMode(greenLed, OUTPUT); 
  pinMode(blueLed, OUTPUT); 
  pinMode(relay1, OUTPUT); 
  pinMode(relay2, OUTPUT); 
  pinMode(relay3, OUTPUT);   
  digitalWrite(relay1, LOW);
  digitalWrite(relay2, LOW);
  digitalWrite(relay3, LOW);  
  digitalWrite(redLed, LOW);
  digitalWrite(greenLed, LOW);
  digitalWrite(blueLed, LOW);
  softSerial.begin(9600);
  softSerial.println("R000");
  initStatus();
  int eventid = T.every(2000, tick);
}

char rxbyte;
byte statusbyte=0;
int currentLed = blueLed;
char commandArray[] = "[Cx]";

void loop() {
  if (softSerial.available() > 0) {
      rxbyte = softSerial.read();
      commandArray[2] = rxbyte;  // rxbyte is a char
      softSerial.print(commandArray);  // echo the command
      if (rxbyte >= 'a' && rxbyte <= 'f') {
        executeCommand(rxbyte);
        sendStatus();
      }
      else  
      if (rxbyte == 's') 
        sendStatus();
      else
        softSerial.print("[E001]");
  }
  T.update();
}

void executeCommand(char cmd) {
      if (cmd == 'a') 
        relay1On();
      else
      if (cmd == 'b') 
        relay1Off();
      else
      if (cmd == 'c') {
        digitalWrite (relay2, ON); 
        statusbyte |= 0x02;
      }
      else
      if (cmd == 'd') {
        digitalWrite (relay2, OFF);  
        statusbyte &= 0x0fd;
      }
      else
      if (cmd == 'e') {
        digitalWrite (relay3, ON); 
        statusbyte |= 0x04;
      }
      else
      if (cmd == 'f') {
        digitalWrite (relay3, OFF);    
        statusbyte &= 0xfb;
      }      
}

void relay1On() {
    digitalWrite (relay1, ON); 
    statusbyte |= 0x01;
    currentLed = redLed;  
}

void relay1Off() {
    digitalWrite (relay1, OFF);
    statusbyte &= 0xfe;
    currentLed = greenLed;
}

char statusArray[] = "[Sx]";
boolean toggle = 0; // keep alive: to indicate communication is not frozen

void sendStatus() {
  if (toggle)
    statusArray[1] = 's';
  else
    statusArray[1] = 'S';
  toggle = !toggle;
  statusArray[2] = '0'+statusbyte;
  softSerial.print(statusArray);
}

int ldrval;
int average=0;
int numsamples=30;
int counter=0;
char dataArray[10];

void tick() {
  digitalWrite (currentLed, ON);  
  ldrval = analogRead(ldr);
  average = average+ldrval;
  counter++;
  if (counter==numsamples) {
    average = average/numsamples;
    sprintf(dataArray, "[D%d]", average);
    softSerial.print(dataArray);
    counter=0;
  }
  T.after(40, ledOff);
}

void ledOff() {
  digitalWrite (blueLed, OFF); 
}

int dayLightThreshold  = 200; // for the current hardware, by observation

void initStatus() {
  int numsamp = 20;
  int lightLevel = 0;
  for (int i=0; i<numsamp; i++) {
     digitalWrite(blueLed, ON);
     lightLevel = lightLevel + analogRead(ldr);
     delay(100);
     digitalWrite(blueLed, OFF);
     delay(150);
  }
  lightLevel = lightLevel/numsamp;  
  if (lightLevel < dayLightThreshold)
     relay1On();
  else
     relay1Off();
}

