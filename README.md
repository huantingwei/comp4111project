# COMP4111 Project

This is a RESTful web service in Java language for library book management.

## Running the program

* ``` git clone https://github.com/huantingwei/comp4111project.git```
* run the SQL file **dbConfig.sql** to establish the database instance and tables
* run **BookManagementServer** class to start the server
  * **The server will first initialize 100 users until it print "Finished initializing 100 users". Please wait until it finishes.**
  * **The server is run on ```http://localhost:8081/BookManagementService```**


## Built With
* [Gradle]() - Dependency Management
* [HTTP Core 4.4](https://hc.apache.org/httpcomponents-core-4.4.x/tutorial/html/index.html) - HTTP framework
* [MySQL 5.7](https://dev.mysql.com/doc/refman/5.7/en/installing.html) - database used to store and query data
* [MySQL Connector/J 5.1](https://dev.mysql.com/downloads/connector/j/5.1.html) - driver for database connection
* [Jackson 2.10.0](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.10) - JSON library


## Authors

* **Huan Ting Wei**
* **Chanhyeok Yim**

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
