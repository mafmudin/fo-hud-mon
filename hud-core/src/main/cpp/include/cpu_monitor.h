#pragma once

namespace devhud {
    struct CpuInfo {
        float usagePercent;
        float systemPercent;
    };

    void    initCpuSampling();
    CpuInfo getCpuInfo();
}
