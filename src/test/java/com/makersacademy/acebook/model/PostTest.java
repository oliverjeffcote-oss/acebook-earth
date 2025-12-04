package com.makersacademy.acebook.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class PostTest {
    @Test
        public void postHasContentAndUserId() {
        User user = new User("Claire");
        user.setId(1L);

        Post post = new Post("hello", user);

        assertThat(post.getContent(), containsString("hello"));
        assertThat(post.getUser().getId(), is(1L));
        }

    };


