# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ─── Glance ActionCallbacks (referenced by class name via actionRunCallback) ──
-keep class * extends androidx.glance.appwidget.action.ActionCallback { *; }

# ─── Kotlinx Serialization ────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
# Keep serializable data classes and their generated $$serializer companions
-keep @kotlinx.serialization.Serializable class com.enderthor.kSafe.** {
    <fields>;
    *** Companion;
}
-keep class com.enderthor.kSafe.**$$serializer { *; }
-dontwarn kotlinx.serialization.**

# ─── Hammerhead karoo-ext — AIDL stubs (IPC with Karoo system) ───────────────
# Small package (~5 interfaces), safe to keep entirely
-keep class io.hammerhead.karooext.aidl.** { *; }

# ─── Hammerhead karoo-ext — base classes our code extends ────────────────────
-keep class io.hammerhead.karooext.extension.KarooExtension { *; }
-keep class io.hammerhead.karooext.extension.DataTypeImpl { *; }
-keep class io.hammerhead.karooext.KarooSystemService { *; }
-keep class io.hammerhead.karooext.internal.ViewEmitter { *; }

# ─── Hammerhead karoo-ext — models used in dispatch / addConsumer calls ───────
# Keep field names and constructors (needed for IPC serialization/reflection)
# but allow R8 to remove unused model classes
-keepnames class io.hammerhead.karooext.models.** { }
-keepclassmembers class io.hammerhead.karooext.models.** {
    <fields>;
    <init>(...);
}

# Mantener las clases de tu aplicación
-keep class com.enderthor.kCustomField.** { *; }
-keep class com.enderthor.kCustomField.datatype.** { *; }
-keep class com.enderthor.kCustomField.extensions.** { *; }
-keep class com.enderthor.kCustomField.screens.** { *; }
-keep class com.enderthor.kCustomField.theme.** { *; }

# Reglas para Timber
-dontwarn org.jetbrains.annotations.**
# R8 elimina en release los call sites de v/d/i/w (incluyendo la string interpolation)
# Solo llegan al logcat los ERROR (Log.ERROR=6 y ASSERT=7)
# No se añade -keep sobre timber.log.Timber para evitar conflicto con -assumenosideeffects
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
}

# Reglas generales para Android
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Mantener constructores necesarios para JSON/Serialización si los usas
-keepclassmembers class * {
    public <init>();
}

# Mantener enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Si usas Kotlin (el plugin de Kotlin ya incluye las reglas principales)
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Mantener interfaces de callbacks y listeners
-keepclassmembers class * {
    void onCommand(**);
    void onStateChange(**);
    void onConnectionStateChange(**);
}

# Reglas específicas para servicios en segundo plano
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver

# Glance/Compose: las librerías incluyen sus propias consumer rules en el AAR.
# Solo se protegen las clases de entrada que R8 no puede rastrear por reflexión.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { <init>(...); }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { <init>(...); }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}


# Mantener las clases específicas de tu aplicación que usas en Glance
-keep class com.enderthor.kCustomField.** {
    <init>(...);
}

# Mantener las clases de datos utilizadas en Glance
-keep class com.enderthor.kCustomField.model.** {
    <init>(...);
}

# Mantener los métodos de los composables
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Mantener los constructores de las clases utilizadas en los composables
-keepclassmembers class * {
    <init>(...);
}
