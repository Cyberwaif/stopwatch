LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := Stopwatch
LOCAL_SRC_FILES := $(call all-java-files-under, app/src/main/java)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/app/src/main/res
LOCAL_MANIFEST_FILE := app/src/main/AndroidManifest.xml
LOCAL_SDK_VERSION := current
LOCAL_AAPT_FLAGS := --auto-add-overlay

include $(BUILD_PACKAGE)