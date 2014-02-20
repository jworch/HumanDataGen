#ifndef OPTIONPARSER_H_DEFINED
#define OPTIONPARSER_H_DEFINED

#include <map>
#include <vector>
#include <string>
#include <sstream>
#include <cstdio>



inline std::string buildFilename(const std::string &baseName, int id)
{
	char cfilename[1024]; //buffer for the filename
	sprintf(cfilename, baseName.c_str(), id);
	return std::string(cfilename);
}

inline std::string buildFilename(const std::string &baseName, int id1, int id2)
{
	char cfilename[1024]; //buffer for the filename
	sprintf(cfilename, baseName.c_str(), id1, id2);
	return std::string(cfilename);
}

inline std::string buildFilename(const std::string &baseName, int id0, int id1, int id2)
{
	char cfilename[1024]; //buffer for the filename
	sprintf(cfilename, baseName.c_str(), id0, id1, id2);
	return std::string(cfilename);
}


/*
class OptionParser
{
    public :
    OptionParser(int argc, char **argv);

    template <class T>
    const T getOption(const char *optName, const char *plugName = 0);

    protected :
    std::map<std::string, std::vector<std::string> > mArguments;
};

OptionParser::OptionParser(int argc, char**argv)
{
    std::vector t;
    for(int i=0;i<argc;++i)
        t.push_back(std::string(argv[i]));
}

*/

/// current restriction, flags have to be separated from the argument
class OptionParser
{
    public :
    inline OptionParser(int argc, char**argv);

    template <class T>
    inline const T getOption(const char *optName);

    protected :
    std::vector<std::string > mArgV;
};



inline OptionParser::OptionParser(int argc, char**argv)
{
    for(int i=0;i<argc;++i)
        mArgV.push_back(std::string(argv[i]));
}


template <class T>
inline const T OptionParser::getOption(const char *optName)
{
    T r;
    for(unsigned int i=0;i<mArgV.size()-1;++i)
    {
        //size_t pos = mArgV[i].find(optName);
        //if(pos != std::string::npos)
        if(mArgV[i].compare(optName) == 0)
		{
            std::stringstream ss(mArgV[i+1]);
            ss >> r;
            break;
        }
    }
    return r;
}



#endif
