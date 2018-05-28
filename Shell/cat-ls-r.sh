#！/bin/bash
TIMES=0
file_name=$1
while  [ -L $file_name ]
do 
	ls -lr $file_name
	file_name=`ls -lr $file_name| awk '{print $NF}'`
#	echo `ls -lr $file_name| awk '{print $NF}'`
#	ls -lr $file_name | awk '{print $NF}' | read  file_name
	echo "-> $file_name"
	let TIMES++
	if [ $TIMES -eq 10 ]; then
	   exit 0
	fi
done

