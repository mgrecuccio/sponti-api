package com.mgrtech.sponti_api.contact.internal.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.contact.internal.application.ContactFacade;
import com.mgrtech.sponti_api.contact.internal.application.command.EditContactCommand;
import com.mgrtech.sponti_api.contact.internal.application.command.SendContactInvitationCommand;
import com.mgrtech.sponti_api.contact.internal.application.view.ContactInvitationView;
import com.mgrtech.sponti_api.contact.api.view.ContactView;
import com.mgrtech.sponti_api.contact.api.view.PendingContactInvitationView;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ContactFacade contactFacade;

    @MockitoBean
    JwtTokenService jwtTokenService;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returns_contact_list_for_authenticated_users() throws Exception {
        given(contactFacade.getAcceptedContacts(42L))
                .willReturn(List.of(new ContactView(
                        33L,
                        "nickName",
                        true,
                        Instant.now()
                )));

        mockMvc.perform(get("/api/v1/contacts")
                .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactUserId").value(33L))
                .andExpect(jsonPath("$[0].nickName").value("nickName"))
                .andExpect(jsonPath("$[0].favorite").value(true));
    }

    @Test
    void send_invitation_and_returns_contact_invitation_view() throws Exception {
        var request = new ContactController.SendContactInvitationRequest(
                "recipient@example.com",
                "teamMate"
        );

        given(contactFacade.sendInvitation(42L, new SendContactInvitationCommand(request.email(), request.nickName())))
                .willReturn(new ContactInvitationView(
                        33L,
                        42L,
                        22L,
                        request.nickName(),
                        "PENDING",
                        Instant.now()
                ));

        mockMvc.perform(post("/api/v1/contacts/invitations")
                .principal(new TestingAuthenticationToken("42", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderUserId").value(42L))
                .andExpect(jsonPath("$.recipientUserId").value(22L))
                .andExpect(jsonPath("$.nickName").value(request.nickName()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void send_invitation_returns_404_not_found_if_user_not_present() throws Exception {
        var request = new ContactController.SendContactInvitationRequest(
                "recipient@example.com",
                "teamMate"
        );

        given(contactFacade.sendInvitation(42L, new SendContactInvitationCommand(request.email(), request.nickName())))
                .willThrow(UserNotFoundException.class);
        mockMvc.perform(post("/api/v1/contacts/invitations")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void send_invitation_returns_400_if_request_not_valid() throws Exception {
        var request = new ContactController.SendContactInvitationRequest(
                "wrong-email",
                "teamMate"
        );

        mockMvc.perform(post("/api/v1/contacts/invitations")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void block_user_returns_no_content() throws Exception {
        mockMvc.perform(post("/api/v1/contacts/{contactUserId}/block", "22")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isNoContent());
    }

    @Test
    void accept_invitation_returns_no_content() throws Exception {
        mockMvc.perform(post("/api/v1/contacts/invitations/{invitationId}/accept", "22")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancel_invitation_returns_no_content() throws Exception {
        mockMvc.perform(delete("/api/v1/contacts/invitations/{invitationId}", 22L)
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isNoContent());

        verify(contactFacade).cancelInvitation(42L, 22L);
    }

    @Test
    void remove_contact_returns_no_content() throws Exception {
        mockMvc.perform(delete("/api/v1/contacts/{contactUserId}", "22")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isNoContent());
    }

    @Test
    void edit_contact_and_returns_contact_view() throws Exception {
        var request = new ContactController.UpdateContactRequest(
                "teamMate",
                true
        );

        given(contactFacade.editContact(
                42L,
                33L,
                new EditContactCommand(request.nickName(), request.favorite()))
        )
                .willReturn(new ContactView(
                        33L,
                        request.nickName(),
                        Boolean.TRUE,
                        Instant.now()
                ));

        mockMvc.perform(put("/api/v1/contacts/{contactUserId}", 33L)
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.contactUserId").value(33L))
                .andExpect(jsonPath("$.nickName").value(request.nickName()))
                .andExpect(jsonPath("$.favorite").value(Boolean.TRUE));
    }

    @Test
    void edit_contact_accepts_null_favorite() throws Exception {
        var request = new ContactController.UpdateContactRequest("teamMate", null);

        given(contactFacade.editContact(
                42L,
                33L,
                new EditContactCommand(request.nickName(), null))
        )
                .willReturn(new ContactView(
                        33L,
                        request.nickName(),
                        Boolean.TRUE,
                        Instant.now()
                ));

        mockMvc.perform(put("/api/v1/contacts/{contactUserId}", 33L)
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactUserId").value(33L))
                .andExpect(jsonPath("$.nickName").value(request.nickName()))
                .andExpect(jsonPath("$.favorite").value(Boolean.TRUE));
    }

    @Test
    void edit_contact_returns_400_if_request_not_valid() throws Exception {
        var request = new ContactController.UpdateContactRequest(null, false);

        mockMvc.perform((put("/api/v1/contacts/{contactUserId}", 22L))
                .principal(new TestingAuthenticationToken("42", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void returns_no_content_if_no_contact_list_is_found_for_authenticated_users() throws Exception {
        given(contactFacade.getAcceptedContacts(42L))
                .willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/contacts")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void returns_5xx_for_unsupported_principal() throws Exception {
        var principal = new Object();
        var authentication = new TestingAuthenticationToken(principal, null);

        mockMvc.perform(get("/api/v1/contacts").principal(authentication))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void returns_pending_invitations_for_authenticated_user() throws Exception {
        given(contactFacade.getPendingIncomingInvitations(42L))
                .willReturn(List.of(new PendingContactInvitationView(
                        110L,
                        18L,
                        "sender@example.com",
                        "Sender",
                        "Team mate",
                        "PENDING",
                        Instant.now()
                )));

        mockMvc.perform(get("/api/v1/contacts/invitations/pending")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invitationId").value(110L))
                .andExpect(jsonPath("$[0].senderUserId").value(18L))
                .andExpect(jsonPath("$[0].senderEmail").value("sender@example.com"))
                .andExpect(jsonPath("$[0].senderDisplayName").value("Sender"))
                .andExpect(jsonPath("$[0].nickName").value("Team mate"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
}
