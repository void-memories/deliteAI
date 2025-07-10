/*
 * SPDX-FileCopyrightText: (C) 2025 DeliteAI Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.deliteai.agents.gmail_assistant.impl

import dev.deliteai.NimbleNet
import dev.deliteai.datamodels.NimbleNetTensor
import dev.deliteai.impl.common.DATATYPE
import org.json.JSONArray
import org.json.JSONObject
import dev.deliteai.agents.gmail_assistant.dataModels.Email
import dev.deliteai.agents.gmail_assistant.dataModels.GmailSummary

class LlmManager {
    suspend fun summarizeEmails(emails: List<Email>): GmailSummary {
        val res = NimbleNet.runMethod(
            methodName = "summarize_emails",
            inputs = hashMapOf(
                "emails" to NimbleNetTensor(
                    data = JSONArray(emails.map { JSONObject(it.toString()) }).toString(),
                    datatype = DATATYPE.STRING,
                    shape = null
                )
            )
        )

        if (!res.status) throw Exception(res.error?.message)

        return GmailSummary(
            summary = res.payload!!["summary"]!!.data as String
        )
    }

    suspend fun promptLlm(prompt: String, queryGmailCallback: Any): String {
        val res = NimbleNet.runMethod(
            methodName = "prompt_llm",
            inputs = hashMapOf(
                "prompt" to NimbleNetTensor(prompt, DATATYPE.STRING, null),
                "queryGmail" to NimbleNetTensor(
                    data = queryGmailCallback,
                    datatype = DATATYPE.FUNCTION,
                    shape = intArrayOf()
                )
            )
        )

        if (!res.status) throw Exception(res.error?.message)
        return res.payload!!["result"]!!.data as String
    }
}
