#pragma once

namespace devhud {
    struct NetInfo {
        long rxBytesPerSec;
        long txBytesPerSec;
    };

    void    initNetSampling();
    NetInfo getNetInfo(long intervalMs);
}
