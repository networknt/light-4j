## data-source module

The module provides datasources for different databases. When API need  process database with data retrieve and data persistent, setting datasource will be the start point for the API.

### Implementation detail:

light-4j data-source module use lightweight java DB connection library (HikariCP) as datasource base:

```
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </dependency>

```
data-source module defined a generic datasource(GenericDataSource)  pre-defined several datasources for major databases (extend GenericDataSource):

 - MysqlDataSource
 - PostgresDataSource
 - MariaDataSource
 - OracleDataSource
 - SqlServerDataSource
 - H2DataSource

If user need define new datasources for other databases, simply create a new datasource class which extend GenericDataSource. 
And if user want to customize the pre-defined datasource with other config values, we can extend the pre-defined datasource and add customized config for the datasource.
Please refer to CustomMysqlDataSource in test package

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
