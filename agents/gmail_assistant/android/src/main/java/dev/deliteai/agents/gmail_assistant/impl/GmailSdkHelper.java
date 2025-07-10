/*
 * SPDX-FileCopyrightText: (C) 2025 DeliteAI Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.deliteai.agents.gmail_assistant.impl;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import dev.deliteai.agents.gmail_assistant.dataModels.Email;
import dev.deliteai.agents.gmail_assistant.impl.common.Constants;

public class GmailSdkHelper {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "DeliteAI Gmail Agent";
    private static GmailSdkHelper instance;
    private Gmail service;

    private GmailSdkHelper() {}

    public static synchronized GmailSdkHelper getInstance() {
        if (instance == null) {
            instance = new GmailSdkHelper();
        }
        return instance;
    }

    public void init(GoogleAccountCredential credential) {
        NetHttpTransport netHttpTransport = new NetHttpTransport();
        service = new Gmail.Builder(netHttpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<Email> fetchEmails(String query) throws IOException {
        List<Email> emails = new ArrayList<>();
        ListMessagesResponse messagesResponse = service.users().messages().list("me")
                .setMaxResults(Constants.MAX_EMAILS)
                .setQ(query)
                .execute();

        if (messagesResponse.getMessages() == null) return emails;

        for (Message msg : messagesResponse.getMessages()) {
            Message fullMessage = service.users().messages().get("me", msg.getId()).execute();
            String subject = null;
            String sender = null;
            for (MessagePartHeader header : fullMessage.getPayload().getHeaders()) {
                if ("Subject".equalsIgnoreCase(header.getName())) {
                    subject = header.getValue();
                } else if ("From".equalsIgnoreCase(header.getName())) {
                    sender = header.getValue();
                }
            }
            String body = getPlainTextFromMessage(fullMessage.getPayload());
            emails.add(new Email(sender, subject, body));
        }
        return emails;
    }

    public List<Email> fetchUnreadEmails() throws IOException {
        return fetchEmails("is:unread");
    }

    private String getPlainTextFromMessage(MessagePart part) {
        if (part.getParts() == null) {
            if ("text/plain".equals(part.getMimeType()) && part.getBody() != null && part.getBody().getData() != null) {
                return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
            }
        } else {
            for (MessagePart subPart : part.getParts()) {
                String result = getPlainTextFromMessage(subPart);
                if (result != null) return result;
            }
        }
        return null;
    }
}
