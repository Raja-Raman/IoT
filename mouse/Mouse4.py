# read mouse coordinates and send over serial port if it moves


from ctypes import windll, Structure, c_long, byref
import time
import serial

port = "COM3"
ser = serial.Serial(port, 9600, timeout=0) 

class POINT(Structure):
    _fields_ = [("x", c_long), ("y", c_long)]

pt = POINT()
def queryMousePosition():
    windll.user32.GetCursorPos(byref(pt))
    return (pt.x, pt.y)

(oldx, oldy) = (0,0)
try :
    while True:
        (x,y) = queryMousePosition()
        if (x==oldx and y==oldy):
            continue
        oldx = x
        oldy = y
        print(x,y)
        #ser.write(x)
        #ser.write(y)
        ser.write("({0},{1})".format(x,y))
        time.sleep(0.1)
except KeyboardInterrupt:
    pass
 
print "bye !"        