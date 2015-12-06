import serial
import socket
from time import sleep
import pici
reload(pici)

# name of USB port that plugs into the iRobot Create
IROBOT_PORT = '/dev/ttyUSB0'
# default baud rate for iRobot Create 
IROBOT_BAUD = 115200

# name of USB port that plugs into the Arduino
ARDUINO_PORT = '/dev/ttyACM0'
# baud rate configured in Arduino code
ARDUINO_BAUD = 9600

# IP address of controller phone
HOST = "192.168.1.111"

# socket server configured for port 8080 in Android app
HTTP_PORT = 8080

# max speed of robot
SPEED = 128

# initialize serial port for communicating with iRobot Create
ser = serial.Serial(IROBOT_PORT, IROBOT_BAUD)
# initialize serial port for communicating with Arduino
arduino = serial.Serial(ARDUINO_PORT, ARDUINO_BAUD)
# intialize socket connection for communicating with Android phone 
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# connect to Android phone
sock.connect((HOST, HTTP_PORT))

# iRobot start command
pici.tx_bytelist(ser,[128])
# Put iRobot Create into full mode
pici.tx_bytelist(ser,[132])

#initialize string to store incoming data from Android phone
datastring = ""

# loop until we receive a string to quit (not implemented yet)
while datastring != "q":
	# grab data from socket
	datastring = sock.recv(1024)
	
	# split data into lines just in case we have a couple messages buffered since our last read 
	datalines = datastring.split("\n")

	# only take most recent data (final element of array is "", so -2)
	data = datalines[-2].split(",")
	
	# process each type of data 
	try: 
		# scale drive control based on speed
		data[:2] = [int(SPEED * float(x)) for x in data[:2]]
		# scale webcam joystick control to 90 degrees in either direction
		data[2:4] = [int(90 * float(x)) + 90 for x in data[2:4]]
		# head tracking data scaled correctly, just needs to be made an int
		data[4:6] = [int(float(x)) for x in data[4:6]]
	# if we get garbage data, just ignore it for now
	except ValueError:
		data = []
	except TypeError:
		data=[]

	if len(data) > 1:		
		# write camera pan/tilt data to arduino (currently using joystick data since we can't get good head tracking data for some phones)
		arduino.write(str(data[3]) + " " + str(data[2])+".")
		# write joystick data to iRobot Create drive
		pici.drive_direct(ser, -data[1]-data[0], -data[1]+data[0])

	#sleep for 50ms
	sleep(0.05)

# stop robot
pici.drive(ser, 0, 0) 
# close serial connection
ser.close() 
# close socket connection
sock.close() 


