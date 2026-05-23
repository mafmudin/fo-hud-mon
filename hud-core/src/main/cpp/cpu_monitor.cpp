#include "cpu_monitor.h"
#include "proc_reader.h"
#include <fstream>
#include <sstream>

namespace devhud {

static long prevProcTicks = 0;
static long prevTotalCpu  = 0;
static long prevIdleCpu   = 0;

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
}

CpuInfo getCpuInfo() {
    long curProcTicks = readProcCpuTicks();
    auto curSys       = readSystemCpuTicks();

    long deltaProcTicks = curProcTicks - prevProcTicks;
    long deltaTotalCpu  = curSys.total - prevTotalCpu;
    long deltaIdleCpu   = curSys.idle  - prevIdleCpu;

    CpuInfo info = {0.0f, 0.0f};

    if (deltaTotalCpu > 0) {
        info.systemPercent = 100.0f * (1.0f - (float)deltaIdleCpu / deltaTotalCpu);
        info.usagePercent  = 100.0f * (float)deltaProcTicks / deltaTotalCpu;
    }

    prevProcTicks = curProcTicks;
    prevTotalCpu  = curSys.total;
    prevIdleCpu   = curSys.idle;

    return info;
}

} // namespace devhud
