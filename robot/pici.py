'''
Python iRobot Create Interface (pici.py)

Python wrapper for using the Create Open Interface (OI) Version 2 
for interaction via PySerial.

Pronounced "peachy" as in "peachy keen".

Originally written by U Hawaii, modified by us to remove unecessary features
http://web.eng.hawaii.edu/~bsb/me492_696/pici/getting_started.html#installation
'''

import serial, sys, time, math
#import pics
#import ipdb

# We need to keep track of these values with globals
PowerLedColorByte = 0  # starts green when power on
PowerLedIntByte = 255
LedByte = 0

def tx_bytelist(ser,intList):
    '''
    Sends a list of bytes to the serial port
    ser - The serial port object (pyserial)
    intList - a list of integers to send as characters to the port
    '''
    #if ( (not isinstance(ser,serial.serialposix.Serial)) and 
    #     (not isinstance(ser, pics.Create)) ):
    #    print((not isinstance(ser, pics.Create)))
    #    raise TypeError("First argument must be a serial or Create object, not <%s>"%type(ser))
    if not isinstance(intList,list):
        raise TypeError("Second argument must be a list!")

    msg = ""
    for il in intList:
        msg = msg+chr(il)
    ser.write(msg)

def getSignedNumber(number, bitLength):
    mask = pow(2,bitLength) - 1
    if number & (1 << (bitLength - 1)):
        return number | ~mask
    else:
        return number & mask


def getUnsignedNumber(number, bitLength):
    mask = pow(2,bitLength) - 1
    return number & mask

def val2bytes(val):
    '''
    Convert a signed 16-bit integer into two unsigned 8-bit bytes
    '''
    lowbyte = getUnsignedNumber(val,8)
    highbyte = getUnsignedNumber(val>>8,8)
    return (highbyte, lowbyte)

def bytes2val(highbyte, lowbyte):
    #ipdb.set_trace()
    number = (highbyte<<8)+lowbyte
    return getSignedNumber(number,16)

def setModePassive(ser):
    tx_bytelist(ser,[128])

def start(ser):
    '''
    Start by setting the mode, playing a song and turning on the lights
    '''
    setModePassive(ser)  # must always send a "start"
    setModeFull(ser)
    # Play start song
    playSongStart(ser)
    # Turn Power led on green, full power
    ledPower(ser,0,255)
    # Turn on play led
    ledAdvancePlay(ser,False,True)


def setModeFull(ser):
    tx_bytelist(ser,[132])


def getSensorsAll(ser):
    '''
    Gets all the sensors at once.
    Note that this resets the distance and angle sensors

    Similar to the MTIC AllSensorsReadRoomba
    
    Args:
        ser : serial (hardware) or pics.Create (simulation) object
    
    Returns:
        out : dictionary with the following keys/values
            bump_left : True/False
            bump_right : True/False
            virtual_wall : True/False
            distance : int
            angle : int
            etc.
    '''

    # May need to flush the buffer
    ser.flushInput()

    # Request all the sensors in group 0
    tx_bytelist(ser,[142, 0])
    
    # Should get 26 bytes back
    resp = ser.read(26)
    #print("RX[%d]: <%s>"%(len(resp),resp))
    if len(resp) < 26:
        print("Warning: getSensorsAll only received %d bytes, not 26"%len(resp))
        return None
    
    # If we did get all the bits, parse them.
    out = dict()
    # Bumps and wheel drops are in the first byte
    bwbyte = ord(resp[0])
    out['bump_right']=bool(bwbyte & 1)
    out['bump_left']=bool(bwbyte & 2)
    out['wheeldrop_right']=bool(bwbyte & 4)
    out['wheeldrop_left']=bool(bwbyte & 8)
    out['wheeldrop_caster']=bool(bwbyte & 16)
    
    # Walls, cliffs and virtual wall
    out['wall']=bool(ord(resp[1]) & 1)
    out['cliff_left']=bool(ord(resp[2]) & 1)
    out['cliff_front_left']=bool(ord(resp[3]) & 1)
    out['cliff_front_right']=bool(ord(resp[4]) & 1)
    out['cliff_right']=bool(ord(resp[5]) & 1)
    out['virtual_wall']=bool(ord(resp[6]) & 1)

    # Over currents, ir-byte, buttons
    # Skip for now

    # Distance
    out['distance']=bytes2val(ord(resp[12]),ord(resp[13]))  # mm

    # Angle
    out['angle']=bytes2val(ord(resp[14]),ord(resp[15])) # mm or degrees?

    return out

def drive(ser,vel,radius):
    """Send a command to drive at a given velocity and radius.
    
    Args:
        ser : serial or sim object
            Where to write the command
        vel : int  
            Velocity to drive [mm/s]
        radius : int
            Radius to drive [mm]

    Returns: 
        N : int 
            Number of bytes written
 
    Raises:
       None

    Examples...

    """
    vhigh, vlow = val2bytes(vel)
    rhigh, rlow = val2bytes(radius)
    tx_bytelist(ser,[137,vhigh,vlow,rhigh,rlow])

def drive_direct(ser,rvel,lvel):
    '''
    Send a command to drive the right and left wheels
    
    Args:
        rvel : int for right wheel velocity [mm/s]
        lvel : int for left wheel velocity [mm/s]

    Returns: 
        None

    '''
    rvelH, rvelL = val2bytes(rvel)
    lvelH, lvelL = val2bytes(lvel)
    tx_bytelist(ser,[145,rvelH,rvelL,lvelH,lvelL])

def driveFwdAngVel(ser, fwdVel, angVel):
    '''
    Wrapper for drive_direct() to set forward and angular velocities
    
    Args:
        ser : serial or simulation object
        fwdVel : int, forward velocity [mm/s]
        angVel : float, angular velocity [rad/s, positive is cw]
       
    Returns:
        None
    '''
    L = 258.0  # distance between wheels in mm
    vr = fwdVel+angVel*L/2.0
    vl = fwdVel-angVel*L/2.0
    drive_direct(ser, int(vr), int(vl))
 
def drive_straight(ser,vel):
    '''
    Wrapper for the drive() command to drive in a straight line
    '''
    vhigh, vlow = val2bytes(vel)
    rhigh, rlow = val2bytes(32768)
    tx_bytelist(ser,[137,vhigh,vlow,rhigh,rlow])

def turn(ser,vel,cw=True):
    '''
    Wrapper for the drive() command to turn in place
    '''
    vhigh, vlow = val2bytes(vel)
    # Clockwise or counter clockwise?
    if cw:
        tx_bytelist(ser,[137,vhigh,vlow,255,255])
    else:
        tx_bytelist(ser,[137,vhigh,vlow,0,1])



def stop(ser):
    ''' 
    Stop driving, turn off play light and play song
    '''
    drive(ser,0,0)
    playSongStop(ser)
    ledAdvancePlay(ser,False,False)

def led(ser, ledbyte, colorbyte, intensitybyte):
    tx_bytelist(ser,[139, ledbyte, colorbyte, intensitybyte])

def ledAdvancePlay(ser, AdvanceBool, PlayBool):
    global PowerLedColorByte, PowerLedIntByte, LedByte
    LedByte = int(AdvanceBool)*8 + int(PlayBool)*2
    led(ser, LedByte, PowerLedColorByte, PowerLedIntByte)

def ledPower(ser, color, intensity):
    global PowerLedColorByte, PowerLedIntByte, LedByte
    PowerLedColorByte = color
    PowerLedIntByte = intensity
    led(ser, LedByte, color, intensity)

def playSongStart(ser):
    '''
    Make and play a two-note starting song
    '''
    tx_bytelist(ser,[140,0,2,48,32,36,16])
    tx_bytelist(ser,[141,0])
    
def playSongStop(ser):
    tx_bytelist(ser,[140,0,2,36,32,48,16])
    tx_bytelist(ser,[141,0])
    
    
    

