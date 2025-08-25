package com.stocks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.util.Properties;

@EnableScheduling
@PropertySource("classpath:application2.properties")
@SpringBootApplication
public class AppConfig {

	@Value("${spring.mail.host}")
	private String mailHost;

	@Value("${spring.mail.port}")
	private int mailPort;

	@Value("${spring.mail.username}")
	private String mailUsername;

	@Value("${spring.mail.password}")
	private String mailPassword;

	@Value("${mail.recipients}")
	private String recipients;

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(AppConfig.class, args);
	}

	@Bean
	public JavaMailSender javaMailSender() {
        /*JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        Properties mailProperties = new Properties();
        mailProperties.put("mail.smtp.auth", true);
        mailProperties.put("mail.smtp.starttls.enable", true);
        ;
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername("nithin0991@gmail.com");
        mailSender.setPassword("fqpt zrbo rpnk saps");
        mailSender.setJavaMailProperties(mailProperties);*/

		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(mailHost);
		mailSender.setPort(mailPort);
		mailSender.setUsername(mailUsername);
		mailSender.setPassword(mailPassword);

		Properties properties = mailSender.getJavaMailProperties();
		properties.put("mail.smtp.starttls.enable", Boolean.TRUE);
		properties.put("mail.transport.protocol", "smtp");
		properties.put("mail.smtp.auth", Boolean.TRUE);
		properties.put("mail.smtp.starttls.required", Boolean.TRUE);
		properties.put("mail.smtp.ssl.enable", Boolean.FALSE);
		properties.put("mail.test-connection", Boolean.TRUE);
		properties.put("mail.debug", Boolean.TRUE);

		mailSender.setJavaMailProperties(properties);


		return mailSender;
	}
}