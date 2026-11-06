#!/bin/bash -e

BUILD="./buildscripts"

. $BUILD/include/path.sh
. $BUILD/include/depinfo.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf {app,.}/build app/src/main/{libs,obj}
	exit 0
else
	exit 255
fi

[ -n "$ANDROID_SIGNING_KEY" ] && BUNDLE=1

nativeprefix () {
	if [ -f $BUILD/prefix/$1/lib/libmpv.so ]; then
		echo "$(realpath "$BUILD/prefix/$1")"
	else
		echo >&2 "Warning: libmpv.so not found in native prefix for $1, support will be omitted"
	fi
}

prefix32=$(nativeprefix "armv7l")
prefix64=$(nativeprefix "arm64")
prefix_x64=$(nativeprefix "x86_64")
prefix_x86=$(nativeprefix "x86")

if [[ -z "$prefix32" && -z "$prefix64" && -z "$prefix_x64" && -z "$prefix_x86" ]]; then
	echo >&2 "Error: no mpv library detected."
	exit 1
fi

### Native parts
PREFIX32="$prefix32" PREFIX64="$prefix64" PREFIX_X64="$prefix_x64" PREFIX_X86="$prefix_x86" \
ndk-build -C app/src/main -j$cores

### Java parts
# Android's gradle plugin needs both of these to correctly strip libraries.
# We could pass them directly to Gradle but by using this file it will persist
# inside Android Studio too.
printf '%s\n' \
	"# This file is automatically written by the build scripts, and read using Gradle" \
	"ndkVersion=$v_ndk_n" "ndkRoot=$ANDROID_NDK_ROOT" >ndk.properties


if [ -n "$DONT_BUILD_RELEASE" ]; then
	./gradlew assembleDebug
else
	./gradlew assembleDebug assembleRelease
	if [ -n "$BUNDLE" ]; then
		# needs to be a separate invocation due to AGP bugs...
		./gradlew bundleRelease
	fi
fi

### Signing
if [ -n "$ANDROID_SIGNING_KEY" ]; then
	cd "app/build/outputs/apk"
	apksigner=${ANDROID_HOME}/build-tools/${v_sdk_build_tools}/apksigner
	signargs=(--ks "${ANDROID_SIGNING_KEY}")
	# allow non-interactive use (e.g. CI) by passing the password via environment
	if [ -n "$ANDROID_SIGNING_PASSWORD" ]; then
		signargs+=(--ks-pass env:ANDROID_SIGNING_PASSWORD)
	fi
	for v in default allstorage; do
		pushd $v
		# sign only the universal debug APK
		"$apksigner" sign "${signargs[@]}" \
			--in debug/app-$v-universal-debug.apk --out debug/app-$v-universal-debug-signed.apk
		# but all of the release APKs
		for apk in release/*-unsigned.apk; do
			"$apksigner" sign "${signargs[@]}" \
				--in $apk --out ${apk/-unsigned/-signed}
		done
		popd
	done
	# and the bundle
	cd ../bundle
	if [ -n "$BUNDLE" ] && [ -z "$ANDROID_SIGNING_ALIAS" ]; then
		echo >&2 "Warning: ANDROID_SIGNING_ALIAS not set, not signing the bundle"
	elif [ -n "$BUNDLE" ]; then
		jsargs=()
		if [ -n "$ANDROID_SIGNING_PASSWORD" ]; then
			jsargs+=(-storepass:env ANDROID_SIGNING_PASSWORD)
		fi
		pushd defaultRelease
		jarsigner -keystore "${ANDROID_SIGNING_KEY}" "${jsargs[@]}" -signedjar \
			app-default-release-signed.aab app-default-release.aab \
			"${ANDROID_SIGNING_ALIAS}"
		popd
	fi
fi
