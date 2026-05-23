#pragma once

namespace devhud {
    struct MemInfo {
        long vmRssKb;
        long vmSizeKb;
        long vmPeakKb;
        int  threadCount;
    };

    MemInfo getMemInfo();
}
