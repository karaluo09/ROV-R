#include <Servo.h>

Servo winch; 
Servo pan;
Servo tilt; 
const int WINCH_STOP = 103; 

void setup() {
  winch.attach(3); 
  tilt.attach(8);
  pan.attach(9);
  tilt.write(80);
  pan.write(85);
  winch.write(103); 
  Serial.begin(9600); 
}

void loop() {
  if(Serial.available()  > 1) {
      int panData = Serial.parseInt();
      int tiltData = Serial.parseInt();
      pan.write(panData);
      tilt.write(tiltData); 
  }
  delay(10); 
}






