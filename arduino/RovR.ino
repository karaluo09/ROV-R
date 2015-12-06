#include <Servo.h>

// store references to each servo motor
Servo pan;
Servo tilt; 

void setup() {
  // initialize tilt servo to be attached to pin 8
  tilt.attach(8);
  // initialize pan servo to be attached to pin 9
  pan.attach(9);

  // write centered values to each servo
  tilt.write(80);
  pan.write(85);

  // initialize serial communications
  Serial.begin(9600); 
}

void loop() {
  // if 2 bytes or more of serial data are available
  if(Serial.available()  > 1) {
      // parse integer from each byte
      int panData = Serial.parseInt();
      int tiltData = Serial.parseInt();

      // write data to servos
      pan.write(panData);
      tilt.write(tiltData); 
  }

  // wait 10ms then loop
  delay(10); 
}






