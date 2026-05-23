#include <jni.h>
#include "cpu_monitor.h"
#include "mem_monitor.h"
#include "net_monitor.h"

extern "C" {

JNIEXPORT void JNICALL
Java_dev_fordewe_fohudmon_MetricCollector_nativeInit(JNIEnv*, jobject) {
    devhud::initCpuSampling();
    devhud::initNetSampling();
}

// Returns: [vmRssKb, vmSizeKb, vmPeakKb, threadCount]
JNIEXPORT jlongArray JNICALL
Java_dev_fordewe_fohudmon_MetricCollector_nativeGetMemInfo(JNIEnv* env, jobject) {
    devhud::MemInfo info = devhud::getMemInfo();
    jlong values[] = {info.vmRssKb, info.vmSizeKb, info.vmPeakKb, info.threadCount};
    jlongArray result = env->NewLongArray(4);
    env->SetLongArrayRegion(result, 0, 4, values);
    return result;
}

// Returns: [usagePercent * 100, systemPercent * 100] as int
JNIEXPORT jintArray JNICALL
Java_dev_fordewe_fohudmon_MetricCollector_nativeGetCpuInfo(JNIEnv* env, jobject) {
    devhud::CpuInfo info = devhud::getCpuInfo();
    jint values[] = {
        (jint)(info.usagePercent * 100),
        (jint)(info.systemPercent * 100)
    };
    jintArray result = env->NewIntArray(2);
    env->SetIntArrayRegion(result, 0, 2, values);
    return result;
}

// Returns: [rxBytesPerSec, txBytesPerSec]
JNIEXPORT jlongArray JNICALL
Java_dev_fordewe_fohudmon_MetricCollector_nativeGetNetInfo(JNIEnv* env, jobject, jlong intervalMs) {
    devhud::NetInfo info = devhud::getNetInfo(intervalMs);
    jlong values[] = {info.rxBytesPerSec, info.txBytesPerSec};
    jlongArray result = env->NewLongArray(2);
    env->SetLongArrayRegion(result, 0, 2, values);
    return result;
}

} // extern "C"
