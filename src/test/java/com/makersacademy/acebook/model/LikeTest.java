package com.makersacademy.acebook.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LikeTest {
    @Test
        public void likeHasPostAndUserId() {
        User user = new User("Claire", "Test@Testemail");
        user.setId(1L);

        Post post = new Post("hello", user);
        post.setId(1L);

        Like like = new Like(post, user);
        assertThat(like.getPost().getId(), is(1L));
        assertThat(like.getUser().getId(), is(1L));

    }
}
