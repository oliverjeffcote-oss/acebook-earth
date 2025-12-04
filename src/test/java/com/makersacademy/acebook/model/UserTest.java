package com.makersacademy.acebook.model;

import jakarta.validation.constraints.AssertTrue;
import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class UserTest {
    @Test
        public void usernameIsSetAndUserIsEnabled() {
        User user = new User("Ollie");
        assertThat(user.getUsername(), containsString("Ollie"));
        assertThat(user.isEnabled(), is(true));
    }
}
