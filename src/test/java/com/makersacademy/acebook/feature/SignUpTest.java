package com.makersacademy.acebook.feature;

import com.github.javafaker.Faker;
//import org.junit.After;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
// Ensure Flyway clean is enabled for this test context
@TestPropertySource(properties = "spring.flyway.clean-disabled=false")
public class SignUpTest {

    WebDriver driver;
    Faker faker;
    @Autowired
    private Flyway flyway;

    @BeforeEach
    public void setup() {
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
        driver = new ChromeDriver();
        faker = new Faker();
        flyway.clean();
        flyway.migrate();
    }

    @AfterEach
    public void tearDown() {
        driver.close();
    }

    @Test
    public void successfulSignUpAlsoLogsInUser() {
        String email = faker.name().username() + "@email.com";

        driver.get("http://localhost:8081/");
        driver.findElement(By.linkText("Sign up")).click();
        driver.findElement(By.name("email")).sendKeys(email);
        driver.findElement(By.name("password")).sendKeys("P@55qw0rd");
        driver.findElement(By.name("action")).click();
//        driver.findElement(By.name("action")).click(); // This line is a duplicate and breaks the testing
        String greetingText = driver.findElement(By.id("greeting")).getText();
        Assertions.assertEquals("Signed in as " + email, greetingText);
    }
}
