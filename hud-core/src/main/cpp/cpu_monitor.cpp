#include "cpu_monitor.h"
#include "proc_reader.h"
#include <fstream>
#include <sstream>
#include <unistd.h>
#include <time.h>

namespace devhud {

static long prevProcTicks           = 0;
static long prevTotalCpu            = 0;
static long prevIdleCpu             = 0;
static struct timespec prevWallTime = {0, 0};

static long readProcCpuTicks() {
    std::string stat = readProcFile("/proc/self/stat");
    if (stat.empty()) return 0;

    std::istringstream ss(stat);
    std::string token;
    long utime = 0, stime = 0;
    for (int i = 0; i < 15; i++) {
        ss >> token;
        if (i == 13) utime = std::stol(token);
        if (i == 14) stime = std::stol(token);
    }
    return utime + stime;
}

struct SystemCpuTicks { long total; long idle; };

static SystemCpuTicks readSystemCpuTicks() {
    std::ifstream file("/proc/stat");
    std::string line;
    std::getline(file, line);

    long user, nice, system, idle, iowait, irq, softirq;
    sscanf(line.c_str(), "cpu %ld %ld %ld %ld %ld %ld %ld",
           &user, &nice, &system, &idle, &iowait, &irq, &softirq);

    long total = user + nice + system + idle + iowait + irq + softirq;
    return {total, idle};
}

void initCpuSampling() {
    prevProcTicks = readProcCpuTicks();
    auto sys      = readSystemCpuTicks();
    prevTotalCpu  = sys.total;
    prevIdleCpu   = sys.idle;
    clock_gettime(CLOCK_MONOTONIC, &prevWallTime);
}

CpuInfo getCpuInfo() {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);

    long curProcTicks = readProcCpuTicks();
    auto curSys       = readSystemCpuTicks();

    long deltaProcTicks = curProcTicks - prevProcTicks;
    long deltaTotalCpu  = curSys.total - prevTotalCpu;
    long deltaIdleCpu   = curSys.idle  - prevIdleCpu;
    long wallMs = (now.tv_sec  - prevWallTime.tv_sec)  * 1000L
                + (now.tv_nsec - prevWallTime.tv_nsec) / 1000000L;

    CpuInfo info = {0.0f, 0.0f};

    if (deltaTotalCpu > 0) {
        // System-wide busy % — /proc/stat aggregate is reliable for this ratio
        info.systemPercent = 100.0f * (1.0f - (float)deltaIdleCpu / deltaTotalCpu);
    }

    if (wallMs > 0) {
        // Use wall clock as denominator to avoid CONFIG_NO_HZ idle-undercount on Android.
        // sysconf(_SC_CLK_TCK) gives kernel HZ (typically 100 on Android).
        // expectedTicks = how many jiffies one core would tick in wallMs.
        long hz = sysconf(_SC_CLK_TCK);
        if (hz < 1) hz = 100;
        float expectedPerCoreTicks = (float)(wallMs * hz) / 1000.0f;
        info.usagePercent = 100.0f * (float)deltaProcTicks / expectedPerCoreTicks;
    }

    prevProcTicks = curProcTicks;
    prevTotalCpu  = curSys.total;
    prevIdleCpu   = curSys.idle;
    prevWallTime  = now;

    return info;
}

} // namespace devhud
