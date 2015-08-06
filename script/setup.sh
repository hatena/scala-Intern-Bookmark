#!/bin/sh

dbname=${1:-internbookmark}

if [[ $(mysql -N -uroot -e "SELECT count(*) FROM mysql.user WHERE user = 'nobody'") -lt "1" ]]; then
  echo "[root] User nobody@localhost (nobody) creating..."
  mysql -uroot -p -e "GRANT ALL PRIVILEGES ON *.* TO 'nobody'@'localhost' IDENTIFIED BY 'nobody' WITH GRANT OPTION"
  echo "User nobody@localhost (nobody) created"
fi

echo "[root] Database \"${dbname}\" deleting..."
mysqladmin -uroot -p drop $dbname -f > /dev/null 2>&1
echo "[root] Database \"${dbname}\" creating..."
mysqladmin -uroot -p create $dbname
echo "Database \"${dbname}\" created"
echo "[nobody] Initializing \"$dbname\""
mysql -unobody -p $dbname < db/schema.sql

echo "[root] Database \"${dbname}_test\" deleting..."
mysqladmin -uroot -p drop ${dbname}_test -f > /dev/null 2>&1
echo "[root] Database \"${dbname}_test\" creating..."
mysqladmin -uroot -p create ${dbname}_test
echo "Database \"${dbname}_test\" created"
echo "[nobody] Initializing \"${dbname}_test\""
mysql -unobody -p ${dbname}_test < db/schema.sql

echo "Done."
