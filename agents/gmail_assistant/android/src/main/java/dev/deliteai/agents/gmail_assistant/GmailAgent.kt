/*
 * SPDX-FileCopyrightText: (C) 2025 DeliteAI Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.deliteai.agents.gmail_assistant

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import dev.deliteai.agents.gmail_assistant.dataModels.GmailSummary
import dev.deliteai.agents.gmail_assistant.impl.Controller
import dev.deliteai.agents.gmail_assistant.impl.DependencyContainer
import java.util.concurrent.atomic.AtomicBoolean
import dev.deliteai.datamodels.NimbleNetResult
import dev.deliteai.datamodels.NimbleNetError
import android.util.Log
import dev.deliteai.NimbleNet
import dev.deliteai.agents.gmail_assistant.impl.common.Constants
import kotlinx.coroutines.delay

object GmailAgent {
    private val isAgentInitialized = AtomicBoolean(false)
    private lateinit var controller: Controller

    suspend fun initialize(credential: GoogleAccountCredential): NimbleNetResult<Unit> =
        runCatchingResult(initRequired = false) {
            if (!isAgentInitialized.get()) {
                val dependencyContainer = DependencyContainer.getInstance()
                controller = dependencyContainer.getController()
                dependencyContainer.getGmailSdkHelper().init(credential)
                isAgentInitialized.set(true)
            }
        }

    /**
     * Summarizes the unread emails in the user's inbox.
     *
     * @return The summary of the unread emails.
     * @throws IllegalStateException if the agent is not initialized.
     */
    suspend fun summarizeUnreadEmails(): NimbleNetResult<GmailSummary> =
        runCatchingResult {
            controller.summarizeUnreadEmails()
        }

    /**
     * Prompts the LLM with a custom prompt and returns the result as a String.
     *
     * Example prompts:
     * - "Draft a response for the last email I received."
     * - "What were the action items from Bob's meeting?"
     *
     * @param prompt The prompt to send to the LLM. This can be any instruction or question about your Gmail data.
     * @return The LLM's response as a String.
     * @throws IllegalStateException if the agent is not initialized.
     */
    suspend fun promptLlm(prompt: String): NimbleNetResult<String> =
        runCatchingResult {
            controller.promptLlm(prompt)
        }

    //helpers
    private suspend fun <T> runCatchingResult(
        initRequired: Boolean = true,
        action: suspend () -> T,
    ): NimbleNetResult<T> {
        if (initRequired) {
            if (!isAgentInitialized.get())
                throw IllegalStateException("Please initialize GmailAgent first.")
            waitForNimbleNetReady()
        }
        return try {
            val result = action()
            NimbleNetResult(status = true, payload = result)
        } catch (e: Exception) {
            Log.e(Constants.TAG, e.toString())
            NimbleNetResult(
                status = false,
                payload = null,
                error = NimbleNetError(123, e.message ?: "Something went wrong")
            )
        }
    }

    private suspend fun waitForNimbleNetReady() {
        while (true) {
            val res = NimbleNet.isReady()
            if (res.status) break
            Log.i(Constants.TAG, "NN is not ready. ${res.error?.message}")
            delay(1000)
        }
    }
}
