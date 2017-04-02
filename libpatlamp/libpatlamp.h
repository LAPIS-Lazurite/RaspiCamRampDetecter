#ifndef _LIBLAMPDETECT_H_
#define _LIBLAMPDETECT_H_

#include <string>
#include <stdint.h>

#ifdef __cplusplus
namespace patlamp {
#endif

	int init(void);
	bool readData(std::string &result);
	void snapShot(std::string filepath);
	void setTextColor(unsigned char r, unsigned char g, unsigned char b);
	int dispImage(bool on);
	int setMapfile(std::string str);
	int setReportInterval(int sec);
	int setDetectInterval(int msec);
	int remove(void);

#ifdef __cplusplus
};
#endif

#endif
