package in.hamids.moneymanager.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;

@Configuration
@Profile("!prod")
public class DataSourceConfig {

    @Value("${supabase.datasource.url}")
    private String supabaseUrl;

    @Value("${supabase.datasource.username}")
    private String supabaseUsername;

    @Value("${supabase.datasource.password}")
    private String supabasePassword;

    @Value("${supabase.datasource.driver-class-name}")
    private String supabaseDriver;

    @Value("${local.datasource.url}")
    private String localUrl;

    @Value("${local.datasource.username}")
    private String localUsername;

    @Value("${local.datasource.password}")
    private String localPassword;

    @Value("${local.datasource.driver-class-name}")
    private String localDriver;

    @Bean
    @Primary
    public DataSource dataSource() {
        if (hasSupabaseConfiguration()) {
            System.out.println("Attempting to connect to Supabase cloud database...");
            try {
                DriverManager.setLoginTimeout(5);
                Class.forName(supabaseDriver);
                try (Connection conn = DriverManager.getConnection(supabaseUrl, supabaseUsername, supabasePassword)) {
                    if (conn.isValid(2)) {
                        System.out.println("Connected to Supabase successfully.");
                        setJpaDialect("org.hibernate.dialect.PostgreSQLDialect");
                        return buildDataSource(
                                supabaseUrl,
                                supabaseUsername,
                                supabasePassword,
                                supabaseDriver,
                                "SupabasePool"
                        );
                    }
                }
            } catch (Throwable e) {
                System.err.println("Supabase connection failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("Falling back to local MySQL database.");
        setJpaDialect("org.hibernate.dialect.MySQLDialect");
        return buildDataSource(
                localUrl,
                localUsername,
                localPassword,
                localDriver,
                "LocalMySqlFallbackPool"
        );
    }

    private boolean hasSupabaseConfiguration() {
        return StringUtils.hasText(supabaseUrl)
                && StringUtils.hasText(supabaseUsername)
                && StringUtils.hasText(supabasePassword)
                && StringUtils.hasText(supabaseDriver);
    }

    private void setJpaDialect(String dialect) {
        System.setProperty("spring.jpa.database-platform", dialect);
        System.setProperty("spring.jpa.properties.hibernate.dialect", dialect);
    }

    private HikariDataSource buildDataSource(
            String url,
            String username,
            String password,
            String driverClassName,
            String poolName
    ) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setPoolName(poolName);
        return dataSource;
    }
}
