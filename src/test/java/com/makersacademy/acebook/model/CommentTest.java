package com.makersacademy.acebook.model;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;



public class CommentTest {
    @Test
    public void commentContentIsPresent() {
        Comment comment = new Comment("This is a comment");
        assertThat(comment.getContent(), containsString("This is a comment"));
    }
}
