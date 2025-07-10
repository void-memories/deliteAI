# SPDX-FileCopyrightText: (C) 2025 DeliteAI Authors
#
# SPDX-License-Identifier: Apache-2.0

from delitepy import nimblenet as nm
from delitepy import ne_re as re

llmMetadata = {
    "endOfTurnToken": "<|im_end|>",
    "maxTokensToGenerate": 2000,
    "tokenizerFileName": "tokenizer.json",
    "temperature": 0.8
}

llm = nm.llm({"name": "qwen3-executorch", "metadata": llmMetadata})


CUSTOM_QUERY_SYSTEM_PROMPT = """You are Delite AI, a chatbot running on device using DeliteAI's platform. Respond to user's questions briefly:

# Tools

You may call one or more functions to assist with the user query.

You are provided with function signatures within <tools></tools> XML tags:
<tools>
[
    {"type": "function", "function": {"name": "queryGmail", "description": "Queries Gmail SDK with the query provided. This returns a list of emails that match the query.", "parameters": {"query": {"type": "string"}}, "required": ["query"]}},
]
</tools>

For each function call, return a json object with function name and arguments within <tool_call></tool_call> XML tags:
<tool_call>
{"name": <function-name>, "arguments": <args-json-object>}
</tool_call>"""

SUMMARIZE_SYSTEMP_PROMPT = "You are Delite AI, a chatbot running on device using DeliteAI's platform. Respond to user's questions briefly:\n"

SYSTEM_PROMPT_START = "<|im_start|>system\n"
USER_PROMPT_BEGIN = "<|im_start|>user\n"
SECTION_END = "\n<|im_end|>\n"
ASSISTANT_RESPONSE_BEGIN = "<|im_start|>assistant\n"
TOOL_RESPONSE_BEGIN = "<im_start>tool\n<tool_call_response>\n"
TOOL_RESPONSE_END = "\n</tool_call_response>\n<im_end>"

text_stream = None
kotlin_functions = {}

def summarize_emails(inp):
    text_stream = None
    emails = inp["emails"]
    system_prompt = SYSTEM_PROMPT_START + SUMMARIZE_SYSTEMP_PROMPT + SECTION_END
    user_query = "Summarize the user's emails in a brief manner focussing on urgent emails and tasks that require immediate attention\n" + emails
    final_prompt = system_prompt + USER_PROMPT_BEGIN + user_query + SECTION_END + ASSISTANT_RESPONSE_BEGIN
    print("final_prompt from prompt_llm function: ", final_prompt)
    text_stream = llm.prompt(final_prompt)
    output = get_response_from_stream(text_stream)
    return {"result": "output"}

def prompt_llm(inp):
    text_stream = None
    query = inp["prompt"]
    func = inp["queryGmail"]
    kotlin_functions["queryGmail"] = func
    system_prompt = SYSTEM_PROMPT_START + CUSTOM_QUERY_SYSTEM_PROMPT + SECTION_END
    final_prompt = system_prompt + USER_PROMPT_BEGIN + query + SECTION_END + ASSISTANT_RESPONSE_BEGIN
    print("final_prompt from prompt_llm function: ", final_prompt)
    text_stream = llm.prompt(final_prompt)
    output = get_response_from_stream(text_stream)

    # Parse tool calls from the output
    tool_calls = get_tool_calls(output)
    if tool_calls:
        print("Tool calls found: ", tool_calls)
        for tool in tool_calls:
            result = call_function(tool)
            print("Result from function call: ", result)
            tool_prompt = TOOL_RESPONSE_BEGIN + result + TOOL_RESPONSE_END + ASSISTANT_RESPONSE_BEGIN
            text_stream = llm.prompt(tool_prompt)
            final_output = get_response_from_stream(text_stream)
            return {"result": "final_output"}

    return {"result": "output"}

def get_response_from_stream(text_stream):
    output = ""
    while text_stream is not None:
        chunk = text_stream.next()
        if chunk is None:
            break
        output = output + chunk
        if text_stream.finished():
            break
    return output

def get_tool_calls(llm_output):
    matches = re.findall(r"<tool_call>([\s\S]*?)</tool_call>", llm_output)
    tool_calls = []
    for match in matches:
        try:
            function_json = nm.parse_json(match.strip())
            print("function_json", function_json)
            function_name = function_json["name"]
            print("function_name", function_name)
            arguments = function_json["arguments"]
            print(arguments)
            tool_calls.append({"name": function_name, "arguments": arguments})
        except:
            print("Could not parse json for a function", match.strip())
    return tool_calls

def call_function(function):
    if function["name"] in kotlin_functions:
        print("Calling function", function["name"])
        func = kotlin_functions[function["name"]]
        return str(func(function["arguments"]))
    else:
        return {"error": "Function not found"}
