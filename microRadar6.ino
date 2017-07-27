// Microwave radar RCWL 0516; measure duty cycle, send over BT   

#define ON   1  // active HIGH
#define OFF  0   

#include <SoftwareSerial.h>
#include "Timer.h"

const int pir1 = A1;
const int pirled1 = A5;
const int radar = 4;
const int radarled = 9;
long timeout = 30L; // MUST be long !***
Timer T;

// Software serial port for BT
const int softTx = 3;  // TX pin for Arduino; connect the Rx of BT shield to this pin
const int softRx = 2;                                                                                                                                                                                                                                                                                                                  ;  // RX pin for Arduino; Tx of the BT shield
SoftwareSerial softSerial(softRx, softTx); // The order is RX, TX

void setup() {
  pinMode(radar, INPUT);
  pinMode(pir1, INPUT);
  pinMode(radarled, OUTPUT); 
  pinMode(pirled1, OUTPUT);  
  digitalWrite(radarled, OFF);
  digitalWrite(pirled1, OFF);
  softSerial.begin(9600);
  blinker();  
  softSerial.println("[R01]");
  int eventid1 = T.every(50, tick);
  int eventid2 = T.every(1000*timeout, action); // time out MUST be long !***
}

void loop() {
  T.update();
}

boolean radarstatus = 0;
boolean pirstatus1 = 0;
int radarhighcount = 0;
int radarlowcount = 0;
int pirhighcount1 = 0;
int pirlowcount1 = 0;

void tick() {
  radarstatus = digitalRead(radar);
  digitalWrite(radarled, radarstatus); // TODO: disable this
  if (radarstatus)
      radarhighcount++;
   else
      radarlowcount++;
      
  pirstatus1 = digitalRead(pir1);
  digitalWrite(pirled1, pirstatus1); // TODO: disabe this
  if (pirstatus1)
      pirhighcount1++;
   else
      pirlowcount1++;      
}

int   radartotalcount = 0;
int   pirtotalcount1 = 0;
float radardutycycle = 0.0f;
float pirdutycycle1 = 0.0f;
float radarthreshold = 0.3f;
float pirthreshold1 = 0.1f;
char  statusstring[32];  

void action() {
  radartotalcount = radarhighcount + radarlowcount;
  pirtotalcount1 = pirhighcount1 + pirlowcount1;
  if(radartotalcount==0)
      softSerial.println("[E01]"); //divide by zero; if tick() has just set them to zero
  else
  if (pirtotalcount1==0)
      softSerial.println("[E02]"); 
  else{
    /* // TODO: enable this
      radardutycycle = (float)radarhighcount/(float)radartotalcount;
      if (radardutycycle > radarthreshold)
          digitalWrite(radarled, ON);
      else
          digitalWrite(radarled, OFF);
      pirdutycycle1 = (float)pirhighcount1/(float)pirtotalcount1;
      if (pirdutycycle1 > pirthreshold1)
          digitalWrite(pirled1, ON);
      else
          digitalWrite(pirled1, OFF);      
      */    
      sprintf(statusstring, "[D%d,%d,%d,%d]", radarhighcount, radarlowcount, pirhighcount1, pirlowcount1); 
      softSerial.println(statusstring);
      
      radarhighcount = 0;
      radarlowcount = 0;       
      pirhighcount1 = 0;
      pirlowcount1 = 0;
  }
}

void blinker() {
  for (int i=0; i<6; i++) {
    digitalWrite(radarled, ON);
    digitalWrite(pirled1, ON);
    delay(300);  
    digitalWrite(radarled, OFF);
    digitalWrite(pirled1, OFF);
    delay(300);   
  }
}

