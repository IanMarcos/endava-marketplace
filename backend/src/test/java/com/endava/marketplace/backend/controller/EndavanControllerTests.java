package com.endava.marketplace.backend.controller;

import com.endava.marketplace.backend.model.Endavan;
import com.endava.marketplace.backend.service.EndavanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EndavanController.class)
@AutoConfigureMockMvc(addFilters = false)
public class EndavanControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EndavanService endavanService;

    @Test
    public void givenUserLoginCredentials_whenCreateUser_thenReturnsCreatedUser() throws Exception{
        Endavan endavan = Endavan.builder()
                .id(1L)
                .name("Endavan #1")
                .email("email@example.com")
                .admin(false)
                .build();

        given(endavanService.saveEndavan()).willReturn(endavan);

        this.mockMvc.perform(post("/api/user/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endavan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", CoreMatchers.is(endavan.getId().intValue())));
    }

    @Test
    public void givenEndavanId_whenGetEndavanById_ReturnsEndavan() throws Exception {
        Long endavanId = 1L;

        Endavan endavan = Endavan.builder()
                .id(1L)
                .name("Endavan #1")
                .email("email@example.com")
                .admin(false)
                .build();

        given(endavanService.findEndavanById(endavanId.intValue())).willReturn(Optional.of(endavan));

        ResultActions response = mockMvc.perform(get("/api/user/get/{endavanId}", endavanId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(endavan)));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(endavan.getId().intValue())))
                .andExpect(jsonPath("$.name", is(endavan.getName())))
                .andExpect(jsonPath("$.email", is(endavan.getEmail())));
    }
}
