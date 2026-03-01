# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /path/to/sdk/tools/proguard/proguard-android.txt
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Moshi: keep classes annotated with @JsonClass (adapters are generated at compile time via KSP)
-keep @com.squareup.moshi.JsonClass class * { *; }
