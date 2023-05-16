## data-source module

The module provides one database configuration and wraps it up with an interface so that application can extend it and implement all the db access along with caches. Unlike the data-source module that supports multiple data sources, it only support one database. Also, it provides an interface with default provider implementation to allow the developer to quickly start a simple database related API. 



### Implementation detail:

light-4j db-provider module use lightweight java DB connection library (HikariCP) as datasource base:

```
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </dependency>

```

You also need to add a database driver as part of your application dependency. For example, here is the postgresql.


```
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
```

In most cases, you need to cache some of the rows from the database in order to speed up the response from your API. You should include Caffeine as the cache layer. 

```
        <dependency>
          <groupId>com.github.ben-manes.caffeine</groupId>
          <artifactId>caffeine</artifactId>
        </dependency>
```

### Database Configuration

There is a config file db-provider.yml that contains all the information that you can create a HiKariCP datasource.

Here is an example. 

```
# For postgres database running in a docker container, you have to use the driverClassName. By
# using the dataSourceClassName, you cannot connect to the database from another docker container.
driverClassName: ${db.driverClassName:org.postgresql.Driver}
jdbcUrl: ${db.jdbcUrl:jdbc:postgresql://timescale:5432/configserver}
username: ${db.username:postgres}
password: ${db.password:secret}
maximumPoolSize: ${db.maximumPoolSize:3}

```

### Module usage detail:
 
There are two config files involve withe the setting (datasource.yml & service.yml) and both two be extend to use values.yml"

For example:

datasource.yml

```
MysqlDataSource: ${datasource.MysqlDataSource:}

```  

service.yml

```
singletons: ${service.singletons:}

```

values.yml

```
# Service Singletons
service.singletons:
  - com.networknt.decrypt.Decryptor:
      - com.networknt.decrypt.AESDecryptor

  - com.networknt.db.GenericDataSource:
      - com.networknt.db.MysqlDataSource:
          - java.lang.String: 

  - com.networknt.accountservic.dao.AccountDao:
      - com.networknt.accountservic.dao.AccountDaoImpl


# datasource.yml
datasource.MysqlDataSource:
  DriverClassName: com.mysql.jdbc.Driver
  jdbcUrl: jdbc:mysql://localhost:3308/account_db?useSSL=false
  username: account_user
  password: CRYPT:odPqWOazjDxeVcOU3j0YCc2+LdwfgiJmoFcWTSoKRUw=
  maximumPoolSize: 2
  connectionTimeout: 5000
  settings:
    idleTimeout: 50000
    minimumIdle: 1
  parameters:
    useServerPrepStmts: 'true'
    cachePrepStmts: 'true'
    cacheCallableStmts: 'true'
    prepStmtCacheSize: '4096'
    prepStmtCacheSqlLimit: '2048'
    verifyServerCertificate: 'false'
    useSSL: 'true'
    requireSSL: 'true'


```
The Database config value include three parts:

- main section which include major config for HikariDataSource, for example connection string,username/password, etc.

- settings section, which used to invoke HikariDataSource set methods for the specified parameters.
  for example, if there is a parameter:  idleTimeout: 50000
  it will call HikariDataSource setIdleTimeout() method for specified value
    
- parameters section which used to specify the datasource config properties for HikariDataSource(addDataSourceProperty())                                            
 
DAO java class:

```
    private DataSource dataSource;

    public AccountDaoImpl(GenericDataSource genericDataSource) {
        dataSource = genericDataSource.getDataSource();
    }

```

- By using datasource factory 

service.yml
```
- com.networknt.db.factory.DataSourceFactory:
    - com.networknt.db.factory.DefaultDataSourceFactory
```

Get the datasource by default datasource factory

```
        DataSource dataSource = SingletonServiceFactory.getBean(DataSourceFactory.class).getDataSource("OracleDataSource");
```


### For the detail document, pleases refer to

* [Tutorial document](https://doc.networknt.com/concern/datasource/#readout) with step by step guide to set and use datasources
