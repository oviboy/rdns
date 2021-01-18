# rdns
simple reverse dns api for cpanel

1. create a mysql database name 'rdnshosting'
2. create a mysql user ('rdnshosting') with a password, and update the password in application.properties; assign the user to the database @1.
3. insert into table 'rdnshosting'.'user', zonename and password:
    eg.: '1.1.1.in-addr.arpa' and 'password' (plain text; yeah, I know!)
4. insert PTR record:
    curl -i -u "1.1.1.in-addr:password" -X POST -H "Content-Type: application/json" --data '{"recordName":"10","ptrDomainName":"google.eu"}' http://yourhostname:8080/add


