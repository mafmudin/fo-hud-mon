#pragma once
#include <string>
#include <map>

namespace devhud {
    std::string readProcFile(const std::string& path);
    std::map<std::string, std::string> parseProcStatus();
}
