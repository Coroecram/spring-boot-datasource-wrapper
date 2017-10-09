---
layout: post
title: How To Create a Data Source Wrapper in Spring Boot
date:   2017-09-28 12:00:00 -0400
---

I am currently working on a project at work that will take results from the source and return them in [GeoJSON Format](https://tools.ietf.org/html/rfc7946). It is based on an earlier project that we helped with that returned results in OData format. However, I was interested in working with Spring Boot instead of Spring for this project, after the project manager gave a good review for it but said that it would take some more work in order to research it. I was excited to work with a new technology!

## TL;DR: The source code is available at <a href="https://github.com/Coroecram/spring-boot-datasource-wrapper">https://github.com/Coroecram/spring-boot-datasource-wrapper</a>. Check out the /src/main/java/org/datasourcewrapper/DSApplication.java and /src/main/resources/application.properties then go implement your own DataSourceWrapper!

There are many similarities between Spring and Spring-Boot, Spring-Boot *is* built on top of Spring. In general, Spring-Boot attempts to make things more convenient and automatic. However, one of the big changes was with the configuration of Beans, which is one of the most important things for any Spring project.

For this project, we needed to build a datasource wrapper, as I like to call it, that would have separate behaviors than a normal DataSource. For example, we needed to reauthenticate user sessions on the underlying data source on a different connection than the one is used to initially connect to the data source in the Spring-Boot initialization or in the JNDI resource when deployed in Tomcat. We've also added a number of custom Exceptions from the datasource to handle anything that may be thrown.

We will start by looking at the application.properties file for the Spring-Boot application. The application.properties file is a way to set externalized property values so that the code can be easily configurable to work in different environments, or to make all configuration changes in one spot, without needing to change anything in the source code. This allows us to follow two design principles at the same time: Encapsulate what varies and Open to extension; Closed to modification.

```
application.properties:

spring.datasource.driver-class-name=<driver>
spring.datasource.url=jdbc:dbw://host:port/database?userAgent=datasource-application
spring.datasource.username=foo
spring.datasource.password=baz
server.port=1234
```

This is a standard pseudo-setup for a jdbc connection in a Spring Boot application. The values are pretty much self explanatory and I don't need to get into details. If you did an @Autowired public DataSource datasource in the main class of your application, it would setup a javax.sql.DataSource on the spot with just that configuration.

```
DSApplication.java:

@SpringBootApplication
public class DSApplication {

    @Autowired
    private Environment env;

    @Bean
    public DataSource dataSource() {
      final DataSource targetDataSource =
          DataSourceBuilder
              .create()
              .driverClassName(this.env.getProperty("spring.datasource.driver-class-name"))
              .url(this.env.getProperty("spring.datasource.url"))
              .username(this.env.getProperty("spring.datasource.username"))
              .password(this.env.getProperty("spring.datasource.password"))
              .build();

      return new DataSourceWrapper(targetDataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }

    public static void main(String[] args) { ... } }
```
This is the main class of our Spring Boot application. Let's go through it step by step.

First we have:
```
@Autowired
private Environment env;
```
This Autowiring is going to bring in the environmental configuration from our application.properties and make it accessible to our class. How it does this would be a good topic for a future blog post. Having access to these settings is important because we don't want to just Autowire the datasource in this application. We need to create the datasource and then pass it into the constructor of our DataSourceWrapper, which is what will be publicly accessible by other components.

This next section of code does just that, it is taking those application.properties variables and manually building a datasource named targetDataSource. Note the final keyword. This data source is not going to change, we are manually creating our targetDataSource and then passing it right to our DataSourceWrapper. This wrapper is what is being Beanified, and will be easily accessible with @Autowired DataSource datasource in all the other components who can't touch that targetDataSource.

```
@Bean
public DataSource dataSource() {
  final DataSource targetDataSource =
      DataSourceBuilder
          .create()
          .driverClassName(this.env.getProperty("spring.datasource.driver-class-name"))
          .url(this.env.getProperty("spring.datasource.url"))
          .username(this.env.getProperty("spring.datasource.username"))
          .password(this.env.getProperty("spring.datasource.password"))
          .build();

  return new DataSourceWrapper(targetDataSource);
  ```

Some of you may be thinking to yourselves: "That's silly, why go through all that trouble, just autowire the datasource using the application.properties configuration implicitly and then pass THAT bean into another DataSourceWrapper bean! Voila, done!"

Well, you're right, that would work, and it actually did work in this application, but it required a method that I found to be less elegant than this. Less elegant because that targetDataSource would always be accessible to other classes in this application. I only want the real targetDataSource so that I can give it to the DataSourceWrapper, that's it. There are also a couple of other steps that needed to be accomplished in order to do that which were bothersome, but I digress.


Finally, in standard Spring/Spring Boot fashion, we just throw that dataSource Bean into a jdbcTemplate for our components to execute with:
```
@Bean
public JdbcTemplate jdbcTemplate(DataSource dataSource) {
  return new JdbcTemplate(dataSource);
}
```

With that, you are good to go, you can go and program that public class DataSourceWrapper implements DataSource { ... }

##How we programmed our DataSourceWrapper

Let me just show you quickly what we did with ours. In our case, we want to change the user connected to the datasource depending on a username and password they put into an HTTP Basic template in a web page. All of that machinery was left out of this code base (We used a filter.), just picture that little username and password dialog popping up in your mind. Once we get that username and password, we set them as instance parameters in the DataSourceWrapper since they may come in handy later (Also handled by the filter):
```
public class DataSourceWrapper implements DataSource {

    // PARAMETER INIT
    private final static String USER_NAME = "user";
    private final static String PASSWORD =  "password";
    private final static String DATABASE_NAME = "databaseName";
```


Then, the filter asks the DataSourceWrapper to kindly authenticate the user and set an authenticated connection for that user to have. This authenticated connection is handed to the ConnectionWrapper which really does nothing more than hang onto that authenticated connection (It does not deserve it's own blog post, only that sentence.) This is all done through the following method which is really so clear if it is unclear it is because you need to look up PreparedStatements in Spring:
```
@Override
public Connection getConnection() throws SQLException {

  PreparedStatement stmt = null;

  try {

    ConnectionWrapper connection = this.authenticatedConnection.get();
    String command;
    if (connection == null) {
      connection = new ConnectionWrapper(this.datasource.getConnection());

      // The CONNECT command allows indicating a user name, a password
      // and a database to initiate a
      // new session in the server with a new profile.

      command = "CONNECT DATABASE ?";

      stmt = connection.prepareStatement(command);
      stmt.setString(1, this.parameters.get().get(DATABASE_NAME));
    } else {
      command = "CONNECT USER ? PASSWORD ? DATABASE ?";

      stmt = connection.prepareStatement(command);
      stmt.setString(1, this.parameters.get().get(USER_NAME));
      stmt.setString(2, this.parameters.get().get(PASSWORD));
      stmt.setString(3, this.parameters.get().get(DATABASE_NAME));
    }

    stmt.execute();
    this.authenticatedConnection.set(connection);

    return connection;
```

Finally, the type of SQLException returned from the datasource will determine if the user is not authenticated. These are caught and raised to the user with custom messages on the DataSource wrapper.

The custom messages are not part of the method but are here for illustration before the catch block of the overridden getConnection method:
```
/* ERRORS
private static final String AUTHENTICATION_ERROR = "The username or password is incorrect";
private static final String AUTHORIZATION_ERROR = "Insufficient privileges to connect to the database";
private static final String CONNECTION_REFUSED_ERROR = "Connection refused";
private static final String DATABASE_NOT_FOUND_ERROR = ".*Database .* not found";
*/
  catch (final SQLException e) {
    if (e.getMessage() != null) {
      if (e.getMessage().contains(CONNECTION_REFUSED_ERROR)) { // Check connection refused
        throw new ConnectException(e);
      }
      if (e.getMessage().contains(AUTHENTICATION_ERROR)) { // Check invalid credentials
        throw new AuthenticationException(e);
      }
      if (e.getMessage().contains(AUTHORIZATION_ERROR)) { // Check insufficient privileges
        throw new AuthorizationException(e);
      }
      if (e.getMessage().matches(DATABASE_NOT_FOUND_ERROR)) { // Check data base name exists
        throw new ResourceNotFoundException(e);
      }
    }
    //logger.error(e);
    throw e;
  } finally {
      //Make sure to close statement connection
    if (stmt != null) {
      JdbcUtils.closeStatement(stmt);
    }
  } }
```

Finally, never forget to close your [Prepared]Statements!
