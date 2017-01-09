#!/bin/bash

while [[ $# -gt 0 ]]; do
KEY="$1"

case $KEY in
-f|--file)
ASSTNAME="$2"
shift
;;
-u|--user)
USERNAME="$2"
shift
;;
-n|--asstnum)
ASSTNUM="$2"
;;


*)
;;
esac
shift
done

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#cd $DIR/run/asst$ASSTNUM/src
cd $DIR/submissions/assignment-$ASSTNUM/src

ENTRY=./UnderTest/$USERNAME/$ASSTNAME

#NAME=$(echo $ENTRY | cut -d'-' -f 2)
#echo ${NAME%??}

if make -B UNDERTEST=$ENTRY >/dev/null
then compiled="compiled"; else exit 0; fi

VAR="$(./target)"
IFS='_' read -r -a RESULTS <<< "$VAR" #cut up the results into 2 pieces and store it in the results array

for i in "${RESULTS[@]}"; do
if [ "$i" == "${RESULTS[0]}" ]; then
PASSRATE=${i}
echo "PASS RATE:@" $PASSRATE "@"
echo "FAILURES:@"
else
echo ${i}; fi
done

exit $exitcode




