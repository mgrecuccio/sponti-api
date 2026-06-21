package com.mgrtech.sponti_api.matching.internal.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.matching.api.ContactLinkType;
import com.mgrtech.sponti_api.matching.api.ContactLinkView;
import com.mgrtech.sponti_api.matching.api.MatchInvitationView;
import com.mgrtech.sponti_api.matching.api.MatchView;
import com.mgrtech.sponti_api.matching.api.SuggestedMatchView;
import com.mgrtech.sponti_api.matching.internal.application.MatchingFacade;
import com.mgrtech.sponti_api.matching.internal.application.command.CreateMatchCommand;
import com.mgrtech.sponti_api.matching.internal.exception.MatchProposalExpiredException;
import com.mgrtech.sponti_api.shared.api.ChannelType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MatchControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenService jwtTokenService;

    @MockitoBean
    MatchingFacade matchingFacade;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void return_match_suggestions_for_authenticated_users() throws Exception {
        given(matchingFacade.getSuggestions(42L))
                .willReturn(List.of(new SuggestedMatchView(
                        33L,
                        "nickName",
                        true,
                        ChannelType.CHAT,
                        123,
                        Instant.parse("2026-03-30T09:00:00Z"),
                        Instant.parse("2026-03-30T09:00:00Z")
                )));

        mockMvc.perform(get("/api/v1/matches/suggestions")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].candidateUserId").value(33L))
                .andExpect(jsonPath("$[0].nickName").value("nickName"))
                .andExpect(jsonPath("$[0].favorite").value(true))
                .andExpect(jsonPath("$[0].score").value(123));
    }

    @Test
    void return_incoming_match_invitations_for_authenticated_user() throws Exception {
        given(matchingFacade.getIncomingMatches(42L))
                .willReturn(List.of(new MatchInvitationView(
                        99L,
                        11L,
                        null,
                        ChannelType.CHAT,
                        "PROPOSED",
                        188,
                        Instant.parse("2026-03-30T09:00:00Z"),
                        Instant.parse("2026-03-30T10:00:00Z"),
                        Instant.parse("2026-03-30T08:00:00Z"),
                        null
                )));

        mockMvc.perform(get("/api/v1/matches/incoming")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(99L))
                .andExpect(jsonPath("$[0].initiatorUserId").value(11L))
                .andExpect(jsonPath("$[0].channelType").value("CHAT"))
                .andExpect(jsonPath("$[0].status").value("PROPOSED"))
                .andExpect(jsonPath("$[0].score").value(188))
                .andExpect(jsonPath("$[0].overlapStart").value("2026-03-30T09:00:00Z"))
                .andExpect(jsonPath("$[0].overlapEnd").value("2026-03-30T10:00:00Z"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-03-30T08:00:00Z"));
    }

    @Test
    void create_match_and_returns_match_view() throws Exception {
        given(matchingFacade
                .createMatch(42L, new CreateMatchCommand(11L, ChannelType.CHAT)))
                .willReturn( new MatchView(
                    1L,
                    11L,
                    ChannelType.CHAT,
                    "PENDING",
                    188,
                    Instant.parse("2026-03-30T09:00:00Z"),
                    Instant.parse("2026-03-30T09:00:00Z"),
                    Instant.parse("2026-03-30T09:00:00Z"),
                    Instant.parse("2026-03-30T09:00:00Z")
                ));

        var request = new MatchController.CreateMatchRequest(11L, ChannelType.CHAT);

        mockMvc.perform(post("/api/v1/matches")
                        .principal(new TestingAuthenticationToken("42", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.candidateUserId").value(11L))
                .andExpect(jsonPath("$.channelType").value("CHAT"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.overlapStart").value("2026-03-30T09:00:00Z"))
                .andExpect(jsonPath("$.overlapEnd").value("2026-03-30T09:00:00Z"))
                .andExpect(jsonPath("$.createdAt").value("2026-03-30T09:00:00Z"))
                .andExpect(jsonPath("$.respondedAt").value("2026-03-30T09:00:00Z"));
    }

    @Test
    void accept_match_and_returns_match_view() throws Exception {
        given(matchingFacade.acceptMatch(42L, 99L))
                .willReturn(new MatchView(
                        99L,
                        11L,
                        ChannelType.CHAT,
                        "ACCEPTED",
                        188,
                        Instant.parse("2026-03-30T09:00:00Z"),
                        Instant.parse("2026-03-30T10:00:00Z"),
                        Instant.parse("2026-03-30T08:00:00Z"),
                        Instant.parse("2026-03-30T09:30:00Z")
                ));

        mockMvc.perform(patch("/api/v1/matches/{id}/accept", 99L)
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99L))
                .andExpect(jsonPath("$.candidateUserId").value(11L))
                .andExpect(jsonPath("$.channelType").value("CHAT"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.score").value(188))
                .andExpect(jsonPath("$.overlapStart").value("2026-03-30T09:00:00Z"))
                .andExpect(jsonPath("$.overlapEnd").value("2026-03-30T10:00:00Z"))
                .andExpect(jsonPath("$.createdAt").value("2026-03-30T08:00:00Z"))
                .andExpect(jsonPath("$.respondedAt").value("2026-03-30T09:30:00Z"));
    }

    @Test
    void decline_match_and_returns_match_view() throws Exception {
        given(matchingFacade.declineMatch(42L, 99L))
                .willReturn(new MatchView(
                        99L,
                        11L,
                        ChannelType.CALL,
                        "DECLINED",
                        90,
                        Instant.parse("2026-03-30T09:00:00Z"),
                        Instant.parse("2026-03-30T10:00:00Z"),
                        Instant.parse("2026-03-30T08:00:00Z"),
                        Instant.parse("2026-03-30T09:30:00Z")
                ));

        mockMvc.perform(patch("/api/v1/matches/{id}/decline", 99L)
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99L))
                .andExpect(jsonPath("$.candidateUserId").value(11L))
                .andExpect(jsonPath("$.channelType").value("CALL"))
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.score").value(90))
                .andExpect(jsonPath("$.overlapStart").value("2026-03-30T09:00:00Z"))
                .andExpect(jsonPath("$.overlapEnd").value("2026-03-30T10:00:00Z"))
                .andExpect(jsonPath("$.createdAt").value("2026-03-30T08:00:00Z"))
                .andExpect(jsonPath("$.respondedAt").value("2026-03-30T09:30:00Z"));
    }

    @Test
    void accept_match_returns_conflict_when_match_is_expired() throws Exception {
        given(matchingFacade.acceptMatch(42L, 99L))
                .willThrow(new MatchProposalExpiredException("Match proposal has expired"));

        mockMvc.perform(patch("/api/v1/matches/{id}/accept", 99L)
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MATCH_PROPOSAL_EXPIRED"))
                .andExpect(jsonPath("$.detail").value("Match proposal has expired"));
    }

    @Test
    void create_contact_link_returns_documented_response() throws Exception {
        given(matchingFacade.createContactLink(99L, 42L))
                .willReturn(new ContactLinkView(
                        ContactLinkType.WHATSAPP,
                        "https://wa.me/32470123456",
                        null
                ));

        mockMvc.perform(post("/api/v1/matches/{id}/contact-link", 99L)
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WHATSAPP"))
                .andExpect(jsonPath("$.url").value("https://wa.me/32470123456"))
                .andExpect(jsonPath("$.expiresAt").doesNotExist());
    }
}
