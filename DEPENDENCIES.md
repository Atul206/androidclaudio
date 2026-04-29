# AndroClaudio — Dependency Reference

## Releases

| File | Size | Purpose |
|---|---|---|
| `androplaudio-core-1.0.0.aar` | 81 KB | Runtime MCP server — Android debug builds |
| `androplaudio-ksp-1.0.0.jar` | 38 KB | KSP processor (generates registries at build time) |
| `AndroClaudio.xcframework` | — | Runtime MCP server — iOS / CMP debug builds |

Download files from the `releases/1.0.0/` folder.

---

## Android Integration (Local AAR/JAR)

### Step 1 — Copy files into your project

```
your-app/
└── app/
    └── libs/
        ├── androplaudio-core-1.0.0.aar
        └── androplaudio-ksp-1.0.0.jar
```

### Step 2 — `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

### Step 3 — `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.23-1.0.20"
}

ksp {
    arg("androplaudio.groupsJson", "${rootDir}/androplaudio-groups.json")
}

dependencies {
    ksp(files("libs/androplaudio-ksp-1.0.0.jar"))
    debugImplementation(files("libs/androplaudio-core-1.0.0.aar"))
}
```

### Step 4 — `Application.kt`

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin { modules(appModule) }       // your existing DI — unchanged
        if (BuildConfig.DEBUG) AndroClaudio.initialize(this)
    }
}
```

### Step 5 — Build and run

```bash
npx androplaudio-setup --project-dir . --output androplaudio-groups.json
./gradlew assembleDebug
adb forward tcp:5173 tcp:5173
```

---

## KMM / CMP Project (Android target)

```kotlin
// shared/build.gradle.kts
dependencies {
    add("kspAndroid", files("libs/androplaudio-ksp-1.0.0.jar"))
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            debugImplementation(files("libs/androplaudio-core-1.0.0.aar"))
        }
    }
}
```

---

## iOS Integration (XCFramework)

### Step 1 — Add XCFramework to Xcode

Drag `AndroClaudio.xcframework` from `releases/1.0.0/` into your Xcode project.
In **Build Phases → Link Binary With Libraries**, add it with **Embed & Sign**.

For debug-only embedding, add it under **Debug** configuration only.

### Step 2 — Generate groups JSON

```bash
npx androplaudio-setup --project-dir . --output androplaudio-groups.json
```

### Step 3 — `AppDelegate.swift`

The KSP-generated registry is compiled into your app module. Pass it to `initialize`:

```swift
import AndroClaudio

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        #if DEBUG
        AndroClaudio.shared.initialize(port: 5173) { registry in
            GeneratedGroupRegistry.shared.registerAll(registry: registry)
        }
        #endif
        return true
    }
}
```

### Step 4 — Forward port and verify

```bash
iproxy 5173 5173 &          # TCP tunnel via usbmuxd
curl http://localhost:5173/tools/list | jq .
```

---

## CMP (Compose Multiplatform) Project

For a CMP project targeting both Android and iOS, wire both entry points:

**Android** (`androidMain`):
```kotlin
// In your Android Application class or platform entry point
if (BuildConfig.DEBUG) AndroClaudio.initialize(application)
```

**iOS** (`iosMain`):
```kotlin
// Called from AppDelegate via Kotlin/Native interop
AndroClaudio.initialize(port = 5173) { registry ->
    GeneratedGroupRegistry.registerAll(registry)
}
```

---

## Public API

Everything else is `internal` — not accessible to consumers.

```kotlin
// Android entry point (androidMain)
object AndroClaudio {
    fun initialize(app: Application, port: Int = 5173)
    fun registerInstance(fqcn: String, instance: Any)  // Hilt / Dagger / manual DI
    fun stop()
}

// iOS / CMP entry point (iosMain)
object AndroClaudio {
    fun initialize(port: Int = 5173, registerGroups: (GroupRegistry) -> Unit = {})
    fun registerInstance(fqcn: String, instance: Any)
    fun stop()
}

// Used in KSP-generated registries
data class ToolMetadata(val name: String, val params: List<ParamMetadata>, val returnType: String)
data class ParamMetadata(val name: String, val type: String)
class GroupRegistry  // registerAll(registry) called from generated code
```

---

## Build New Release

```bash
# Android AAR + KSP JAR
./gradlew :androplaudio-core:bundleDebugAar :androplaudio-ksp:jar

# iOS XCFramework
./gradlew :androplaudio-core:assembleAndroClaudioXCFramework
```

Output files:
```
androplaudio-core/build/outputs/aar/androplaudio-core-debug.aar
androplaudio-ksp/build/libs/androplaudio-ksp-1.0.0.jar
androplaudio-core/build/XCFrameworks/release/AndroClaudio.xcframework
```

Copy to `releases/1.0.0/` for distribution.
