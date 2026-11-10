package com.example.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * SpringBootServletInitializer: 외부 Tomcat 에 war 로 배포할 때 Tomcat 이 이 클래스로 앱을 시작한다.
 * java -jar 로 실행할 때는 main() 이 쓰인다.
 */
@SpringBootApplication
public class ReservationApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(ReservationApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(ReservationApplication.class);
    }
}
