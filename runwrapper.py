import sys
import subprocess

#TODO: add c or java support

idlength = 9

if __name__ == "__main__":
	tries = 0
	if len(sys.argv) > 0:
		for arg in sys.argv:
			if arg[:6] == 'fname=':
				fname = arg[6:]
			elif arg[:5] == 'anum=':
				anum = arg[5:]
	#TODO: make sure its the path from which this script is being called
	results = subprocess.check_output(['bash ./run.sh -f '+ fname + ' -n ' + anum], shell=True)	
	indexprb= results.find('PASS')
	indexpre= indexprb+len("PASS RATE:")+1
	indexflb= results.find('FAIL')
	indexfle= results.find('FAIL')+len("FAILURES:")+1
	studid=results[:idlength]
	passrate=results[indexpre:indexflb-1]
	failures= results[indexfle:]
	print(studid+";"+passrate+";"+failures)
