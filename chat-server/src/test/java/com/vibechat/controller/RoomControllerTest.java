package com.vibechat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibechat.service.room.RoomServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomServiceImpl roomService;

//    @Test
//    void createRoom_whenAuthenticated_shouldReturnCreated() throws Exception {
//        // Given
//        Long userId = 1L;
//        RoomCreateRequest request = new RoomCreateRequest();
//        request.setTitle("New Room");
//        request.setIsPrivate(false);
//        request.setTags(Collections.singletonList("java"));
//
//        RoomResponse response = new RoomResponse();
//        response.setId(1L);
//        response.setTitle("New Room");
//
//        when(roomService.createRoom(any(RoomCreateRequest.class), eq(userId))).thenReturn(response);
//
//        MockHttpSession session = new MockHttpSession();
//        session.setAttribute("userId", userId);
//
//        // When & Then
//        mockMvc.perform(post("/api/rooms")
//                .session(session)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").value(1L))
//                .andExpect(jsonPath("$.title").value("New Room"));
//    }
}
