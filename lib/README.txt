Put the JDBC driver jar(s) in this folder. The run scripts add everything in "lib" to the classpath.

SQLite (default):  sqlite-jdbc-<version>.jar  from https://github.com/xerial/sqlite-jdbc/releases
                   (if you get a NoClassDefFoundError for org/slf4j, also add slf4j-api-<version>.jar)
MySQL / MariaDB:   mysql-connector-j-<version>.jar  (see db.properties.example in the project root)

The jar files are not committed to git (see .gitignore).
