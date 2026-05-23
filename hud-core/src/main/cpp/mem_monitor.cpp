#include "mem_monitor.h"
#include "proc_reader.h"
#include <string>

namespace devhud {

MemInfo getMemInfo() {
    MemInfo info = {0, 0, 0, 0};
    auto status = parseProcStatus();

    auto parseKb = [&](const std::string& key) -> long {
        auto it = status.find(key);
        if (it == status.end()) return 0L;
        return std::stol(it->second);
    };

    info.vmRssKb  = parseKb("VmRSS");
    info.vmSizeKb = parseKb("VmSize");
    info.vmPeakKb = parseKb("VmPeak");

    auto threadIt = status.find("Threads");
    if (threadIt != status.end())
        info.threadCount = std::stoi(threadIt->second);

    return info;
}

} // namespace devhud
