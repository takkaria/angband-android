LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := v4
LOCAL_CFLAGS := -DUSE_AND -DANGDROID_ANGBAND_PLUGIN -std=c99 \
		-DANGDROID_V4_PLUGIN

include $(LOCAL_PATH)/../../common/DocGlobalExtras.mk

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../include \
$(LOCAL_PATH)/../../curses \
$(LOCAL_PATH)/../patch \
$(LOCAL_PATH)/../extsrc/src

LOCAL_LDLIBS := -llog

LOCAL_SRC_FILES := \
../../curses/curses.c \
../../curses/wcstombs.c \
../../curses/mbstowcs.c \
../../curses/wctomb.c \
../../common/angdroid.c

include $(LOCAL_PATH)/../extsrc/src/Makefile.src

LOCAL_SRC_FILES += $(patsubst %.o,%.c, \
		$(addprefix ../extsrc/src/,$(ANGFILES) $(ZFILES)) \
	)

include $(BUILD_SHARED_LIBRARY)
