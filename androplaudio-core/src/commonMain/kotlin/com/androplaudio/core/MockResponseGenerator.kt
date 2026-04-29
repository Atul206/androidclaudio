package com.androplaudio.core

import kotlinx.serialization.json.*

internal object MockResponseGenerator {

    fun generate(fn: String, params: List<ParamMetadata>, returnType: String): JsonElement {
        return when {
            returnType == "Unit" || returnType == "void" -> JsonNull
            returnType == "Boolean" -> JsonPrimitive(true)
            returnType == "Int" || returnType == "Long" -> JsonPrimitive(42)
            returnType == "Double" || returnType == "Float" -> JsonPrimitive(1.0)
            returnType == "String" -> JsonPrimitive(mockStringFromFn(fn))
            returnType.startsWith("List") || returnType.startsWith("Flow") ->
                JsonArray(listOf(mockObject(params)))
            else -> mockFromFnName(fn, params)
        }
    }

    private fun mockFromFnName(fn: String, params: List<ParamMetadata>): JsonElement = when {
        fn.startsWith("get") || fn.startsWith("fetch") || fn.startsWith("load") ->
            mockObject(params)
        fn.startsWith("is") || fn.startsWith("has") || fn.startsWith("can") ->
            JsonPrimitive(true)
        fn.startsWith("validate") || fn.startsWith("check") ->
            JsonObject(mapOf("valid" to JsonPrimitive(true), "_mock" to JsonPrimitive(true)))
        fn.startsWith("process") || fn.startsWith("execute") || fn.startsWith("run") ->
            JsonObject(mapOf("success" to JsonPrimitive(true), "status" to JsonPrimitive("COMPLETED"), "_mock" to JsonPrimitive(true)))
        fn.startsWith("create") || fn.startsWith("save") || fn.startsWith("add") ->
            JsonObject(mapOf("id" to JsonPrimitive("mock-id-001"), "success" to JsonPrimitive(true), "_mock" to JsonPrimitive(true)))
        fn.startsWith("convert") ->
            buildConversionMock(params)
        else -> mockObject(params)
    }

    private fun mockObject(params: List<ParamMetadata>): JsonElement {
        val fields = mutableMapOf<String, JsonElement>()
        for (param in params) {
            fields[param.name] = mockValueForParam(param)
        }
        fields["_mock"] = JsonPrimitive(true)
        return JsonObject(fields)
    }

    private fun buildConversionMock(params: List<ParamMetadata>): JsonElement {
        val fields = mutableMapOf<String, JsonElement>()
        for (param in params) {
            fields[param.name] = mockValueForParam(param)
        }
        fields["convertedAmount"] = JsonPrimitive(11.97)
        fields["rate"] = JsonPrimitive(0.01197)
        fields["_mock"] = JsonPrimitive(true)
        return JsonObject(fields)
    }

    private fun mockStringFromFn(fn: String): String = when {
        fn.contains("id", ignoreCase = true) -> "mock-id-123"
        fn.contains("name", ignoreCase = true) -> "Mock Name"
        fn.contains("token", ignoreCase = true) -> "mock_token_abc"
        fn.contains("url", ignoreCase = true) -> "https://mock.example.com"
        fn.contains("email", ignoreCase = true) -> "mock@example.com"
        fn.contains("status", ignoreCase = true) -> "SUCCESS"
        fn.contains("currency", ignoreCase = true) -> "INR"
        else -> "mock_${fn}_result"
    }

    private fun mockValueForParam(param: ParamMetadata): JsonElement = when {
        param.type == "Double" || param.type == "Float" -> JsonPrimitive(1.0)
        param.type == "Int" || param.type == "Long" -> JsonPrimitive(1)
        param.type == "Boolean" -> JsonPrimitive(true)
        param.name.contains("amount", ignoreCase = true) -> JsonPrimitive(1000.0)
        param.name.contains("id", ignoreCase = true) -> JsonPrimitive("mock-id")
        param.name.contains("currency", ignoreCase = true) -> JsonPrimitive("INR")
        param.name.contains("limit", ignoreCase = true) -> JsonPrimitive(10)
        else -> JsonPrimitive("mock_${param.name}")
    }
}
