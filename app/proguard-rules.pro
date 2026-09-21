# Rules for the shrunk release build (minifyEnabled/shrinkResources in build.gradle).
#
# R8 removes and renames everything it cannot see being used. Whatever reaches this app from the
# outside instead -- the system starting a component by the name it read from the manifest, an
# inflater building a view out of an XML file, a binder handing the Glyph over -- is invisible to
# it and has to be named here, or the release build compiles cleanly and then breaks on a phone.
#
# The same rules apply to both flavours: `fdroid`, which is built without the Nothing Glyph SDK,
# and `full`, which ships it. Rules naming the SDK simply match nothing in the `fdroid` build.


# -- Crash reports -------------------------------------------------------------------------------

# Keep the line numbers in stack traces, and hide the source file names that would otherwise be
# left in beside them. What translates an obfuscated trace back is the mapping file written to
# app/build/outputs/mapping/<flavour>Release/mapping.txt: Play takes it with the bundle, and it is
# worth keeping a copy of it beside every APK published anywhere else, since it is the only way to
# read a crash from that exact build.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile


# -- Components the system starts by name --------------------------------------------------------

# AGP already generates keep rules for everything in the merged manifest, so these repeat what it
# does. They are written out anyway because the cost of getting them wrong is invisible until an
# update ships: the launcher and the quick settings panel remember a placed widget or tile by its
# class name, so a rename would quietly drop the widgets and the tile people already have.
-keep class rocks.poopjournal.flashy.QSTileService { <init>(); }
-keep class rocks.poopjournal.flashy.widgets.* { <init>(); }
-keep class rocks.poopjournal.flashy.receivers.* { <init>(); }
-keep class rocks.poopjournal.flashy.activities.* { <init>(); }

# The settings screen is a fragment, and the fragment manager rebuilds it from its class name after
# the process is killed in the background. Keeping the no-argument constructor of every fragment
# keeps that from throwing when someone comes back to the app a day later.
-keep class * extends androidx.fragment.app.Fragment { <init>(); }


# -- Views and preferences built from XML --------------------------------------------------------

# Custom views named in res/layout and preferences named in res/xml are instantiated by name by
# LayoutInflater and PreferenceInflater. AAPT2 generates keep rules from the resources for the
# first, and androidx.preference ships consumer rules for the second, so both are covered already;
# these two keep the constructors of the views the app itself defines, which are the ones a future
# refactor could move out of a layout and into code and leave R8 free to rename.
-keepclasseswithmembers class rocks.poopjournal.flashy.** extends android.view.View {
    <init>(android.content.Context);
    <init>(android.content.Context, android.util.AttributeSet);
    <init>(android.content.Context, android.util.AttributeSet, int);
}


# -- Nothing Glyph SDK (`full` flavour only) -----------------------------------------------------

# libs/glyph-matrix-sdk-2.0.aar is proprietary and ships without consumer rules of its own, so
# nothing tells R8 what it needs. It talks to the Glyph service over AIDL and is written in Kotlin
# against a stdlib it does not bundle, which is exactly the shape that breaks under shrinking, so
# the SDK is kept whole. It is a small library and only the `full` flavour carries it.
-keep class com.nothing.** { *; }
-keep class com.nothinglondon.** { *; }
-dontwarn com.nothing.**
-dontwarn com.nothinglondon.**


# -- Desugaring ----------------------------------------------------------------------------------

# Core library desugaring pulls in classes that reference JDK APIs missing from the Android
# platform. They are never reached on a phone, and warning about them would fail the build.
-dontwarn java.lang.invoke.**
-dontwarn javax.lang.model.element.**
-dontwarn sun.misc.**