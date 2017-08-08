// Microwave radar RCWL 0516 and PIR State machine. Beeper alert.MADE active HIGH hardware.State machine debugged.
#include <SoftwareSerial.h>
#include "Timer.h"

#define VACANT      0
#define OCCUPIED    1
#define WATCHING    2

const int pir1 = A1;
const int pirled1 = 10;
const int radar = 4;
const int radarled = 9;
const int roomstatusled = A5;
const int buzzer = 6; // it is active LOW

// *** Timer durations MUST be unsigned long int ! ***
unsigned long tickinterval = 100UL;  // milliSec 
unsigned long statusinterval = 5*1000UL;
unsigned long prereleasedelay =  10*1000UL; //  2*60*1000UL;  // 
unsigned long releasedelay = 5*1000UL;  // 10*1000UL; 
boolean occupancystatus = 1;   
Timer T;

// Software serial port for BT
const int softTx = 3;  // TX pin for Arduino; connect the Rx of BT shield to this pin
const int softRx = 2;                                                                                                                                                                                                                                                                                                                  ;  // RX pin for Arduino; Tx of the BT shield
SoftwareSerial softSerial(softRx, softTx); // The order is RX, TX
#define ser Serial
//#define ser softSerial

void setup() {
  pinMode(radar, INPUT);
  pinMode(pir1, INPUT);
  pinMode(radarled, OUTPUT); 
  pinMode(pirled1, OUTPUT);  
  pinMode(roomstatusled, OUTPUT); 
  pinMode(buzzer, OUTPUT); 
  digitalWrite(radarled, LOW);
  digitalWrite(pirled1, LOW);
  digitalWrite(roomstatusled, LOW);
  digitalWrite(buzzer, HIGH); // active LOW
  ser.begin(9600);
  //ser.begin (38400); // in case 9800 is not working with BT
  ser.println("R");
  blinker();    
  occupancystatus = OCCUPIED;  
  occupyRoom(); // program starts with occupied status
  int timerid1 = T.every(tickinterval, tick);
  //ser.print("Base timer ID: ");
  //ser.println(timerid1);
  T.every(statusinterval ,sendStatus);
}

void loop() {
  T.update();
}

boolean radarstatus = 0;
boolean pirstatus1 = 0;
boolean timedout = 0;

void tick() {
    radarstatus = digitalRead(radar);
    digitalWrite(radarled, radarstatus); 
    pirstatus1 = digitalRead(pir1);
    digitalWrite(pirled1, pirstatus1);  
    switch(occupancystatus) {
        case VACANT:
            if (radarstatus & pirstatus1) {
              occupyRoom();
              occupancystatus = OCCUPIED;
            }
        break;
        case OCCUPIED:
            if (!(radarstatus | pirstatus1)) {
              startWatching();
              occupancystatus = WATCHING;
            }
        break;        
        case WATCHING:
            if (radarstatus | pirstatus1) {
              stopWatching();
              occupancystatus = OCCUPIED;
            }        
            else
            if (timedout) {
               timedout = 0;
               releaseRoom();
               occupancystatus = VACANT;  
            }   
        break;          
    }
}

void sendStatus() {
  if (occupancystatus == VACANT)
    ser.print(VACANT);
  else
    ser.print(OCCUPIED);
}

void occupyRoom() {
  digitalWrite(roomstatusled, HIGH);
  ser.print(OCCUPIED); 
  T.pulse(buzzer, 100, HIGH); // 500
}

void releaseRoom() {
  digitalWrite(roomstatusled, LOW);
  ser.print(VACANT); 
}

int timer1id = -1;
int timer2id = -1;

void startWatching() {
  if (timer1id == -1) // is this necessary ?
      timer1id = T.after(prereleasedelay, timer1Fired);
}

void stopWatching() {
  if (timer1id != -1)
    T.stop(timer1id);
  if (timer2id != -1)
    T.stop(timer2id);  
  timer1id = -1;    
  timer2id = -1;
}

void timer1Fired() {
  if (timer2id == -1) // ?
      timer2id = T.after(releasedelay, timer2Fired);
  timer1id = -1;
  ser.print(WATCHING); 
  T.oscillate (buzzer,15, HIGH, 7);  //100, 6
  T.oscillate (roomstatusled, 100, HIGH, 4);
}

void timer2Fired() {
  timedout = 1;
  timer2id = -1;  
}
 
void blinker() {
  for (int i=0; i<6; i++) {
    digitalWrite(radarled, HIGH);
    digitalWrite(pirled1, HIGH);
    digitalWrite(roomstatusled, HIGH);
    delay(300);  
    digitalWrite(radarled, LOW);
    digitalWrite(pirled1, LOW);
    digitalWrite(roomstatusled, LOW);
    delay(300);   
  }
}

