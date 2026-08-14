package com.utms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtmsApplicationTest {

    @Test
    void main_applicationClassExists() {
        assertThat(UtmsApplication.class).isNotNull();
    }
}
