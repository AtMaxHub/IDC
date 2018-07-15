#!/bin/bash
echo 
cd /usr/local/nginx/logs/ 
for i in `ls |grep 'log$'`
do
SOURCE_FILE=`echo $i | grep "log"` 
echo '--->' $SOURCE_FILE
cp -v $SOURCE_FILE  bak/$SOURCE_FILE.`date +%Y%m%d`
if [ $? -eq 0 ]
    then
    echo `date +'%F %T'`> $SOURCE_FILE
fi
done
