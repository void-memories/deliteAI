/*
 * SPDX-FileCopyrightText: (C) 2025 DeliteAI Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.deliteai.agents.gmail_assistant.impl

internal class DependencyContainer private constructor() {
    private val llmManagerSingleton = LlmManager()
    private val gmailSdkHelperSingleton = GmailSdkHelper.getInstance()
    fun getLlmManager(): LlmManager = llmManagerSingleton
    fun getController(): Controller = Controller(this)
    fun getGmailSdkHelper(): GmailSdkHelper = gmailSdkHelperSingleton

    companion object {
        private var instance: DependencyContainer? = null

        @Synchronized
        fun getInstance(): DependencyContainer {
            if (instance == null) {
                instance = DependencyContainer()
            }
            return instance!!
        }
    }
}
