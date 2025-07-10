/*
 * SPDX-FileCopyrightText: (C) 2025 DeliteAI Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.deliteai.agents.gmail_assistant.impl

import dev.deliteai.agents.gmail_assistant.dataModels.GmailSummary
import dev.deliteai.datamodels.NimbleNetTensor
import org.json.JSONArray
import org.json.JSONObject

internal class Controller(private val dependencyContainer: DependencyContainer) {
    private val llmManager: LlmManager = dependencyContainer.getLlmManager()
    private val gmailSdkHelper = dependencyContainer.getGmailSdkHelper()

    suspend fun summarizeUnreadEmails(): GmailSummary {
        val unreadEmails = gmailSdkHelper.fetchUnreadEmails()
        return llmManager.summarizeEmails(unreadEmails)
    }

    suspend fun promptLlm(prompt: String): String {
        return llmManager.promptLlm(prompt, queryGmailCallbackFunction())
    }

    private fun queryGmailCallbackFunction() = fun(inp: HashMap<String, NimbleNetTensor>?): HashMap<String, NimbleNetTensor>? {
        val valueTensor = inp!!["query"] as NimbleNetTensor
        val queryString = valueTensor.data as String
        val emails = gmailSdkHelper.fetchEmails(queryString)
        val emailContent = JSONArray(emails.map { JSONObject(it.toString()) }).toString()
        return hashMapOf(
            "emails" to NimbleNetTensor(emailContent),
        )
    }
}
