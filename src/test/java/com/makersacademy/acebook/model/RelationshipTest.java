package com.makersacademy.acebook.model;

import jakarta.validation.constraints.AssertTrue;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class RelationshipTest {
    @Test
        public void relationshipHasUserIdsAndStatus() {
        User user1 = new User("Ollie");
        user1.setId(1L);

        User user2 = new User("Claire");
        user2.setId(2L);

        Relationship relationship = new Relationship(user1, user2);
        assertThat(relationship.getRequester().getId(), is(1L));
        assertThat(relationship.getReceiver().getId(), is(2L));


    }
}
