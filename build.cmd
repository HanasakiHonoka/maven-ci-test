call mvn dependency:copy-dependencies -DoutputDirectory=.\lib -DincludeScope=runtime
call mvn test
call mvn package
