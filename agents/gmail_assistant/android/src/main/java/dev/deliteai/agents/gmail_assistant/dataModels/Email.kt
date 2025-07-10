/*
 * SPDX-FileCopyrightText: (C) 2025 DeliteAI Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.deliteai.agents.gmail_assistant.dataModels

data class Email(
    val sender: String,
    val subject: String,
    val body: String
)
