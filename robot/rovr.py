import serial
import socket
from time import sleep
import pici
reload(pici)

IROBOT_PORT = '/dev/ttyUSB0'
IROBOT_BAUD = 115200
ARDUINO_PORT = '/dev/ttyACM0'
ARDUINO_BAUD = 9600
HOST = "192.168.1.111"
HTTP_PORT = 8080
SPEED = 128

ser = serial.Serial(IROBOT_PORT, IROBOT_BAUD)
arduino = serial.Serial(ARDUINO_PORT, ARDUINO_BAUD)
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect((HOST, HTTP_PORT))

# iRobot start command
pici.tx_bytelist(ser,[128])
# Put iRobot Create into full mode
pici.tx_bytelist(ser,[132])
datastring = ""

while datastring != "q":
	datastring = sock.recv(1024)
	datas = datastring.split("\n")
	print datastring
	#print datas	
	data = datas[-2].split(",")
	
	try: 
		data[:2] = [int(SPEED * float(x)) for x in data[:2]]
		data[2:4] = [int(90 * float(x)) + 90 for x in data[2:4]]
		data[4:6] = [int(float(x)) for x in data[4:6]]
	except ValueError:
		data = []
	except TypeError:
		data=[]

	#print data
	if len(data) > 1:
		if data[0] > 0.1:
			data[0] = SPEED
		if data[0] < -0.1:
			data[0] = -SPEED
		if data[1] > 0.1:
			data[1] = SPEED
		if data[1] < -0.1:
			data[1] = -SPEED
		print data
		
		#arduino.write("90 90.")
		arduino.write(str(data[3]) + " " + str(data[2])+".")
		pici.drive_direct(ser, -data[1]-data[0], -data[1]+data[0])
	sleep(0.05)
pici.drive(ser, 0, 0) # stop robot
ser.close() # close serial connection
sock.close() # close socket connection


