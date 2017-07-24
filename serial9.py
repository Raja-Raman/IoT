# Reads serial data from Arduino & logs it to a CSV file
# Use it with microRadar4A.ino
# use the delimiters '[' and ']' on the two ends of the line.
# discard partially received lines.
# NOTE: the last 2 characters are \r and \n. So the right square
# bracket is at 3rd from the end.

import serial
import time
ser = serial.Serial('COM7', 9600, timeout=0)
#ser = serial.Serial('/dev/ttyACM0', 9600, timeout=0)

f = open('radar1.csv', 'wt')
f.write('timeStamp,muradarON,muradarOFF,pir1ON,pir1OFF\n')
 
print "Press ^C to quit..."
 
while 1:
  try:
      x = ser.readline()
      n = len(x)
      if (n > 2):
          print x, n, x[0], x[1], x[-3]
          if (x[0]=='['  and x[-3]==']'):
              x = x[1:-3]
              if(x[0]=='D'):
                   x = x[1:]
                   s = x.split()
                   print 'data:', s[0], s[1], s[2], s[3]	
                   f.write ('00.00' +',' +s[0] +',' +s[1] +',' +s[2] +',' +s[3] +'\n')    
          #time.sleep(2)         
  except serial.SerialTimeoutException:
          print('*** Cannot read serial port ***')
  except KeyboardInterrupt:
	 f.close()
	 print '\n', 'Done!'	
	 time.sleep(2)
	 exit()
	  
  
     