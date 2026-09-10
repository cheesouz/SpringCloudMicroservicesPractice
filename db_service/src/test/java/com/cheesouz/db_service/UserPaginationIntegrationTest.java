package com.cheesouz.db_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsDefaultFirstPage() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberOfElements").value(20))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalElements").value(100))
                .andExpect(jsonPath("$.totalPages").value(5));
    }

    @Test
    void filtersByLastNameIgnoreCase() throws Exception {
        mockMvc.perform(get("/users").param("lastName", "riNgEr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value("Ringer"));
    }

    @Test
    void filtersByDateOfBirth() throws Exception {
        mockMvc.perform(get("/users").param("dateOfBirth", "1978-07-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Yasmin"));
    }

    @Test
    void sortsByLastNameDescending() throws Exception {
        mockMvc.perform(get("/users").param("sort", "lastName,desc").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastName").value("Yakovliv"));
    }

    @Test
    void rejectsUnsupportedSortProperty() throws Exception {
        mockMvc.perform(get("/users").param("sort", "firstName,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnsupportedSortDirection() throws Exception {
        mockMvc.perform(get("/users").param("sort", "lastName,sideways"))
                .andExpect(status().isBadRequest());
    }
}