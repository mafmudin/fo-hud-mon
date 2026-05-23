#include "net_monitor.h"
#include <fstream>
#include <sstream>
#include <string>

namespace devhud {

static long prevRxBytes = 0;
static long prevTxBytes = 0;

static void readNetBytes(long& outRx, long& outTx) {
    outRx = 0; outTx = 0;
    std::ifstream file("/proc/net/dev");
    std::string line;

    std::getline(file, line);
    std::getline(file, line);

    while (std::getline(file, line)) {
        std::istringstream ss(line);
        std::string iface;
        ss >> iface;

        if (iface == "lo:") continue;

        long rx, tx, dummy;
        ss >> rx >> dummy >> dummy >> dummy >> dummy >> dummy >> dummy >> dummy >> tx;
        outRx += rx;
        outTx += tx;
    }
}

void initNetSampling() {
    readNetBytes(prevRxBytes, prevTxBytes);
}

NetInfo getNetInfo(long intervalMs) {
    long curRx, curTx;
    readNetBytes(curRx, curTx);

    float seconds = intervalMs / 1000.0f;
    NetInfo info = {
        (long)((curRx - prevRxBytes) / seconds),
        (long)((curTx - prevTxBytes) / seconds)
    };

    prevRxBytes = curRx;
    prevTxBytes = curTx;

    return info;
}

} // namespace devhud
