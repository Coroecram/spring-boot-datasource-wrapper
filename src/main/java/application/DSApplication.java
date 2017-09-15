package application;

import javax.sql.DataSource;
import datasourcewrapper.DataSourceWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@ServletComponentScan
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
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
