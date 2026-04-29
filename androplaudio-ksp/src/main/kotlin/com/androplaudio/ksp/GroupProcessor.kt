package com.androplaudio.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import kotlinx.serialization.json.Json
import java.io.File

public class GroupProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        GroupProcessor(environment.codeGenerator, environment.logger, environment.options)
}

internal class GroupProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {

    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()
        invoked = true

        val groupsJsonPath = options["androplaudio.groupsJson"] ?: run {
            logger.warn("androplaudio.groupsJson not set in KSP options — skipping generation")
            return emptyList()
        }

        val groupsFile = File(groupsJsonPath)
        if (!groupsFile.exists()) {
            logger.warn("groups.json not found at $groupsJsonPath — skipping generation")
            return emptyList()
        }

        val config = try {
            Json { ignoreUnknownKeys = true }.decodeFromString<GroupsConfig>(groupsFile.readText())
        } catch (e: Exception) {
            logger.error("Failed to parse groups.json: ${e.message}")
            return emptyList()
        }

        logger.info("androplaudio-ksp: generating registries for ${config.groups.size} groups")

        val groupRegistryNames = mutableListOf<String>()

        for (group in config.groups) {
            val registryName = groupIdToRegistryName(group.id)
            groupRegistryNames.add(registryName)

            val ksClass = resolver.getClassDeclarationByName(
                resolver.getKSNameFromString(group.`class`)
            )

            if (ksClass != null) {
                generateRegistryFromKSClass(ksClass, registryName)
            } else {
                generateReflectionOnlyRegistry(group, registryName)
            }
        }

        generateMasterRegistry(groupRegistryNames, config.groups)

        return emptyList()
    }

    private fun generateRegistryFromKSClass(
        ksClass: KSClassDeclaration,
        registryName: String,
    ) {
        val methods = ksClass.getAllFunctions()
            .filter { fn ->
                !LifecycleFilter.shouldExclude(fn.simpleName.asString()) &&
                    fn.isEffectivelyPublic()
            }
            .toList()

        val toolMetadataEntries = buildToolMetadataList(methods)
        val callWhenBranches = buildCallWhenBranches(methods)

        val content = """
package com.androplaudio.generated

import com.androplaudio.core.ToolMetadata
import com.androplaudio.core.ParamMetadata

object $registryName {

    val tools: List<ToolMetadata> = $toolMetadataEntries

    fun call(fn: String, args: Map<String, Any?>, instance: Any): Any? {
        return when (fn) {
            $callWhenBranches
            else -> throw IllegalArgumentException("Unknown function: ${'$'}fn in $registryName")
        }
    }
}
        """.trimIndent()

        writeFile("com.androplaudio.generated", registryName, content)
    }

    private fun generateReflectionOnlyRegistry(group: GroupEntry, registryName: String) {
        val fqcn = group.`class`
        val content = """
package com.androplaudio.generated

import com.androplaudio.core.ToolMetadata
import com.androplaudio.core.ParamMetadata

object $registryName {

    private val EXCLUDED = setOf(
        "onCreate", "onDestroy", "onStart", "onStop", "onPause", "onResume",
        "onBind", "onUnbind", "onStartCommand", "onRebind", "onReceive",
        "doWork", "onStopped", "getForegroundInfo",
        "equals", "hashCode", "toString", "copy",
        "query", "insert", "delete", "update", "getType", "openFile",
    )

    val tools: List<ToolMetadata> by lazy {
        try {
            Class.forName("$fqcn").declaredMethods
                .filter { it.name !in EXCLUDED }
                .filter { !it.name.startsWith("access${'$'}") }
                .filter { !it.name.startsWith("component") }
                .map { method ->
                    ToolMetadata(
                        name = method.name,
                        params = method.parameters.map { p ->
                            ParamMetadata(p.name ?: "arg", p.type.simpleName)
                        },
                        returnType = method.returnType.simpleName,
                    )
                }
        } catch (_: ClassNotFoundException) {
            emptyList()
        }
    }

    fun call(fn: String, args: Map<String, Any?>, instance: Any): Any? {
        val method = instance::class.java.declaredMethods.firstOrNull { it.name == fn }
            ?: throw IllegalArgumentException("Unknown function: ${'$'}fn in $registryName")
        return method.invoke(instance, *args.values.toTypedArray())
    }
}
        """.trimIndent()

        writeFile("com.androplaudio.generated", registryName, content)
    }

    private fun generateMasterRegistry(registryNames: List<String>, groups: List<GroupEntry>) {
        val registrations = groups.zip(registryNames).joinToString("\n        ") { (group, reg) ->
            """registry.register("${group.id}", "${group.layer}", "${group.`class`}", $reg.tools) { fn, args, inst -> $reg.call(fn, args, inst) }"""
        }

        val content = """
package com.androplaudio.generated

import com.androplaudio.core.GroupRegistry

object GeneratedGroupRegistry {

    fun registerAll(registry: GroupRegistry) {
        $registrations
    }
}
        """.trimIndent()

        writeFile("com.androplaudio.generated", "GeneratedGroupRegistry", content)
    }

    private fun buildToolMetadataList(methods: List<KSFunctionDeclaration>): String {
        if (methods.isEmpty()) return "emptyList()"
        val entries = methods.joinToString(",\n        ") { fn ->
            val fnName = fn.simpleName.asString()
            val params = fn.parameters
                .filter { p -> p.type.resolve().declaration.qualifiedName?.asString() != "kotlin.coroutines.Continuation" }
                .joinToString(", ") { p ->
                    val pName = p.name?.asString() ?: "arg"
                    val pType = p.type.resolve().declaration.simpleName.asString()
                    """ParamMetadata("$pName", "$pType")"""
                }
            val returnType = fn.returnType?.resolve()?.declaration?.simpleName?.asString() ?: "Any"
            """ToolMetadata("$fnName", listOf($params), "$returnType")"""
        }
        return "listOf(\n        $entries\n    )"
    }

    private fun buildCallWhenBranches(methods: List<KSFunctionDeclaration>): String {
        return methods.joinToString("\n            ") { fn ->
            val fnName = fn.simpleName.asString()
            val isSuspend = Modifier.SUSPEND in fn.modifiers
            val valueParams = fn.parameters.filter { p ->
                p.type.resolve().declaration.qualifiedName?.asString() != "kotlin.coroutines.Continuation"
            }
            val paramTypeClasses = valueParams.joinToString(", ") { p ->
                val fqType = p.type.resolve().declaration.qualifiedName?.asString() ?: "kotlin.Any"
                "${fqType}::class.java"
            }
            val argCasts = valueParams.joinToString(", ") { p ->
                val pName = p.name?.asString() ?: "arg"
                val fqType = p.type.resolve().declaration.qualifiedName?.asString() ?: "kotlin.Any"
                """args["$pName"] as ${kotlinTypeToCast(fqType)}"""
            }

            if (isSuspend) {
                """
"$fnName" -> {
                val method = instance::class.java.getMethod("$fnName", $paramTypeClasses, kotlin.coroutines.Continuation::class.java)
                kotlinx.coroutines.runBlocking {
                    @Suppress("UNCHECKED_CAST")
                    kotlinx.coroutines.suspendCoroutine<Any?> { cont ->
                        method.invoke(instance, $argCasts, cont)
                    }
                }
            }""".trimIndent()
            } else {
                """
"$fnName" -> {
                val method = instance::class.java.getMethod("$fnName"${if (paramTypeClasses.isNotEmpty()) ", $paramTypeClasses" else ""})
                method.invoke(instance${if (argCasts.isNotEmpty()) ", $argCasts" else ""})
            }""".trimIndent()
            }
        }
    }

    private fun writeFile(packageName: String, fileName: String, content: String) {
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false),
            packageName = packageName,
            fileName = fileName,
        )
        file.bufferedWriter().use { it.write(content) }
    }

    private fun groupIdToRegistryName(groupId: String): String {
        val parts = groupId.split('.', '-')
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
        return "Generated_${parts}Registry"
    }

    private fun KSFunctionDeclaration.isEffectivelyPublic(): Boolean =
        Modifier.PUBLIC in modifiers ||
            (Modifier.PRIVATE !in modifiers && Modifier.PROTECTED !in modifiers && Modifier.INTERNAL !in modifiers) ||
            Modifier.INTERNAL in modifiers

    private fun kotlinTypeToCast(fqType: String): String = when (fqType) {
        "kotlin.Int" -> "Int"
        "kotlin.Long" -> "Long"
        "kotlin.Double" -> "Double"
        "kotlin.Float" -> "Float"
        "kotlin.Boolean" -> "Boolean"
        "kotlin.String" -> "String"
        "kotlin.collections.List" -> "List<*>"
        "kotlin.collections.Map" -> "Map<*, *>"
        else -> "Any?"
    }
}
