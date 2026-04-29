package com.androplaudio.core

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*

internal actual class MCPServer actual constructor(
    private val registry: GroupRegistry,
    private val diResolver: InstanceResolver?,
    private val modeResolver: ModeResolver,
    private val port: Int,
) {
    private var engine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    actual fun start() {
        engine = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
            }
            routing { configureRoutes() }
        }.start(wait = false)
    }

    actual fun stop() {
        engine?.stop(1000, 5000)
        engine = null
    }

    private fun Routing.configureRoutes() {

        get("/") {
            call.respondText(
                """{"server":"androplaudio","version":"1.0","port":$port}""",
                ContentType.Application.Json,
            )
        }

        get("/tools/list") {
            val groupId = call.request.queryParameters["group"]
            if (groupId == null) {
                call.respond(registry.allDescriptors())
            } else {
                val detail = registry.getDetail(groupId)
                if (detail == null) {
                    call.respond(HttpStatusCode.NotFound, CallResponse(error = "Group not found: $groupId"))
                } else {
                    call.respond(detail)
                }
            }
        }

        post("/tools/call") {
            val request = try {
                call.receive<CallRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, CallResponse(error = "Invalid request: ${e.message}"))
                return@post
            }

            val group = registry.getGroup(request.group)
            if (group == null) {
                call.respond(HttpStatusCode.NotFound, CallResponse(error = "Group not found: ${request.group}"))
                return@post
            }

            val tool = group.tools.find { it.name == request.fn }
            if (tool == null) {
                call.respond(HttpStatusCode.NotFound, CallResponse(error = "Function not found: ${request.fn} in group ${request.group}"))
                return@post
            }

            if (!modeResolver.isLive(request.group)) {
                val mockResult = MockResponseGenerator.generate(request.fn, tool.params, tool.returnType)
                call.respond(CallResponse(result = mockResult, mock = true))
                return@post
            }

            val instance = diResolver?.resolve(group.className)
            if (instance == null) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CallResponse(error = "Could not resolve instance of ${group.className}"),
                )
                return@post
            }

            val rawArgs = request.args.mapValues { (_, v) -> jsonElementToAny(v) }

            val result = try {
                group.callHandler(request.fn, rawArgs, instance)
            } catch (e: Exception) {
                call.respond(CallResponse(error = e.message ?: "Unknown error"))
                return@post
            }

            call.respond(CallResponse(result = anyToJsonElement(result)))
        }
    }

    private fun jsonElementToAny(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.content == "true" || element.content == "false" -> element.content.toBoolean()
            element.content.contains('.') -> element.content.toDoubleOrNull() ?: element.content
            else -> element.content.toIntOrNull() ?: element.content.toLongOrNull() ?: element.content
        }
        is JsonArray -> element.map { jsonElementToAny(it) }
        is JsonObject -> element.mapValues { (_, v) -> jsonElementToAny(v) }
    }

    private fun anyToJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is List<*> -> JsonArray(value.map { anyToJsonElement(it) })
        is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to anyToJsonElement(v) })
        else -> JsonPrimitive(value.toString())
    }
}
